package uk.gov.hmcts.reform.pcs.sdp;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.sdp.model.ClaimSnapshot;
import uk.gov.hmcts.reform.pcs.sdp.model.ClaimSnapshotRecord;
import uk.gov.hmcts.reform.pcs.sdp.model.Producer;
import uk.gov.hmcts.reform.pcs.sdp.model.SnapshotDimensions;
import uk.gov.hmcts.reform.pcs.sdp.model.SnapshotGround;
import uk.gov.hmcts.reform.pcs.sdp.model.SnapshotMilestones;
import uk.gov.hmcts.reform.pcs.sdp.model.SnapshotParty;
import uk.gov.hmcts.reform.pcs.sdp.model.SnapshotPaymentReference;
import uk.gov.hmcts.reform.pcs.sdp.model.SnapshotResponse;

/**
 * Maps PCS claim source values into SDP snapshot model fields.
 */
@Component
public class SnapshotMapper {

    private static final String CONTRACT_VERSION = "4.1.0";
    private static final String PRODUCER_SYSTEM = "possessions-ccd";
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
                .environment(resolveEnvironment(environment))
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
                                            String caseManagementLocation,
                                            String legislativeCountry) {
        return SnapshotDimensions.builder()
            .claimType(resolveClaimType(claimantType, againstTrespassers, legislativeCountry))
            .claimSubType(resolveClaimSubType(claimantType, againstTrespassers))
            .language(resolveLanguage(languageUsed))
            .tenancyType(resolveTenancyType(tenancyType))
            .submittedType(SUBMITTED_TYPE_DIGITAL)
            .caseManagementLocationCode(resolveCaseManagementLocationCode(caseManagementLocation))
            .build();
    }

    public SnapshotMilestones mapMilestones(String servingMethod,
                                            String noticeDate,
                                            String claimSubmittedDate,
                                            String claimIssuedDate,
                                            String claimFeePaidDate) {
        return SnapshotMilestones.builder()
            .postalClaimReceivedByCourtDate(resolvePostalReceivedDate(servingMethod, noticeDate))
            .claimSubmittedToSystemDate(claimSubmittedDate)
            .claimFeePaidDate(claimFeePaidDate)
            .issueClaimDate(claimIssuedDate)
            .build();
    }

    public SnapshotParty mapParty(String partyId, String role, String claimantType, boolean represented) {
        return SnapshotParty.builder()
            .partyId(partyId)
            .partyType("CLAIMANT".equals(role) ? resolvePartyType(claimantType) : null)
            .partyRole(resolvePartyRole(role))
            .partyRepresented(represented ? "Yes" : "No")
            .build();
    }

    public SnapshotGround mapGround(String code, String description, String category) {
        return SnapshotGround.builder()
            .groundsCode(code)
            .groundsDescription(description)
            .groundsSubType(resolveGroundsSubType(category))
            .build();
    }

    public SnapshotPaymentReference mapPayment(String paymentReference) {
        return SnapshotPaymentReference.builder()
            .paymentReference(paymentReference)
            .build();
    }

    public SnapshotResponse mapResponse(String responseId,
                                        String defendantId,
                                        String makeCounterClaim,
                                        String respondedDate) {
        return SnapshotResponse.builder()
            .responseId(responseId)
            .defendantId(defendantId)
            .responseType("YES".equals(makeCounterClaim) ? "Counter Claim" : "Defence")
            .respondedDate(respondedDate)
            .build();
    }

    private String resolveEnvironment(String environment) {
        if ("prod".equals(environment)) {
            return "prod";
        }
        if ("aat".equals(environment)) {
            return "test";
        }
        if ("preview".equals(environment) || "local".equals(environment)) {
            return "dev";
        }
        return environment;
    }

    private String resolveClaimType(String claimantType, String againstTrespassers, String legislativeCountry) {
        if ("YES".equals(againstTrespassers)) {
            return "AAA3-TCM";
        }
        if ("PROVIDER_OF_SOCIAL_HOUSING".equals(claimantType)) {
            return "AAA3-SLD";
        }
        if ("COMMUNITY_LANDLORD".equals(claimantType)) {
            return "AAA3-WSL";
        }
        if ("MORTGAGE_LENDER".equals(claimantType)) {
            return "AAA3-MOR";
        }
        if ("PRIVATE_LANDLORD".equals(claimantType) && "WALES".equals(legislativeCountry)) {
            return "AAA3-WPL";
        }
        return "AAA3-PLD";
    }

    private String resolveClaimSubType(String claimantType, String againstTrespassers) {
        if ("YES".equals(againstTrespassers)) {
            return "Possession";
        }
        if ("PROVIDER_OF_SOCIAL_HOUSING".equals(claimantType) || "COMMUNITY_LANDLORD".equals(claimantType)) {
            return "Social";
        }
        if ("MORTGAGE_LENDER".equals(claimantType)) {
            return "Mortgage";
        }
        return "Private";
    }

    private String resolveLanguage(String languageUsed) {
        if ("WELSH".equals(languageUsed)) {
            return "cym";
        }
        if ("ENGLISH_AND_WELSH".equals(languageUsed)) {
            return "engAndCym";
        }
        return "eng";
    }

    private String resolveTenancyType(String tenancyType) {
        if ("ASSURED_TENANCY".equals(tenancyType)) {
            return "AAA3-ATY";
        }
        if ("SECURE_TENANCY".equals(tenancyType)) {
            return "AAA3-STY";
        }
        if ("INTRODUCTORY_TENANCY".equals(tenancyType)) {
            return "AAA3-ITY";
        }
        if ("FLEXIBLE_TENANCY".equals(tenancyType)) {
            return "AAA3-FTY";
        }
        if ("DEMOTED_TENANCY".equals(tenancyType)) {
            return "AAA3-DTY";
        }
        if ("STANDARD_CONTRACT".equals(tenancyType)) {
            return "AAA3-STN";
        }
        if ("SECURE_CONTRACT".equals(tenancyType)) {
            return "AAA3-SEC";
        }
        return "AAA3-OTH";
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

    private String resolvePartyType(String claimantType) {
        if ("PROVIDER_OF_SOCIAL_HOUSING".equals(claimantType)) {
            return "AAA3-SHS";
        }
        if ("MORTGAGE_LENDER".equals(claimantType)) {
            return "AAA3-MPL";
        }
        if ("COMMUNITY_LANDLORD".equals(claimantType)) {
            return "AAA3-RCL";
        }
        if ("OTHER".equals(claimantType)) {
            return "AAA3-OTH";
        }
        return "AAA3-PLD";
    }

    private String resolveGroundsSubType(String category) {
        if (category == null) {
            return null;
        }
        if (category.contains("MANDATORY")) {
            return "Mandatory";
        }
        if (category.contains("DISCRETIONARY")) {
            return "Discretionary";
        }
        return category;
    }
}
