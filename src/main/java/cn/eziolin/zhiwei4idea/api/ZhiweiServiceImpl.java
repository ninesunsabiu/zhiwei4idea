package cn.eziolin.zhiwei4idea.api;

import cn.eziolin.zhiwei4idea.api.model.BaseResponse;
import cn.eziolin.zhiwei4idea.api.model.Card;
import cn.eziolin.zhiwei4idea.api.model.ViewMeta;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.util.io.HttpRequests;
import io.vavr.Tuple;
import io.vavr.Tuple3;
import io.vavr.collection.HashMap;
import io.vavr.collection.List;
import io.vavr.control.Option;
import io.vavr.control.Try;
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
  // Tuple3 is (userName, password, cookie) I need record !!
  private HashMap<String, Tuple3<String, String, String>> cookieCache = HashMap.empty();

  private Supplier<HttpRequest.Builder> getNewHttpRequestBuilder() {
    return () -> {
      return HttpRequest.newBuilder()
          .setHeader("Content-Type", HttpRequests.JSON_CONTENT_TYPE)
          .setHeader("Accept", HttpRequests.JSON_CONTENT_TYPE);
    };
  }

  @Override
  public @NotNull Try<String> login(
      @NotNull String domain, @NotNull String userName, @NotNull String password) {

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
                        HashMap.of(domain, Tuple.of(userName, password, cookie)), (xs, x) -> x));
  }

  @Override
  public @NotNull Try<String> getCookie(@NotNull String domain) {
    return Try.of(
        () ->
            this.cookieCache
                .get(domain)
                .map(v -> v._3)
                .getOrElseThrow(ZhiweiApiError.make("未找到 Cookie")));
  }

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

  @Override
  public @NotNull Try<List<Card>> findCardList(@NotNull String domain, @NotNull String keyword) {
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

                  var filterObj = Map.of("filter", Arrays.asList(keywordFilter, pageableFilter));
                  return new ObjectMapper().writeValueAsString(filterObj);
                });

    return getCookie(domain)
        .flatMap(
            cookie -> {
              return payload
                  .get()
                  .map(HttpRequest.BodyPublishers::ofString)
                  .map(requestBuilder.get()::POST)
                  .map(builder -> builder.setHeader("Cookie", cookie))
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
  }
}
