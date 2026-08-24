package uk.gov.hmcts.reform.pcs.ccd.domain.documentupload;

import lombok.Getter;
import uk.gov.hmcts.ccd.sdk.api.HasLabel;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;

import java.util.Set;

import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry.ENGLAND;
import static uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry.WALES;

@Getter
public enum CaseworkerDocumentType implements HasLabel {

    WITNESS_STATEMENT("Witness statement", Set.of(ENGLAND, WALES)),
    RENT_STATEMENT("Rent statement", Set.of(ENGLAND, WALES)),
    TENANCY_AGREEMENT("Tenancy agreement", Set.of(ENGLAND)),
    OCCUPATION_LICENCE("Occupation contract or licence", Set.of(WALES)),
    CERTIFICATE_OF_SERVICE("Certificate of service", Set.of(ENGLAND, WALES)),
    ENERGY_PERFORMANCE_CERTIFICATE("Energy performance certificate", Set.of(WALES)),
    GAS_SAFETY_CERTIFICATE("Gas safety certificate", Set.of(WALES)),
    EICR_REPORT("Electrical Installation Condition Report (EICR)", Set.of(WALES)),
    CORRESPONDENCE_BETWEEN_PARTIES("Correspondence between parties", Set.of(ENGLAND, WALES)),
    CORRESPONDENCE_FROM_CLAIMANT("Correspondence from claimant", Set.of(ENGLAND, WALES)),
    CORRESPONDENCE_FROM_DEFENDANT("Correspondence from defendant", Set.of(ENGLAND, WALES)),
    POSSESSION_NOTICE("Possession notice", Set.of(ENGLAND, WALES)),
    NOTICE_FOR_SERVICE_OUT_OF_JURISDICTION("Notice for service out of the jurisdiction", Set.of(ENGLAND, WALES)),
    PHOTOGRAPHIC_EVIDENCE("Photographic evidence", Set.of(ENGLAND, WALES)),
    INSPECTION_OR_REPORT("Inspection or report", Set.of(ENGLAND, WALES)),
    AMENDED_CLAIM_FORM("Amended claim form", Set.of(ENGLAND, WALES)),
    PART_20_COUNTERCLAIM("Part 20 counterclaim", Set.of(ENGLAND, WALES)),
    CERTIFICATE_OF_SUITABILITY_AS_LF("Certificate of suitability as litigation friend", Set.of(ENGLAND, WALES)),
    LEGAL_AID_CERTIFICATE("Legal aid certificate", Set.of(ENGLAND, WALES)),
    NOTICE_OF_HEARING("Notice of hearing", Set.of(ENGLAND, WALES)),
    WITH_NOTICE_ORDER("With notice order", Set.of(ENGLAND, WALES)),
    WITHOUT_NOTICE_ORDER("Without notice order", Set.of(ENGLAND, WALES)),
    NOTICE_OF_ALLOCATION_TO_TRACK("Notice of allocation to track", Set.of(ENGLAND, WALES)),
    OTHER("Other document", Set.of(ENGLAND, WALES));

    private final String label;
    private final Set<LegislativeCountry> legislativeCountries;

    CaseworkerDocumentType(String label, Set<LegislativeCountry> legislativeCountries) {
        this.label = label;
        this.legislativeCountries = requireNonNull(legislativeCountries, "legislative countries must not be null");
    }

    public boolean isApplicableFor(LegislativeCountry legislativeCountry) {
        return legislativeCountries.contains(legislativeCountry);
    }

}
