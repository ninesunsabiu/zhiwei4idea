package cn.eziolin.zhiwei4idea;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Pattern;

public class VuCodeCompletionProvider extends CompletionProvider<CompletionParameters> {

    private static Pattern pattern = Pattern.compile("card::([\\S]+)$");

    @Override
    protected void addCompletions(@NotNull CompletionParameters parameters,
                                  @NotNull ProcessingContext context,
                                  @NotNull CompletionResultSet result) {
        if (!CheckinProjectPanelHolder.getInstance().getAffectedFiles().isEmpty()) {
            String beforeInput = getBeforeInputString(parameters);
            addCards(beforeInput, result);
        }
    }

    private String getBeforeInputString(@NotNull CompletionParameters parameters) {
        var position = parameters.getPosition();
        int offsetInFile = parameters.getOffset();
        return position.getText().substring(0, offsetInFile);
    }


    private void addCards(String beforeInput, CompletionResultSet result) {
        var matcher = pattern.matcher(beforeInput);
        if (matcher.find()) {
            var searchKeyword = matcher.group(1);
            System.out.println(searchKeyword);
        }
    }
}
