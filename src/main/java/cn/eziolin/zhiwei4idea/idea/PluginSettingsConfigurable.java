package cn.eziolin.zhiwei4idea.idea;

import cn.eziolin.zhiwei4idea.ConfigSettingsUiComponent;
import cn.eziolin.zhiwei4idea.idea.service.ConfigSettingsState;
import com.intellij.openapi.options.Configurable;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

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
        boolean modify = !myUiComponent.getConfigPath().equals(settingsState.getConfigFilePath());
        modify |= !myUiComponent.getDomainText().equals(settingsState.getDomain());
        return modify;
    }

    @Override
    public void apply() {
        var settingState = ConfigSettingsState.getInstance();
        settingState.setConfigFilePath(myUiComponent.getConfigPath());
        settingState.setDomain(myUiComponent.getDomainText());
    }

    @Override
    public void reset() {
        var settingState = ConfigSettingsState.getInstance();
        settingState.getConfigFilePathSafe().ifPresent(myUiComponent::setConfigPath);
        settingState.getDomainSafe().ifPresent(myUiComponent::setDomainText);
    }

    @Override
    public void disposeUIResources() {
        myUiComponent = null;
    }
}
