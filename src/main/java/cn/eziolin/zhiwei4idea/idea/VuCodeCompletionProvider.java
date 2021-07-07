package cn.eziolin.zhiwei4idea.idea;

import cn.eziolin.zhiwei4idea.idea.service.CompletionService;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.util.ProcessingContext;
import org.apache.commons.collections.CollectionUtils;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public class VuCodeCompletionProvider extends CompletionProvider<CompletionParameters> {

    private final Function<CompletionParameters, Optional<List<LookupElementBuilder>>> addCompletionsFn;

    public VuCodeCompletionProvider(
            Function<CompletionParameters, Optional<List<LookupElementBuilder>>> addCompletionsFn
    ) {
        this.addCompletionsFn = addCompletionsFn;
    }

    @Override
    protected void addCompletions(@NotNull CompletionParameters parameters,
                                  @NotNull ProcessingContext context,
                                  @NotNull CompletionResultSet result) {
        var project = parameters.getPosition().getProject();
        if (CollectionUtils.isNotEmpty(project.getService(CompletionService.class).getAffectedFiles())) {
            addCompletionsFn.apply(parameters).ifPresent(result::addAllElements);
        }
    }

}
