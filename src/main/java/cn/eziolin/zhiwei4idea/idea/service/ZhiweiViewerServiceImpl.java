package cn.eziolin.zhiwei4idea.idea.service;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.jcef.JBCefApp;
import com.intellij.ui.jcef.JBCefBrowser;
import org.cef.network.CefCookie;
import org.cef.network.CefCookieManager;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.net.HttpCookie;

public class ZhiweiViewerServiceImpl implements ZhiweiViewerService, Disposable {

    private JBCefBrowser webView;

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
    public void setUrl(String url) {

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
    public @NotNull JComponent getWebViewComponent() {
        return webView.getComponent();
    }

    @Override
    public void dispose() {
        Disposer.dispose(webView);
    }
}
