/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2020 SonarSource SA
 * sonarlint@sonarsource.com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonarlint.eclipse.ui.internal.binding.wizard.project;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.IPageChangedListener;
import org.eclipse.jface.dialogs.PageChangedEvent;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.TriggerType;
import org.sonarlint.eclipse.core.internal.engine.connected.ConnectedEngineFacade;
import org.sonarlint.eclipse.core.internal.jobs.ProjectStorageUpdateJob;
import org.sonarlint.eclipse.core.internal.resources.SonarLintProjectConfiguration;
import org.sonarlint.eclipse.core.internal.resources.SonarLintProjectConfiguration.EclipseProjectBinding;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;
import org.sonarlint.eclipse.ui.internal.SonarLintUiPlugin;
import org.sonarlint.eclipse.ui.internal.binding.actions.JobUtils;
import org.sonarlint.eclipse.ui.internal.binding.wizard.connection.ServerConnectionWizard;
import org.sonarlint.eclipse.ui.internal.util.wizard.ParentAwareWizard;
import org.sonarlint.eclipse.ui.internal.util.wizard.WizardDialogWithoutHelp;
import org.sonarsource.sonarlint.core.client.api.connected.ConnectedSonarLintEngine.State;
import org.sonarsource.sonarlint.core.client.api.connected.RemoteProject;
import org.sonarsource.sonarlint.core.client.api.util.TextSearchIndex;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toCollection;
import static org.sonarlint.eclipse.core.internal.utils.StringUtils.isEmpty;

public class ProjectBindingWizard extends ParentAwareWizard implements INewWizard, IPageChangedListener {

  private static final String STORE_LAST_SELECTED_SERVER_ID = "ProjectBindingWizard.last_selected_server";

  private final ProjectBindingModel model;
  private final ConnectionSelectionWizardPage serverSelectionWizardPage;
  private final RemoteProjectSelectionWizardPage remoteProjectSelectionWizardPage;
  private final ProjectsSelectionWizardPage projectsSelectionWizardPage;

  private ProjectBindingWizard(String title, ProjectBindingModel model) {
    super();
    this.model = model;
    setNeedsProgressMonitor(true);
    setWindowTitle(title);
    setHelpAvailable(false);
    projectsSelectionWizardPage = new ProjectsSelectionWizardPage(model);
    setDialogSettings(SonarLintUiPlugin.getDefault().getDialogSettings());
    serverSelectionWizardPage = new ConnectionSelectionWizardPage(model);
    remoteProjectSelectionWizardPage = new RemoteProjectSelectionWizardPage(model);
  }

  private ProjectBindingWizard(Collection<ISonarLintProject> selectedProjects, @Nullable ConnectedEngineFacade selectedServer) {
    this("Bind to a SonarQube or SonarCloud project", new ProjectBindingModel());
    this.model.setProjects(selectedProjects.stream()
      .sorted(comparing(ISonarLintProject::getName))
      .collect(toCollection(ArrayList::new)));
    if (selectedServer != null) {
      this.model.setServer(selectedServer);
      this.model.setSkipServer(true);
    } else if (SonarLintCorePlugin.getServersManager().getServers().size() == 1) {
      // Only one server configured, pre-select it
      this.model.setServer((ConnectedEngineFacade) SonarLintCorePlugin.getServersManager().getServers().get(0));
    } else {
      String lastSelectedServer = this.getDialogSettings().get(STORE_LAST_SELECTED_SERVER_ID);
      if (lastSelectedServer != null) {
        SonarLintCorePlugin.getServersManager().findById(lastSelectedServer)
          .ifPresent(s -> this.model.setServer((ConnectedEngineFacade) s));
      }
    }
    Set<String> projectKeys = selectedProjects.stream()
      .map(SonarLintCorePlugin::loadConfig)
      .map(SonarLintProjectConfiguration::getProjectBinding)
      .filter(Optional<EclipseProjectBinding>::isPresent)
      .map(Optional<EclipseProjectBinding>::get)
      .map(EclipseProjectBinding::projectKey)
      .collect(Collectors.toSet());
    if (projectKeys.size() == 1) {
      // If all projects are bound to the same remote project, then use it as default
      model.setRemoteProjectKey(projectKeys.iterator().next());
    }
  }

