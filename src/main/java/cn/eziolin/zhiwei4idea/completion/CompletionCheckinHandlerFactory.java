package cn.eziolin.zhiwei4idea.completion;

import com.intellij.openapi.vcs.CheckinProjectPanel;
import com.intellij.openapi.vcs.changes.CommitContext;
import com.intellij.openapi.vcs.checkin.CheckinHandler;
import com.intellij.openapi.vcs.checkin.VcsCheckinHandlerFactory;
import git4idea.GitVcs;
import io.vavr.control.Option;
import org.jetbrains.annotations.NotNull;

public class CompletionCheckinHandlerFactory extends VcsCheckinHandlerFactory {

  protected CompletionCheckinHandlerFactory() {
    super(GitVcs.getKey());
  }

  @NotNull
  @Override
  protected CheckinHandler createVcsHandler(
      @NotNull CheckinProjectPanel panel, @NotNull CommitContext commitContext) {
    // hack the panel
    Option.of(panel.getProject().getService(CompletionService.class))
        .forEach(service -> service.setCheckinProjectPanel(panel));
    return CheckinHandler.DUMMY;
  }
}
