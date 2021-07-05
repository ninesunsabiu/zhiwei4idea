package cn.eziolin.zhiwei4idea;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.vcs.CheckinProjectPanel;
import com.intellij.ui.EditorTextComponent;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.Collections;

public class CompletionServiceImpl implements CompletionService, Disposable {
    private WeakReference<CheckinProjectPanel> panelRef;

    @Override
    public void setCheckinProjectPanel(CheckinProjectPanel panel) {
        panelRef = new WeakReference<>(panel);
    }

    @Override
    public Boolean isCommitUiActive() {
        CheckinProjectPanel panel = panelRef.get();
        if (null == panel) {
            return false;
        }
        var component = panel.getPreferredFocusedComponent();
        return component instanceof EditorTextComponent && ((EditorTextComponent) component).getComponent().isFocusOwner();
    }

    @Override
    public Collection<File> getAffectedFiles() {
        CheckinProjectPanel panel = panelRef.get();
        if (panel != null && isCommitUiActive()) {
            return panel.getFiles();
        }
        return Collections.emptyList();
    }

    @Override
    public void dispose() {
        panelRef = null;
    }
}
