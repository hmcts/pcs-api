package uk.gov.hmcts.reform.pcs.ccd.view;

import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
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
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.shared.AdditionalDefendantInformationTabDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.shared.ClaimantInformationTabDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.shared.DefendantInfomationTabDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.shared.RentArrearsTabDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.summary.GroundsForPossessionTabDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.summary.NoticeTabDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.summary.ReasonsForPossessionTabDetails;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static uk.gov.hmcts.ccd.sdk.type.YesOrNo.NO;

@Component
public class CaseSummaryTabView {

    private static final DateTimeFormatter SUMMARY_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter SUBMITTED_DATE_FORMATTER =
        DateTimeFormatter.ofPattern("d MMMM yyyy, h:mm:ssa", Locale.UK);
    private static final Pattern GROUND_REFERENCE_PATTERN =
        Pattern.compile("\\(ground ([^)]+)\\)", Pattern.CASE_INSENSITIVE);
    private static final Pattern SECTION_REFERENCE_PATTERN =
        Pattern.compile("\\(section ([^)]+)\\)", Pattern.CASE_INSENSITIVE);
    private static final Pattern SECTION_84A_CONDITION_PATTERN =
        Pattern.compile("^Condition ([1-5]) of Section 84A of the Housing Act 1985$");
    private static final String ANTISOCIAL_BEHAVIOUR = "Antisocial behaviour";
    private static final String SECTION_84A_CONDITION_1_PREFIX = "Condition 1";
    private static final String SECTION_84A_CONDITION_2_PREFIX = "Condition 2";
    private static final String SECTION_84A_CONDITION_3_PREFIX = "Condition 3";
    private static final String SECTION_84A_CONDITION_4_PREFIX = "Condition 4";
    private static final String SECTION_84A_CONDITION_5_PREFIX = "Condition 5";
    private static final String BREACH_OF_THE_TENANCY = "Breach of the tenancy";
    private static final String ABSOLUTE_GROUNDS = "Absolute grounds";
    private static final String OTHER = "Other";
    private static final String OTHER_GROUNDS = "Other grounds";
    private static final String NO_GROUNDS = "No grounds";
    private static final String PARAGRAPH_25B_2_SCHEDULE_12 = "paragraph 25B(2) of Schedule 12";
    private static final String GROUND_1 = "1";
    private static final String GROUND_2 = "2";
    private static final String GROUND_2A = "2A";
    private static final String GROUND_2ZA = "2ZA";
    private static final String GROUND_3 = "3";
    private static final String GROUND_4 = "4";
    private static final String GROUND_5 = "5";
    private static final String GROUND_6 = "6";
    private static final String GROUND_7 = "7";
    private static final String GROUND_7A = "7A";
    private static final String GROUND_7B = "7B";
    private static final String GROUND_8 = "8";
    private static final String GROUND_9 = "9";
    private static final String GROUND_10 = "10";
    private static final String GROUND_10A = "10A";
    private static final String GROUND_11 = "11";
    private static final String GROUND_12 = "12";
    private static final String GROUND_13 = "13";
    private static final String GROUND_14 = "14";
    private static final String GROUND_14A = "14A";
    private static final String GROUND_14ZA = "14ZA";
    private static final String GROUND_15 = "15";
    private static final String GROUND_15A = "15A";
    private static final String GROUND_16 = "16";
    private static final String GROUND_17 = "17";
    private static final String GROUND_A = "A";
    private static final String GROUND_B = "B";
    private static final String GROUND_C = "C";
    private static final String GROUND_D = "D";
    private static final String GROUND_E = "E";
    private static final String GROUND_F = "F";
    private static final String GROUND_G = "G";
    private static final String GROUND_H = "H";
    private static final String GROUND_I = "I";
    private static final String SECTION_157 = "157";
    private static final String SECTION_170 = "170";
    private static final String SECTION_178 = "178";
    private static final String SECTION_181 = "181";
    private static final String SECTION_186 = "186";
    private static final String SECTION_187 = "187";
    private static final String SECTION_191 = "191";
    private static final String SECTION_199 = "199";

    public SummaryTab buildSummaryTab(PCSCase pcsCase) {
        ReasonsForPossessionTabDetails reasonsForPossession = buildReasonsForPossession(pcsCase);
        String dateSubmitted = formatSubmittedDate(pcsCase.getDateSubmitted());

        return SummaryTab.builder()
            .repossessedPropertyAddress(pcsCase.getPropertyAddress())
            .groundsForPossession(GroundsForPossessionTabDetails.builder()
                                      .grounds(getGrounds(pcsCase))
                                      .build())
            .reasonsForPossession(reasonsForPossession)
            .dateClaimSubmitted(dateSubmitted)
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
            .firstName(getDefendantFirstName(defendant))
            .lastName(getDefendantLastName(defendant))
            .addressForService(addressForService)
            .build();
    }

