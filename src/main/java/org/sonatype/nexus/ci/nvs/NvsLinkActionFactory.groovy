package org.sonatype.nexus.ci.nvs

import javax.annotation.Nonnull

import hudson.Extension
import hudson.model.Action
import hudson.model.Project
import jenkins.model.TransientActionFactory

@Extension
class NvsLinkActionFactory
    extends TransientActionFactory<Project>
{
  @Override
  Class<Project> type() {
    return Project.class
  }

  @Nonnull
  @Override
  Collection<? extends Action> createFor(@Nonnull Project project) {
    return Collections.singleton(new NvsLinkAction(project))
  }
}
