package cn.eziolin.zhiwei4idea.completion;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.CheckinProjectPanel;
import io.vavr.control.Option;

import java.io.File;
import java.util.Collection;

public interface CompletionService {
  /** 持有一个 check in project panel 为了计算得到此时是否在 commit dialogue */
  void setCheckinProjectPanel(CheckinProjectPanel panel);

  /** 此时是否 focus 在 commit 对话框中 */
  Boolean isCommitUiActive();

  /** 返回此时 commit 对话框中确认改动的文件集合 */
  Collection<File> getAffectedFiles();

  static Option<CompletionService> getInstance(Project project) {
    return Option.of(project).map(it -> it.getService(CompletionService.class)).flatMap(Option::of);
  }
}
