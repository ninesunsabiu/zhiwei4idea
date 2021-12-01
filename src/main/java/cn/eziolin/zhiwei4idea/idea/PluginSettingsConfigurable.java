package cn.eziolin.zhiwei4idea.idea;

import cn.eziolin.zhiwei4idea.idea.service.ConfigSettingsState;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.UnnamedConfigurable;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import io.vavr.Tuple;
import io.vavr.collection.List;
import io.vavr.control.Option;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.function.Consumer;

class ConfigSettingsUiComponent implements UnnamedConfigurable {
  private final JPanel myMainPanel;
  private final JBTextField myConfigPathText = new JBTextField();
  private final JBTextField myDomainText = new JBTextField();

  public ConfigSettingsUiComponent() {
    myMainPanel =
        FormBuilder.createFormBuilder()
            .addLabeledComponent(new JLabel("config path"), myConfigPathText, 1, false)
            .addLabeledComponent(new JLabel("host url"), myDomainText, 1, false)
            .addComponentFillVertically(new JPanel(), 0)
            .getPanel();
  }

  public String getConfigPath() {
    return myConfigPathText.getText();
  }

  public void setConfigPath(String newText) {
    myConfigPathText.setText(newText);
  }

  public String getDomain() {
    return myDomainText.getText();
  }

  public void setDomainText(String newDomain) {
    myDomainText.setText(newDomain);
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
                state ->
                    getConfigPath().equals(state.getConfigFilePath())
                        && getDomain().equals(state.getDomain()))
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
              state.setDomain(getDomain());
            });
  }

  @Override
  public void reset() {
    ConfigSettingsState.getInstanceSafe()
        .forEach(
            (it) ->
                List.of(
                        Tuple.of(it.getConfigFilePath(), (Consumer<String>) this::setConfigPath),
                        Tuple.of(it.getDomain(), (Consumer<String>) this::setDomainText))
                    .forEach((data) -> Option.of(data._1).forEach(data._2)));
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
