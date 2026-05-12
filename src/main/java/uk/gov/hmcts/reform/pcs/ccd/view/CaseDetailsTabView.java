package uk.gov.hmcts.reform.pcs.ccd.view;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantType;
import uk.gov.hmcts.reform.pcs.ccd.domain.NoticeServedDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.NoticeServiceMethod;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceType;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.AssuredAdditionalDiscretionaryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.AssuredAdditionalMandatoryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.AssuredDiscretionaryGround;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.AssuredMandatoryGround;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.AssuredNoArrearsPossessionGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.AssuredRentArrearsGround;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.AssuredRentArrearsPossessionGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.IntroductoryDemotedOrOtherGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.IntroductoryDemotedOtherGroundsForPossession;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.SecureAntisocialAdditionalGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.SecureOrFlexibleDiscretionaryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.SecureOrFlexibleDiscretionaryGroundsAlternativeAccomm;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.SecureOrFlexibleMandatoryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.SecureOrFlexibleMandatoryGroundsAlternativeAccomm;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.SecureOrFlexiblePossessionGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.details.ActionsTakenTabDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.details.CaseDetailsTab;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.details.ClaimTabDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.details.GroundsForPossessionTabDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.details.NoticeTabDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.details.TenancyLicenceTabDetails;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;

import static uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceType.ASSURED_TENANCY;
import static uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceType.DEMOTED_TENANCY;
import static uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceType.FLEXIBLE_TENANCY;
import static uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceType.INTRODUCTORY_TENANCY;
import static uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceType.OTHER;
import static uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceType.SECURE_TENANCY;

@Component
public class CaseDetailsTabView {

    private static final String NO_ANSWER = " ";
    private static final DateTimeFormatter PATTERN = DateTimeFormatter.ofPattern("d MMMM yyyy");
    private static final String SEPARATE = ", ";

    public CaseDetailsTab buildCaseDetailsTab(PCSCase pcsCase) {
        ClaimTabDetails claimTabDetails = buildClaimTabDetails(pcsCase);
        GroundsForPossessionTabDetails groundsForPossessionTabDetails = buildGroundsForPossessionTabDetails(pcsCase);
        TenancyLicenceTabDetails tenancyLicenceTabDetails = buildTenancyLicenceTabDetails(pcsCase);
        NoticeTabDetails noticeTabDetails = buildNoticeTabDetails(pcsCase);
        ActionsTakenTabDetails actionsTakenTabDetails = buildActionsTakenTabDetails(pcsCase);

        return CaseDetailsTab.builder()
            .claimDetails(claimTabDetails)
            .propertyAddress(pcsCase.getPropertyAddress())
            .groundsForPossessionDetails(groundsForPossessionTabDetails)
            .tenancyLicenceDetails(tenancyLicenceTabDetails)
            .noticeDetails(noticeTabDetails)
            .actionsTakenDetails(actionsTakenTabDetails)
            .build();
    }

    private ClaimTabDetails buildClaimTabDetails(PCSCase pcsCase) {
        String claimantType = pcsCase.getClaimantType() != null
            ? ClaimantType.fromName(pcsCase.getClaimantType().getValueCode()).getLabel() : NO_ANSWER;
        VerticalYesNo trespassClaim = pcsCase.getClaimAgainstTrespassers();

        return ClaimTabDetails.builder()
            .claimantType(claimantType)
            .trespassClaim(trespassClaim != null ? trespassClaim.getLabel() : NO_ANSWER)
            .build();
    }

