package cn.eziolin.zhiwei4idea.setting;

import cn.eziolin.zhiwei4idea.setting.model.PluginConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import io.vavr.control.Option;
import io.vavr.control.Try;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;

@State(
    name = "cn.eziolin.zhiwei4idea.setting.ConfigSettingsState",
    storages = {@Storage("zhiwei_for_idea.xml")})
public class ConfigSettingsState implements PersistentStateComponent<ConfigSettingsState> {
  private String myConfigFilePath;
  private String myDomain;
  private PluginConfig myPluginConfig;

  @Override
  public @Nullable ConfigSettingsState getState() {
    return this;
  }

  @Override
  public void loadState(@NotNull ConfigSettingsState state) {
    XmlSerializerUtil.copyBean(state, this);
  }

  public @Nullable String getConfigFilePath() {
    return myConfigFilePath;
  }

  public void setConfigFilePath(@NotNull String configFilePath) {
    setConfigPathAndTryLoadPersistent(configFilePath);
  }

  public Try<PluginConfig> setConfigPathAndTryLoadPersistent(@NotNull String path) {
    this.myConfigFilePath = path;
    return readConfigYamlToPluginConfig(path).peek(this::setPluginConfig);
  }

  public @Nullable String getDomain() {
    return myDomain;
  }

  public void setDomain(@NotNull String domain) {
    this.myDomain = domain;
  }

  private void setPluginConfig(@NotNull PluginConfig pluginConfig) {
    this.myPluginConfig = pluginConfig;
  }

  private Try<PluginConfig> readConfigYamlToPluginConfig(String configFilePath) {
    return Option.of(configFilePath)
        .flatMap(
            s -> {
              String path = s.replaceAll("^~", System.getProperty("user.home"));
              File configYaml = new File(path);
              return configYaml.isFile() ? Option.some(configYaml) : Option.none();
            })
        .map(Try::success)
        .getOrElse(Try.failure(new Error("找不到配置文件: " + configFilePath)))
        .mapTry(file -> new ObjectMapper(new YAMLFactory()).readValue(file, PluginConfig.class));
  }

  public static Option<ConfigSettingsState> getInstanceSafe() {
    return Option.of(
        ApplicationManager.getApplication().getService(ConfigSettingsState.class).getState());
  }

  public static Option<PluginConfig> getPluginConfigSafe() {
    return getInstanceSafe().flatMap(state -> Option.of(state.myPluginConfig));
  }

  public static Option<String> getPersistentCookie() {
    return getPluginConfigSafe().flatMap(config -> Option.of(config.getCookie()));
  }

  public static Option<String> getDomainSafe() {
    return getInstanceSafe().flatMap(state -> Option.of(state.myDomain));
  }
}
