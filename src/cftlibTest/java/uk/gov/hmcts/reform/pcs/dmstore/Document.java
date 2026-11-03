package uk.gov.hmcts.reform.pcs.dmstore;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

import java.util.Date;
import java.util.Map;

@Builder(toBuilder = true)
@Value
@JsonIgnoreProperties(ignoreUnknown = true)
public class Document {

    Long size;
    String mimeType;
    String originalDocumentName;
    String hashToken;
    Date createdOn;
    String createdBy;
    String lastModifiedBy;
    Date modifiedOn;
    Date ttl;
    Map<String, String> metadata;
    @JsonProperty("_links")
    Links links;

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Links {
        public Link self;
        public Link binary;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Link {
        public String href;
    }
}
