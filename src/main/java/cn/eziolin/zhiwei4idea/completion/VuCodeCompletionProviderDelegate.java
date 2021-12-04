package cn.eziolin.zhiwei4idea.completion;

import cn.eziolin.zhiwei4idea.api.ZhiweiApi;
import cn.eziolin.zhiwei4idea.api.model.Card;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.application.ex.ApplicationUtil;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.util.TextRange;
import io.vavr.Tuple;
import io.vavr.control.Option;
import io.vavr.control.Try;

import java.util.function.BiConsumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class VuCodeCompletionProviderDelegate
    implements BiConsumer<CompletionParameters, CompletionResultSet> {
  private static final String prefix = "card::";
  private static final Pattern pattern = Pattern.compile(prefix + "([\\S]+)$");

  public static Option<String> getBeforeInputString(CompletionParameters parameters) {
    var position = parameters.getPosition();
    int offsetInFile = parameters.getOffset();
    var startAndEnd = position.getText().substring(0, offsetInFile);
    var matcher = pattern.matcher(startAndEnd);
    if (matcher.find()) {
      return Option.of(matcher.group(1));
    }
    return Option.none();
  }

  private static Stream<LookupElementBuilder> getCards(String searchKeyword) {
    return searchCards(searchKeyword)
        .map(
            card -> {
              return LookupElementBuilder.create("#" + card.code)
                  .withTypeText(card.name)
                  .withLookupString(card.name)
                  .withInsertHandler(
                      ((context, item) -> {
                        var document = context.getDocument();
                        var startOffset = context.getStartOffset();
                        var tailOffset = context.getTailOffset();
                        var lookupString = item.getLookupString();
                        var prefixIndex =
                            document.getText(new TextRange(0, startOffset)).lastIndexOf(prefix);
                        document.replaceString(prefixIndex, tailOffset, lookupString);
                      }));
            });
  }

  private static Stream<Card> searchCards(String keyword) {
    return ZhiweiApi.searchCard(keyword)
        .flatMap(
            (response) -> {
              return Try.of(
                      () -> {
                        return ApplicationUtil.runWithCheckCanceled(
                            response, ProgressManager.getInstance().getProgressIndicator());
                      })
                  .flatMapTry(res -> res.body().get())
                  .map(it -> it.resultValue.caches.stream())
                  .toValidation(err -> new ZhiweiApi.ZhiweiApiError(err.getMessage()));
            })
        .peekError(
            error -> {
              Notifications.Bus.notify(
                  new Notification(
                      "Zhiwei Notification",
                      "Search Card" + "\n" + error.getMessage(),
                      NotificationType.ERROR));
            })
        .getOrElse(Stream.empty());
  }

  @Override
  public void accept(CompletionParameters parameters, CompletionResultSet resultSet) {
    getBeforeInputString(parameters)
        .map(keyword -> Tuple.of(resultSet.withPrefixMatcher(keyword), keyword))
        .forEach(tuple -> tuple._1.addAllElements(getCards(tuple._2).collect(Collectors.toList())));
  }
}
