package cn.eziolin.zhiwei4idea.api;

import cn.eziolin.zhiwei4idea.api.model.BaseResponse;
import cn.eziolin.zhiwei4idea.api.model.ViewMeta;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class ZhiweiApi {
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
        var client = HttpClient.newHttpClient();
        var apiUri = "https://search-zhiwei-card.ninesuns-lin.workers.dev/api/v1/view/whole/filter?orgId=771ac1a5-fca5-4af2-b744-27b16e989b18";
        var request = HttpRequest.newBuilder()
                .uri(URI.create(apiUri))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();
        try {
            return Optional.ofNullable(client.sendAsync(request, new JsonBodyHandler<>(ViewMeta.class)));
        } catch (Exception e) {
            return Optional.empty();
        }
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
