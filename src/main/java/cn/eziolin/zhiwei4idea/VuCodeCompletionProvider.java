package cn.eziolin.zhiwei4idea;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.openapi.project.Project;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

public class VuCodeCompletionProvider extends CompletionProvider<CompletionParameters> {

    private Project getProject(CompletionParameters parameters) {
        return parameters.getPosition().getProject();
    }

    @Override
    protected void addCompletions(@NotNull CompletionParameters parameters,
                                  @NotNull ProcessingContext context,
                                  @NotNull CompletionResultSet result) {
        if (CheckinProjectPanelHolder.getInstance().isCommitUiActive()) {
            System.out.println("Congratulations it works!");
        }
    }
}