  public static WizardDialog createDialogSkipServerSelection(Shell activeShell, Collection<ISonarLintProject> selectedProjects, ConnectedEngineFacade selectedServer) {
    return new WizardDialogWithoutHelp(activeShell, new ProjectBindingWizard(selectedProjects, selectedServer));
  }

  public static WizardDialog createDialog(Shell activeShell, Collection<ISonarLintProject> selectedProjects) {
    if (SonarLintCorePlugin.getServersManager().getServers().isEmpty()) {
      return ServerConnectionWizard.createDialog(activeShell);
    }
    return new WizardDialogWithoutHelp(activeShell, new ProjectBindingWizard(selectedProjects, null));
  }

  @Override
  public void init(IWorkbench workbench, IStructuredSelection selection) {
    // Nothing to do
  }

  @Override
  public IWizardPage getStartingPage() {
    if (model.getEclipseProjects().isEmpty()) {
      return projectsSelectionWizardPage;
    }
    if (!model.isSkipServerSelection()) {
      return serverSelectionWizardPage;
    }
    return remoteProjectSelectionWizardPage;
  }

  @Override
  public void addPages() {
    addPage(projectsSelectionWizardPage);
    addPage(serverSelectionWizardPage);
    addPage(remoteProjectSelectionWizardPage);
  }

  @Nullable
  @Override
  public IWizardPage getNextPage(IWizardPage page) {
    if (page == projectsSelectionWizardPage) {
      return model.isSkipServerSelection() ? remoteProjectSelectionWizardPage : serverSelectionWizardPage;
    }
    if (page == serverSelectionWizardPage) {
      return remoteProjectSelectionWizardPage;
    }
    return null;
  }

  @Nullable
  @Override
  public IWizardPage getPreviousPage(IWizardPage page) {
    // This method is only used for the first page of a wizard,
    // because every following page remember the previous one on its own
    if (page == serverSelectionWizardPage) {
      return projectsSelectionWizardPage;
    }
    return null;
  }

  @Override
  public boolean canFinish() {
    IWizardPage currentPage = getContainer().getCurrentPage();
    return currentPage == remoteProjectSelectionWizardPage && super.canFinish();
  }

  @Override
  public boolean performFinish() {
    String serverId = model.getServer().getId();
    getDialogSettings().put(STORE_LAST_SELECTED_SERVER_ID, serverId);
    String projectKey = model.getRemoteProjectKey();
    ProjectStorageUpdateJob job = new ProjectStorageUpdateJob(serverId, projectKey);
    model.getEclipseProjects().forEach(p -> {
      boolean changed = false;
      SonarLintProjectConfiguration projectConfig = SonarLintCorePlugin.loadConfig(p);
      String oldServerId = projectConfig.getProjectBinding().map(EclipseProjectBinding::connectionId).orElse(null);
      String oldProjectKey = projectConfig.getProjectBinding().map(EclipseProjectBinding::projectKey).orElse(null);
      if (!Objects.equals(serverId, oldServerId) || !Objects.equals(projectKey, oldProjectKey)) {
        projectConfig.setProjectBinding(new EclipseProjectBinding(serverId, projectKey, "", ""));
        changed = true;
      }
      if (changed) {
        SonarLintUiPlugin.unsubscribeToNotifications(p);
        SonarLintCorePlugin.saveConfig(p, projectConfig);
        p.deleteAllMarkers(SonarLintCorePlugin.MARKER_ON_THE_FLY_ID);
        p.deleteAllMarkers(SonarLintCorePlugin.MARKER_REPORT_ID);
        SonarLintCorePlugin.clearIssueTracker(p);
        JobUtils.notifyServerViewAfterBindingChange(p, oldServerId);
        SonarLintUiPlugin.subscribeToNotifications(p);
      }
    });
    JobUtils.scheduleAnalysisOfOpenFiles(job, model.getEclipseProjects(), TriggerType.BINDING_CHANGE);
    job.schedule();
    return true;
  }

