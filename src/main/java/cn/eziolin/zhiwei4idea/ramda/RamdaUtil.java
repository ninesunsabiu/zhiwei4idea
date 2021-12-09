package cn.eziolin.zhiwei4idea.ramda;

import cn.eziolin.zhiwei4idea.api.ZhiweiService;
import cn.eziolin.zhiwei4idea.setting.ConfigSettingsState;
import cn.eziolin.zhiwei4idea.setting.model.PluginConfig;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.application.ApplicationManager;
import io.vavr.Function2;
import io.vavr.Function3;
import io.vavr.Tuple3;
import io.vavr.collection.Seq;
import io.vavr.control.Option;
import io.vavr.control.Validation;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;
import java.util.function.Supplier;

public class RamdaUtil {
  public static Supplier<Void> ignore = () -> null;

  public static Function<String, Function<String, String>> appendStr =
      right -> left -> left + right;

  public static Function<String, Function<String, String>> prependStr =
      left -> right -> left + right;

  public static Function<String, Function<Seq<String>, String>> joinStrWith =
      character -> seq -> seq.mkString(character);

  public static Function<String, Supplier<Throwable>> throwMsg = str -> () -> new Throwable(str);

  public static Function<PluginConfig, Validation<String, Tuple3<String, String, String>>>
      pluginConfigValidator =
          (@NotNull PluginConfig config) -> {
            var validConfig =
                Validation.combine(
                    Option.of(config.getDomain()).toValidation(() -> "缺少域名配置"),
                    Option.of(config.getUsername()).toValidation(() -> "缺少用户名配置"),
                    Option.of(config.getPassword()).toValidation(() -> "缺少密码配置"));
            return validConfig.ap(Tuple3::new).mapError(seq -> seq.mkString("\t"));
          };

  public static Function2<PluginConfig, ZhiweiService, Validation<String, String>> loginWithConfig =
      (config, service) -> {
        var validConfig = pluginConfigValidator.apply(config);

        return validConfig
            .map(
                (c) -> {
                  return Option.of(service)
                      .toValidation(() -> "获取 ZhiweiService 失败")
                      .flatMap(
                          zhiweiService -> {
                            return zhiweiService
                                .login(c._1, c._2, c._3)
                                .toValidation(Throwable::getMessage);
                          });
                })
            .flatMap(v -> v);
      };

  public static Function3<String, String, NotificationType, Void> showMessage =
      (title, content, type) -> {
        Notifications.Bus.notify(new Notification("Zhiwei Notification", title, content, type));
        return RamdaUtil.ignore.get();
      };

  public static Supplier<Validation<String, ZhiweiService>> getZhiweiService =
      () -> {
        var it = ApplicationManager.getApplication().getService(ZhiweiService.class);
        return Option.of(it).toValidation(() -> "奇怪的没有得到 Service");
      };

  public static Function<String, Validation<String, Tuple3<String, String, String>>> getConfigEnv =
      env -> {
        return ConfigSettingsState.getPluginConfigSafe(env)
            .map(pluginConfigValidator)
            .getOrElse(() -> Validation.invalid("查找不到对应的环境：" + env));
      };
}
