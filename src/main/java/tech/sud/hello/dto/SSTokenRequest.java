package tech.sud.hello.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/** JSON request body for the corresponding token operation. */
public class SSTokenRequest {
    @JsonProperty("ss_token")
    @JsonDeserialize(using = StrictStringDeserializer.class)
    private String ssToken;

    public String getSsToken() {
        return ssToken;
    }

    public void setSsToken(String ssToken) {
        this.ssToken = ssToken;
    }
}
