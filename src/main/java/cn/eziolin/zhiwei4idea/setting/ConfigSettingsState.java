package cn.eziolin.zhiwei4idea.setting;

import cn.eziolin.zhiwei4idea.setting.model.PluginConfig;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import io.vavr.collection.HashMap;
import io.vavr.collection.Map;
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
  private Map<String, PluginConfig> myPluginConfig;

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

  public Try<Map<String, PluginConfig>> setConfigPathAndTryLoadPersistent(@NotNull String path) {
    this.myConfigFilePath = path;
    return readConfigYamlToPluginConfig(path).peek(this::setPluginConfig);
  }

  private void setPluginConfig(@NotNull Map<String, PluginConfig> pluginConfig) {
    this.myPluginConfig = pluginConfig;
  }

  public @Nullable Map<String, PluginConfig> getPluginConfig() {
    return this.myPluginConfig;
  }

  private Try<Map<String, PluginConfig>> readConfigYamlToPluginConfig(String configFilePath) {
    return Option.of(configFilePath)
        .flatMap(
            s -> {
              String path = s.replaceAll("^~", System.getProperty("user.home"));
              File configYaml = new File(path);
              return configYaml.isFile() ? Option.some(configYaml) : Option.none();
            })
        .map(Try::success)
        .getOrElse(Try.failure(new Error("找不到配置文件: " + configFilePath)))
        .mapTry(
            file -> {
              var objectMapper = new ObjectMapper(new YAMLFactory());
              JavaType type =
                  objectMapper
                      .getTypeFactory()
                      .constructParametricType(
                          java.util.Map.class, String.class, PluginConfig.class);
              java.util.Map<String, PluginConfig> t = objectMapper.readValue(file, type);
              return HashMap.ofAll(t);
            });
  }

  public static Option<ConfigSettingsState> getInstanceSafe() {
    return Option.of(
        ApplicationManager.getApplication().getService(ConfigSettingsState.class).getState());
  }

  public static Option<PluginConfig> getPluginConfigSafe(@NotNull String env) {
    return getInstanceSafe()
        .flatMap(state -> Option.of(state.getPluginConfig()))
        .flatMap(it -> it.get(env));
  }
}
