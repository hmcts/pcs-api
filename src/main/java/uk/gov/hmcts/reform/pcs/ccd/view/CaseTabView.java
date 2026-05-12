package uk.gov.hmcts.reform.pcs.ccd.view;

import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.AdditionalReasons;
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantInformation;
import uk.gov.hmcts.reform.pcs.ccd.domain.NoticeServedDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentArrearsSection;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentPaymentFrequency;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceType;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.ClaimGroundSummary;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.CasePartiesTab;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.ClaimantTabDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.DefendantTabDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.summary.GroundsForPossessionTabDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.summary.NoticeTabDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.summary.ReasonsForPossessionTabDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.shared.RentArrearsTabDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.shared.AdditionalDefendantInformationTabDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.shared.ClaimantInformationTabDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.shared.DefendantInfomationTabDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.summary.SummaryTab;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.summary.TenancyTabDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.OccupationLicenceDetailsWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.OccupationLicenceTypeWales;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static uk.gov.hmcts.ccd.sdk.type.YesOrNo.NO;

@Component
public class CaseTabView {

    private static final String NAME_UNKNOWN = "Person unknown";
    private static final DateTimeFormatter SUMMARY_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter SUBMITTED_DATE_FORMATTER =
        DateTimeFormatter.ofPattern("d MMMM yyyy, h:mm:ssa", Locale.UK);

    public void setCaseTabFields(PCSCase pcsCase) {
        CasePartiesTab casePartiesTab = buildCasePartiesTab(pcsCase);
        SummaryTab summaryTab = buildSummaryTab(pcsCase);
        pcsCase.setCasePartiesTab(casePartiesTab);
        pcsCase.setSummaryTab(summaryTab);
    }

    private CasePartiesTab buildCasePartiesTab(PCSCase pcsCase) {
        CasePartiesTab tab = CasePartiesTab.builder().build();

        List<ListValue<Party>> allClaimants = pcsCase.getAllClaimants();
        if (!CollectionUtils.isEmpty(allClaimants)) {
            Party claimant = allClaimants.getFirst().getValue();
            ClaimantTabDetails claimantTabDetails = createClaimantTabDetails(claimant);
            tab.setClaimantDetails(claimantTabDetails);
        }

        if (!CollectionUtils.isEmpty(pcsCase.getAllDefendants())) {
            List<ListValue<Party>> allDefendants = new ArrayList<>(pcsCase.getAllDefendants());
            Party defendant1 = allDefendants.removeFirst().getValue();
            DefendantTabDetails defendant1TabDetails = createDefendantTabDetails(defendant1, pcsCase);
            tab.setDefendantOneDetails(defendant1TabDetails);

            if (!allDefendants.isEmpty()) {
                List<ListValue<DefendantTabDetails>> additionalDefendants = allDefendants
                    .stream().map(partyListValue -> {
                        Party defendant = partyListValue.getValue();
                        DefendantTabDetails defendantTabDetails = createDefendantTabDetails(defendant, pcsCase);
                        return ListValue.<DefendantTabDetails>builder().value(defendantTabDetails).build();
                    }).toList();

                tab.setDefendantsDetails(additionalDefendants);
            }
        }

        return tab;
    }

    private ClaimantTabDetails createClaimantTabDetails(Party claimant) {
        return ClaimantTabDetails.builder()
            .name(claimant.getOrgName())
            .emailAddress(claimant.getEmailAddress())
            .serviceAddress(claimant.getAddress())
            .telephoneNumber(claimant.getPhoneNumber())
            .build();
    }

    private DefendantTabDetails createDefendantTabDetails(Party defendant, PCSCase pcsCase) {
        AddressUK defendantAddress = defendant.getAddress() != null
            ? defendant.getAddress() : pcsCase.getPropertyAddress();
        String defendantFirstName = NAME_UNKNOWN;
        String defendantLastName = NAME_UNKNOWN;

        if (defendant.getNameKnown() == VerticalYesNo.YES) {
            defendantFirstName = defendant.getFirstName();
            defendantLastName = defendant.getLastName();
        }

        return DefendantTabDetails.builder()
            .serviceAddress(defendantAddress)
            .firstName(defendantFirstName)
            .lastName(defendantLastName)
            .build();
    }

