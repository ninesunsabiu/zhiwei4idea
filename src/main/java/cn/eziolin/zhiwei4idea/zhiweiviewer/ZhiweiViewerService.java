package cn.eziolin.zhiwei4idea.zhiweiviewer;

import io.vavr.control.Option;

import javax.swing.*;

public interface ZhiweiViewerService {
  void setCookie(String cookieStr);

  Option<JComponent> getWebViewComponent();

  void reload();
}