    private GroundsForPossessionTabDetails buildGroundsForPossessionTabDetails(PCSCase pcsCase) {
        AssuredRentArrearsPossessionGrounds assuredRentArrearsPossessionGrounds =
            pcsCase.getAssuredRentArrearsPossessionGrounds();
        AssuredNoArrearsPossessionGrounds  noRentArrearsPossessionGrounds = pcsCase.getNoRentArrearsGroundsOptions();
        SecureOrFlexiblePossessionGrounds secureOrFlexiblePossessionGrounds =
            pcsCase.getSecureOrFlexiblePossessionGrounds();
        IntroductoryDemotedOtherGroundsForPossession introductoryDemotedOrOtherGroundsForPossession =
            pcsCase.getIntroductoryDemotedOrOtherGroundsForPossession();

        if(
            assuredRentArrearsPossessionGrounds == null &&
            noRentArrearsPossessionGrounds == null &&
            secureOrFlexiblePossessionGrounds == null
        ) {
            return GroundsForPossessionTabDetails.builder()
                .grounds(NO_ANSWER)
                .build();
        }

        String grounds = NO_ANSWER;
        String otherGroundsDescription = "";
        TenancyLicenceDetails tenancyLicenceDetails = pcsCase.getTenancyLicenceDetails();
        TenancyLicenceType tenancyType = tenancyLicenceDetails != null ?
            tenancyLicenceDetails.getTypeOfTenancyLicence() : null;

        if (
            assuredRentArrearsPossessionGrounds != null &&
            pcsCase.getClaimDueToRentArrears() == YesOrNo.YES &&
            tenancyType == ASSURED_TENANCY
        ) {
            grounds = getAssuredRentArrearsPossessionGrounds(assuredRentArrearsPossessionGrounds);
        } else if (
            noRentArrearsPossessionGrounds != null &&
            pcsCase.getClaimDueToRentArrears() == YesOrNo.NO &&
            tenancyType == ASSURED_TENANCY
        ) {
            grounds = getAssuredNoArrearsPossessionGrounds(noRentArrearsPossessionGrounds);
        } else if (
            secureOrFlexiblePossessionGrounds != null &&
            (tenancyType == SECURE_TENANCY || tenancyType == FLEXIBLE_TENANCY)
        ) {
            grounds = getSecureOrFlexiblePossessionGrounds(secureOrFlexiblePossessionGrounds);
        } else if (
            introductoryDemotedOrOtherGroundsForPossession != null &&
            (tenancyType == INTRODUCTORY_TENANCY || tenancyType == DEMOTED_TENANCY || tenancyType == OTHER)
        ) {
            grounds = getIntroductoryDemotedOrOtherGrounds(introductoryDemotedOrOtherGroundsForPossession);
            otherGroundsDescription = introductoryDemotedOrOtherGroundsForPossession.getOtherGroundDescription();
        }

        return GroundsForPossessionTabDetails
            .builder()
            .grounds(grounds)
            .otherGroundsDescription(otherGroundsDescription)
            .build();
    }

    private String getAssuredRentArrearsPossessionGrounds(AssuredRentArrearsPossessionGrounds grounds) {
        StringBuilder allGrounds = new StringBuilder();
        Set<AssuredRentArrearsGround> rentArrearsGrounds =
            grounds.getRentArrearsGrounds();
        Set<AssuredAdditionalMandatoryGrounds> additionalMandatoryGrounds =
            grounds.getAdditionalMandatoryGrounds();
        Set<AssuredAdditionalDiscretionaryGrounds> additionalDiscretionaryGrounds =
            grounds.getAdditionalDiscretionaryGrounds();

        if (rentArrearsGrounds != null) {
            for (AssuredRentArrearsGround rentArrearsGround : rentArrearsGrounds) {
                allGrounds.append(rentArrearsGround.getLabel()).append(SEPARATE);
            }
        }

        if (additionalMandatoryGrounds != null) {
            for(AssuredAdditionalMandatoryGrounds mandatoryGround : additionalMandatoryGrounds) {
                allGrounds.append(mandatoryGround.getLabel()).append(SEPARATE);
            }
        }

        if (additionalDiscretionaryGrounds != null) {
            for(AssuredAdditionalDiscretionaryGrounds discretionaryGround : additionalDiscretionaryGrounds) {
                allGrounds.append(discretionaryGround.getLabel()).append(SEPARATE);
            }
        }

        return allGrounds.toString();
    }

    private String getAssuredNoArrearsPossessionGrounds(AssuredNoArrearsPossessionGrounds grounds) {
        StringBuilder allGrounds = new StringBuilder();
        Set<AssuredMandatoryGround> mandatoryGrounds = grounds.getMandatoryGrounds();
        Set<AssuredDiscretionaryGround> discretionaryGrounds =
            grounds.getDiscretionaryGrounds();

        for(AssuredMandatoryGround mandatoryGround : mandatoryGrounds) {
            allGrounds.append(mandatoryGround.getLabel()).append(SEPARATE);
        }

        for(AssuredDiscretionaryGround discretionaryGround : discretionaryGrounds) {
            allGrounds.append(discretionaryGround.getLabel()).append(SEPARATE);
        }

        return allGrounds.toString();
    }

