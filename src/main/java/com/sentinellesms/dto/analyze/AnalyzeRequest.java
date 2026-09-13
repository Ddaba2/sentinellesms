package com.sentinellesms.dto.analyze;

import jakarta.validation.constraints.AssertTrue;
import lombok.Data;

@Data
public class AnalyzeRequest {

    private String text;
    private String url;
    private String phone;
    private String language = "fr";

    @AssertTrue(message = "Fournir au moins text, url ou phone")
    public boolean isValidPayload() {
        return (text != null && !text.isBlank())
                || (url != null && !url.isBlank())
                || (phone != null && !phone.isBlank());
    }
}
