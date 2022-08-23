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
package org.sonarlint.eclipse.ui.internal.binding;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IDecoratorManager;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionContext;
import org.eclipse.ui.contexts.IContextService;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.navigator.CommonNavigator;
import org.eclipse.ui.navigator.CommonViewer;
import org.eclipse.ui.navigator.CommonViewerSiteFactory;
import org.eclipse.ui.navigator.ICommonViewerSite;
import org.eclipse.ui.navigator.NavigatorActionService;
import org.eclipse.ui.part.PageBook;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.engine.connected.IConnectedEngineFacade;
import org.sonarlint.eclipse.core.internal.engine.connected.IConnectedEngineFacadeLifecycleListener;
import org.sonarlint.eclipse.core.internal.engine.connected.IConnectedEngineFacadeListener;
import org.sonarlint.eclipse.ui.internal.Messages;
import org.sonarlint.eclipse.ui.internal.SonarLintUiPlugin;
import org.sonarlint.eclipse.ui.internal.binding.wizard.connection.ServerConnectionWizard;

/**
 * A view of connections, their modules, and status.
 */
public class BindingsView extends CommonNavigator {
  public static final String ID = SonarLintUiPlugin.PLUGIN_ID + ".ServersView";

  private static final String SERVERS_VIEW_CONTEXT = "org.eclipse.ui.sonarlintServerViewScope";

  protected CommonViewer tableViewer;
  private Control mainPage;
  private Control noServersPage;
  private PageBook book;

  private IConnectedEngineFacadeLifecycleListener serverResourceListener;
  private IConnectedEngineFacadeListener serverListener;

  public BindingsView() {
    super();
  }

  @Override
  public void createPartControl(Composite parent) {
    // Add PageBook as parent composite
    FormToolkit toolkit = new FormToolkit(parent.getDisplay());
    book = new PageBook(parent, SWT.NONE);
    super.createPartControl(book);
    // Main page for the Servers tableViewer
    mainPage = getCommonViewer().getControl();
    // Page prompting to define a new server
    noServersPage = createDefaultPage(toolkit);
    book.showPage(mainPage);

    IContextService contextSupport = (IContextService) getSite().getService(IContextService.class);
    contextSupport.activateContext(SERVERS_VIEW_CONTEXT);
    deferInitialization();
  }

  /**
   * Creates a page displayed when there are no servers defined.
   * 
   * @param kit
   * @return Control
   */
  private Control createDefaultPage(FormToolkit kit) {
    Form form = kit.createForm(book);
    Composite body = form.getBody();
    GridLayout layout = new GridLayout(2, false);
    body.setLayout(layout);

    Link hlink = new Link(body, SWT.NONE);
    hlink.setText(Messages.ServersView_noServers);
    hlink.setBackground(book.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
    GridData gd = new GridData(SWT.LEFT, SWT.FILL, true, false);
    hlink.setLayoutData(gd);
    hlink.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        WizardDialog wd = ServerConnectionWizard.createDialog(book.getShell());
        if (wd.open() == Window.OK) {
          toggleDefaultPage();
        }
      }
    });

    // Create the context menu for the default page
    final CommonViewer commonViewer = this.getCommonViewer();
    if (commonViewer != null) {
      ICommonViewerSite commonViewerSite = CommonViewerSiteFactory
        .createCommonViewerSite(this.getViewSite());

      if (commonViewerSite != null) {
        // Note: actionService cannot be null
        final NavigatorActionService actionService = new NavigatorActionService(commonViewerSite,
          commonViewer, commonViewer.getNavigatorContentService());

        MenuManager menuManager = new MenuManager("#PopupMenu");
        menuManager.addMenuListener(mgr -> {
          ISelection selection = commonViewer.getSelection();
          actionService.setContext(new ActionContext(selection));
          actionService.fillContextMenu(mgr);
        });
        Menu menu = menuManager.createContextMenu(body);

        // It is necessary to set the menu in two places:
        // 1. The white space in the server view
        // 2. The text and link in the server view. If this menu is not set, if the
        // user right clicks on the text or uses shortcut keys to open the context menu,
        // the context menu will not come up
        body.setMenu(menu);
        hlink.setMenu(menu);
      }
    }

    return form;
  }

  /**
   * Switch between the servers and default/empty page. 
   * 
   */
  void toggleDefaultPage() {
    if (tableViewer.getTree().getItemCount() < 1) {
      book.showPage(noServersPage);
    } else {
      book.showPage(mainPage);
    }
  }

  private void deferInitialization() {
    Job job = new Job(Messages.jobInitializingServersView) {
      @Override
      public IStatus run(IProgressMonitor monitor) {
        deferredInitialize();
        return Status.OK_STATUS;
      }
    };

    job.setSystem(true);
    job.setPriority(Job.SHORT);
    job.schedule();
  }

  protected void deferredInitialize() {
    addListener();

    Display.getDefault().asyncExec(() -> {
      try {
        tableViewer = getCommonViewer();
        getSite().setSelectionProvider(tableViewer);

        try {
          if (tableViewer.getTree().getItemCount() > 0) {
            Object obj = tableViewer.getTree().getItem(0).getData();
            tableViewer.setSelection(new StructuredSelection(obj));
          } else {
            toggleDefaultPage();
          }
        } catch (Exception e1) {
          throw new IllegalStateException("Unable to update servers", e1);
        }
      } catch (Exception e2) {
        // ignore - view has already been closed
      }
    });

  }

  protected void refreshConnectionContent(final IConnectedEngineFacade server) {
    Display.getDefault().asyncExec(() -> {
      if (!tableViewer.getTree().isDisposed()) {
        tableViewer.refresh(server, true);
      }
    });
  }

  protected void refreshConnectionState() {
    Display.getDefault().asyncExec(() -> {
      IDecoratorManager dm = PlatformUI.getWorkbench().getDecoratorManager();
      dm.update(BindingsViewDecorator.ID);
      if (tableViewer != null) {
        tableViewer.setSelection(tableViewer.getSelection());
      }
    });
  }

  protected void addListener() {
    // To enable the UI updating of servers and its childrens
    serverResourceListener = new IConnectedEngineFacadeLifecycleListener() {
      @Override
      public void connectionAdded(IConnectedEngineFacade server) {
        addServer(server);
        server.addConnectedEngineListener(serverListener);
      }

      @Override
      public void connectionChanged(IConnectedEngineFacade server) {
        refreshConnectionContent(server);
      }

      @Override
      public void connectionRemoved(IConnectedEngineFacade server) {
        removeServer(server);
        server.removeConnectedEngineListener(serverListener);
      }
    };
    SonarLintCorePlugin.getServersManager().addServerLifecycleListener(serverResourceListener);

    serverListener = facade -> {
      refreshConnectionState();
      refreshConnectionContent(facade);
    };

    // add listeners to servers
    for (IConnectedEngineFacade server : SonarLintCorePlugin.getServersManager().getServers()) {
      server.addConnectedEngineListener(serverListener);
    }

  }

  protected void addServer(final IConnectedEngineFacade server) {
    Display.getDefault().asyncExec(() -> {
      tableViewer.add(tableViewer.getInput(), server);
      toggleDefaultPage();
    });
  }

  protected void removeServer(final IConnectedEngineFacade server) {
    Display.getDefault().asyncExec(() -> {
      tableViewer.remove(tableViewer.getInput(), new Object[] {server});
      toggleDefaultPage();
    });
  }

  @Override
  public void dispose() {
    SonarLintCorePlugin.getServersManager().removeServerLifecycleListener(serverResourceListener);
    super.dispose();
  }

}