    private String getSecureOrFlexiblePossessionGrounds(SecureOrFlexiblePossessionGrounds grounds) {
        StringBuilder allGrounds = new StringBuilder();

        Set<SecureOrFlexibleDiscretionaryGrounds> secureOrFlexibleDiscretionaryGrounds =
            grounds.getSecureOrFlexibleDiscretionaryGrounds();
        Set<SecureOrFlexibleMandatoryGrounds> secureOrFlexibleMandatoryGrounds =
            grounds.getSecureOrFlexibleMandatoryGrounds();
        Set<SecureOrFlexibleDiscretionaryGroundsAlternativeAccomm> secureOrFlexibleDiscretionaryGroundsAlt
            = grounds.getSecureOrFlexibleDiscretionaryGroundsAlt();
        Set<SecureOrFlexibleMandatoryGroundsAlternativeAccomm> secureOrFlexibleMandatoryGroundsAlt
            = grounds.getSecureOrFlexibleMandatoryGroundsAlt();
        Set<SecureAntisocialAdditionalGrounds> secureAntisocialAdditionalGrounds
            = grounds.getSecureAntisocialAdditionalGrounds();

        if (secureOrFlexibleDiscretionaryGrounds != null) {
            for (SecureOrFlexibleDiscretionaryGrounds discretionaryGround : secureOrFlexibleDiscretionaryGrounds) {
                allGrounds.append(discretionaryGround.getLabel()).append(SEPARATE);
            }
        }

        if (secureOrFlexibleMandatoryGrounds != null) {
            for (SecureOrFlexibleMandatoryGrounds mandatoryGround : secureOrFlexibleMandatoryGrounds) {
                allGrounds.append(mandatoryGround.getLabel()).append(SEPARATE);
            }
        }

        if (secureOrFlexibleDiscretionaryGroundsAlt != null) {
            for (
                SecureOrFlexibleDiscretionaryGroundsAlternativeAccomm altDiscretionaryGround :
                secureOrFlexibleDiscretionaryGroundsAlt
            ) {
                allGrounds.append(altDiscretionaryGround.getLabel()).append(SEPARATE);
            }
        }

        if (secureOrFlexibleMandatoryGroundsAlt != null) {
            for (
                SecureOrFlexibleMandatoryGroundsAlternativeAccomm altMandatoryGround :
                secureOrFlexibleMandatoryGroundsAlt
            ) {
                allGrounds.append(altMandatoryGround).append(SEPARATE);
            }
        }

        if (secureAntisocialAdditionalGrounds != null) {
            for (SecureAntisocialAdditionalGrounds antisocialGround : secureAntisocialAdditionalGrounds) {
                allGrounds.append(antisocialGround.getLabel()).append(SEPARATE);
            }
        }

        return allGrounds.toString();
    }

    private String getIntroductoryDemotedOrOtherGrounds(
        IntroductoryDemotedOtherGroundsForPossession grounds
    ) {
        StringBuilder allGrounds = new StringBuilder();

        Set<IntroductoryDemotedOrOtherGrounds> introductoryDemotedOrOtherGrounds =
            grounds.getIntroductoryDemotedOrOtherGrounds();

        if (introductoryDemotedOrOtherGrounds != null) {
            for (IntroductoryDemotedOrOtherGrounds otherGrounds : introductoryDemotedOrOtherGrounds) {
                allGrounds.append(otherGrounds.getLabel()).append(SEPARATE);
            }
        }

        return allGrounds.toString();
    }

    private TenancyLicenceTabDetails buildTenancyLicenceTabDetails(PCSCase pcsCase) {
        TenancyLicenceDetails tenancyLicenceDetails = pcsCase.getTenancyLicenceDetails();
        if (tenancyLicenceDetails == null) {
            return TenancyLicenceTabDetails.builder()
                .typeOfTenancyLicence(NO_ANSWER)
                .tenancyLicenceDate(NO_ANSWER)
                .hasCopyOfTenancyLicence(NO_ANSWER)
                .build();
        }

        TenancyLicenceType tenancyType = tenancyLicenceDetails.getTypeOfTenancyLicence();
        LocalDate tenancyDate = tenancyLicenceDetails.getTenancyLicenceDate();
        VerticalYesNo hasTenancyLicence = tenancyLicenceDetails.getHasCopyOfTenancyLicence();

        return TenancyLicenceTabDetails.builder()
            .typeOfTenancyLicence(tenancyType != null ? tenancyType.getLabel() : NO_ANSWER)
            .tenancyLicenceDate(tenancyDate != null ? tenancyDate.format(PATTERN) : NO_ANSWER)
            .hasCopyOfTenancyLicence(hasTenancyLicence != null ? hasTenancyLicence.getLabel() : NO_ANSWER)
            .tenancyLicenceDocuments(tenancyLicenceDetails.getTenancyLicenceDocuments())
            .reasonsForNoTenancyLicenceDocuments(tenancyLicenceDetails.getReasonsForNoTenancyLicenceDocuments())
            .build();
    }

