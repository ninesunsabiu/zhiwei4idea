package cn.eziolin.zhiwei4idea.idea.service;

import cn.eziolin.zhiwei4idea.model.PluginConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Optional;

@State(
    name = "cn.eziolin.zhiwei4idea.idea.service.ConfigSettingsState",
    storages = {@Storage("zhiwei_for_idea.xml")})
public class ConfigSettingsState implements PersistentStateComponent<ConfigSettingsState> {
  private String configFilePath;
  private String domain;
  private PluginConfig pluginConfig;

  private static Optional<PluginConfig> loadPluginConfigWhenPathChange(String configFilePath) {
    return Optional.ofNullable(configFilePath)
        .map(
            (it) -> {
              String path = it.replaceAll("^~", System.getProperty("user.home"));
              try {
                File configYaml = new File(path);
                // create config file if not exist
                var created = configYaml.createNewFile();
                if (!created) {
                  ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
                  return mapper.readValue(new File(path), PluginConfig.class);
                }
                return null;
              } catch (Exception e) {
                return null;
              }
            });
  }

  public static ConfigSettingsState getInstance() {
    return ApplicationManager.getApplication().getService(ConfigSettingsState.class);
  }

  @Override
  public @Nullable ConfigSettingsState getState() {
    return this;
  }

  @Override
  public void loadState(@NotNull ConfigSettingsState state) {
    XmlSerializerUtil.copyBean(state, this);
  }

  public Optional<PluginConfig> getPluginConfig() {
    return Optional.ofNullable(pluginConfig);
  }

  public String getConfigFilePath() {
    return configFilePath;
  }

  public void setConfigFilePath(String configFilePath) {
    this.configFilePath = configFilePath;
    loadPluginConfigWhenPathChange(configFilePath).ifPresent(config -> this.pluginConfig = config);
  }

  public Optional<String> getConfigFilePathSafe() {
    return Optional.ofNullable(configFilePath);
  }

  public String getDomain() {
    return domain;
  }

  public void setDomain(String domain) {
    this.domain = domain;
  }

  public Optional<String> getDomainSafe() {
    return Optional.ofNullable(domain);
  }

  public void saveCookie(String cookieStr) {
    getPluginConfig().ifPresent(it -> it.setCookie(cookieStr));
  }
}
