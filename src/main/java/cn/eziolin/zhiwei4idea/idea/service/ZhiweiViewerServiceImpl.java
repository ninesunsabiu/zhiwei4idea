package cn.eziolin.zhiwei4idea.idea.service;

import cn.eziolin.zhiwei4idea.api.ZhiweiApi;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.jcef.JBCefApp;
import com.intellij.ui.jcef.JBCefBrowser;
import io.vavr.control.Validation;
import org.cef.network.CefCookie;
import org.cef.network.CefCookieManager;

import javax.swing.*;
import java.net.HttpCookie;
import java.util.Optional;
import java.util.function.Consumer;

public class ZhiweiViewerServiceImpl implements ZhiweiViewerService, Disposable {

  private JBCefBrowser webView;

  public ZhiweiViewerServiceImpl() {
    Validation.combine(
            ConfigSettingsState.getInstanceSafe()
                .fold(
                    () -> Validation.invalid(new Error("does not set domain")), Validation::valid),
            JBCefApp.isSupported()
                ? Validation.valid(true)
                : Validation.invalid(new Error("Does not Support JCEF")))
        .ap(
            (state, _support) ->
                ConfigSettingsState.getDomainSafe().fold(JBCefBrowser::new, JBCefBrowser::new))
        .forEach(jbCefBrowser -> webView = jbCefBrowser);
  }

  @Override
  public void setCookie(String cookieStr) {
    ConfigSettingsState.getDomainSafe()
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
  public Optional<JComponent> getWebViewComponent() {
    return Optional.ofNullable(webView).map(JBCefBrowser::getComponent);
  }

  @Override
  public void reload() {
    ZhiweiApi.initSdk(
        ((Consumer<String>) this::setCookie)
            .andThen(
                str ->
                    ConfigSettingsState.getDomainSafe().forEach(webView.getCefBrowser()::loadURL)));
  }

  @Override
  public void dispose() {
    Optional.ofNullable(webView).ifPresent(Disposer::dispose);
  }
}
