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
package org.sonarlint.eclipse.core.internal.engine;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.eclipse.jdt.annotation.Nullable;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.internal.utils.StringUtils;
import org.sonarsource.sonarlint.core.client.api.common.Language;
import org.sonarsource.sonarlint.core.client.api.common.PluginDetails;
import org.sonarsource.sonarlint.core.client.api.common.SkipReason;

import static java.util.stream.Collectors.toList;
import static org.sonarlint.eclipse.core.internal.utils.SonarLintUtils.getEnabledLanguages;
import static org.sonarsource.sonarlint.core.client.api.common.Language.getLanguagesByPluginKey;

public class SkippedPluginsNotifier {

  private SkippedPluginsNotifier() {
  }

  public static void notifyForSkippedPlugins(Collection<PluginDetails> allPlugins, @Nullable String connectionId) {
    List<PluginDetails> skippedPlugins = allPlugins.stream().filter(p -> p.skipReason().isPresent()).collect(toList());
    if (!skippedPlugins.isEmpty()) {
      List<Language> skippedLanguages = skippedPlugins.stream()
        .flatMap(p -> getLanguagesByPluginKey(p.key()).stream())
        .filter(l -> getEnabledLanguages().contains(l))
        .collect(toList());
      String longMessage = buildLongMessage(connectionId, skippedPlugins, skippedLanguages);
      String notificationTitle;
      String notificationMsg;
      if (skippedLanguages.isEmpty()) {
        notificationTitle = "Rules not available";
        notificationMsg = "Some rules are not available until some requirements are satisfied";
      } else {
        notificationTitle = "Language analysis not available";
        notificationMsg = String.format("%s analysis will not be available until some requirements are satisfied",
          skippedLanguages.stream()
            .map(Language::getLanguageKey)
            .map(StringUtils::capitalize)
            .collect(Collectors.joining(", ")));
      }
      SonarLintLogger.get().showNotification(notificationTitle, notificationMsg, longMessage);
    }
  }

  private static String buildLongMessage(String connectionId, List<PluginDetails> skippedPlugins, List<Language> skippedLanguages) {
    boolean includeVMtips = false;
    StringBuilder longMessage = new StringBuilder();
    longMessage.append("Some analyzers");
    if (connectionId != null) {
      longMessage.append(" from connection '").append(connectionId).append("'");
    }
    longMessage.append(" can not be loaded.\n\n");
    if (!skippedLanguages.isEmpty()) {
      longMessage.append(String.format("%s analysis will not be available until following requirements are satisfied:%n",
        skippedLanguages.stream()
          .map(Language::getLanguageKey)
          .map(StringUtils::capitalize)
          .collect(Collectors.joining(", "))));
    }
    for (PluginDetails skippedPlugin : skippedPlugins) {
      SkipReason skipReason = skippedPlugin.skipReason().orElseThrow(IllegalStateException::new);
      if (skipReason instanceof SkipReason.IncompatiblePluginApi) {
        // Should never occurs in standalone mode
        longMessage.append(String.format(
          " - '%s' is not compatible with this version of SonarLint. Ensure you are using the latest version of SonarLint and check SonarLint output for details.%n",
          skippedPlugin.name()));
      } else if (skipReason instanceof SkipReason.UnsatisfiedDependency) {
        // Should never occurs in standalone mode
        SkipReason.UnsatisfiedDependency skipReasonCasted = (SkipReason.UnsatisfiedDependency) skipReason;
        longMessage.append(String.format(" - '%s' is missing dependency '%s'%n", skippedPlugin.name(), skipReasonCasted.getDependencyKey()));
      } else if (skipReason instanceof SkipReason.IncompatiblePluginVersion) {
        // Should never occurs in standalone mode
        SkipReason.IncompatiblePluginVersion skipReasonCasted = (SkipReason.IncompatiblePluginVersion) skipReason;
        longMessage.append(String.format(" - '%s' is too old for SonarLint. Current version is %s. Minimal supported version is %s. Please update your binding.%n",
          skippedPlugin.name(),
          skippedPlugin.version(), skipReasonCasted.getMinVersion()));
      } else if (skipReason instanceof SkipReason.UnsatisfiedJreRequirement) {
        includeVMtips = true;
        SkipReason.UnsatisfiedJreRequirement skipReasonCasted = (SkipReason.UnsatisfiedJreRequirement) skipReason;
        longMessage.append(String.format(" - '%s' requires Java runtime version %s or later. Current version is %s.%n", skippedPlugin.name(),
          skipReasonCasted.getMinVersion(), skipReasonCasted.getCurrentVersion()));
      }
    }
    if (includeVMtips) {
      longMessage
        .append("\nPlease see <a href=\"https://wiki.eclipse.org/Eclipse.ini#Specifying_the_JVM\">the Eclipse Wiki</a> to configure your IDE to run with a more recent JRE.");
    }
    return longMessage.toString();
  }

}
