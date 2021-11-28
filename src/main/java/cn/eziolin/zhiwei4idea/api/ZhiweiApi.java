package cn.eziolin.zhiwei4idea.api;

import cn.eziolin.zhiwei4idea.api.model.BaseResponse;
import cn.eziolin.zhiwei4idea.api.model.ViewMeta;
import cn.eziolin.zhiwei4idea.idea.service.ConfigSettingsState;
import cn.eziolin.zhiwei4idea.model.PluginConfig;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.util.io.HttpRequests;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class ZhiweiApi {

  private static <W> HttpResponse.BodyHandler<Supplier<BaseResponse<W>>> asJSON(
      Class<W> targetType) {
    HttpResponse.BodySubscriber<InputStream> upstream =
        HttpResponse.BodySubscribers.ofInputStream();

    final Function<InputStream, Supplier<BaseResponse<W>>> fn =
        (InputStream inputStream) ->
            () -> {
              try (InputStream stream = inputStream) {
                ObjectMapper objectMapper = new ObjectMapper();
                JavaType type =
                    objectMapper
                        .getTypeFactory()
                        .constructParametricType(BaseResponse.class, targetType);
                return objectMapper.readValue(stream, type);
              } catch (IOException e) {
                throw new UncheckedIOException(e);
              }
            };

    return (response) -> HttpResponse.BodySubscribers.mapping(upstream, fn);
  }

  private static Optional<ConfigSettingsState> getState() {
    return Optional.ofNullable(ConfigSettingsState.getInstance());
  }

  private static Optional<HttpRequest.Builder> getMyHttpRequestBuilder(String api) {
    var headerBuilder =
        getState()
            .flatMap(ConfigSettingsState::getPluginConfig)
            .map(PluginConfig::getCookie)
            .map(
                it ->
                    HttpRequest.newBuilder()
                        .setHeader("cookie", it)
                        .setHeader("content-type", HttpRequests.JSON_CONTENT_TYPE)
                        .setHeader("accept", HttpRequests.JSON_CONTENT_TYPE));
    return getState()
        .flatMap(ConfigSettingsState::getDomainSafe)
        .map(domain -> domain + api)
        .map(URI::create)
        .flatMap(uri -> headerBuilder.map(builder -> builder.uri(uri)));
  }

  public static Optional<CompletableFuture<HttpResponse<Supplier<BaseResponse<ViewMeta>>>>>
      searchCard(String keyword) {
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
    Supplier<String> payload =
        () -> {
          try {
            return new ObjectMapper().writeValueAsString(filterObj);
          } catch (JsonProcessingException e) {
            return "";
          }
        };
    var apiUrl = "/api/v1/view/whole/filter?orgId=771ac1a5-fca5-4af2-b744-27b16e989b18";
    return getMyHttpRequestBuilder(apiUrl)
        .map(
            builder -> {
              var client = HttpClient.newHttpClient();
              var request =
                  builder.POST(HttpRequest.BodyPublishers.ofString(payload.get())).build();
              return client.sendAsync(request, asJSON(ViewMeta.class));
            });
  }

  public static Optional<String> getCookieStr() {
    var maybeState = getState();
    return maybeState
        .map(ConfigSettingsState::getDomain)
        .flatMap(
            domain ->
                maybeState
                    .flatMap(ConfigSettingsState::getPluginConfig)
                    .map(
                        config -> {
                          try {
                            return HttpRequests.post(
                                    domain + "/login", HttpRequests.JSON_CONTENT_TYPE)
                                .tuner(
                                    connection ->
                                        Map.of("flag", "json")
                                            .forEach(connection::addRequestProperty))
                                .connect(
                                    request -> {
                                      var urlConnection =
                                          (HttpURLConnection) request.getConnection();
                                      String jsonInputString =
                                          String.format(
                                              "{\"username\":\"%s\",\"password\":\"%s\"}",
                                              config.getUsername(), config.getPassword());
                                      try (OutputStream os = urlConnection.getOutputStream()) {
                                        byte[] input =
                                            jsonInputString.getBytes(StandardCharsets.UTF_8);
                                        os.write(input, 0, input.length);
                                      }
                                      return urlConnection.getHeaderField("set-cookie");
                                    });
                          } catch (IOException e) {
                            return null;
                          }
                        }));
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
                    .ifPresent(
                        (cookieStr) -> {
                          getState().ifPresent(it -> it.saveCookie(cookieStr));
                          done.accept(cookieStr);
                        }));
  }
}
