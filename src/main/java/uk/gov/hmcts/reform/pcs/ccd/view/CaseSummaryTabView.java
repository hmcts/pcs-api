package uk.gov.hmcts.reform.pcs.ccd.view;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.domain.NoticeServedDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceType;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.shared.GroundsForPossessionTabDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.summary.NoticeTabDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.shared.ReasonsForPossessionTabDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.summary.SummaryTab;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.summary.TenancyTabDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.OccupationLicenceDetailsWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.OccupationLicenceTypeWales;
import uk.gov.hmcts.reform.pcs.ccd.view.builder.AdditionalDefendantInformationTabDetailsBuilder;
import uk.gov.hmcts.reform.pcs.ccd.view.builder.ClaimantInformationTabDetailsBuilder;
import uk.gov.hmcts.reform.pcs.ccd.view.builder.DefendantInformationTabDetailsBuilder;
import uk.gov.hmcts.reform.pcs.ccd.view.builder.GroundsBuilder;
import uk.gov.hmcts.reform.pcs.ccd.view.builder.ReasonsForPossessionTabDetailsBuilder;
import uk.gov.hmcts.reform.pcs.ccd.view.builder.RentArrearsTabDetailsBuilder;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import static uk.gov.hmcts.reform.pcs.config.ClockConfiguration.UK_ZONE_ID;

@AllArgsConstructor
@Component
public class CaseSummaryTabView {

    private static final DateTimeFormatter SUMMARY_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter SUBMITTED_DATE_FORMATTER =
        DateTimeFormatter.ofPattern("d MMMM yyyy, h:mm:ssa", Locale.UK);

    private final GroundsBuilder groundsBuilder;
    private final RentArrearsTabDetailsBuilder rentArrearsTabDetailsBuilder;
    private final ReasonsForPossessionTabDetailsBuilder reasonsForPossessionTabDetailsBuilder;
    private final ClaimantInformationTabDetailsBuilder claimantInformationTabDetailsBuilder;
    private final DefendantInformationTabDetailsBuilder defendantInformationTabDetailsBuilder;
    private final AdditionalDefendantInformationTabDetailsBuilder additionalDefendantInformationTabDetailsBuilder;

    public SummaryTab buildSummaryTab(PCSCase pcsCase) {
        ReasonsForPossessionTabDetails reasonsForPossession =
            reasonsForPossessionTabDetailsBuilder.buildSummaryReasonsForPossession(pcsCase);
        String dateSubmitted = formatSubmittedDate(pcsCase.getDateSubmitted());

        return SummaryTab.builder()
            .repossessedPropertyAddress(pcsCase.getPropertyAddress())
            .groundsForPossession(GroundsForPossessionTabDetails.builder()
                                      .grounds(groundsBuilder.getGrounds(pcsCase))
                                      .build())
            .reasonsForPossession(reasonsForPossession)
            .dateClaimSubmitted(dateSubmitted)
            .claimantDetails(claimantInformationTabDetailsBuilder.createSummaryClaimantTabDetails(pcsCase))
            .defendantDetails(defendantInformationTabDetailsBuilder.buildSummaryDefendantOneDetails(pcsCase))
            .additionalDefendants(
                additionalDefendantInformationTabDetailsBuilder.buildSummaryAdditionalDefendantsDetails(pcsCase)
            )
            .rentArrearsDetails(rentArrearsTabDetailsBuilder.buildRentArrearsTabDetails(pcsCase))
            .tenancyDetails(buildTenancyTabDetails(pcsCase))
            .noticeDetails(buildNoticeTabDetails(pcsCase))
            .build();
    }

    private String formatSubmittedDate(LocalDateTime dateSubmitted) {
        if (dateSubmitted == null) {
            return null;
        }

        LocalDateTime ukDateSubmitted = dateSubmitted
            .atZone(ZoneId.systemDefault())
            .withZoneSameInstant(UK_ZONE_ID)
            .toLocalDateTime();

        return ukDateSubmitted.format(SUBMITTED_DATE_FORMATTER).replace("am", "AM").replace("pm", "PM");
    }

    private TenancyTabDetails buildTenancyTabDetails(PCSCase pcsCase) {
        TenancyLicenceDetails tenancyLicenceDetails = pcsCase.getTenancyLicenceDetails();
        OccupationLicenceDetailsWales occupationLicenceDetailsWales = pcsCase.getOccupationLicenceDetailsWales();

        if ((tenancyLicenceDetails == null || tenancyLicenceDetails.getTypeOfTenancyLicence() == null)
            && (occupationLicenceDetailsWales == null
            || occupationLicenceDetailsWales.getOccupationLicenceTypeWales() == null)) {
            return null;
        }

        String agreementType = tenancyLicenceDetails != null && tenancyLicenceDetails.getTypeOfTenancyLicence() != null
            ? getAgreementType(tenancyLicenceDetails)
            : getOccupationLicenceAgreementType(occupationLicenceDetailsWales);
        String agreementStartDate = tenancyLicenceDetails != null
            && tenancyLicenceDetails.getTenancyLicenceDate() != null
            ? tenancyLicenceDetails.getTenancyLicenceDate().format(SUMMARY_DATE_FORMATTER)
            : getOccupationLicenceStartDate(occupationLicenceDetailsWales);

        return TenancyTabDetails.builder()
            .agreementType(agreementType)
            .agreementStartDate(agreementStartDate)
            .build();
    }

    private String getAgreementType(TenancyLicenceDetails tenancyLicenceDetails) {
        if (tenancyLicenceDetails.getTypeOfTenancyLicence() == TenancyLicenceType.OTHER) {
            return tenancyLicenceDetails.getDetailsOfOtherTypeOfTenancyLicence();
        }

        return tenancyLicenceDetails.getTypeOfTenancyLicence().getLabel();
    }

    private String getOccupationLicenceAgreementType(OccupationLicenceDetailsWales occupationLicenceDetailsWales) {
        if (occupationLicenceDetailsWales.getOccupationLicenceTypeWales() == OccupationLicenceTypeWales.OTHER) {
            return occupationLicenceDetailsWales.getOtherLicenceTypeDetails();
        }

        return occupationLicenceDetailsWales.getOccupationLicenceTypeWales().getLabel();
    }

    private String getOccupationLicenceStartDate(OccupationLicenceDetailsWales occupationLicenceDetailsWales) {
        if (occupationLicenceDetailsWales == null || occupationLicenceDetailsWales.getLicenceStartDate() == null) {
            return null;
        }

        return occupationLicenceDetailsWales.getLicenceStartDate().format(SUMMARY_DATE_FORMATTER);
    }

    private NoticeTabDetails buildNoticeTabDetails(PCSCase pcsCase) {
        NoticeServedDetails noticeServedDetails = pcsCase.getNoticeServedDetails();

        if (noticeServedDetails == null || noticeServedDetails.getNoticeEmailSentDateTime() == null) {
            return null;
        }

        return NoticeTabDetails.builder()
            .noticeServedDate(noticeServedDetails.getNoticeEmailSentDateTime().format(SUMMARY_DATE_FORMATTER))
            .build();
    }
}
