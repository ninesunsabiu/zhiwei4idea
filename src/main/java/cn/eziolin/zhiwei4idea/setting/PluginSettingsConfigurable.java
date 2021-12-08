package cn.eziolin.zhiwei4idea.setting;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.UnnamedConfigurable;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

class ConfigSettingsUiComponent implements UnnamedConfigurable {
  private final JPanel myMainPanel;
  private final JBTextField myConfigPathText = new JBTextField();

  public ConfigSettingsUiComponent() {
    myMainPanel =
        FormBuilder.createFormBuilder()
            .addLabeledComponent(new JLabel("config path"), myConfigPathText, 1, false)
            .addComponentFillVertically(new JPanel(), 0)
            .getPanel();
  }

  public String getConfigPath() {
    return myConfigPathText.getText();
  }

  public void setConfigPath(String newText) {
    myConfigPathText.setText(newText);
  }

  @Override
  public @Nullable JComponent createComponent() {
    return myMainPanel;
  }

  @Override
  public boolean isModified() {
    var sameAsPersistent =
        ConfigSettingsState.getInstanceSafe()
            .map(
                state -> {
                  return getConfigPath().equals(state.getConfigFilePath());
                })
            .getOrElse(false);
    return !sameAsPersistent;
  }

  @Override
  public void apply() {
    ConfigSettingsState.getInstanceSafe()
        .forEach(
            state -> {
              state
                  .setConfigPathAndTryLoadPersistent(getConfigPath())
                  .onFailure(
                      throwable ->
                          Notifications.Bus.notify(
                              new Notification(
                                  "Zhiwei Notification",
                                  "Zhiwei",
                                  "加载 yaml 配置文件错误"
                                      + "\n"
                                      + "部分功能将无法正常使用"
                                      + "\n"
                                      + throwable.getMessage(),
                                  NotificationType.ERROR)));
            });
  }

  @Override
  public void reset() {
    ConfigSettingsState.getInstanceSafe()
        .forEach(
            (it) -> {
              setConfigPath(it.getConfigFilePath());
            });
  }
}

public class PluginSettingsConfigurable implements Configurable {
  private ConfigSettingsUiComponent delegate = new ConfigSettingsUiComponent();

  @Nls(capitalization = Nls.Capitalization.Title)
  @Override
  public String getDisplayName() {
    return "Zhiwei4Idea";
  }

  @Override
  public @Nullable JComponent createComponent() {
    return delegate.createComponent();
  }

  @Override
  public boolean isModified() {
    return delegate.isModified();
  }

  @Override
  public void apply() {
    delegate.apply();
  }

  @Override
  public void reset() {
    delegate.reset();
  }

  @Override
  public void disposeUIResources() {
    delegate = null;
  }
}
