package cn.eziolin.zhiwei4idea.idea;

import cn.eziolin.zhiwei4idea.CompletionCheckinHandler;
import cn.eziolin.zhiwei4idea.idea.service.CompletionService;
import com.intellij.openapi.vcs.CheckinProjectPanel;
import com.intellij.openapi.vcs.changes.CommitContext;
import com.intellij.openapi.vcs.checkin.CheckinHandler;
import com.intellij.openapi.vcs.checkin.VcsCheckinHandlerFactory;
import git4idea.GitVcs;
import org.jetbrains.annotations.NotNull;

public class CompletionCheckinHandlerFactory extends VcsCheckinHandlerFactory {

    protected CompletionCheckinHandlerFactory() {
        super(GitVcs.getKey());
    }

    @NotNull
    @Override
    protected CheckinHandler createVcsHandler(@NotNull CheckinProjectPanel panel, @NotNull CommitContext commitContext) {
        var project = panel.getProject();
        // hack the panel
        project.getService(CompletionService.class).setCheckinProjectPanel(panel);
        return new CompletionCheckinHandler();
    }
}