    private DefendantInfomationTabDetails createSummaryDefendantDetails(Party defendant, PCSCase pcsCase) {
        AddressUK addressForService = getSummaryDefendantAddressForService(defendant, pcsCase);

        if (defendant.getNameKnown() != VerticalYesNo.YES && addressForService == null) {
            return null;
        }

        return DefendantInfomationTabDetails.builder()
            .firstName(getDefendantFirstName(defendant))
            .lastName(getDefendantLastName(defendant))
            .addressForService(addressForService)
            .build();
    }

    private String getDefendantFirstName(Party defendant) {
        return defendant.getNameKnown() == VerticalYesNo.YES ? defendant.getFirstName() : CaseTabView.NAME_UNKNOWN;
    }

    private String getDefendantLastName(Party defendant) {
        return defendant.getNameKnown() == VerticalYesNo.YES ? defendant.getLastName() : CaseTabView.NAME_UNKNOWN;
    }

    private AddressUK getSummaryDefendantAddressForService(Party defendant, PCSCase pcsCase) {
        if (defendant.getAddressKnown() != VerticalYesNo.YES) {
            return pcsCase.getPropertyAddress();
        }

        return defendant.getAddress() != null ? defendant.getAddress() : pcsCase.getPropertyAddress();
    }

    private ReasonsForPossessionTabDetails buildReasonsForPossession(PCSCase pcsCase) {
        AdditionalReasons additionalReasons = pcsCase.getAdditionalReasonsForPossession();
        ReasonsForPossessionTabDetails reasonsForPossession =
            buildReasonsForPossessionFromGroundSummaries(pcsCase);
        String additionalReasonsText = additionalReasons == null
            || additionalReasons.getHasReasons() != VerticalYesNo.YES ? null : additionalReasons.getReasons();

        if (reasonsForPossession == null && additionalReasonsText == null) {
            return null;
        }

        if (reasonsForPossession == null) {
            reasonsForPossession = ReasonsForPossessionTabDetails.builder().build();
        }

        reasonsForPossession.setAdditionalReasonsForPossession(additionalReasonsText);
        return reasonsForPossession;
    }

    private ReasonsForPossessionTabDetails buildReasonsForPossessionFromGroundSummaries(PCSCase pcsCase) {
        if (CollectionUtils.isEmpty(pcsCase.getClaimGroundSummaries())) {
            return null;
        }

        ReasonsForPossessionTabDetails reasonsForPossession = ReasonsForPossessionTabDetails.builder().build();
        boolean hasReason = false;

        for (ListValue<ClaimGroundSummary> listValue : pcsCase.getClaimGroundSummaries()) {
            ClaimGroundSummary summary = listValue.getValue();
            if (summary == null || !StringUtils.hasText(summary.getReason())) {
                continue;
            }

            setGroundReason(reasonsForPossession, summary.getLabel(), summary.getReason());
            hasReason = true;
        }

        return hasReason ? reasonsForPossession : null;
    }

    private void setGroundReason(ReasonsForPossessionTabDetails reasonsForPossession,
                                 String groundLabel,
                                 String reason) {
        Matcher groundMatcher = GROUND_REFERENCE_PATTERN.matcher(groundLabel);
        if (groundMatcher.find()) {
            setGroundNumberReason(reasonsForPossession, groundMatcher.group(1), reason);
            return;
        }

        Matcher sectionMatcher = SECTION_REFERENCE_PATTERN.matcher(groundLabel);
        if (sectionMatcher.find()) {
            setSectionReason(reasonsForPossession, sectionMatcher.group(1), reason);
            return;
        }

        if (groundLabel.startsWith(SECTION_84A_CONDITION_1_PREFIX)) {
            reasonsForPossession.setCondition1OfSection84A(reason);
        } else if (groundLabel.startsWith(SECTION_84A_CONDITION_2_PREFIX)) {
            reasonsForPossession.setCondition2OfSection84A(reason);
        } else if (groundLabel.startsWith(SECTION_84A_CONDITION_3_PREFIX)) {
            reasonsForPossession.setCondition3OfSection84A(reason);
        } else if (groundLabel.startsWith(SECTION_84A_CONDITION_4_PREFIX)) {
            reasonsForPossession.setCondition4OfSection84A(reason);
        } else if (groundLabel.startsWith(SECTION_84A_CONDITION_5_PREFIX)) {
            reasonsForPossession.setCondition5OfSection84A(reason);
        } else if (ANTISOCIAL_BEHAVIOUR.equals(groundLabel)) {
            reasonsForPossession.setAntisocialBehaviour(reason);
        } else if (BREACH_OF_THE_TENANCY.equals(groundLabel)) {
            reasonsForPossession.setBreachOfTheTenancy(reason);
        } else if (ABSOLUTE_GROUNDS.equals(groundLabel)) {
            reasonsForPossession.setAbsoluteGrounds(reason);
        } else if (OTHER.equals(groundLabel) || OTHER_GROUNDS.equals(groundLabel)) {
            reasonsForPossession.setOtherGrounds(reason);
        } else if (NO_GROUNDS.equals(groundLabel)) {
            reasonsForPossession.setNoGrounds(reason);
        } else if (groundLabel.contains(PARAGRAPH_25B_2_SCHEDULE_12)) {
            reasonsForPossession.setParagraph25B2Schedule12(reason);
        }
    }

