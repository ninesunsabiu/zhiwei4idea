package cn.eziolin.zhiwei4idea.api;

import cn.eziolin.zhiwei4idea.api.model.BaseResponse;
import cn.eziolin.zhiwei4idea.api.model.Card;
import cn.eziolin.zhiwei4idea.api.model.ViewMeta;
import cn.eziolin.zhiwei4idea.ramda.RamdaUtil;
import cn.eziolin.zhiwei4idea.setting.model.PluginConfig;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.util.io.HttpRequests;
import io.vavr.Function2;
import io.vavr.Function3;
import io.vavr.Tuple;
import io.vavr.Tuple3;
import io.vavr.collection.HashMap;
import io.vavr.collection.List;
import io.vavr.collection.Seq;
import io.vavr.control.Option;
import io.vavr.control.Try;
import io.vavr.control.Validation;
import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

public class ZhiweiServiceImpl implements ZhiweiService {
  private static class ConfigRecord {
    private final String userName;
    private final String password;
    private final String cookie;

    private ConfigRecord(String userName, String password, String cookie) {
      this.userName = userName;
      this.password = password;
      this.cookie = cookie;
    }

    public static ConfigRecord of(String userName, String password, String cookie) {
      return new ConfigRecord(userName, password, cookie);
    }
  }

  private HashMap<String, ConfigRecord> cookieCache = HashMap.empty();

  private static <W> HttpResponse.BodyHandler<Supplier<Try<BaseResponse<W>>>> asJSON(
      Class<W> targetType) {
    HttpResponse.BodySubscriber<InputStream> upstream =
        HttpResponse.BodySubscribers.ofInputStream();

    final Function<InputStream, Supplier<Try<BaseResponse<W>>>> fn =
        (InputStream inputStream) -> {
          return () -> {
            return Try.withResources(() -> inputStream)
                .of(
                    (stream) -> {
                      ObjectMapper objectMapper = new ObjectMapper();
                      JavaType type =
                          objectMapper
                              .getTypeFactory()
                              .constructParametricType(BaseResponse.class, targetType);
                      return objectMapper.readValue(stream, type);
                    });
          };
        };

    return (response) -> HttpResponse.BodySubscribers.mapping(upstream, fn);
  }

  private Supplier<HttpRequest.Builder> getNewHttpRequestBuilder() {
    return () -> {
      return HttpRequest.newBuilder()
          .setHeader("Content-Type", HttpRequests.JSON_CONTENT_TYPE)
          .setHeader("Accept", HttpRequests.JSON_CONTENT_TYPE);
    };
  }

  private Function<HttpRequest.Builder, HttpRequest.Builder> setCookieInBuilder(String cookie) {
    return builder -> Tuple.of("Cookie", cookie).apply(builder::setHeader);
  }

  private <T> Try<T> transformVToT(Validation<Seq<String>, Try<T>> v) {
    return v.isInvalid()
        ? Try.failure(ZhiweiApiError.of(RamdaUtil.joinStrWith.apply("\t").apply(v.getError())))
        : v.get();
  }

  Function2<String, String, Validation<String, String>> notNullStringValidator =
      (paramName, input) -> Option.of(input).toValidation(() -> paramName + "不能为空");

  private final Function3<String, String, String, Try<String>> loginProcess =
      (domain, userName, password) -> {
        Supplier<String> payloadSupplier =
            () -> String.format("{\"username\":\"%s\",\"password\":\"%s\"}", userName, password);

        var requestBuilder =
            getNewHttpRequestBuilder()
                .get()
                .setHeader("flag", "json")
                .uri(URI.create(domain + "/login"))
                .POST(HttpRequest.BodyPublishers.ofString(payloadSupplier.get()));

        return Try.of(
                () -> {
                  HttpClient client = HttpClient.newHttpClient();
                  var response =
                      client.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
                  var status = response.statusCode();

                  return Option.ofOptional(
                          status == 200
                              ? response.headers().firstValue("Set-Cookie")
                              : Optional.empty())
                      .get();
                })
            .recoverWith(ignored -> Try.failure(ZhiweiApiError.of("登录失败")))
            .peek(
                (cookie) ->
                    this.cookieCache =
                        this.cookieCache.merge(
                            HashMap.of(domain, ConfigRecord.of(userName, password, cookie)),
                            (xs, x) -> x));
      };

