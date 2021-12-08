package cn.eziolin.zhiwei4idea.completion;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.vcs.CheckinProjectPanel;
import com.intellij.ui.EditorTextComponent;
import io.vavr.control.Option;

import java.io.File;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.Collections;

public class CompletionServiceImpl implements CompletionService, Disposable {
  private WeakReference<CheckinProjectPanel> myPanelRef = null;

  private Option<CheckinProjectPanel> getCheckinProjectPanel() {
    return Option.of(myPanelRef).map(Reference::get);
  }

  @Override
  public void setCheckinProjectPanel(CheckinProjectPanel panel) {
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
        .getOrElse(() -> false);
  }

  @Override
  public Collection<File> getAffectedFiles() {
    return getCheckinProjectPanel()
        .map(it -> isCommitUiActive() ? it.getFiles() : null)
        .getOrElse(Collections::emptyList);
  }

  @Override
  public void dispose() {
    myPanelRef = null;
  }
}
