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
package org.sonarlint.eclipse.ui.internal.binding.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.sonarlint.eclipse.ui.internal.Messages;
import org.sonarlint.eclipse.ui.internal.SonarLintImages;
import org.sonarlint.eclipse.ui.internal.binding.wizard.connection.ServerConnectionWizard;

/**
 * An action to invoke the new server and server configuration wizard.
 */
public class NewConnectionWizardAction extends Action {

  /**
   * New server action.
   */
  public NewConnectionWizardAction() {
    super();

    setImageDescriptor(SonarLintImages.WIZ_NEW_SERVER);
    setText(Messages.actionNewServer);
  }

  @Override
  public void run() {
    IWorkbench workbench = PlatformUI.getWorkbench();
    WizardDialog dialog = ServerConnectionWizard.createDialog(workbench.getActiveWorkbenchWindow().getShell());
    dialog.open();
  }

}
