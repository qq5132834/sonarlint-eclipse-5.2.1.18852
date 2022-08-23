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
package org.sonarlint.eclipse.ui.internal.console;

import org.eclipse.jface.preference.JFacePreferences;
import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceColors;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;
import org.sonarlint.eclipse.ui.internal.Messages;
import org.sonarlint.eclipse.ui.internal.SonarLintUiPlugin;

public class SonarLintConsole extends MessageConsole implements IPropertyChangeListener {

  public static final String P_VERBOSE_OUTPUT = "debugOutput"; //$NON-NLS-1$
  public static final String P_ANALYZER_OUTPUT = "showAnalyzerOutput"; //$NON-NLS-1$
  public static final String P_SHOW_CONSOLE = "showConsole"; //$NON-NLS-1$
  public static final String P_SHOW_CONSOLE_NEVER = "never"; //$NON-NLS-1$
  public static final String P_SHOW_CONSOLE_ON_OUTPUT = "onOutput"; //$NON-NLS-1$
  public static final String P_SHOW_CONSOLE_ON_ERROR = "onError"; //$NON-NLS-1$

  public static final String TITLE = Messages.SonarConsole_title;

  private MessageConsoleStream infoStream;
  private MessageConsoleStream warnStream;
  private MessageConsoleStream debugStream;

  public SonarLintConsole(ImageDescriptor imageDescriptor) {
    super(TITLE, imageDescriptor);
    this.infoStream = newMessageStream();
    this.warnStream = newMessageStream();
    this.debugStream = newMessageStream();
  }

  @Override
  protected void init() {
    super.init();

    JFaceResources.getFontRegistry().addListener(SonarLintConsole.this);

    Display display = Display.getDefault();

    ColorRegistry colorRegistry = JFaceResources.getColorRegistry();

    Color linkColor = colorRegistry.get(JFacePreferences.HYPERLINK_COLOR);
    if (linkColor == null) {
      linkColor = JFaceColors.getActiveHyperlinkText(display);
    }

    Color errorColorColor = colorRegistry.get(JFacePreferences.ERROR_COLOR);
    if (errorColorColor == null) {
      errorColorColor = JFaceColors.getErrorText(display);
    }

    getWarnStream().setColor(errorColorColor);
    getDebugStream().setColor(linkColor);
  }

  public void bringConsoleToFront() {
    if (PlatformUI.isWorkbenchRunning()) {
      IConsoleManager manager = ConsolePlugin.getDefault().getConsoleManager();
      if (!isVisible()) {
        manager.addConsoles(new IConsole[] {this});
      }
      manager.showConsoleView(this);
    }
  }

  private static boolean isVisible() {
    IConsoleManager conMan = ConsolePlugin.getDefault().getConsoleManager();
    IConsole[] existing = conMan.getConsoles();
    for (int i = 0; i < existing.length; i++) {
      if (SonarLintConsole.TITLE.equals(existing[i].getName())) {
        return true;
      }
    }
    return false;
  }

  public void info(String msg, boolean fromAnalyzer) {
    if (showAnalysisLogs() || !fromAnalyzer) {
      if (isShowConsoleOnOutput()) {
        bringConsoleToFront();
      }
      write(getInfoStream(), msg);
    }
  }

  public void error(String msg, boolean fromAnalyzer) {
    if (showAnalysisLogs() || !fromAnalyzer) {
      if (isShowConsoleOnOutput() || isShowConsoleOnError()) {
        bringConsoleToFront();
      }
      write(getWarnStream(), msg);
    }
  }

  public void debug(String msg, boolean fromAnalyzer) {
    if (isVerboseEnabled() && (showAnalysisLogs() || !fromAnalyzer)) {
      if (isShowConsoleOnOutput()) {
        bringConsoleToFront();
      }
      write(getDebugStream(), msg);
    }
  }

  private static void write(MessageConsoleStream stream, String msg) {
    if (msg == null) {
      return;
    }
    stream.println(msg);
  }

  private MessageConsoleStream getInfoStream() {
    return infoStream;
  }

  private MessageConsoleStream getWarnStream() {
    return warnStream;
  }

  public MessageConsoleStream getDebugStream() {
    return debugStream;
  }

  private static String getShowConsolePreference() {
    return SonarLintUiPlugin.getDefault().getPreferenceStore().getString(SonarLintConsole.P_SHOW_CONSOLE);
  }

  private static boolean isShowConsoleOnOutput() {
    return P_SHOW_CONSOLE_ON_OUTPUT.equals(getShowConsolePreference());
  }

  private static boolean isShowConsoleOnError() {
    return P_SHOW_CONSOLE_ON_ERROR.equals(getShowConsolePreference());
  }

  public static boolean isVerboseEnabled() {
    return SonarLintUiPlugin.getDefault().getPreferenceStore().getBoolean(SonarLintConsole.P_VERBOSE_OUTPUT);
  }

  public static boolean showAnalysisLogs() {
    return SonarLintUiPlugin.getDefault().getPreferenceStore().getBoolean(SonarLintConsole.P_ANALYZER_OUTPUT);
  }

  @Override
  protected void dispose() {
    super.dispose();
    JFaceResources.getFontRegistry().removeListener(SonarLintConsole.this);
  }

  @Override
  public void propertyChange(PropertyChangeEvent event) {
    // refresh font to react to font size change
    setFont(null);
  }

}
