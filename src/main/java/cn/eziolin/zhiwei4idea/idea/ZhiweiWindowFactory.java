package cn.eziolin.zhiwei4idea.idea;

import cn.eziolin.zhiwei4idea.api.ZhiweiApi;
import cn.eziolin.zhiwei4idea.idea.service.ZhiweiViewerService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class ZhiweiWindowFactory implements ToolWindowFactory {

  @Override
  public void init(@NotNull ToolWindow toolWindow) {
    toolWindow.setStripeTitle("ZhiweiViewer");
  }

  @Override
  public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
    Optional.ofNullable(project.getService(ZhiweiViewerService.class))
        .ifPresent(
            zhiweiViewerService -> {
              ZhiweiApi.initSdk(zhiweiViewerService::setCookie);

              zhiweiViewerService
                  .getWebViewComponent()
                  .ifPresent(toolWindow.getComponent().getParent()::add);
            });
  }
}
