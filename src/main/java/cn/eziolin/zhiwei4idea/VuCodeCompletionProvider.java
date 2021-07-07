package cn.eziolin.zhiwei4idea;

import cn.eziolin.zhiwei4idea.api.ZhiweiApi;
import cn.eziolin.zhiwei4idea.api.model.BaseResponse;
import cn.eziolin.zhiwei4idea.api.model.ViewMeta;
import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.application.ex.ApplicationUtil;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.util.TextRange;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

import java.net.http.HttpResponse;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class VuCodeCompletionProvider extends CompletionProvider<CompletionParameters> {

    private static final String prefix = "card::";
    private static final Pattern pattern = Pattern.compile(prefix + "([\\S]+)$");

    @Override
    protected void addCompletions(@NotNull CompletionParameters parameters,
                                  @NotNull ProcessingContext context,
                                  @NotNull CompletionResultSet result) {
        var project = parameters.getPosition().getProject();
        ConfigSettingsState.getInstance().getPluginConfig().ifPresent(
                pluginConfig -> {
                    if (!project.getService(CompletionService.class).getAffectedFiles().isEmpty()) {
                        String beforeInput = getBeforeInputString(parameters);
                        addCards(beforeInput, result);
                    }
                }
        );
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
            searchCards(searchKeyword)
            .map(
                (it) -> it.body().get()
            ).map(
                (it) -> it.resultValue.caches.stream()
                        .map((card -> LookupElementBuilder
                                        .create("#" + card.code)
                                        .withTypeText(card.name)
                                        .withLookupString(card.name)
                                        .withInsertHandler(((context, item) -> {
                                            var document = context.getDocument();
                                            var startOffset = context.getStartOffset();
                                            var tailOffset = context.getTailOffset();
                                            var lookupString = item.getLookupString();
                                            var prefixIndex = document.getText(new TextRange(0, startOffset)).lastIndexOf(prefix);
                                            document.replaceString(prefixIndex, tailOffset, lookupString);
                                        }))
                        )).collect(Collectors.toList())
            ).ifPresent(result::addAllElements);
        }
    }

    private Optional<HttpResponse<Supplier<BaseResponse<ViewMeta>>>> searchCards(String keyword) {
        return ZhiweiApi.searchCard(keyword).map(
                (response) -> {
                    try {
                        return ApplicationUtil.runWithCheckCanceled(
                                response,
                                ProgressManager.getInstance().getProgressIndicator()
                        );
                    } catch (ExecutionException e) {
                       return  null;
                    }
                }
        );
    }
}
