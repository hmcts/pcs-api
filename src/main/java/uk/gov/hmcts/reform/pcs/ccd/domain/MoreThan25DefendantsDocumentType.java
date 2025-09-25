package uk.gov.hmcts.reform.pcs.ccd.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.ccd.sdk.api.HasLabel;

@Getter
@AllArgsConstructor
public enum MoreThan25DefendantsDocumentType implements HasLabel {

    @JsonProperty("PDF")
    PDF(".pdf"),

    @JsonProperty("DOCX")
    DOCX(".docx");

    private final String extension;

    @Override
    public String getLabel() {
        return name();
    }
}
