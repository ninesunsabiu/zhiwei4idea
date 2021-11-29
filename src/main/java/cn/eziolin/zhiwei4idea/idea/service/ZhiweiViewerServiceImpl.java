package cn.eziolin.zhiwei4idea.idea.service;

import cn.eziolin.zhiwei4idea.api.ZhiweiApi;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.jcef.JBCefApp;
import com.intellij.ui.jcef.JBCefBrowser;
import org.cef.network.CefCookie;
import org.cef.network.CefCookieManager;

import javax.swing.*;
import java.net.HttpCookie;
import java.util.Optional;
import java.util.function.Consumer;

public class ZhiweiViewerServiceImpl implements ZhiweiViewerService, Disposable {

  private JBCefBrowser webView;

  public ZhiweiViewerServiceImpl() {
    if (JBCefApp.isSupported()) {
      ConfigSettingsState.getInstanceSafe()
          .flatMap(ConfigSettingsState::getDomainSafe)
          .map(JBCefBrowser::new)
          .ifPresentOrElse(
              (jbCefBrowser -> webView = jbCefBrowser), () -> webView = new JBCefBrowser());
    }
  }

  @Override
  public void setCookie(String cookieStr) {
    ConfigSettingsState.getInstanceSafe()
        .flatMap(ConfigSettingsState::getDomainSafe)
        .ifPresent(
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
  public Optional<JComponent> getWebViewComponent() {
    return Optional.ofNullable(webView).map(JBCefBrowser::getComponent);
  }

  @Override
  public void reload() {
    ZhiweiApi.initSdk(
        ((Consumer<String>) this::setCookie)
            .andThen(
                str ->
                    ConfigSettingsState.getInstanceSafe()
                        .flatMap(ConfigSettingsState::getDomainSafe)
                        .ifPresent(webView.getCefBrowser()::loadURL)));
  }

  @Override
  public void dispose() {
    Optional.ofNullable(webView).ifPresent(Disposer::dispose);
  }
}
