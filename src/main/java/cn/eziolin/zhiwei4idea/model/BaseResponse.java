package cn.eziolin.zhiwei4idea.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class BaseResponse {
    public final int result;
    public final ViewMeta resultValue;

    public BaseResponse(@JsonProperty("result") int result,
                        @JsonProperty("resultValue") ViewMeta resultValue) {
        this.result = result;
        this.resultValue = resultValue;
    }
}
