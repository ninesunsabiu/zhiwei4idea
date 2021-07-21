package cn.eziolin.zhiwei4idea.idea.service;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.jcef.JBCefApp;
import com.intellij.ui.jcef.JBCefBrowser;
import org.cef.network.CefCookie;
import org.cef.network.CefCookieManager;

import javax.swing.*;
import java.net.HttpCookie;
import java.util.Optional;

public class ZhiweiViewerServiceImpl implements ZhiweiViewerService, Disposable {

    private JBCefBrowser webView = null;

    public ZhiweiViewerServiceImpl() {
        if (JBCefApp.isSupported()) {
            var defaultUrl = ConfigSettingsState.getInstance().getDomainSafe();

            defaultUrl.map(JBCefBrowser::new)
                    .ifPresentOrElse(
                            (jbCefBrowser -> webView = jbCefBrowser),
                            () -> webView = new JBCefBrowser()
                    );
        }
    }

    @Override
    public void setCookie(String cookieStr) {
        var domainOptional = ConfigSettingsState.getInstance().getDomainSafe();
        domainOptional.ifPresent(
                domain -> HttpCookie.parse("set-cookie:" + cookieStr).forEach(
                        cookie -> CefCookieManager.getGlobalManager().setCookie(
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
                                                System.currentTimeMillis() + cookie.getMaxAge()
                                        )
                                )
                        )
                )
        );
    }

    @Override
    public Optional<JComponent> getWebViewComponent() {
        return Optional.ofNullable(webView).map(JBCefBrowser::getComponent);
    }

    @Override
    public void reload() {
        webView.getCefBrowser().reload();
    }

    @Override
    public void dispose() {
        Optional.ofNullable(webView).ifPresent(Disposer::dispose);
    }
}
