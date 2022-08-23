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
package org.sonarlint.eclipse.core.internal.telemetry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.sonarlint.eclipse.core.internal.engine.connected.ConnectedEngineFacadeManager.PREF_SERVERS;

import java.nio.file.Path;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.engine.connected.IConnectedEngineFacade;
import org.sonarlint.eclipse.core.internal.resources.SonarLintProjectConfiguration;
import org.sonarlint.eclipse.core.internal.resources.SonarLintProjectConfiguration.EclipseProjectBinding;
import org.sonarlint.eclipse.tests.common.SonarTestCase;
import org.sonarsource.sonarlint.core.telemetry.TelemetryClient;
import org.sonarsource.sonarlint.core.telemetry.TelemetryManager;

public class SonarLintTelemetryTest extends SonarTestCase {
  private SonarLintTelemetry telemetry;
  private TelemetryManager engine = mock(TelemetryManager.class);

  private static IProject project;
  private static SonarLintProjectConfiguration configuration;
  private static final IEclipsePreferences ROOT = InstanceScope.INSTANCE.getNode(SonarLintCorePlugin.PLUGIN_ID);

  @BeforeClass
  public static void setUp() throws Exception {
    project = importEclipseProject("SimpleProject");
    // Configure the project
    configuration = SonarLintCorePlugin.getInstance().getProjectConfigManager().load(new ProjectScope(project),
        "A Project");
  }

  @Before
  public void start() throws Exception {
    this.telemetry = createTelemetry();

    project.open(MONITOR);
    ROOT.node(PREF_SERVERS).removeNode();
    SonarLintCorePlugin.getServersManager().stop();
    SonarLintCorePlugin.getServersManager().init();
  }

  private SonarLintTelemetry createTelemetry() {
    when(engine.isEnabled()).thenReturn(true);

    SonarLintTelemetry telemetry = new SonarLintTelemetry() {
      public TelemetryManager newTelemetryManager(Path path, TelemetryClient client) {
        return engine;
      }
    };
    telemetry.init();
    return telemetry;
  }

  @Test
  public void telemetry_should_be_deactivated_for_tests() {
    assertThat(SonarLintTelemetry.shouldBeActivated()).isFalse();
  }

  @Test
  public void stop_should_trigger_stop_telemetry() {
    when(engine.isEnabled()).thenReturn(true);
    telemetry.stop();
    verify(engine).isEnabled();
    verify(engine).stop();
  }

  @Test
  public void test_scheduler() {
    assertThat(telemetry.getScheduledJob()).isNotNull();
    telemetry.stop();
    assertThat(telemetry.getScheduledJob()).isNull();
  }

  @Test
  public void optOut_should_trigger_disable_telemetry() {
    when(engine.isEnabled()).thenReturn(true);
    telemetry.optOut(true);
    verify(engine).disable();
    telemetry.stop();
  }

  @Test
  public void should_not_opt_out_twice() {
    when(engine.isEnabled()).thenReturn(false);
    telemetry.optOut(true);
    verify(engine).isEnabled();
    verifyNoMoreInteractions(engine);
  }

  @Test
  public void optIn_should_trigger_enable_telemetry() {
    when(engine.isEnabled()).thenReturn(false);
    telemetry.optOut(false);
    verify(engine).enable();
  }

  @Test
  public void upload_should_trigger_upload_when_enabled() {
    when(engine.isEnabled()).thenReturn(true);
    telemetry.upload();
    verify(engine).isEnabled();
    verify(engine).uploadLazily();
  }

  @Test
  public void upload_should_not_trigger_upload_when_disabled() {
    when(engine.isEnabled()).thenReturn(false);
    telemetry.upload();
    verify(engine).isEnabled();
    verifyNoMoreInteractions(engine);
  }

  @Test
  public void analysisDoneOnMultipleFiles_should_trigger_analysisDoneOnMultipleFiles_when_enabled() {
    when(engine.isEnabled()).thenReturn(true);
    telemetry.analysisDoneOnMultipleFiles();
    verify(engine).isEnabled();
    verify(engine).analysisDoneOnMultipleFiles();
  }

