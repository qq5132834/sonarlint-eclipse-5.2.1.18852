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

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.engine.connected.ConnectedEngineFacade;
import org.sonarlint.eclipse.core.internal.engine.connected.IConnectedEngineFacade;
import org.sonarlint.eclipse.core.internal.engine.connected.RemoteSonarProject;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;

public class BindingsViewContentProvider extends BaseContentProvider implements ITreeContentProvider {

  @Override
  public Object[] getElements(Object element) {
    return SonarLintCorePlugin.getServersManager().getServers().toArray();
  }

  @Override
  public Object[] getChildren(Object element) {
    if (element instanceof IConnectedEngineFacade) {
      ConnectedEngineFacade server = (ConnectedEngineFacade) element;
      return server.getBoundRemoteProjects(new NullProgressMonitor()).toArray();
    }
    if (element instanceof RemoteSonarProject) {
      RemoteSonarProject project = (RemoteSonarProject) element;
      return ((ConnectedEngineFacade) getParent(element)).getBoundProjects(project.getProjectKey()).toArray();
    }
    return new Object[0];
  }

  @Override
  public Object getParent(Object element) {
    if (element instanceof ISonarLintProject) {
      return SonarLintCorePlugin.getServersManager()
        .resolveBinding((ISonarLintProject) element)
        .flatMap(b -> b.getEngineFacade().getRemoteProject(b.getProjectBinding().projectKey(), new NullProgressMonitor()))
        .orElse(null);
    }
    if (element instanceof RemoteSonarProject) {
      return SonarLintCorePlugin.getServersManager().findById(((RemoteSonarProject) element).getServerId()).orElse(null);
    }
    return null;
  }

  @Override
  public boolean hasChildren(Object element) {
    if (element instanceof IConnectedEngineFacade) {
      return !((IConnectedEngineFacade) element).getBoundProjects().isEmpty();
    }
    if (element instanceof RemoteSonarProject) {
      RemoteSonarProject project = (RemoteSonarProject) element;
      return !((ConnectedEngineFacade) getParent(element)).getBoundProjects(project.getProjectKey()).isEmpty();
    }
    return false;
  }
}
