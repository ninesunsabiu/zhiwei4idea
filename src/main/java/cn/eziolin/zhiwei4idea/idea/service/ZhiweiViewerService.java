package cn.eziolin.zhiwei4idea.idea.service;

import javax.swing.*;
import java.util.Optional;

public interface ZhiweiViewerService {
    void setUrl(String url);

    void setCookie(String cookieStr);

    Optional<JComponent> getWebViewComponent();
}
