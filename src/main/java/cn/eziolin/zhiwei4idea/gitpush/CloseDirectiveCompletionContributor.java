package cn.eziolin.zhiwei4idea.gitpush;

import cn.eziolin.zhiwei4idea.completion.CompletionService;
import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PlainTextTokenTypes;
import com.intellij.util.ProcessingContext;
import io.vavr.Tuple;
import io.vavr.collection.HashSet;
import org.apache.commons.collections.CollectionUtils;
import org.jetbrains.annotations.NotNull;

public class CloseDirectiveCompletionContributor extends CompletionContributor {

  public CloseDirectiveCompletionContributor() {
    extend(
        CompletionType.BASIC,
        PlatformPatterns.psiElement(PlainTextTokenTypes.PLAIN_TEXT),
        new CompletionProvider<>() {
          @Override
          protected void addCompletions(
              @NotNull CompletionParameters parameters,
              @NotNull ProcessingContext context,
              @NotNull CompletionResultSet result) {

            Tuple.of(parameters.getPosition().getProject())
                .apply(CompletionService::getInstance)
                .map(CompletionService::getAffectedFiles)
                .filter(CollectionUtils::isNotEmpty)
                .forEach(
                    ignored -> {
                      var triggerKeywords =
                          HashSet.of(
                              "close",
                              "closes",
                              "closed",
                              "fix",
                              "fixes",
                              "fixed",
                              "resolve",
                              "resolves",
                              "resolved");

                      var lookupElementSet =
                          triggerKeywords
                              .map(LookupElementBuilder::create)
                              .map(
                                  it -> {
                                    return it.withTypeText("@ag:" + it.getLookupString())
                                        .withCaseSensitivity(false);
                                  })
                              .map(
                                  it -> {
                                    return it.withInsertHandler(
                                        (ctx, item) -> {
                                          var lookupString = item.getLookupString();
                                          var document = ctx.getDocument();
                                          var startOffset = ctx.getStartOffset();
                                          var tailOffset = ctx.getTailOffset();
                                          document.replaceString(
                                              startOffset, tailOffset, "@ag:" + lookupString + " ");
                                        });
                                  });

                      result.addAllElements(lookupElementSet);
                    });
          }
        });
  }
}
