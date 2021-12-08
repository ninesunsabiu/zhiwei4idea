package cn.eziolin.zhiwei4idea.zhiweiviewer;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import io.vavr.control.Option;
import org.jetbrains.annotations.NotNull;

public class ReloadZhiweiViewerAction extends AnAction {
  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    Option.of(e.getProject())
        .map(it -> it.getService(ZhiweiViewerService.class))
        .flatMap(Option::of)
        .forEach(ZhiweiViewerService::reload);
  }
}
