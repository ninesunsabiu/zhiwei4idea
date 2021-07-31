package cn.eziolin.zhiwei4idea.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ViewMeta {

  public final List<Card> caches;

  public ViewMeta(@JsonProperty("caches") List<Card> caches) {
    this.caches = caches;
  }
}
