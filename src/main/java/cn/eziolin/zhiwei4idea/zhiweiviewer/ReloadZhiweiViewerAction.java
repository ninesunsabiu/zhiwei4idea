package cn.eziolin.zhiwei4idea.zhiweiviewer;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class ReloadZhiweiViewerAction extends AnAction {
  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    Optional.ofNullable(e.getProject())
        .ifPresent(it -> it.getService(ZhiweiViewerService.class).reload());
  }
}
