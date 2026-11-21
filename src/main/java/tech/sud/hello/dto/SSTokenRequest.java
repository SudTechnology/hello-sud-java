package tech.sud.hello.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/** JSON request body for the corresponding token operation. */
public class SSTokenRequest {
    @JsonProperty("ss_token")
    private String ssToken;

    public String getSsToken() {
        return ssToken;
    }

    public void setSsToken(String ssToken) {
        this.ssToken = ssToken;
    }
}
