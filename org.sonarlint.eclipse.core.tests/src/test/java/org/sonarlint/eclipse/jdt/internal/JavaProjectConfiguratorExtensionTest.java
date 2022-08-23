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
package org.sonarlint.eclipse.jdt.internal;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.JavaCore;
import org.junit.Before;
import org.junit.Test;
import org.sonarlint.eclipse.core.internal.resources.DefaultSonarLintProjectAdapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JavaProjectConfiguratorExtensionTest {
  private JavaProjectConfiguratorExtension extension;

  @Before
  public void setUp() {
    extension = new JavaProjectConfiguratorExtension();
  }

  @Test
  public void should_configurate_projects_java_nature() throws CoreException {
    IProject project = mock(IProject.class);
    when(project.hasNature(JavaCore.NATURE_ID)).thenReturn(true);
    assertThat(extension.canConfigure(new DefaultSonarLintProjectAdapter(project))).isTrue();
  }
}
