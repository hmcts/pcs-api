package uk.gov.hmcts.reform.pcs.ccd.view;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantType;
import uk.gov.hmcts.reform.pcs.ccd.domain.NoticeServedDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.NoticeServiceMethod;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceType;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.ActionsTakenTabDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.CaseDetailsTab;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.ClaimTabDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.GroundsForPossessionTabDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.NoticeTabDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.TenancyLicenceTabDetails;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class CaseDetailsTabView {

    private static final String NO_ANSWER = " ";
    private static final DateTimeFormatter pattern = DateTimeFormatter.ofPattern("d MMM yyyy");

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
        return GroundsForPossessionTabDetails
            .builder()
            .build();
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
            .tenancyLicenceDate(tenancyDate != null ? tenancyDate.format(pattern) : NO_ANSWER)
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
                        noticeTabDetails.setNoticeDate(date != null ? date.format(pattern) : NO_ANSWER);
                    }
                    case DELIVERED_PERMITTED_PLACE -> {
                        LocalDate date = noticeServedDetails.getNoticeDeliveredDate();
                        noticeTabDetails.setNoticeDate(date != null ? date.format(pattern) : NO_ANSWER);
                    }
                    case PERSONALLY_HANDED -> {
                        LocalDateTime dateTime = noticeServedDetails.getNoticeHandedOverDateTime();
                        String name = noticeTabDetails.getNoticePersonName();
                        noticeTabDetails.setNoticeDate(dateTime != null ? dateTime.format(pattern) : NO_ANSWER);
                        noticeTabDetails.setNoticePersonName(name != null ? name : NO_ANSWER);
                    }
                    case EMAIL -> {
                        LocalDateTime dateTime = noticeServedDetails.getNoticeEmailSentDateTime();
                        String emailAddress = noticeServedDetails.getNoticeEmailAddress();
                        noticeTabDetails.setNoticeDate( dateTime != null ? dateTime.format(pattern) : NO_ANSWER);
                        noticeTabDetails.setNoticeEmailAddress(emailAddress != null ? emailAddress : NO_ANSWER);
                    }
                    case OTHER_ELECTRONIC -> {
                        LocalDateTime dateTime = noticeServedDetails.getNoticeOtherElectronicDateTime();
                        noticeTabDetails.setNoticeDate(dateTime != null ? dateTime.format(pattern) : NO_ANSWER);
                    }
                    case OTHER -> {
                        LocalDateTime dateTime = noticeServedDetails.getNoticeOtherDateTime();
                        String explanation = noticeTabDetails.getNoticeOtherExplanation();
                        noticeTabDetails.setNoticeDate(dateTime != null ? dateTime.format(pattern) : NO_ANSWER);
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
