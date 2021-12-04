package cn.eziolin.zhiwei4idea.completion;

import cn.eziolin.zhiwei4idea.completion.service.CompletionService;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.util.ProcessingContext;
import org.apache.commons.collections.CollectionUtils;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiConsumer;

public class VuCodeCompletionProvider extends CompletionProvider<CompletionParameters> {

  private final BiConsumer<CompletionParameters, CompletionResultSet> addCompletionsFn;

  public VuCodeCompletionProvider(
      BiConsumer<CompletionParameters, CompletionResultSet> addCompletionsFn) {
    this.addCompletionsFn = addCompletionsFn;
  }

  @Override
  protected void addCompletions(
      @NotNull CompletionParameters parameters,
      @NotNull ProcessingContext context,
      @NotNull CompletionResultSet result) {
    var project = parameters.getPosition().getProject();
    if (CollectionUtils.isNotEmpty(
        project.getService(CompletionService.class).getAffectedFiles())) {
      addCompletionsFn.accept(parameters, result);
    }
  }
}
