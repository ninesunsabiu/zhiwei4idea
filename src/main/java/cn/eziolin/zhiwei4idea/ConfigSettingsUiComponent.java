package cn.eziolin.zhiwei4idea;

import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class ConfigSettingsUiComponent {
    private final JPanel myMainPanel;
    private final JBTextField myConfigPathText = new JBTextField();

    public ConfigSettingsUiComponent() {
        myMainPanel = FormBuilder.createFormBuilder()
                .addLabeledComponent(
                        new JLabel("config path"),
                        myConfigPathText,
                        1,
                        false
                )
                .addComponentFillVertically(new JPanel(), 0).getPanel();
    }

    public JComponent getMainPanel() {
        return myMainPanel;
    }

    public JComponent getTextField() {
        return myConfigPathText;
    }

    @NotNull
    public String getConfigPath() {
        return myConfigPathText.getText();
    }

    public void setConfigPath(@NotNull String newText) {
        myConfigPathText.setText(newText);
    }
}
