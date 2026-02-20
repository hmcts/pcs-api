package uk.gov.hmcts.reform.pcs.ccd.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.ccd.sdk.api.HasLabel;

@AllArgsConstructor
@Getter
public enum AdditionalDocumentType implements HasLabel {

    WITNESS_STATEMENT("Witness statement"),
    RENT_STATEMENT("Rent statement"),
    TENANCY_AGREEMENT("Tenancy agreement"),
    CERTIFICATE_OF_SERVICE("Certificate of service"),
    CORRESPONDENCE_FROM_DEFENDANT("Correspondence from defendant"),
    CORRESPONDENCE_FROM_CLAIMANT("Correspondence from claimant"),
    POSSESSION_NOTICE("Possession notice"),
    NOTICE_FOR_SERVICE_OUT_OF_JURISDICTION("Notice for service out of the jurisdiction"),
    PHOTOGRAPHIC_EVIDENCE("Photographic evidence"),
    INSPECTION_OR_REPORT("Inspection or report"),
    CERTIFICATE_OF_SUITABILITY_AS_LF("Certificate of suitability as litigation friend"),
    LEGAL_AID_CERTIFICATE("Legal aid certificate"),
    OTHER("Other document");

    private final String label;

}
