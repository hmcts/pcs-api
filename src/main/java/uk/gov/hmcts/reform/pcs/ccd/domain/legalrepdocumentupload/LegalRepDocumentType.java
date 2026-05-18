package uk.gov.hmcts.reform.pcs.ccd.domain.legalrepdocumentupload;

import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.ccd.sdk.api.HasLabel;

@AllArgsConstructor
@Getter
public enum LegalRepDocumentType implements HasLabel {

    PHOTOGRAPHIC_EVIDENCE("Photographic evidence"),
    POLICE_REPORT("Police report"),
    WITNESS_STATEMENT("Witness statement"),
    OTHER("Other document");

    private final String label;

}
