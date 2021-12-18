package cn.eziolin.zhiwei4idea.searchCode;

import cn.eziolin.zhiwei4idea.api.ZhiweiService;
import cn.eziolin.zhiwei4idea.api.model.Card;
import cn.eziolin.zhiwei4idea.ramda.RamdaUtil;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ex.ApplicationUtil;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.util.TextRange;
import io.vavr.Function2;
import io.vavr.Tuple;
import io.vavr.collection.List;
import io.vavr.control.Option;
import io.vavr.control.Try;
import io.vavr.control.Validation;

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
    return matcher.find() ? Option.some(matcher.group(1)) : Option.none();
  }

  private static List<Card> searchCards(String keyword) {
    var zhiweiService = RamdaUtil.getZhiweiService.get();
    var configRet = RamdaUtil.getConfigEnv.apply("tkb");
    Function2<ZhiweiService, String, Validation<String, List<Card>>> findCardList =
        (service, domain) -> {
          return service.findCardList(domain, keyword).toValidation(Throwable::getMessage);
        };

    return zhiweiService
        .combine(configRet)
        .ap(
            (service, config) -> {
              return Try.of(
                      () -> {
                        var indicator = ProgressManager.getInstance().getProgressIndicator();
                        return ApplicationUtil.runWithCheckCanceled(
                            () -> Tuple.of(service, config._1).apply(findCardList), indicator);
                      })
                  .getOrElseGet(ignored -> Validation.valid(List.<Card>empty()));
            })
        .mapError(RamdaUtil.joinStrWith.apply("\t"))
        .flatMap(it -> it)
        .peekError(
            errorMsg -> {
              RamdaUtil.showMessage.apply("查询卡片错误", errorMsg, NotificationType.WARNING);
            })
        .fold(ignored -> List.empty(), Function.identity());
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

  @Override
  public void accept(CompletionParameters parameters, CompletionResultSet resultSet) {
    getBeforeInputString(parameters)
        .map(keyword -> Tuple.of(resultSet.withPrefixMatcher(keyword), keyword))
        .forEach(
            tuple -> {
              getCards(tuple._2)
                  .transform(
                      (list) -> {
                        return list.nonEmpty()
                            ? Option.some(list)
                            : Option.<List<LookupElementBuilder>>none();
                      })
                  .forEach(
                      lookupList -> {
                        tuple._1.addAllElements(lookupList.collect(Collectors.toList()));
                      });
            });
  }
}
