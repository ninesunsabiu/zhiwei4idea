package cn.eziolin.zhiwei4idea.completion;

import cn.eziolin.zhiwei4idea.api.model.Card;
import cn.eziolin.zhiwei4idea.ramda.RamdaUtil;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.util.TextRange;
import io.vavr.Tuple;
import io.vavr.collection.List;
import io.vavr.control.Option;

import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class VuCodeCompletionProviderDelegate
    implements BiConsumer<CompletionParameters, CompletionResultSet> {
  private static final String prefix = "card::";
  private static final Pattern pattern = Pattern.compile(prefix + "([\\S]+)$");

  private static Option<String> getBeforeInputString(CompletionParameters parameters) {
    var position = parameters.getPosition();
    int offsetInFile = parameters.getOffset();
    var startAndEnd = position.getText().substring(0, offsetInFile);
    var matcher = pattern.matcher(startAndEnd);
    if (matcher.find()) {
      return Option.of(matcher.group(1));
    }
    return Option.none();
  }

  private static List<LookupElementBuilder> getCards(String searchKeyword) {
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

  private static List<Card> searchCards(String keyword) {
    var zhiweiService = RamdaUtil.getZhiweiService.get();
    var configRet = RamdaUtil.getConfigEnv.apply("tkb");

    return configRet
        .flatMap(
            config -> {
              return zhiweiService.map(service -> Tuple.of(service, config._1));
            })
        .flatMap(
            serviceAndDomain -> {
              return serviceAndDomain
                  ._1
                  .findCardList(serviceAndDomain._2, keyword)
                  .toValidation(Throwable::getMessage);
            })
        .peekError(
            errorMsg -> {
              RamdaUtil.showMessage.apply("查询卡片错误", errorMsg, NotificationType.WARNING);
            })
        .fold(ignored -> List.empty(), Function.identity());
  }

  @Override
  public void accept(CompletionParameters parameters, CompletionResultSet resultSet) {
    getBeforeInputString(parameters)
        .map(keyword -> Tuple.of(resultSet.withPrefixMatcher(keyword), keyword))
        .forEach(tuple -> tuple._1.addAllElements(getCards(tuple._2).collect(Collectors.toList())));
  }
}
