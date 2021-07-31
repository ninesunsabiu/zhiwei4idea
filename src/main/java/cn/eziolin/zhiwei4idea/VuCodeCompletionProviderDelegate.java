package cn.eziolin.zhiwei4idea;

import cn.eziolin.zhiwei4idea.api.ZhiweiApi;
import cn.eziolin.zhiwei4idea.api.model.BaseResponse;
import cn.eziolin.zhiwei4idea.api.model.ViewMeta;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.application.ex.ApplicationUtil;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.util.TextRange;

import java.net.http.HttpResponse;
import java.util.AbstractMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class VuCodeCompletionProviderDelegate
    implements BiConsumer<CompletionParameters, CompletionResultSet> {
  private static final String prefix = "card::";
  private static final Pattern pattern = Pattern.compile(prefix + "([\\S]+)$");

  public static Optional<String> getBeforeInputString(CompletionParameters parameters) {
    var position = parameters.getPosition();
    int offsetInFile = parameters.getOffset();
    var startAndEnd = position.getText().substring(0, offsetInFile);
    var matcher = pattern.matcher(startAndEnd);
    if (matcher.find()) {
      return Optional.ofNullable(matcher.group(1));
    }
    return Optional.empty();
  }

  private static Optional<List<LookupElementBuilder>> getCards(String searchKeyword) {
    return searchCards(searchKeyword)
        .map((it) -> it.body().get())
        .map(
            (it) ->
                it.resultValue.caches.stream()
                    .map(
                        (card ->
                            LookupElementBuilder.create("#" + card.code)
                                .withTypeText(card.name)
                                .withLookupString(card.name)
                                .withInsertHandler(
                                    ((context, item) -> {
                                      var document = context.getDocument();
                                      var startOffset = context.getStartOffset();
                                      var tailOffset = context.getTailOffset();
                                      var lookupString = item.getLookupString();
                                      var prefixIndex =
                                          document
                                              .getText(new TextRange(0, startOffset))
                                              .lastIndexOf(prefix);
                                      document.replaceString(prefixIndex, tailOffset, lookupString);
                                    }))))
                    .collect(Collectors.toList()));
  }

  private static Optional<HttpResponse<Supplier<BaseResponse<ViewMeta>>>> searchCards(
      String keyword) {
    return ZhiweiApi.searchCard(keyword)
        .map(
            (response) -> {
              try {
                return ApplicationUtil.runWithCheckCanceled(
                    response, ProgressManager.getInstance().getProgressIndicator());
              } catch (ExecutionException e) {
                return null;
              }
            });
  }

  @Override
  public void accept(CompletionParameters parameters, CompletionResultSet resultSet) {
    getBeforeInputString(parameters)
        .map(
            keyword -> new AbstractMap.SimpleEntry<>(resultSet.withPrefixMatcher(keyword), keyword))
        .ifPresent(
            simpleEntry -> {
              var result = simpleEntry.getKey();
              var keyword = simpleEntry.getValue();
              getCards(keyword).ifPresent(result::addAllElements);
            });
  }
}
