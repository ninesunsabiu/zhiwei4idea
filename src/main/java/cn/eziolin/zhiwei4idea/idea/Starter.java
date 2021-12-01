package cn.eziolin.zhiwei4idea.idea;

import cn.eziolin.zhiwei4idea.api.ZhiweiApi;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManagerListener;
import org.jetbrains.annotations.NotNull;

public class Starter implements ProjectManagerListener {
  @Override
  public void projectOpened(@NotNull Project project) {
    ZhiweiApi.initSdk();
  }
}
