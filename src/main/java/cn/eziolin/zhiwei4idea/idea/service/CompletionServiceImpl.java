package cn.eziolin.zhiwei4idea.idea.service;

import cn.eziolin.zhiwei4idea.api.ZhiweiApi;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.vcs.CheckinProjectPanel;
import com.intellij.ui.AncestorListenerAdapter;
import com.intellij.ui.EditorTextComponent;

import javax.swing.event.AncestorEvent;
import java.io.File;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

public class CompletionServiceImpl implements CompletionService, Disposable {
  private final AncestorListenerAdapter myWhenCheckinPanelShownChange =
      new AncestorListenerAdapter() {
        @Override
        public void ancestorAdded(AncestorEvent event) {
          ZhiweiApi.initSdk();
        }
      };

  private WeakReference<CheckinProjectPanel> myPanelRef = null;

  private Optional<CheckinProjectPanel> getCheckinProjectPanel() {
    return Optional.ofNullable(myPanelRef).map(Reference::get);
  }

  @Override
  public void setCheckinProjectPanel(CheckinProjectPanel panel) {
    panel.getPreferredFocusedComponent().addAncestorListener(myWhenCheckinPanelShownChange);
    myPanelRef = new WeakReference<>(panel);
  }

  @Override
  public Boolean isCommitUiActive() {
    return getCheckinProjectPanel()
        .map(
            it -> {
              var component = it.getPreferredFocusedComponent();
              return component instanceof EditorTextComponent
                  && ((EditorTextComponent) component).getComponent().isFocusOwner();
            })
        .orElse(false);
  }

  @Override
  public Collection<File> getAffectedFiles() {
    return getCheckinProjectPanel()
        .map(it -> isCommitUiActive() ? it.getFiles() : null)
        .orElse(Collections.emptyList());
  }

  @Override
  public void dispose() {
    getCheckinProjectPanel()
        .ifPresent(
            it ->
                it.getPreferredFocusedComponent()
                    .removeAncestorListener(myWhenCheckinPanelShownChange));

    myPanelRef = null;
  }
}
