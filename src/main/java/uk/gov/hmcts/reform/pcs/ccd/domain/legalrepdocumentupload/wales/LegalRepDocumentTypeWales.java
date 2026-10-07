package uk.gov.hmcts.reform.pcs.ccd.domain.legalrepdocumentupload.wales;

import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.ccd.sdk.api.HasLabel;

@AllArgsConstructor
@Getter
public enum LegalRepDocumentTypeWales implements HasLabel {

    RENT_STATEMENT("Rent or payments"),
    OCCUPATION_LICENCE("Occupation contract or licence"),
    CORRESPONDENCE_FROM_DEFENDANT("Correspondence from Defendant"),
    CORRESPONDENCE_FROM_CLAIMANT("Correspondence from Claimant"),
    PHOTOGRAPHIC_EVIDENCE("Photographic evidence"),
    CERTIFICATE_OF_SUITABILITY_AS_LF("Certificate of suitability as litigation friend"),
    LEGAL_AID_CERTIFICATE("Legal aid certificate"),
    OTHER("Other document"),
    WITNESS_STATEMENT("Witness statement");

    private final String label;

}
