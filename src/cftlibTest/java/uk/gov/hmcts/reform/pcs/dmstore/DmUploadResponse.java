package uk.gov.hmcts.reform.pcs.dmstore;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Builder
@Value
@JsonIgnoreProperties(ignoreUnknown = true)
public class DmUploadResponse {

    @JsonProperty("_embedded")
    Embedded embedded;

    @Builder
    @Value
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Embedded {
        List<Document> documents;
    }

}