    private SummaryTab buildSummaryTab(PCSCase pcsCase) {
        ReasonsForPossessionTabDetails reasonsForPossession = buildReasonsForPossession(pcsCase);
        String dateSubmitted = formatSubmittedDate(pcsCase.getDateSubmitted());

        return SummaryTab.builder()
            .repossessedPropertyAddress(pcsCase.getPropertyAddress())
            .groundsForPossession(GroundsForPossessionTabDetails.builder()
                                      .grounds(getGrounds(pcsCase))
                                      .build())
            .claimSubmittedDate(dateSubmitted)
            .reasonsForPossession(reasonsForPossession)
            .possessionReasonsSubmittedDate(reasonsForPossession == null ? null : dateSubmitted)
            .claimantDetails(createSummaryClaimantTabDetails(pcsCase))
            .defendantDetails(createSummaryDefendantOneDetails(pcsCase))
            .additionalDefendants(createAdditionalSummaryDefendantsDetails(pcsCase))
            .rentArrearsDetails(buildRentArrearsTabDetails(pcsCase))
            .tenancyDetails(buildTenancyTabDetails(pcsCase))
            .noticeDetails(buildNoticeTabDetails(pcsCase))
            .build();
    }

    private String formatSubmittedDate(LocalDateTime dateSubmitted) {
        if (dateSubmitted == null) {
            return null;
        }

        return dateSubmitted.format(SUBMITTED_DATE_FORMATTER).replace("am", "AM").replace("pm", "PM");
    }

    private ClaimantInformationTabDetails createSummaryClaimantTabDetails(PCSCase pcsCase) {
        String claimantName = getSummaryClaimantName(pcsCase);
        if (claimantName == null) {
            return null;
        }

        return ClaimantInformationTabDetails.builder()
            .claimantName(claimantName)
            .build();
    }

    private String getSummaryClaimantName(PCSCase pcsCase) {
        ClaimantInformation claimantInformation = pcsCase.getClaimantInformation();
        if (claimantInformation != null) {
            if (claimantInformation.getOrgNameFound() == NO) {
                return claimantInformation.getFallbackClaimantName();
            }

            if (claimantInformation.getIsClaimantNameCorrect() == VerticalYesNo.NO) {
                return claimantInformation.getOverriddenClaimantName();
            }

            if (claimantInformation.getClaimantName() != null) {
                return claimantInformation.getClaimantName();
            }
        }

        if (CollectionUtils.isEmpty(pcsCase.getAllClaimants())) {
            return null;
        }

        return pcsCase.getAllClaimants().getFirst().getValue().getOrgName();
    }

    private DefendantInfomationTabDetails createSummaryDefendantOneDetails(PCSCase pcsCase) {
        if (CollectionUtils.isEmpty(pcsCase.getAllDefendants())) {
            return null;
        }

        return createSummaryDefendantDetails(pcsCase.getAllDefendants().getFirst().getValue(), pcsCase);
    }

    private List<ListValue<AdditionalDefendantInformationTabDetails>> createAdditionalSummaryDefendantsDetails(
        PCSCase pcsCase) {
        if (CollectionUtils.isEmpty(pcsCase.getAllDefendants()) || pcsCase.getAllDefendants().size() < 2) {
            return null;
        }

        return pcsCase.getAllDefendants().stream()
            .skip(1)
            .map(ListValue::getValue)
            .map(defendant -> createAdditionalSummaryDefendantDetails(defendant, pcsCase))
            .filter(defendantDetails -> defendantDetails != null)
            .map(defendantDetails -> ListValue.<AdditionalDefendantInformationTabDetails>builder()
                .value(defendantDetails)
                .build())
            .toList();
    }

    private AdditionalDefendantInformationTabDetails createAdditionalSummaryDefendantDetails(Party defendant,
                                                                                        PCSCase pcsCase) {
        AddressUK addressForService = getSummaryDefendantAddressForService(defendant, pcsCase);

        if (defendant.getNameKnown() != VerticalYesNo.YES && addressForService == null) {
            return null;
        }

        return AdditionalDefendantInformationTabDetails.builder()
            .firstName(defendant.getNameKnown() == VerticalYesNo.YES ? defendant.getFirstName() : null)
            .lastName(defendant.getNameKnown() == VerticalYesNo.YES ? defendant.getLastName() : null)
            .addressForService(addressForService)
            .build();
    }

