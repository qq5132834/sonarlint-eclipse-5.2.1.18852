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
package org.sonarlint.eclipse.core.internal.notifications;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonarsource.sonarlint.core.client.api.util.FileUtils;

import static org.assertj.core.api.Assertions.assertThat;

public class NotificationsTrackerTest {

  @Rule
  public TemporaryFolder tmp = new TemporaryFolder();

  @Test
  public void should_return_current_time_when_storage_missing() throws IOException {
    NotificationsTracker tracker = new NotificationsTracker(tmp.newFolder().toPath());
    ZonedDateTime pivot = ZonedDateTime.now().minus(1, ChronoUnit.MINUTES);
    assertThat(pivot.isBefore(tracker.getLastEventPolling())).isTrue();
  }

  @Test
  public void should_return_current_time_previously_set() throws IOException {
    Path basedir = tmp.newFolder().toPath();
    NotificationsTracker tracker = new NotificationsTracker(basedir);
    ZonedDateTime pivot = ZonedDateTime.now().minus(1, ChronoUnit.HOURS);
    tracker.setLastEventPolling(pivot);

    assertThat(new NotificationsTracker(basedir).getLastEventPolling()).isEqualTo(pivot.truncatedTo(ChronoUnit.MILLIS));
  }

  @Test
  public void should_create_required_subdirs() throws IOException {
    Path basedir = tmp.newFolder().toPath().resolve("sub").resolve("sub2");
    NotificationsTracker tracker = new NotificationsTracker(basedir);
    ZonedDateTime pivot = ZonedDateTime.now().minus(1, ChronoUnit.HOURS);
    tracker.setLastEventPolling(pivot);

    assertThat(basedir.toFile().isDirectory()).isTrue();
  }

  @Test
  public void should_return_current_time_when_storage_corrupt() throws IOException {
    Path basedir = tmp.newFolder().toPath();
    Files.write(basedir.resolve(NotificationsTracker.FILENAME), "garbage".getBytes());

    NotificationsTracker tracker = new NotificationsTracker(basedir);
    ZonedDateTime pivot = ZonedDateTime.now().minus(1, ChronoUnit.MINUTES);
    assertThat(pivot.isBefore(tracker.getLastEventPolling())).isTrue();
  }

  @Test
  public void should_return_current_time_when_storage_broken() throws IOException {
    Path notReallyDir = tmp.newFile().toPath();
    NotificationsTracker tracker = new NotificationsTracker(notReallyDir);
    ZonedDateTime pivot = ZonedDateTime.now().minus(1, ChronoUnit.MINUTES);
    assertThat(pivot.isBefore(tracker.getLastEventPolling())).isTrue();
  }

  @Test
  public void should_not_crash_when_cannot_write_storage() throws IOException {
    Path notReallyDir = tmp.newFile().toPath();
    NotificationsTracker tracker = new NotificationsTracker(notReallyDir);
    ZonedDateTime pivot = ZonedDateTime.now().minus(1, ChronoUnit.MINUTES);
    tracker.setLastEventPolling(pivot);
    assertThat(tracker.getLastEventPolling()).isEqualTo(pivot);
  }

  @Test
  public void should_return_time_without_re_reading() throws IOException {
    Path basedir = tmp.newFolder().toPath();
    NotificationsTracker tracker = new NotificationsTracker(basedir);
    ZonedDateTime pivot = ZonedDateTime.now().minus(1, ChronoUnit.MINUTES);
    tracker.setLastEventPolling(pivot);

    FileUtils.deleteRecursively(basedir);
    assertThat(tracker.getLastEventPolling()).isEqualTo(pivot);
  }
}