    private NoticeTabDetails buildNoticeTabDetails(PCSCase pcsCase) {
        if (pcsCase.getNoticeServed() == null) {
            return NoticeTabDetails.builder()
                .noticeServed(NO_ANSWER)
                .noticeMethod(NO_ANSWER)
                .noticeDate(NO_ANSWER)
                .build();
        }

        NoticeTabDetails noticeTabDetails = NoticeTabDetails.builder()
            .noticeServed(pcsCase.getNoticeServed().getValue())
            .noticeMethod(NO_ANSWER)
            .noticeDate(NO_ANSWER)
            .build();

        NoticeServedDetails noticeServedDetails = pcsCase.getNoticeServedDetails();
        if (noticeTabDetails != null) {
            NoticeServiceMethod method = noticeServedDetails.getNoticeServiceMethod();
            noticeTabDetails.setNoticeDocuments(noticeServedDetails.getNoticeDocuments());

            if (method != null) {
                noticeTabDetails.setNoticeMethod(method.getLabel());
                switch (method) {
                    case FIRST_CLASS_POST -> {
                        LocalDate date = noticeServedDetails.getNoticePostedDate();
                        noticeTabDetails.setNoticeDate(date != null ? date.format(PATTERN) : NO_ANSWER);
                    }
                    case DELIVERED_PERMITTED_PLACE -> {
                        LocalDate date = noticeServedDetails.getNoticeDeliveredDate();
                        noticeTabDetails.setNoticeDate(date != null ? date.format(PATTERN) : NO_ANSWER);
                    }
                    case PERSONALLY_HANDED -> {
                        LocalDateTime dateTime = noticeServedDetails.getNoticeHandedOverDateTime();
                        String name = noticeTabDetails.getNoticePersonName();
                        noticeTabDetails.setNoticeDate(dateTime != null ? dateTime.format(PATTERN) : NO_ANSWER);
                        noticeTabDetails.setNoticePersonName(name != null ? name : NO_ANSWER);
                    }
                    case EMAIL -> {
                        LocalDateTime dateTime = noticeServedDetails.getNoticeEmailSentDateTime();
                        String emailAddress = noticeServedDetails.getNoticeEmailAddress();
                        noticeTabDetails.setNoticeDate( dateTime != null ? dateTime.format(PATTERN) : NO_ANSWER);
                        noticeTabDetails.setNoticeEmailAddress(emailAddress != null ? emailAddress : NO_ANSWER);
                    }
                    case OTHER_ELECTRONIC -> {
                        LocalDateTime dateTime = noticeServedDetails.getNoticeOtherElectronicDateTime();
                        noticeTabDetails.setNoticeDate(dateTime != null ? dateTime.format(PATTERN) : NO_ANSWER);
                    }
                    case OTHER -> {
                        LocalDateTime dateTime = noticeServedDetails.getNoticeOtherDateTime();
                        String explanation = noticeTabDetails.getNoticeOtherExplanation();
                        noticeTabDetails.setNoticeDate(dateTime != null ? dateTime.format(PATTERN) : NO_ANSWER);
                        noticeTabDetails.setNoticeOtherExplanation(explanation != null ? explanation : NO_ANSWER);
                    }
                };
            }
        }

        return noticeTabDetails;
    }

    private ActionsTakenTabDetails buildActionsTakenTabDetails(PCSCase pcsCase) {
        VerticalYesNo preactionProtocol = pcsCase.getPreActionProtocolCompleted();
        VerticalYesNo mediationAttempted = pcsCase.getMediationAttempted();
        VerticalYesNo settlementAttempted = pcsCase.getSettlementAttempted();

        return ActionsTakenTabDetails.builder()
            .preactionProtocolFollowed(preactionProtocol != null ? preactionProtocol.getLabel() :NO_ANSWER)
            .mediationAttempted(mediationAttempted != null ? mediationAttempted.getLabel() : NO_ANSWER)
            .settlementAttempted(settlementAttempted != null ? settlementAttempted.getLabel() : NO_ANSWER)
            .build();
    }
}
