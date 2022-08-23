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
package org.sonarlint.eclipse.core.internal.tracking;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.eclipse.jdt.annotation.Nullable;
import org.sonarlint.eclipse.core.internal.proto.Sonarlint;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;
import org.sonarsource.sonarlint.core.client.api.connected.objectstore.HashingPathMapper;
import org.sonarsource.sonarlint.core.client.api.connected.objectstore.PathMapper;
import org.sonarsource.sonarlint.core.client.api.connected.objectstore.Reader;
import org.sonarsource.sonarlint.core.client.api.connected.objectstore.Writer;
import org.sonarsource.sonarlint.core.client.api.util.FileUtils;

public class IssueStore {
  private Path basePath;
  private IndexedObjectStore<String, Sonarlint.Issues> store;

  public IssueStore(Path storeBasePath, ISonarLintProject project) {
    this.basePath = storeBasePath;
    FileUtils.mkdirs(storeBasePath);
    StoreIndex<String> index = new StringStoreIndex(storeBasePath);
    PathMapper<String> mapper = new HashingPathMapper(storeBasePath, 2);
    StoreKeyValidator<String> validator = new PathStoreKeyValidator(project);
    Reader<Sonarlint.Issues> reader = is -> {
      try {
        return Sonarlint.Issues.parseFrom(is);
      } catch (IOException e) {
        throw new IllegalStateException("Failed to read issues", e);
      }
    };
    Writer<Sonarlint.Issues> writer = (os, issues) -> {
      try {
        issues.writeTo(os);
      } catch (IOException e) {
        throw new IllegalStateException("Failed to save issues", e);
      }
    };
    store = new IndexedObjectStore<>(index, mapper, reader, writer, validator);
    store.deleteInvalid();
  }

  public boolean contains(String key) {
    return store.contains(key);
  }

  public void save(String key, Collection<Trackable> issues) throws IOException {
    store.write(key, transform(issues));
  }

  @Nullable
  public Collection<Trackable> read(String key) throws IOException {
    Optional<Sonarlint.Issues> issues = store.read(key);
    if (issues.isPresent()) {
      return transform(issues.get());
    }
    return null;
  }

  public void clean() {
    store.deleteInvalid();
  }

  public void clear() {
    FileUtils.deleteRecursively(basePath);
    FileUtils.mkdirs(basePath);
  }

  private static Collection<Trackable> transform(Sonarlint.Issues protoIssues) {
    return protoIssues.getIssueList().stream()
      .map(IssueStore::transform)
      .filter(Objects::nonNull)
      .collect(Collectors.toList());
  }

  private static Sonarlint.Issues transform(Collection<Trackable> localIssues) {
    Sonarlint.Issues.Builder builder = Sonarlint.Issues.newBuilder();
    localIssues.stream()
      .map(IssueStore::transform)
      .filter(Objects::nonNull)
      .forEach(builder::addIssue);

    return builder.build();
  }

  private static Trackable transform(Sonarlint.Issues.Issue issue) {
    return new ProtobufIssueTrackable(issue);
  }

  private static Sonarlint.Issues.Issue transform(Trackable localIssue) {
    Sonarlint.Issues.Issue.Builder builder = Sonarlint.Issues.Issue.newBuilder()
      .setRuleKey(localIssue.getRuleKey())
      .setMessage(localIssue.getMessage())
      .setResolved(localIssue.isResolved())
      .setSeverity(localIssue.getSeverity())
      .setType(localIssue.getType());

    if (localIssue.getAssignee() != null) {
      builder.setAssignee(localIssue.getAssignee());
    }
    if (localIssue.getCreationDate() != null) {
      builder.setCreationDate(localIssue.getCreationDate());
    }
    if (localIssue.getLineHash() != null) {
      builder.setChecksum(localIssue.getLineHash());
    }
    if (localIssue.getServerIssueKey() != null) {
      builder.setServerIssueKey(localIssue.getServerIssueKey());
    }
    if (localIssue.getLine() != null) {
      builder.setLine(localIssue.getLine());
    }
    if (localIssue.getMarkerId() != null) {
      builder.setMarkerId(localIssue.getMarkerId());
    }
    return builder.build();
  }
}
