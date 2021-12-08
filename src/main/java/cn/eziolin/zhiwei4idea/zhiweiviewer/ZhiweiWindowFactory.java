package cn.eziolin.zhiwei4idea.zhiweiviewer;

import cn.eziolin.zhiwei4idea.api.ZhiweiService;
import cn.eziolin.zhiwei4idea.ramda.RamdaUtil;
import cn.eziolin.zhiwei4idea.setting.ConfigSettingsState;
import cn.eziolin.zhiwei4idea.setting.model.PluginConfig;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import io.vavr.control.Option;
import io.vavr.control.Validation;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class ZhiweiWindowFactory implements ToolWindowFactory {

  @Override
  public void init(@NotNull ToolWindow toolWindow) {
    toolWindow.setStripeTitle("ZhiweiViewer");
  }

  @Override
  public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
    Supplier<Option<ZhiweiViewerService>> getViewer =
        () -> {
          var viewer = project.getService(ZhiweiViewerService.class);
          return Option.of(viewer);
        };

    Consumer<String> addToToolWindow =
        url -> {
          var viewerMaybe = getViewer.get();
          var container = toolWindow.getComponent().getParent();
          viewerMaybe.flatMap(ZhiweiViewerService::getWebViewComponent).forEach(container::add);
        };

    Consumer<String> setCookie =
        url -> {
          var zhiweiService = ApplicationManager.getApplication().getService(ZhiweiService.class);
          var cookie = Option.of(zhiweiService).flatMap(i -> i.getCookie(url).toOption());
          var viewer = getViewer.get();
          Validation.combine(
                  cookie.toValidation(RamdaUtil.throwMsg.apply("没有Cookie")),
                  viewer.toValidation(RamdaUtil.throwMsg.apply("获取zhiweiViewer失败")))
              .ap(
                  (c, v) -> {
                    v.setCookie(c);
                    return RamdaUtil.ignore.get();
                  });
        };

    ConfigSettingsState.getPluginConfigSafe("tkb")
        .map(PluginConfig::getDomain)
        .flatMap(Option::of)
        .peek(setCookie)
        .peek(addToToolWindow);
  }
}
