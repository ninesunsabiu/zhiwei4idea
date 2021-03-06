package cn.eziolin.zhiwei4idea.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class BaseResponse<T> {
  public final int result;
  public final T resultValue;

  public BaseResponse(
      @JsonProperty("result") int result, @JsonProperty("resultValue") T resultValue) {
    this.result = result;
    this.resultValue = resultValue;
  }
}
