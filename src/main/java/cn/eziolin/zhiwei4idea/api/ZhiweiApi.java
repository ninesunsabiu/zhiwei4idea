package cn.eziolin.zhiwei4idea.api;

import cn.eziolin.zhiwei4idea.api.model.BaseResponse;
import cn.eziolin.zhiwei4idea.api.model.ViewMeta;
import cn.eziolin.zhiwei4idea.idea.service.ConfigSettingsState;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.util.io.HttpRequests;
import io.vavr.Tuple;
import io.vavr.control.Option;
import io.vavr.control.Try;
import io.vavr.control.Validation;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class ZhiweiApi {
  public static final class ZhiweiApiError extends Error {
    public ZhiweiApiError(String message) {
      super(message);
    }
  }

  private static <W> HttpResponse.BodyHandler<Supplier<Try<BaseResponse<W>>>> asJSON(
      Class<W> targetType) {
    HttpResponse.BodySubscriber<InputStream> upstream =
        HttpResponse.BodySubscribers.ofInputStream();

    final Function<InputStream, Supplier<Try<BaseResponse<W>>>> fn =
        (InputStream inputStream) ->
            () ->
                Try.withResources(() -> inputStream)
                    .of(
                        (stream) -> {
                          ObjectMapper objectMapper = new ObjectMapper();
                          JavaType type =
                              objectMapper
                                  .getTypeFactory()
                                  .constructParametricType(BaseResponse.class, targetType);
                          return objectMapper.readValue(stream, type);
                        });

    return (response) -> HttpResponse.BodySubscribers.mapping(upstream, fn);
  }

  private static Validation<ZhiweiApiError, HttpRequest.Builder> getMyHttpRequestBuilder(
      String api) {
    return getMyHttpRequestBuilder(api, true);
  }

  private static Validation<ZhiweiApiError, HttpRequest.Builder> getMyHttpRequestBuilder(
      String api, Boolean needCookie) {

    // 这种地方就不能用 var 了 暂时还不知道为什么
    Validation<ZhiweiApiError, String> domainValid =
        ConfigSettingsState.getDomainSafe()
            .fold(() -> Validation.invalid(new ZhiweiApiError("域名为配置")), Validation::valid);

    return domainValid
        .map(
            domain -> {
              return HttpRequest.newBuilder(URI.create(domain + api))
                  .setHeader("Content-Type", HttpRequests.JSON_CONTENT_TYPE)
                  .setHeader("Accept", HttpRequests.JSON_CONTENT_TYPE);
            })
        .flatMap(
            builder -> {
              Validation<ZhiweiApiError, String> cookieValid =
                  ConfigSettingsState.getPersistentCookie()
                      .fold(
                          () -> Validation.invalid(new ZhiweiApiError("cookie 不存在")),
                          Validation::valid);
              return needCookie
                  ? cookieValid.map(cookie -> builder.setHeader("cookie", cookie))
                  : Validation.valid(builder);
            });
  }

  public static Validation<
          ZhiweiApiError, CompletableFuture<HttpResponse<Supplier<Try<BaseResponse<ViewMeta>>>>>>
      searchCard(String keyword) {
    var apiUrl = "/api/v1/view/whole/filter?orgId=771ac1a5-fca5-4af2-b744-27b16e989b18";
    return getMyHttpRequestBuilder(apiUrl)
        .flatMap(
            builder -> {
              return Try.of(
                      () -> {
                        var keywordFilter =
                            Map.ofEntries(
                                Map.entry("flag", "keyword"),
                                Map.entry("operator", "Include"),
                                Map.entry("tag", "ComparableArgument"),
                                Map.entry("type", "TEXT"),
                                Map.entry("value", keyword));
                        var pageableFilter =
                            Map.ofEntries(
                                Map.entry("flag", "updateDate"),
                                Map.entry("multiple", false),
                                Map.entry("name", "$page"),
                                Map.entry("order", "desc"),
                                Map.entry("page", 0),
                                Map.entry("pageSize", 20),
                                Map.entry("required", true),
                                Map.entry("tag", "PageableArgument"),
                                Map.entry("type", "DATE"));

                        var filterObj =
                            Map.of("filter", Arrays.asList(keywordFilter, pageableFilter));
                        return new ObjectMapper().writeValueAsString(filterObj);
                      })
                  .toValidation(throwable -> new ZhiweiApiError(throwable.getMessage()))
                  // 如果能够正常正常得到 payload的话
                  .map(payload -> Tuple.of(builder, payload));
            })
        .map(
            tuple -> {
              var request = tuple._1.POST(HttpRequest.BodyPublishers.ofString(tuple._2)).build();
              return HttpClient.newHttpClient().sendAsync(request, asJSON(ViewMeta.class));
            });
  }

  public static Validation<ZhiweiApiError, String> getCookieStr() {
    return getMyHttpRequestBuilder("/login", false)
        .flatMap(
            builder -> {
              return ConfigSettingsState.getPluginConfigSafe()
                  .fold(
                      () -> Validation.invalid(new ZhiweiApiError("未配置账号密码")),
                      config -> {
                        return Validation.valid(
                            builder
                                .setHeader("flag", "json")
                                .POST(
                                    HttpRequest.BodyPublishers.ofString(
                                        String.format(
                                            "{\"username\":\"%s\",\"password\":\"%s\"}",
                                            config.getUsername(), config.getPassword()))));
                      });
            })
        .flatMap(
            builder -> {
              return Try.of(
                      () -> {
                        return Option.ofOptional(
                                HttpClient.newHttpClient()
                                    .send(builder.build(), HttpResponse.BodyHandlers.ofString())
                                    .headers()
                                    .firstValue("Set-Cookie"))
                            .getOrElseThrow(() -> new ZhiweiApiError("登录失败"));
                      })
                  .toValidation(err -> new ZhiweiApiError(err.getMessage()));
            });
  }

  public static void initSdk() {
    Consumer<String> noop = (String cookieStr) -> {};
    initSdk(noop);
  }

  public static void initSdk(Consumer<String> done) {
    ApplicationManager.getApplication()
        .executeOnPooledThread(
            () ->
                getCookieStr()
                    .forEach(
                        (cookieStr) -> {
                          ConfigSettingsState.getPluginConfigSafe()
                              .forEach(config -> config.setCookie(cookieStr));
                          done.accept(cookieStr);
                        }));
  }
}
