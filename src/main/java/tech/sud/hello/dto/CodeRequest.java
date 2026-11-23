package tech.sud.hello.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/** JSON request body for the corresponding token operation. */
public class CodeRequest {
    @JsonProperty("code")
    @JsonDeserialize(using = StrictStringDeserializer.class)
    private String code;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
