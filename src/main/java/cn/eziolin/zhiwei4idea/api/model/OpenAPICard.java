package cn.eziolin.zhiwei4idea.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * openApi 中返回的卡片结构<br>
 * 挺丰富的 但是现在只用这几个<br>
 * 主要是用来拿卡片 ID 和 价值流中的状态
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class OpenAPICard {
  public final String id;
  /** 卡片编号 */
  public final String code;
  /** 卡片名称 aka 标题 */
  public final String name;
  /** 卡片当前价值流ID */
  public final String currentStreamId;
  /** 卡片当前价值流状态ID */
  public final String currentStatusId;

  public OpenAPICard(
      @JsonProperty("id") String id,
      @JsonProperty("displayCode") String code,
      @JsonProperty("name") String name,
      @JsonProperty("currentStreamId") String currentStreamId,
      @JsonProperty("currentStatusId") String currentStatusId) {
    this.id = id;
    this.code = code;
    this.name = name;
    this.currentStreamId = currentStreamId;
    this.currentStatusId = currentStatusId;
  }
}
