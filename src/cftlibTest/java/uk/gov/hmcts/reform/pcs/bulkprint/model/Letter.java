package uk.gov.hmcts.reform.pcs.bulkprint.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.reform.sendletter.api.model.v3.Document;

import java.util.List;

@Data
@Builder
public class Letter {

    @JsonProperty("documents")
    public final List<Document> documents;

    @JsonProperty("type")
    public final String type;

    @JsonProperty("additional_data")
    public final AdditionalData additionalData;

}
