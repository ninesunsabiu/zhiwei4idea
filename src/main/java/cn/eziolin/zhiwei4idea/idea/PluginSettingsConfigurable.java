package cn.eziolin.zhiwei4idea.idea;

import cn.eziolin.zhiwei4idea.ConfigSettingsUiComponent;
import cn.eziolin.zhiwei4idea.idea.service.ConfigSettingsState;
import com.intellij.openapi.options.Configurable;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Optional;

public class PluginSettingsConfigurable implements Configurable {
    private ConfigSettingsUiComponent myUiComponent;

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "Zhiwei4Idea";
    }

    @Override
    public JComponent getPreferredFocusedComponent() {
        return myUiComponent.getTextField();
    }

    @Override
    public @Nullable JComponent createComponent() {
        myUiComponent = new ConfigSettingsUiComponent();
        return myUiComponent.getMainPanel();
    }

    @Override
    public boolean isModified() {
        ConfigSettingsState settingsState = ConfigSettingsState.getInstance();
        return !myUiComponent.getConfigPath().equals(settingsState.configFilePath);
    }

    @Override
    public void apply() {
        var settingState = ConfigSettingsState.getInstance();
        settingState.configFilePath = myUiComponent.getConfigPath();
    }

    @Override
    public void reset() {
        var settingState = ConfigSettingsState.getInstance();
        Optional
                .ofNullable(settingState.configFilePath)
                .ifPresent(
                        (it) -> myUiComponent.setConfigPath(it)
                );
    }

    @Override
    public void disposeUIResources() {
        myUiComponent = null;
    }
}
