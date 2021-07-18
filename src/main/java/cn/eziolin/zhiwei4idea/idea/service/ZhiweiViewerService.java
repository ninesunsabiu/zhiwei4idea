package cn.eziolin.zhiwei4idea.idea.service;

import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public interface ZhiweiViewerService {
    void setUrl(String url);

    void setCookie(String cookieStr);

    @NotNull
    JComponent getWebViewComponent();
}
