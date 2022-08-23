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
package org.sonarlint.eclipse.ui.internal.markers;

import org.eclipse.core.resources.IMarker;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IMarkerResolution2;
import org.eclipse.ui.PlatformUI;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.internal.markers.MarkerUtils;
import org.sonarlint.eclipse.ui.internal.SonarLintImages;
import org.sonarlint.eclipse.ui.internal.views.RuleDescriptionWebView;

public class ShowRuleDescriptionMarkerResolver implements IMarkerResolution2 {

  private final IMarker marker;

  public ShowRuleDescriptionMarkerResolver(IMarker marker) {
    this.marker = marker;
  }

  @Override
  public String getDescription() {
    return "Open rule description to better understand the issue: " + marker.getAttribute(IMarker.MESSAGE, "unknown");
  }

  @Override
  public String getLabel() {
    return "Open description of rule " + marker.getAttribute(MarkerUtils.SONAR_MARKER_RULE_KEY_ATTR, "unknown");
  }

  @Override
  public void run(IMarker marker) {
    try {
      RuleDescriptionWebView view = (RuleDescriptionWebView) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView(RuleDescriptionWebView.ID);
      view.setInput(marker);
    } catch (Exception e) {
      SonarLintLogger.get().error("Unable to open rule description view", e);
    }
  }

  @Override
  public Image getImage() {
    return SonarLintImages.RESOLUTION_SHOW_RULE;
  }
}
