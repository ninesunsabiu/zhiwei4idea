package cn.eziolin.zhiwei4idea.api;

import cn.eziolin.zhiwei4idea.api.model.BaseResponse;
import cn.eziolin.zhiwei4idea.api.model.ViewMeta;
import cn.eziolin.zhiwei4idea.idea.service.ConfigSettingsState;
import cn.eziolin.zhiwei4idea.model.PluginConfig;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class ZhiweiApi {
    private static ConfigSettingsState getState() { return ConfigSettingsState.getInstance(); }

    private static Optional<HttpRequest.Builder> getMyHttpRequestBuilder() {
        return getState().getPluginConfig().map(
                        PluginConfig::getCookie
                ).map(
                        it -> HttpRequest.newBuilder()
                                .setHeader("cookie", it)
                                .setHeader("content-type", HttpRequests.JSON_CONTENT_TYPE)
                                .setHeader("accept", HttpRequests.JSON_CONTENT_TYPE)
                );
    }


    public static Optional<CompletableFuture<HttpResponse<Supplier<BaseResponse<ViewMeta>>>>> searchCard(String keyword) {
        String payload = String.format(
                "{" +
                    "\"filter\":[" +
                        "{" +
                            "\"flag\":\"keyword\"," +
                            "\"operator\":\"Include\"," +
                            "\"tag\":\"ComparableArgument\"," +
                            "\"type\":\"TEXT\"," +
                            // format here
                            "\"value\":\"%s\"" +
                        "}," +
                        "{" +
                            "\"flag\":\"updateDate\"," +
                            "\"multiple\":false," +
                            "\"name\":\"$page\"," +
                            "\"order\":\"desc\"," +
                            "\"page\":0," +
                            "\"pageSize\":20," +
                            "\"required\":true," +
                            "\"tag\":\"PageableArgument\"," +
                            "\"type\":\"DATE\"" +
                        "}" +
                    "]" +
                "}"
                ,
                keyword
        );
        var apiUrl = "/api/v1/view/whole/filter?orgId=771ac1a5-fca5-4af2-b744-27b16e989b18";
        return  Optional.of(getState())
                .map(ConfigSettingsState::getDomain)
                .map(domain -> domain + apiUrl)
                .map(URI::create)
                .flatMap(
                        uri -> getMyHttpRequestBuilder().map(builder -> builder.uri(uri))
                ).map(
                        builder -> {
                            var client = HttpClient.newHttpClient();
                            var request = builder.POST(HttpRequest.BodyPublishers.ofString(payload)).build();
                            return client.sendAsync(request, new JsonBodyHandler<>(ViewMeta.class));
                        }
                );
    }

    public static Optional<String> getCookieStr() {
        var maybeState = Optional.of(getState());
        return maybeState.map(ConfigSettingsState::getDomain).flatMap(
                domain -> maybeState.flatMap(ConfigSettingsState::getPluginConfig).map(
                        config -> {
                            try {
                                return HttpRequests
                                        .post(domain + "/login", HttpRequests.JSON_CONTENT_TYPE)
                                        .tuner(
                                                connection -> Map.of("flag", "json").forEach(connection::addRequestProperty)
                                        ).connect(
                                                request -> {
                                                    var urlConnection = (HttpURLConnection) request.getConnection();
                                                    String jsonInputString = String.format("{\"username\":\"%s\",\"password\":\"%s\"}", config.getUsername(), config.getPassword());
                                                    try (OutputStream os = urlConnection.getOutputStream()) {
                                                        byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
                                                        os.write(input, 0, input.length);
                                                    }
                                                    return urlConnection.getHeaderField("set-cookie");
                                                }
                                        );
                            } catch (IOException e) {
                                return null;
                            }
                        }
                )
        );
    }
}

class JsonBodyHandler<T> implements HttpResponse.BodyHandler<Supplier<BaseResponse<T>>> {

    private final Class<T> targetClass;

    public JsonBodyHandler(Class<T> targetClass) {
        this.targetClass = targetClass;
    }

    @Override
    public HttpResponse.BodySubscriber<Supplier<BaseResponse<T>>> apply(HttpResponse.ResponseInfo responseInfo) {
        return asJSON(this.targetClass);
    }


    public static <W> HttpResponse.BodySubscriber<Supplier<BaseResponse<W>>> asJSON(Class<W> targetType) {
        HttpResponse.BodySubscriber<InputStream> upstream = HttpResponse.BodySubscribers.ofInputStream();

        return HttpResponse.BodySubscribers.mapping(
                upstream,
                inputStream -> toSupplierOfType(inputStream, targetType));
    }

    public static <W> Supplier<BaseResponse<W>> toSupplierOfType(InputStream inputStream, Class<W> targetType) {
        return () -> {
            try (InputStream stream = inputStream) {
                ObjectMapper objectMapper = new ObjectMapper();
                JavaType type = objectMapper.getTypeFactory().constructParametricType(BaseResponse.class, targetType);
                return objectMapper.readValue(stream, type);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        };
    }
}
