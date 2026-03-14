package shared.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AiMessageRequest(
        @JsonProperty("request_id")
        String requestId,
        String text
) {}