  @Override
  public @NotNull Try<String> login(String domain, String userName, String password) {

    var inputValidator =
        Validation.combine(
            notNullStringValidator.apply("域名", domain),
            notNullStringValidator.apply("用户名", userName),
            notNullStringValidator.apply("密码", password));

    return inputValidator.ap(loginProcess).transform(this::transformVToT);
  }

  @Override
  public @NotNull Try<String> getCookie(String domain) {
    return domain == null
        ? Try.failure(ZhiweiApiError.of("域名配置不能为空"))
        : Try.of(
            () ->
                this.cookieCache
                    .get(domain)
                    .map(v -> v.cookie)
                    .getOrElseThrow(ZhiweiApiError.make("未找到 Cookie")));
  }

  private final Function2<String, String, Try<List<Card>>> findCardListProcess =
      (domain, keyword) -> {
        var apiUrl = "/api/v1/view/whole/filter?orgId=771ac1a5-fca5-4af2-b744-27b16e989b18";
        Supplier<HttpRequest.Builder> requestBuilder =
            () -> getNewHttpRequestBuilder().get().uri(URI.create(domain + apiUrl));

        Supplier<Try<String>> payload =
            () ->
                Try.of(
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
                    });

        return getCookie(domain)
            .flatMap(
                cookie -> {
                  return payload
                      .get()
                      .map(HttpRequest.BodyPublishers::ofString)
                      .map(requestBuilder.get()::POST)
                      .map(setCookieInBuilder(cookie))
                      .map(HttpRequest.Builder::build);
                })
            .flatMapTry(
                request -> {
                  HttpClient client = HttpClient.newHttpClient();
                  return client
                      .sendAsync(request, asJSON(ViewMeta.class))
                      .thenApply(
                          response -> {
                            return response.body().get();
                          })
                      .thenApply(
                          tryViewMeta -> {
                            return tryViewMeta
                                .map((viewMeta) -> viewMeta.resultValue.caches)
                                .map(List::ofAll);
                          })
                      .get();
                })
            .recoverWith(
                ((Function<Throwable, ZhiweiApiError>) ZhiweiApiError::of).andThen(Try::failure));
      };

  @Override
  public @NotNull Try<List<Card>> findCardList(String domain, String keyword) {

    var inputValidator =
        Validation.combine(
            notNullStringValidator.apply("域名", domain),
            notNullStringValidator.apply("密码", keyword));

    return inputValidator.ap(findCardListProcess).transform(this::transformVToT);
  }

  private final Function2<Tuple3<String, String, String>, String, Try<String>>
      searchIdForEverythingProcess =
          (config, id) -> {
            var api = "/api/v1/debug/watch/" + id;
            var domainTuple = Tuple.of(config._1);
            var tryCookie = domainTuple.apply(this::getCookie).map(domainTuple::append);

            return tryCookie
                .map(
                    domainAndCookie -> {
                      var domain = domainAndCookie._1;
                      var cookie = domainAndCookie._2;
                      return Tuple.of(
                              getNewHttpRequestBuilder().get().uri(URI.create(domain + api)).GET())
                          .apply(setCookieInBuilder(cookie))
                          .build();
                    })
                .mapTry(
                    request -> {
                      HttpClient client = HttpClient.newHttpClient();
                      return client.send(request, HttpResponse.BodyHandlers.ofString()).body();
                    });
          };

  @Override
  public @NotNull Try<String> searchIdForEverything(PluginConfig config, String id) {
    var configWithValidation = RamdaUtil.pluginConfigValidator.apply(config);
    return configWithValidation
        .combine(notNullStringValidator.apply("ID", id))
        .ap(searchIdForEverythingProcess)
        .transform(this::transformVToT);
  }
}