    private DefendantInfomationTabDetails createSummaryDefendantDetails(Party defendant, PCSCase pcsCase) {
        AddressUK addressForService = getSummaryDefendantAddressForService(defendant, pcsCase);

        if (defendant.getNameKnown() != VerticalYesNo.YES && addressForService == null) {
            return null;
        }

        return DefendantInfomationTabDetails.builder()
            .firstName(defendant.getNameKnown() == VerticalYesNo.YES ? defendant.getFirstName() : null)
            .lastName(defendant.getNameKnown() == VerticalYesNo.YES ? defendant.getLastName() : null)
            .addressForService(addressForService)
            .build();
    }

    private AddressUK getSummaryDefendantAddressForService(Party defendant, PCSCase pcsCase) {
        if (defendant.getAddressKnown() != VerticalYesNo.YES) {
            return null;
        }

        return defendant.getAddress() != null ? defendant.getAddress() : pcsCase.getPropertyAddress();
    }

    private ReasonsForPossessionTabDetails buildReasonsForPossession(PCSCase pcsCase) {
        AdditionalReasons additionalReasons = pcsCase.getAdditionalReasonsForPossession();
        if (additionalReasons == null || additionalReasons.getHasReasons() != VerticalYesNo.YES) {
            return null;
        }

        return ReasonsForPossessionTabDetails.builder()
            .groundReasons(additionalReasons.getHasReasons().getLabel())
            .additionalReasonsForPossession(additionalReasons.getReasons())
            .build();
    }

    private RentArrearsTabDetails buildRentArrearsTabDetails(PCSCase pcsCase) {
        RentDetails rentDetails = pcsCase.getRentDetails();
        RentArrearsSection rentArrears = pcsCase.getRentArrears();

        String rentAmount = rentDetails == null ? null : formatMoney(rentDetails.getCurrentRent());
        String calculationFrequency = getRentCalculationFrequency(rentDetails);
        String dailyRate = getDailyRate(rentDetails);
        String arrearsTotal = rentArrears == null ? null : formatMoney(rentArrears.getTotal());
        String judgmentRequested = pcsCase.getArrearsJudgmentWanted() == null
            ? null : pcsCase.getArrearsJudgmentWanted().getLabel();

        if (rentAmount == null
            && calculationFrequency == null
            && dailyRate == null
            && arrearsTotal == null
            && judgmentRequested == null) {
            return null;
        }

        return RentArrearsTabDetails.builder()
            .rentAmount(rentAmount)
            .calculationFrequency(calculationFrequency)
            .dailyRate(dailyRate)
            .arrearsTotal(arrearsTotal)
            .judgmentRequested(judgmentRequested)
            .build();
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

    private String getRentCalculationFrequency(RentDetails rentDetails) {
        if (rentDetails == null || rentDetails.getFrequency() == null) {
            return null;
        }

        if (rentDetails.getFrequency() == RentPaymentFrequency.OTHER && rentDetails.getOtherFrequency() != null) {
            return rentDetails.getOtherFrequency();
        }

        return rentDetails.getFrequency().getLabel();
    }

    private String getDailyRate(RentDetails rentDetails) {
        if (rentDetails == null) {
            return null;
        }

        if (rentDetails.getPerDayCorrect() == VerticalYesNo.NO && rentDetails.getAmendedDailyCharge() != null) {
            return formatMoney(rentDetails.getAmendedDailyCharge());
        }

        if (rentDetails.getDailyCharge() != null) {
            return formatMoney(rentDetails.getDailyCharge());
        }

        if (rentDetails.getFormattedCalculatedDailyCharge() != null) {
            return rentDetails.getFormattedCalculatedDailyCharge();
        }

        return formatMoney(rentDetails.getCalculatedDailyCharge());
    }

    private String formatMoney(BigDecimal amount) {
        if (amount == null) {
            return null;
        }

        if (amount.stripTrailingZeros().scale() <= 0) {
            amount = amount.stripTrailingZeros();
        }

        return "£" + amount.toPlainString();
    }

    private String getGrounds(PCSCase pcsCase) {
        if (CollectionUtils.isEmpty(pcsCase.getClaimGroundSummaries())) {
            return null;
        }

        return pcsCase.getClaimGroundSummaries().stream()
            .map(ListValue::getValue)
            .map(ClaimGroundSummary::getLabel)
            .reduce((firstGround, secondGround) -> firstGround + ", " + secondGround)
            .orElse(null);
    }
}
