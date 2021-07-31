package cn.eziolin.zhiwei4idea.idea;

import cn.eziolin.zhiwei4idea.VuCodeCompletionProviderDelegate;
import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PlainTextTokenTypes;

public class VuCodeCompletionContributor extends CompletionContributor {
  public VuCodeCompletionContributor() {
    extend(
        CompletionType.BASIC,
        PlatformPatterns.psiElement(PlainTextTokenTypes.PLAIN_TEXT),
        new VuCodeCompletionProvider(new VuCodeCompletionProviderDelegate()));
  }
}
