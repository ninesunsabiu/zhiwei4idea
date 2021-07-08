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
        storages = {@Storage("zhiwei_for_idea.xml")}
)
public class ConfigSettingsState implements PersistentStateComponent<ConfigSettingsState> {
    public String configFilePath;
    private PluginConfig pluginConfig;

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
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        Optional.ofNullable(this.configFilePath).ifPresent(
                (it) -> {
                    String path = it.replaceAll("^~", System.getProperty("user.home"));
                    try {
                        File configYaml = new File(path);
                        // create config file if not exist
                        var created = configYaml.createNewFile();
                        if (!created) {
                            pluginConfig = mapper.readValue(new File(path), PluginConfig.class);
                        }
                    } catch (Exception e) {
                        pluginConfig = null;
                    }
                }
        );
    }

    public Optional<PluginConfig> getPluginConfig() {
        return Optional.ofNullable(pluginConfig);
    }
}
