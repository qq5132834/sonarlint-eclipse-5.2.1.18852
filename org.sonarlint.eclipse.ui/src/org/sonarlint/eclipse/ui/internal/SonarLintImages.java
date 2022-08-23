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
package org.sonarlint.eclipse.ui.internal;

import java.net.URL;
import java.util.Locale;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.resource.CompositeImageDescriptor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;

public final class SonarLintImages {

  public static final ImageDescriptor SONARWIZBAN_IMG = createImageDescriptor("sonarqube-48x200.png"); //$NON-NLS-1$
  public static final ImageDescriptor IMG_WIZBAN_NEW_SERVER = createImageDescriptor("new_server_wiz.png"); //$NON-NLS-1$
  public static final ImageDescriptor SONARLINT_CONSOLE_IMG_DESC = createImageDescriptor("sonarlint-16x16.png"); //$NON-NLS-1$
  public static final ImageDescriptor UPDATE_IMG = createImageDescriptor("update.gif"); //$NON-NLS-1$
  public static final ImageDescriptor SYNCED_IMG = createImageDescriptor("synced.gif"); //$NON-NLS-1$
  public static final ImageDescriptor MARK_OCCURENCES_IMG = createImageDescriptor("mark_occurrences.png"); //$NON-NLS-1$
  public static final ImageDescriptor WIZ_NEW_SERVER = createImageDescriptor("wiz_new_server.gif"); //$NON-NLS-1$
  public static final ImageDescriptor SQ_LABEL_DECORATOR = createImageDescriptor("onde-label-decorator.gif"); //$NON-NLS-1$
  public static final ImageDescriptor EDIT_SERVER = createImageDescriptor("full/menu16/editconfig.png"); //$NON-NLS-1$
  public static final ImageDescriptor UNBIND = createImageDescriptor("full/menu16/disconnect_co.png"); //$NON-NLS-1$

  public static final Image ISSUE_ANNOTATION = createImage("full/annotation16/issue.png"); //$NON-NLS-1$
  public static final Image RESOLUTION_SHOW_RULE = createImage("full/marker_resolution16/showrule.png"); //$NON-NLS-1$
  public static final Image RESOLUTION_SHOW_LOCATIONS = createImage("full/marker_resolution16/showlocations.png"); //$NON-NLS-1$
  public static final Image RESOLUTION_DISABLE_RULE = createImage("full/marker_resolution16/disablerule.png"); //$NON-NLS-1$
  public static final Image BALLOON_IMG = createImage("sonarlint-16x16.png"); //$NON-NLS-1$
  public static final Image IMG_SEVERITY_BLOCKER = createImage("severity/blocker.png"); //$NON-NLS-1$
  public static final Image IMG_SEVERITY_CRITICAL = createImage("severity/critical.png"); //$NON-NLS-1$
  public static final Image IMG_SEVERITY_MAJOR = createImage("severity/major.png"); //$NON-NLS-1$
  public static final Image IMG_SEVERITY_MINOR = createImage("severity/minor.png"); //$NON-NLS-1$
  public static final Image IMG_SEVERITY_INFO = createImage("severity/info.png"); //$NON-NLS-1$

  public static final Image IMG_TYPE_BUG = createImage("type/bug.png"); //$NON-NLS-1$
  public static final Image IMG_TYPE_CODE_SMELL = createImage("type/code_smell.png"); //$NON-NLS-1$
  public static final Image IMG_TYPE_VULNERABILITY = createImage("type/vulnerability.png"); //$NON-NLS-1$

  public static final Image SONARQUBE_SERVER_ICON_IMG = createImage("logo/sonarqube-16px.png"); //$NON-NLS-1$
  public static final Image SONARQUBE_PROJECT_ICON_IMG = createImage("project-16x16.png"); //$NON-NLS-1$
  public static final Image SONARCLOUD_SERVER_ICON_IMG = createImage("logo/sonarcloud-16px.png"); //$NON-NLS-1$
  public static final Image IMG_SONARQUBE_LOGO = createImage("logo/sonarqube-black-256px.png"); //$NON-NLS-1$
  public static final Image IMG_SONARCLOUD_LOGO = createImage("logo/sonarcloud-black-256px.png"); //$NON-NLS-1$

  public static final Image IMG_OPEN_EXTERNAL = createImage("external-link-16.png"); //$NON-NLS-1$

  public static final Image NOTIFICATION_CLOSE = createImage("popup/notification-close.gif"); //$NON-NLS-1$
  public static final Image NOTIFICATION_CLOSE_HOVER = createImage("popup/notification-close-active.gif"); //$NON-NLS-1$

  public static final ImageDescriptor DEBUG = createImageDescriptor("debug.gif"); //$NON-NLS-1$
  public static final ImageDescriptor SHOW_CONSOLE = createImageDescriptor("showConsole.gif"); //$NON-NLS-1$

  private SonarLintImages() {
  }

  @Nullable
  public static Image getIssueImage(String severity, @Nullable String type) {
    String key = severity + "/" + type;
    ImageRegistry imageRegistry = SonarLintUiPlugin.getDefault().getImageRegistry();
    Image image = imageRegistry.get(key);
    if (image == null) {
      ImageDescriptor severityImage = createImageDescriptor("severity/" + severity.toLowerCase(Locale.ENGLISH) + ".png");
      ImageDescriptor typeImage = null;
      if (type != null) {
        typeImage = createImageDescriptor("type/" + type.toLowerCase(Locale.ENGLISH) + ".png");
      }
      imageRegistry.put(key, new CompositeSeverityTypeImage(severityImage, typeImage));
    }
    return imageRegistry.get(key);
  }

  @Nullable
  public static Image getSeverityImage(String severity) {
    return createImage("severity/" + severity.toLowerCase(Locale.ENGLISH) + ".png");
  }

  @Nullable
  public static Image getTypeImage(String type) {
    return createImage("type/" + type.toLowerCase(Locale.ENGLISH) + ".png");
  }

  private static class CompositeSeverityTypeImage extends CompositeImageDescriptor {

    private final ImageDescriptor severity;
    private final ImageDescriptor type;

    public CompositeSeverityTypeImage(ImageDescriptor severity, @Nullable ImageDescriptor type) {
      this.severity = severity;
      this.type = type;
    }

    @Override
    protected void drawCompositeImage(int width, int height) {
      // Keep using deprecated methods for backward compatibility
      if (type != null) {
        drawImage(type.getImageData(), 0, 0);
        drawImage(severity.getImageData(), 16, 0);
      } else {
        drawImage(severity.getImageData(), 0, 0);
      }
    }

    @Override
    protected Point getSize() {
      return new Point(32, 16);
    }

  }

  @Nullable
  private static URL getIconUrl(String key) {
    return SonarLintUiPlugin.getDefault().getBundle().getEntry("icons/" + key);
  }

  private static Image createImage(String key) {
    createImageDescriptor(key);
    ImageRegistry imageRegistry = SonarLintUiPlugin.getDefault().getImageRegistry();
    return imageRegistry.get(key);
  }

  @Nullable
  private static ImageDescriptor createImageDescriptor(String key) {
    ImageRegistry imageRegistry = SonarLintUiPlugin.getDefault().getImageRegistry();
    ImageDescriptor imageDescriptor = imageRegistry.getDescriptor(key);
    if (imageDescriptor == null) {
      URL url = getIconUrl(key);
      if (url != null) {
        imageDescriptor = ImageDescriptor.createFromURL(url);
      } else {
        imageDescriptor = ImageDescriptor.getMissingImageDescriptor();
      }
      imageRegistry.put(key, imageDescriptor);
    }
    return imageDescriptor;
  }

}
