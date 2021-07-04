package cn.eziolin.zhiwei4idea.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Card {
    public final String id;
    public final String code;
    public final String name;

    public Card(@JsonProperty("id") String id,
                @JsonProperty("code") String code,
                @JsonProperty("name") String name) {
        this.id = id;
        this.code = code;
        this.name = name;
    }
}
