package cn.eziolin.zhiwei4idea.idea;

import cn.eziolin.zhiwei4idea.api.ZhiweiApi;
import cn.eziolin.zhiwei4idea.idea.service.ZhiweiViewerService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import org.jetbrains.annotations.NotNull;

public class ZhiweiWindowFactory implements ToolWindowFactory {

  @Override
  public void init(@NotNull ToolWindow toolWindow) {
    toolWindow.setStripeTitle("ZhiweiViewer");
  }

  @Override
  public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
    var component = toolWindow.getComponent();
    var parentContainer = component.getParent();
    var viewerService = project.getService(ZhiweiViewerService.class);

    ZhiweiApi.initSdk(viewerService::setCookie);
    viewerService.getWebViewComponent().ifPresent(parentContainer::add);
  }
}
