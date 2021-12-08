package cn.eziolin.zhiwei4idea.zhiweiviewer;

import cn.eziolin.zhiwei4idea.api.ZhiweiService;
import cn.eziolin.zhiwei4idea.ramda.RamdaUtil;
import cn.eziolin.zhiwei4idea.setting.ConfigSettingsState;
import cn.eziolin.zhiwei4idea.setting.model.PluginConfig;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.jcef.JBCefApp;
import com.intellij.ui.jcef.JBCefBrowser;
import io.vavr.control.Option;
import io.vavr.control.Validation;
import org.cef.network.CefCookie;
import org.cef.network.CefCookieManager;

import javax.swing.*;
import java.net.HttpCookie;

public class ZhiweiViewerServiceImpl implements ZhiweiViewerService, Disposable {

  private JBCefBrowser webView;

  private Option<PluginConfig> getTkbConfig() {
    return ConfigSettingsState.getPluginConfigSafe("tkb");
  }

  private Option<String> getTkbUrl() {
    return getTkbConfig().map(PluginConfig::getDomain).flatMap(Option::of);
  }

  public ZhiweiViewerServiceImpl() {
    Validation.combine(
            ConfigSettingsState.getInstanceSafe()
                .fold(
                    () -> Validation.invalid(new Error("does not set domain")), Validation::valid),
            JBCefApp.isSupported()
                ? Validation.valid(true)
                : Validation.invalid(new Error("Does not Support JCEF")))
        .ap((state, _support) -> getTkbUrl().fold(JBCefBrowser::new, JBCefBrowser::new))
        .forEach(jbCefBrowser -> webView = jbCefBrowser);
  }

  @Override
  public void setCookie(String cookieStr) {
    getTkbUrl()
        .forEach(
            domain ->
                HttpCookie.parse("set-cookie:" + cookieStr)
                    .forEach(
                        cookie ->
                            CefCookieManager.getGlobalManager()
                                .setCookie(
                                    domain,
                                    new CefCookie(
                                        cookie.getName(),
                                        cookie.getValue(),
                                        cookie.getDomain(),
                                        cookie.getPath(),
                                        cookie.getSecure(),
                                        cookie.isHttpOnly(),
                                        new java.util.Date(),
                                        new java.util.Date(),
                                        cookie.hasExpired(),
                                        new java.util.Date(
                                            System.currentTimeMillis() + cookie.getMaxAge())))));
  }

  @Override
  public Option<JComponent> getWebViewComponent() {
    return Option.of(webView).map(JBCefBrowser::getComponent);
  }

  @Override
  public void reload() {
    var zhiweiService = ApplicationManager.getApplication().getService(ZhiweiService.class);
    var loginFn = RamdaUtil.loginWithConfig.curried();
    getTkbConfig()
        .peek(
            config -> {
              Option.of(config)
                  .toValidation(() -> "tkb 配置不存在")
                  .flatMap(RamdaUtil.pluginConfigValidator)
                  .forEach(
                      c -> {
                        webView.getCefBrowser().loadURL(c._1);
                      });
            })
        .map(loginFn)
        .map(fn -> fn.apply(zhiweiService))
        .forEach(
            cookieRet -> {
              cookieRet.peek(err -> {}, this::setCookie);
            });
  }

  @Override
  public void dispose() {
    Option.of(webView).forEach(Disposer::dispose);
  }
}