    private void setGroundNumberReason(ReasonsForPossessionTabDetails reasonsForPossession,
                                       String ground,
                                       String reason) {
        switch (ground) {
            case GROUND_1 -> reasonsForPossession.setGround1(reason);
            case GROUND_2 -> reasonsForPossession.setGround2(reason);
            case GROUND_2A -> reasonsForPossession.setGround2A(reason);
            case GROUND_2ZA -> reasonsForPossession.setGround2ZA(reason);
            case GROUND_3 -> reasonsForPossession.setGround3(reason);
            case GROUND_4 -> reasonsForPossession.setGround4(reason);
            case GROUND_5 -> reasonsForPossession.setGround5(reason);
            case GROUND_6 -> reasonsForPossession.setGround6(reason);
            case GROUND_7 -> reasonsForPossession.setGround7(reason);
            case GROUND_7A -> reasonsForPossession.setGround7A(reason);
            case GROUND_7B -> reasonsForPossession.setGround7B(reason);
            case GROUND_8 -> reasonsForPossession.setGround8(reason);
            case GROUND_9 -> reasonsForPossession.setGround9(reason);
            case GROUND_10 -> reasonsForPossession.setGround10(reason);
            case GROUND_10A -> reasonsForPossession.setGround10A(reason);
            case GROUND_11 -> reasonsForPossession.setGround11(reason);
            case GROUND_12 -> reasonsForPossession.setGround12(reason);
            case GROUND_13 -> reasonsForPossession.setGround13(reason);
            case GROUND_14 -> reasonsForPossession.setGround14(reason);
            case GROUND_14A -> reasonsForPossession.setGround14A(reason);
            case GROUND_14ZA -> reasonsForPossession.setGround14ZA(reason);
            case GROUND_15 -> reasonsForPossession.setGround15(reason);
            case GROUND_15A -> reasonsForPossession.setGround15A(reason);
            case GROUND_16 -> reasonsForPossession.setGround16(reason);
            case GROUND_17 -> reasonsForPossession.setGround17(reason);
            case GROUND_A -> reasonsForPossession.setGroundA(reason);
            case GROUND_B -> reasonsForPossession.setGroundB(reason);
            case GROUND_C -> reasonsForPossession.setGroundC(reason);
            case GROUND_D -> reasonsForPossession.setGroundD(reason);
            case GROUND_E -> reasonsForPossession.setGroundE(reason);
            case GROUND_F -> reasonsForPossession.setGroundF(reason);
            case GROUND_G -> reasonsForPossession.setGroundG(reason);
            case GROUND_H -> reasonsForPossession.setGroundH(reason);
            case GROUND_I -> reasonsForPossession.setGroundI(reason);
            default -> {
            }
        }
    }

    private void setSectionReason(ReasonsForPossessionTabDetails reasonsForPossession,
                                  String section,
                                  String reason) {
        switch (section) {
            case SECTION_157 -> reasonsForPossession.setSection157(reason);
            case SECTION_170 -> reasonsForPossession.setSection170(reason);
            case SECTION_178 -> reasonsForPossession.setSection178(reason);
            case SECTION_181 -> reasonsForPossession.setSection181(reason);
            case SECTION_186 -> reasonsForPossession.setSection186(reason);
            case SECTION_187 -> reasonsForPossession.setSection187(reason);
            case SECTION_191 -> reasonsForPossession.setSection191(reason);
            case SECTION_199 -> reasonsForPossession.setSection199(reason);
            default -> {
            }
        }
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

        List<String> grounds = new ArrayList<>(pcsCase.getClaimGroundSummaries().stream()
            .map(ListValue::getValue)
            .map(ClaimGroundSummary::getLabel)
            .toList());

        groupSection84AConditions(grounds);

        return grounds.stream()
            .reduce((firstGround, secondGround) -> firstGround + "\n" + secondGround)
            .orElse(null);
    }

    private void groupSection84AConditions(List<String> grounds) {
        List<String> section84AConditions = grounds.stream()
            .filter(this::isSection84ACondition)
            .sorted(this::compareSection84AConditions)
            .toList();

        if (section84AConditions.isEmpty()) {
            return;
        }

        int antisocialIndex = grounds.indexOf(ANTISOCIAL_BEHAVIOUR);
        int groupIndex = antisocialIndex >= 0 ? antisocialIndex : grounds.indexOf(section84AConditions.getFirst());
        grounds.set(groupIndex, ANTISOCIAL_BEHAVIOUR + ": " + String.join(", ", section84AConditions));
        grounds.removeAll(section84AConditions);
    }

    private boolean isSection84ACondition(String label) {
        return SECTION_84A_CONDITION_PATTERN.matcher(label).matches();
    }

    private int compareSection84AConditions(String firstCondition, String secondCondition) {
        return Integer.compare(
            getSection84AConditionNumber(firstCondition),
            getSection84AConditionNumber(secondCondition)
        );
    }

    private int getSection84AConditionNumber(String label) {
        Matcher matcher = SECTION_84A_CONDITION_PATTERN.matcher(label);
        return matcher.matches() ? Integer.parseInt(matcher.group(1)) : 0;
    }
}
