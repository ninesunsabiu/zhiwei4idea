package cn.eziolin.zhiwei4idea;

import cn.eziolin.zhiwei4idea.model.BaseResponse;
import cn.eziolin.zhiwei4idea.model.Card;
import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.util.TextRange;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Collections;
import java.util.List;
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
        if (!project.getService(CompletionService.class).getAffectedFiles().isEmpty()) {
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
            var cards = searchCards(searchKeyword);
            var completions = cards.stream()
                    .map((
                            card -> LookupElementBuilder
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
                    ))
                    .collect(Collectors.toList());
            result.addAllElements(completions);
        }
    }

    private List<Card> searchCards(String search) {
        String payload = String.format(
                "{" +
                        "\"filter\":[" +
                            "{" +
                                "\"flag\":\"keyword\"," +
                                "\"operator\":\"Include\"," +
                                "\"tag\":\"ComparableArgument\"," +
                                "\"type\":\"TEXT\"," +
                                // format here
                                "\"value\":\"%s\"" +
                            "}," +
                            "{" +
                                "\"flag\":\"updateDate\"," +
                                "\"multiple\":false," +
                                "\"name\":\"$page\"," +
                                "\"order\":\"desc\"," +
                                "\"page\":0," +
                                "\"pageSize\":10," +
                                "\"required\":true," +
                                "\"tag\":\"PageableArgument\"," +
                                "\"type\":\"DATE\"" +
                            "}" +
                        "]" +
                "}"
                ,
                search
        );
        var client = HttpClient.newHttpClient();
        var request = HttpRequest.newBuilder(
                URI.create("https://search-zhiwei-card.ninesuns-lin.workers.dev/api/v1/view/whole/filter?orgId=771ac1a5-fca5-4af2-b744-27b16e989b18")
        )
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();
        try {
            HttpResponse<Supplier<BaseResponse>> response =
                    client.send(request, new JsonBodyHandler<>(BaseResponse.class));
            return response.body().get().resultValue.caches;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Collections.emptyList();
    }
}
