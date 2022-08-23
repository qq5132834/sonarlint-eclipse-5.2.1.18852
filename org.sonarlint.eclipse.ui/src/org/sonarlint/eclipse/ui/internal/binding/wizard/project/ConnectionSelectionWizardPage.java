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

import org.eclipse.core.databinding.Binding;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.UpdateValueStrategy;
import org.eclipse.core.databinding.beans.BeanProperties;
import org.eclipse.jface.databinding.fieldassist.ControlDecorationSupport;
import org.eclipse.jface.databinding.viewers.ViewersObservables;
import org.eclipse.jface.databinding.wizard.WizardPageSupport;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.engine.connected.ConnectedEngineFacade;
import org.sonarlint.eclipse.core.internal.engine.connected.IConnectedEngineFacade;
import org.sonarlint.eclipse.ui.internal.SonarLintImages;
import org.sonarlint.eclipse.ui.internal.binding.wizard.connection.ServerConnectionWizard;
import org.sonarlint.eclipse.ui.internal.util.wizard.ParentAwareWizard;

public class ConnectionSelectionWizardPage extends AbstractProjectBindingWizardPage {

  private Binding serverBinding;

  public ConnectionSelectionWizardPage(ProjectBindingModel model) {
    super("server_id_page", "Choose the SonarQube or SonarCloud server connection", model, 2);
  }

  @Override
  protected void doCreateControl(Composite container) {
    ComboViewer serverCombo = new ComboViewer(container, SWT.READ_ONLY);
    serverCombo.setContentProvider(ArrayContentProvider.getInstance());
    serverCombo.setLabelProvider(new LabelProvider() {
      @Override
      public String getText(Object element) {
        IConnectedEngineFacade current = (IConnectedEngineFacade) element;
        return current.getId();
      }

      @Override
      public Image getImage(Object element) {
        if (((IConnectedEngineFacade) element).isSonarCloud()) {
          return SonarLintImages.SONARCLOUD_SERVER_ICON_IMG;
        } else {
          return SonarLintImages.SONARQUBE_SERVER_ICON_IMG;
        }
      }
    });
    serverCombo.setInput(SonarLintCorePlugin.getServersManager().getServers());
    ConnectedEngineFacade server = model.getServer();
    if (server != null) {
      final ISelection selection = new StructuredSelection(server);
      serverCombo.setSelection(selection);
    }

    DataBindingContext dbc = new DataBindingContext();
    serverBinding = dbc.bindValue(
      ViewersObservables.observeSingleSelection(serverCombo),
      BeanProperties.value(ProjectBindingModel.class, ProjectBindingModel.PROPERTY_SERVER)
        .observe(model),
      new UpdateValueStrategy().setBeforeSetValidator(new MandatoryServerValidator("You must select a server connection")), null);
    ControlDecorationSupport.create(serverBinding, SWT.LEFT | SWT.TOP);

    WizardPageSupport.create(this, dbc);

    Button addBtn = new Button(container, SWT.PUSH);
    addBtn.setText("New...");
    addBtn.addSelectionListener(new SelectionAdapter() {

      @Override
      public void widgetSelected(SelectionEvent e) {
        ServerConnectionWizard.createDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), model.getEclipseProjects()).open();
        ((ParentAwareWizard) getWizard()).getParent().close();
      }

    });
  }

  @Override
  public void setVisible(boolean visible) {
    super.setVisible(visible);
    if (visible) {
      serverBinding.validateTargetToModel();
    }
  }

}
