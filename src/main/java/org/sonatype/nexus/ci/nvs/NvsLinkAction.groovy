package org.sonatype.nexus.ci.nvs

import javax.annotation.CheckForNull

import hudson.model.Action
import hudson.model.Project

class NvsLinkAction implements Action
{
  private Project project

  NvsLinkAction (Project project) {
    this.project = project
  }

  @CheckForNull
  @Override
  String getIconFileName() {
    return null
  }

  @CheckForNull
  @Override
  String getDisplayName() {
    return null
  }

  @CheckForNull
  @Override
  String getUrlName() {
    return null
  }
}