  @Test
  public void analysisDoneOnMultipleFiles_should_not_trigger_analysisDoneOnMultipleFiles_when_disabled() {
    when(engine.isEnabled()).thenReturn(false);
    telemetry.analysisDoneOnMultipleFiles();
    verify(engine).isEnabled();
    verifyNoMoreInteractions(engine);
  }

  @Test
  public void analysisDoneOnSingleFile_should_trigger_analysisDoneOnSingleFile_when_enabled() {
    when(engine.isEnabled()).thenReturn(true);
    String language = "java";
    int time = 123;
    telemetry.analysisDoneOnSingleFile(language, time);
    verify(engine).isEnabled();
    verify(engine).analysisDoneOnSingleLanguage(language, time);
  }

  @Test
  public void analysisDoneOnSingleFile_should_not_trigger_analysisDoneOnSingleFile_when_disabled() {
    when(engine.isEnabled()).thenReturn(false);
    String language = "java";
    int time = 123;
    telemetry.analysisDoneOnSingleFile(language, time);
    verify(engine).isEnabled();
    verifyNoMoreInteractions(engine);
  }

  @Test
  public void should_not_report_sonar_cloud_usage_when_project_is_not_bound() {
    clearImportedProjectBinding();

    assertThat(SonarLintTelemetry.isAnyOpenProjectBoundToSonarCloud()).isFalse();
  }

  @Test
  public void should_not_report_sonar_cloud_usage_when_project_is_bound_to_localhost() {
    addServer("localhost", "http://localhost:9000");
    bindImportedProjectToServer("localhost");

    assertThat(SonarLintTelemetry.isAnyOpenProjectBoundToSonarCloud()).isFalse();
  }

  @Test
  public void should_not_report_sonar_cloud_usage_when_sonar_cloud_server_connected_but_project_is_not_bound() {
    addServer("sonarcloud", "https://sonarcloud.io");

    assertThat(SonarLintTelemetry.isAnyOpenProjectBoundToSonarCloud()).isFalse();
  }

  @Test
  public void should_report_sonar_cloud_usage_when_project_is_bound_to_sonar_cloud() {
    addServer("sonarcloud", "https://sonarcloud.io");
    bindImportedProjectToServer("sonarcloud");

    assertThat(SonarLintTelemetry.isAnyOpenProjectBoundToSonarCloud()).isTrue();
  }

  @Test
  public void should_not_report_connected_mode_usage_when_project_is_not_bound() {
    clearImportedProjectBinding();

    assertThat(SonarLintTelemetry.isAnyOpenProjectBound()).isFalse();
  }

  @Test
  public void should_report_connected_mode_usage_when_project_is_bound() {
    addServer("localhost", "http://localhost:9000");
    bindImportedProjectToServer("localhost");

    assertThat(SonarLintTelemetry.isAnyOpenProjectBound()).isTrue();
  }

  @Test
  public void should_not_report_any_connected_usage_when_bound_project_is_closed() throws CoreException {
    addServer("sonarcloud", "https://sonarcloud.io");
    bindImportedProjectToServer("sonarcloud");
    project.close(MONITOR);

    assertThat(SonarLintTelemetry.isAnyOpenProjectBound()).isFalse();
    assertThat(SonarLintTelemetry.isAnyOpenProjectBoundToSonarCloud()).isFalse();
  }

  private void addServer(String id, String url) {
    IConnectedEngineFacade server = SonarLintCorePlugin.getServersManager().create(id, url, "", "", "", false);
    SonarLintCorePlugin.getServersManager().addServer(server, "login", "pwd");
  }

  private void bindImportedProjectToServer(String url) {
    configuration.setProjectBinding(new EclipseProjectBinding(url, "myProjectKey", "aPrefix", "aSuffix"));
    SonarLintCorePlugin.getInstance().getProjectConfigManager().save(new ProjectScope(project), configuration);
  }

  private void clearImportedProjectBinding() {
    configuration.setProjectBinding(null);
    SonarLintCorePlugin.getInstance().getProjectConfigManager().save(new ProjectScope(project), configuration);
  }
}
