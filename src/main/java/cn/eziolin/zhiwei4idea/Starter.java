package cn.eziolin.zhiwei4idea;

import java.util.function.Function;
import java.util.function.Supplier;

import org.jetbrains.annotations.NotNull;

import com.intellij.notification.NotificationType;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManagerListener;

import cn.eziolin.zhiwei4idea.gitpush.IssueCardCloser;
import cn.eziolin.zhiwei4idea.ramda.RamdaUtil;
import cn.eziolin.zhiwei4idea.setting.ConfigSettingsState;
import cn.eziolin.zhiwei4idea.setting.model.PluginConfig;
import io.vavr.Function2;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.collection.Map;
import io.vavr.collection.Traversable;
import io.vavr.control.Option;
import io.vavr.control.Validation;

public class Starter implements ProjectManagerListener {
    @Override
    public void projectOpened(@NotNull Project project) {
        // start git push listener
        project.getService(IssueCardCloser.class).activate();

        new Task.Backgroundable(project, "Login zhiwei", true) {

            @NotNull
            private final Function<
                PluginConfig, Validation<String, String>
            > loginWithConfig = config -> RamdaUtil.getZhiweiService
                .get()
                .map(it -> Tuple.of(config).append(it))
                .flatMap(param -> param.apply(RamdaUtil.loginWithConfig));

            @NotNull
            private Function<String, Void> showErrorMessage(Function<Object, Void> done) {
                return errMsg -> Tuple.of("Zhiwei", errMsg, NotificationType.ERROR)
                        .apply(RamdaUtil.showMessage.andThen(done));
            }

            @NotNull
            private final Function2<
                Validation<String, String>,
                Tuple2<String, Validation<String, String>>,
                Validation<String, String>
            > collectionLoginInfo = (acc, item) -> {
                var envName = item._1;
                var loginRet = item._2;
                var itemValidation = loginRet.mapError(errStr -> envName + ": " + errStr);
                return acc
                    .combine(itemValidation)
                    .ap((a, b) -> "")
                    .mapError(errorSeq -> errorSeq.mkString("\n"));
            };


            @NotNull
            private  Function<Map<String, PluginConfig>, Void> loginAllEnv(Function<Object, Void> done) {
                return (configEntry) -> configEntry
                        .mapValues(loginWithConfig)
                        .foldLeft(Validation.valid(""), collectionLoginInfo)
                        .fold(showErrorMessage(done), done);
            }

            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setText("Zhiwei login...");
                indicator.setIndeterminate(false);
                indicator.setFraction(0.0);

                Function<Object, Void> done = ignored -> {
                    indicator.setFraction(1.0);
                    return RamdaUtil.ignore.get();
                };

                Supplier<Void> whenConfigIsEmpty = () -> Tuple.of(
                    "Zhiwei login",
                    "不存在对应的配置信息 Zhiwei 相关功能可能无法正常运行",
                    NotificationType.WARNING
                )
                .apply(RamdaUtil.showMessage.andThen(done));

                ConfigSettingsState.getInstanceSafe()
                        .map(ConfigSettingsState::getPluginConfig)
                        .flatMap(Option::of)
                        .filterNot(Traversable::isEmpty)
                        .fold(whenConfigIsEmpty, loginAllEnv(done));
            }
        }
        .setCancelText("取消登录")
        .queue();
    }
}