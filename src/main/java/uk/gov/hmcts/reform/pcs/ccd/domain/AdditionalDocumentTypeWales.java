package uk.gov.hmcts.reform.pcs.ccd.domain;

import lombok.Getter;
import uk.gov.hmcts.ccd.sdk.api.HasLabel;

import java.util.Arrays;

@Getter
public enum AdditionalDocumentTypeWales implements HasLabel {

    WITNESS_STATEMENT("Witness statement"),
    RENT_STATEMENT("Rent statement"),
    OCCUPATION_LICENCE("Occupation contract or licence"),
    ENERGY_PERFORMANCE_CERTIFICATE("Energy performance certificate"),
    GAS_SAFETY_CERTIFICATE("Gas safety certificate"),
    EICR_REPORT("Electrical Installation Condition Report (EICR)"),
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

    AdditionalDocumentTypeWales(String label) {
        this.label = label;
    }

    public static AdditionalDocumentTypeWales getValueFromLabel(String label) {
        return Arrays.stream(values()).filter(v -> v.getLabel().equals(label)).findFirst()
            .orElseThrow(() -> new IllegalArgumentException("No AdditionalDocumentTypeWales with label: " + label));
    }

}