  @Override
  public void pageChanged(PageChangedEvent event) {
    if (event.getSelectedPage() == remoteProjectSelectionWizardPage) {
      Display.getDefault().asyncExec(() -> {
        boolean success = true;
        boolean fetchProjectList = false;
        if (model.getServer().getStorageState() == State.UPDATING) {
          success = waitForServerUpdate(remoteProjectSelectionWizardPage);
        } else if (model.getServer().getStorageState() != State.UPDATED) {
          success = tryUpdateServerStorage(remoteProjectSelectionWizardPage);
        } else {
          fetchProjectList = true;
        }
        success = tryLoadProjectList(remoteProjectSelectionWizardPage, fetchProjectList);
        if (success && isEmpty(model.getRemoteProjectKey())) {
          tryAutoBind();
        }
      });
    }
  }

  private void tryAutoBind() {
    TextSearchIndex<RemoteProject> index = model.getProjectIndex();
    RemoteProject bestCandidate = null;
    for (ISonarLintProject project : model.getEclipseProjects()) {
      Map<RemoteProject, Double> results = index.search(project.getName());
      if (results.isEmpty()) {
        continue;
      }
      List<Map.Entry<RemoteProject, Double>> entries = new ArrayList<>(results.entrySet());
      entries.sort(
        Comparator.comparing(Map.Entry<RemoteProject, Double>::getValue).reversed()
          .thenComparing(Comparator.comparing(e -> e.getKey().getName(), String.CASE_INSENSITIVE_ORDER)));
      if (bestCandidate == null) {
        bestCandidate = entries.get(0).getKey();
      } else if (!entries.get(0).getKey().equals(bestCandidate)) {
        // Multiple best candidates, give up
        return;
      }
    }
    if (bestCandidate != null) {
      model.setRemoteProjectKey(bestCandidate.getKey());
    }

  }

  private boolean waitForServerUpdate(WizardPage currentPage) {
    currentPage.setMessage(null);
    try {
      getContainer().run(true, true, new IRunnableWithProgress() {

        @Override
        public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
          monitor.beginTask("Waiting for background server storage update task to complete", IProgressMonitor.UNKNOWN);
          try {
            while (model.getServer().getStorageState() == State.UPDATING) {
              if (monitor.isCanceled()) {
                throw new InterruptedException("Cancelled");
              }
              Thread.sleep(500);
            }
          } finally {
            monitor.done();
          }
        }
      });
    } catch (InvocationTargetException e) {
      SonarLintLogger.get().debug("Error wating for server storage update to complete", e.getCause());
      currentPage.setMessage(e.getCause().getMessage(), IMessageProvider.ERROR);
      return false;
    } catch (InterruptedException e) {
      return false;
    }
    return true;
  }

  private boolean tryUpdateServerStorage(WizardPage currentPage) {
    currentPage.setMessage(null);
    try {
      getContainer().run(true, true, new IRunnableWithProgress() {

        @Override
        public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
          monitor.beginTask("Update SonarLint storage for the server", IProgressMonitor.UNKNOWN);
          try {
            model.getServer().updateStorage(monitor);
          } finally {
            monitor.done();
          }
        }
      });
    } catch (InvocationTargetException e) {
      SonarLintLogger.get().debug("Unable to update the storage for the server", e.getCause());
      currentPage.setMessage(e.getCause().getMessage(), IMessageProvider.ERROR);
      return false;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return false;
    }
    return true;
  }

  private boolean tryLoadProjectList(WizardPage currentPage, boolean fetchProjectList) {
    currentPage.setMessage(null);
    try {
      getContainer().run(true, true, new IRunnableWithProgress() {

        @Override
        public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
          try {
            if (fetchProjectList) {
              model.getServer().updateProjectList(monitor);
            }
            model.setProjectIndex(model.getServer().computeProjectIndex());
          } finally {
            monitor.done();
          }
        }
      });
    } catch (InvocationTargetException e) {
      SonarLintLogger.get().debug("Unable to fetch project list", e.getCause());
      currentPage.setMessage(e.getCause().getMessage(), IMessageProvider.ERROR);
      return false;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return false;
    }
    return true;
  }
}
