package cn.eziolin.zhiwei4idea.idea.service;

import cn.eziolin.zhiwei4idea.api.ZhiweiApi;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.vcs.CheckinProjectPanel;
import com.intellij.ui.AncestorListenerAdapter;
import com.intellij.ui.EditorTextComponent;

import javax.swing.event.AncestorEvent;
import java.io.File;
import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

public class CompletionServiceImpl implements CompletionService, Disposable {
  private final AncestorListenerAdapter whenCheckinPanelShownChange =
      new AncestorListenerAdapter() {
        @Override
        public void ancestorAdded(AncestorEvent event) {
          ZhiweiApi.initSdk();
        }
      };
  private WeakReference<CheckinProjectPanel> panelRef;

  @Override
  public void setCheckinProjectPanel(CheckinProjectPanel panel) {
    panel.getPreferredFocusedComponent().addAncestorListener(whenCheckinPanelShownChange);
    panelRef = new WeakReference<>(panel);
  }

  @Override
  public Boolean isCommitUiActive() {
    CheckinProjectPanel panel = panelRef.get();
    if (null == panel) {
      return false;
    }
    var component = panel.getPreferredFocusedComponent();
    return component instanceof EditorTextComponent
        && ((EditorTextComponent) component).getComponent().isFocusOwner();
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
    Optional.ofNullable(panelRef.get())
        .ifPresent(
            it ->
                it.getPreferredFocusedComponent()
                    .removeAncestorListener(whenCheckinPanelShownChange));
    panelRef = null;
  }
}
