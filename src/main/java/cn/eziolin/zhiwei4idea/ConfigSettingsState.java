package cn.eziolin.zhiwei4idea;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@State(
        name = "cn.eziolin.zhiwei4idea.ConfigSettingsState",
        storages = {@Storage("zhiwei_for_idea.xml")}
)
public class ConfigSettingsState implements PersistentStateComponent<ConfigSettingsState> {
    public String configFilePath;

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
}
