package cn.eziolin.zhiwei4idea.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class BatchEditResult {
  public final List<String> successIds;

  public BatchEditResult(@JsonProperty("successIds") List<String> successIds) {
    this.successIds = successIds;
  }
}
