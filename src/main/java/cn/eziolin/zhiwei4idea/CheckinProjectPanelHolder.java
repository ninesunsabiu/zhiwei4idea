package cn.eziolin.zhiwei4idea;

import com.intellij.openapi.vcs.CheckinProjectPanel;
import com.intellij.ui.EditorTextComponent;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.Collections;

public class CheckinProjectPanelHolder {
    private static CheckinProjectPanelHolder singleInstance = null;
    static CheckinProjectPanelHolder getInstance() {
        if (singleInstance == null) {
            singleInstance = new CheckinProjectPanelHolder();
        }
        return singleInstance;
    }

    WeakReference<CheckinProjectPanel> panelRef;

    public CheckinProjectPanel getPanel() {
        return this.panelRef.get();
    }

    public void setPanel(CheckinProjectPanel panelRef) {
        this.panelRef = new WeakReference<>(panelRef);
    }

    public Boolean isCommitUiActive() {
        CheckinProjectPanel panel = getPanel();
        if (null == panel) {
            return false;
        }
        var component = panel.getPreferredFocusedComponent();
        return component instanceof EditorTextComponent && ((EditorTextComponent) component).getComponent().isFocusOwner();
    }

    public Collection<File> getAffectedFiles() {
        CheckinProjectPanel panel = getPanel();
        if (isCommitUiActive()) {
            return panel.getFiles();
        }
        return Collections.emptyList();
    }

    private CheckinProjectPanelHolder() {}

}
