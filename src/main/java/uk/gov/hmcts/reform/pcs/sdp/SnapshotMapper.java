package uk.gov.hmcts.reform.pcs.sdp;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.sdp.model.ClaimSnapshot;
import uk.gov.hmcts.reform.pcs.sdp.model.ClaimSnapshotRecord;
import uk.gov.hmcts.reform.pcs.sdp.model.Producer;
import uk.gov.hmcts.reform.pcs.sdp.model.SnapshotDimensions;
import uk.gov.hmcts.reform.pcs.sdp.model.SnapshotMilestones;
import uk.gov.hmcts.reform.pcs.sdp.model.SnapshotParty;

/**
 * Maps PCS claim source values into SDP snapshot model fields.
 */
@Component
public class SnapshotMapper {

    private static final String CONTRACT_VERSION = "4.1.0";
    private static final String PRODUCER_SYSTEM = "possessions-ccd";
    private static final String CLAIM_TYPE_DEFAULT = "AAA3-PLD";
    private static final String CLAIM_TYPE_SOCIAL = "AAA3-SLD";
    private static final String CLAIM_TYPE_TRESPASS = "AAA3-TCM";
    private static final String TENANCY_SECURE = "AAA3-STY";
    private static final String TENANCY_OTHER = "AAA3-OTH";
    private static final String SUBMITTED_TYPE_DIGITAL = "Digital";

    /**
     * Assembles a full SDP snapshot record from mapped claim parts and export metadata.
     */
    public ClaimSnapshotRecord mapRecord(String environment,
                                         String extractDate,
                                         String snapshotId,
                                         String publishedDate,
                                         ClaimSnapshot claim) {
        return ClaimSnapshotRecord.builder()
            .contractVersion(CONTRACT_VERSION)
            .producer(Producer.builder()
                .system(PRODUCER_SYSTEM)
                .environment(environment)
                .build())
            .extractDate(extractDate)
            .snapshotId(snapshotId)
            .publishedDate(publishedDate)
            .claim(claim)
            .build();
    }

    public SnapshotDimensions mapDimensions(String claimantType,
                                            String againstTrespassers,
                                            String languageUsed,
                                            String tenancyType,
                                            String caseManagementLocation) {
        return SnapshotDimensions.builder()
            .claimType(resolveClaimType(claimantType, againstTrespassers))
            .claimSubType(resolveClaimSubType(claimantType))
            .language(resolveLanguage(languageUsed))
            .tenancyType(resolveTenancyType(tenancyType))
            .submittedType(SUBMITTED_TYPE_DIGITAL)
            .caseManagementLocationCode(resolveCaseManagementLocationCode(caseManagementLocation))
            .build();
    }

    public SnapshotMilestones mapMilestones(String servingMethod,
                                            String noticeDate,
                                            String claimSubmittedDate,
                                            String claimIssuedDate) {
        return SnapshotMilestones.builder()
            .postalClaimReceivedByCourtDate(resolvePostalReceivedDate(servingMethod, noticeDate))
            .claimSubmittedToSystemDate(claimSubmittedDate)
            .claimFeePaidDate(claimSubmittedDate)
            .issueClaimDate(claimIssuedDate)
            .build();
    }

    public SnapshotParty mapParty(String partyId, String role) {
        return SnapshotParty.builder()
            .partyId(partyId)
            .partyRole(resolvePartyRole(role))
            .build();
    }

    private String resolveClaimType(String claimantType, String againstTrespassers) {
        if ("PROVIDER_OF_SOCIAL_HOUSING".equals(claimantType)) {
            return CLAIM_TYPE_SOCIAL;
        }
        if ("YES".equals(againstTrespassers)) {
            return CLAIM_TYPE_TRESPASS;
        }
        return CLAIM_TYPE_DEFAULT;
    }

    private String resolveClaimSubType(String claimantType) {
        if (claimantType == null || claimantType.isBlank()) {
            return "Private";
        }
        String withoutPrefix = claimantType.replace("PROVIDER_OF_", "");
        return withoutPrefix.substring(0, 1).toUpperCase() + withoutPrefix.substring(1).toLowerCase();
    }

    private String resolveLanguage(String languageUsed) {
        return "WELSH".equals(languageUsed) ? "cym" : "eng";
    }

    private String resolveTenancyType(String tenancyType) {
        return "SECURE_TENANCY".equals(tenancyType) ? TENANCY_SECURE : TENANCY_OTHER;
    }

    private String resolveCaseManagementLocationCode(String caseManagementLocation) {
        String locCode = (caseManagementLocation == null || caseManagementLocation.isBlank())
            ? "0000"
            : caseManagementLocation;
        return "MRD-CRT-" + String.format("%4s", locCode).replace(' ', '0');
    }

    private String resolvePostalReceivedDate(String servingMethod, String noticeDate) {
        return "FIRST_CLASS_POST".equals(servingMethod) ? noticeDate : null;
    }

    private String resolvePartyRole(String role) {
        if ("CLAIMANT".equals(role)) {
            return "Claimant";
        }
        if ("DEFENDANT".equals(role)) {
            return "Defendant";
        }
        return role;
    }
}
