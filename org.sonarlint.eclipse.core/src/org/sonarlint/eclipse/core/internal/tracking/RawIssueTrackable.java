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

import java.util.List;
import org.eclipse.jdt.annotation.Nullable;
import org.sonarlint.eclipse.core.internal.markers.TextRange;
import org.sonarsource.sonarlint.core.client.api.common.analysis.Issue;
import org.sonarsource.sonarlint.core.client.api.common.analysis.Issue.Flow;

import static org.sonarlint.eclipse.core.internal.tracking.DigestUtils.digest;

public class RawIssueTrackable implements Trackable {

  private final Issue issue;
  private final TextRange textRange;
  private final Integer textRangeHash;
  private final Integer lineHash;
  private Long markerId;

  public RawIssueTrackable(Issue issue) {
    this(issue, null, null, null);
  }

  public RawIssueTrackable(Issue issue, @Nullable TextRange textRange, @Nullable String textRangeContent, @Nullable String lineContent) {
    this.issue = issue;
    this.textRange = textRange;
    this.textRangeHash = textRangeContent != null ? checksum(textRangeContent) : null;
    this.lineHash = lineContent != null ? checksum(lineContent) : null;
  }

  @Override
  public Long getMarkerId() {
    return markerId;
  }

  @Override
  public void setMarkerId(Long id) {
    this.markerId = id;
  }

  private static int checksum(String content) {
    return digest(content).hashCode();
  }

  @Nullable
  @Override
  public Integer getLine() {
    return issue.getStartLine();
  }

  @Nullable
  @Override
  public String getMessage() {
    return issue.getMessage();
  }

  @Override
  public Integer getTextRangeHash() {
    return textRangeHash;
  }

  @Override
  public Integer getLineHash() {
    return lineHash;
  }

  @Override
  public String getRuleKey() {
    return issue.getRuleKey();
  }

  @Override
  public String getRuleName() {
    return issue.getRuleName();
  }

  @Override
  public String getSeverity() {
    return issue.getSeverity();
  }

  @Override
  public String getRawSeverity() {
    return issue.getSeverity();
  }

  @Nullable
  @Override
  public String getType() {
    return issue.getType();
  }

  @Nullable
  @Override
  public String getRawType() {
    return issue.getType();
  }

  @Override
  public TextRange getTextRange() {
    return textRange;
  }

  @Nullable
  @Override
  public String getServerIssueKey() {
    return null;
  }

  @Nullable
  @Override
  public Long getCreationDate() {
    return null;
  }

  @Override
  public boolean isResolved() {
    return false;
  }

  @Override
  public String getAssignee() {
    return "";
  }

  @Override
  public List<Flow> getFlows() {
    return issue.flows();
  }

}
