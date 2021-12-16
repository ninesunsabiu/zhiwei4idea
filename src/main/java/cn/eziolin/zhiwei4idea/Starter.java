package cn.eziolin.zhiwei4idea;

import cn.eziolin.zhiwei4idea.gitpush.IssueCardCloser;
import cn.eziolin.zhiwei4idea.ramda.RamdaUtil;
import cn.eziolin.zhiwei4idea.setting.ConfigSettingsState;
import cn.eziolin.zhiwei4idea.setting.model.PluginConfig;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManagerListener;
import io.vavr.Tuple;
import io.vavr.collection.List;
import io.vavr.collection.Map;
import io.vavr.collection.Traversable;
import io.vavr.control.Option;
import io.vavr.control.Validation;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;
import java.util.function.Supplier;

public class Starter implements ProjectManagerListener {
  @Override
  public void projectOpened(@NotNull Project project) {
    // start git push listener
    project.getService(IssueCardCloser.class).activate();

    new Task.Backgroundable(project, "Login zhiwei", true) {

      @Override
      public void run(@NotNull ProgressIndicator indicator) {
        indicator.setText("Zhiwei login...");
        indicator.setIndeterminate(false);
        indicator.setFraction(0.0);
        Function<Object, Void> done =
            ignored -> {
              indicator.setFraction(1.0);
              return RamdaUtil.ignore.get();
            };

        Supplier<Void> whenConfigIsEmpty =
            () -> {
              return Tuple.of(
                      "Zhiwei login", "不存在对应的配置信息 Zhiwei 相关功能可能无法正常运行", NotificationType.WARNING)
                  .apply(RamdaUtil.showMessage.andThen(done));
            };

        Function<Map<String, PluginConfig>, Void> loginAllEnv =
            (configEntry) -> {
              return configEntry
                  .mapValues(
                      config -> {
                        return RamdaUtil.getZhiweiService
                            .get()
                            .flatMap(
                                service -> {
                                  return RamdaUtil.loginWithConfig.apply(config, service);
                                });
                      })
                  .reduceLeft(
                      (left, right) -> {
                        var leftVal = left._2.mapError(str -> List.of(left._1, str).mkString(": "));
                        var rightVal =
                            right._2.mapError(str -> List.of(right._1, str).mkString(": "));
                        var validator =
                            Validation.combine(leftVal, rightVal)
                                .ap(
                                    (errMsgFromLeft, errMsgFromRight) -> {
                                      return errMsgFromLeft + "\t" + errMsgFromLeft;
                                    })
                                .mapError(seq -> seq.reduceLeft((a, b) -> a + "\t" + b));
                        return Tuple.of("", validator);
                      })
                  ._2
                  .fold(
                      errMsg -> {
                        return Tuple.of("Zhiwei", errMsg, NotificationType.ERROR)
                            .apply(RamdaUtil.showMessage.andThen(done));
                      },
                      done);
            };

        ConfigSettingsState.getInstanceSafe()
            .map(ConfigSettingsState::getPluginConfig)
            .flatMap(Option::of)
            .filterNot(Traversable::isEmpty)
            .fold(whenConfigIsEmpty, loginAllEnv);
      }
    }.setCancelText("取消登录").queue();
  }
}
