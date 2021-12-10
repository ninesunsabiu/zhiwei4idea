package cn.eziolin.zhiwei4idea.searchId;

import cn.eziolin.zhiwei4idea.ramda.RamdaUtil;
import cn.eziolin.zhiwei4idea.setting.ConfigSettingsState;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.psi.PsiManager;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import io.vavr.Tuple;
import io.vavr.control.Option;
import io.vavr.control.Try;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

public class SearchEverythingById extends AnAction {

  public void searchZhiweiEntityById(Project project, String env, String id) {

    new Task.Backgroundable(project, "Searching zhiwei entity", true) {

      @Override
      public void run(@NotNull ProgressIndicator indicator) {
        indicator.setIndeterminate(false);
        indicator.setFraction(0.0);
        Consumer<Object> done = ignored -> indicator.setFraction(1.0);

        var zhiweiService = RamdaUtil.getZhiweiService.get();
        var config = ConfigSettingsState.getPluginConfigSafe(env).toValidation(() -> "不存在对应的配置");

        zhiweiService
            .combine(config)
            .ap(
                (service, configNotNil) -> {
                  return service.searchIdForEverything(configNotNil, id);
                })
            .peekError(
                error -> {
                  Tuple.of(
                          "Zhiwei Search Id",
                          RamdaUtil.joinStrWith.apply("\t").apply(error),
                          NotificationType.ERROR)
                      .apply(RamdaUtil.showMessage);
                })
            .peek(done, done)
            .forEach(
                tryGetResponse -> {
                  tryGetResponse.forEach(
                      entityContent -> {
                        Try.of(
                            () -> {
                              var file = FileUtil.createTempFile("response", ".json", true);

                              FileUtil.writeToFile(
                                  file, entityContent.getBytes(StandardCharsets.UTF_8));

                              Option.of(VfsUtil.findFileByIoFile(file, true))
                                  .forEach(
                                      vFile -> {
                                        ApplicationManager.getApplication()
                                            .invokeLater(
                                                () -> {
                                                  FileEditorManager.getInstance(project)
                                                      .openFile(vFile, true);

                                                  Option.of(
                                                          PsiManager.getInstance(project)
                                                              .findFile(vFile))
                                                      .forEach(
                                                          (it) ->
                                                              WriteCommandAction
                                                                  .runWriteCommandAction(
                                                                      project,
                                                                      "Reformat",
                                                                      "reformat",
                                                                      () ->
                                                                          CodeStyleManager
                                                                              .getInstance(project)
                                                                              .reformat(it)));
                                                });
                                      });
                              return Option.<Void>none();
                            });
                      });
                });
      }
    }.setCancelText("Cancel Searching").queue();
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    var project = e.getProject();

    class IdInputDialog extends DialogWrapper {

      private final JBTextField myIdTextInput = new JBTextField();
      private final ComboBox<String> myComboBox = new ComboBox<>();
      private final SearchEverythingById delegate;
      private final Project project;

      public IdInputDialog(SearchEverythingById delegate, Project proj) {
        super(true); // use current window as parent
        this.delegate = delegate;
        this.project = proj;
        setTitle("Id Searching");
        RamdaUtil.getAllPluginConfig
            .get()
            .forEach(
                map -> {
                  myComboBox.setModel(
                      new DefaultComboBoxModel<>(map.keySet().toJavaArray(String[]::new)));
                });
        init();
      }

      @Nullable
      @Override
      protected JComponent createCenterPanel() {
        return FormBuilder.createFormBuilder()
            .addLabeledComponent(new JLabel("id"), myIdTextInput)
            .addLabeledComponent(new JLabel("env"), myComboBox)
            .getPanel();
      }

      @Override
      protected void doOKAction() {
        super.doOKAction();
        delegate.searchZhiweiEntityById(project, myComboBox.getItem(), myIdTextInput.getText());
      }
    }

    var myDialog = new IdInputDialog(this, project);
    myDialog.setSize(300, -1);
    myDialog.show();
  }
}
