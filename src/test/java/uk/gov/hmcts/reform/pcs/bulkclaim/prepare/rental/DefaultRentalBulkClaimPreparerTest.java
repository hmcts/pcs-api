package uk.gov.hmcts.reform.pcs.bulkclaim.prepare.rental;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.bulkclaim.config.BulkClaimProperties;
import uk.gov.hmcts.reform.pcs.bulkclaim.model.BulkClaimPreparationResult;
import uk.gov.hmcts.reform.pcs.bulkclaim.model.BulkClaimPreparationResult.Prepared;
import uk.gov.hmcts.reform.pcs.bulkclaim.model.BulkClaimPreparationResult.Rejected;
import uk.gov.hmcts.reform.pcs.bulkclaim.model.BulkFailReason;
import uk.gov.hmcts.reform.pcs.bulkclaim.model.PreparedBulkClaim;
import uk.gov.hmcts.reform.pcs.bulkclaim.org.BulkClaimantOrganisationResolver;
import uk.gov.hmcts.reform.pcs.bulkclaim.prepare.common.BulkPropertyEligibility;
import uk.gov.hmcts.reform.pcs.bulkclaim.prepare.common.BulkPropertyEligibility.PropertyEligibility;
import uk.gov.hmcts.reform.pcs.bulkclaim.xml.BulkClaimXmlCodec;
import uk.gov.hmcts.reform.pcs.bulkclaim.xml.generated.RentalClaimSet;
import uk.gov.hmcts.reform.pcs.ccd.domain.CompletionNextStep;
import uk.gov.hmcts.reform.pcs.ccd.domain.NoticeServiceMethod;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentPaymentFrequency;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceType;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.statementoftruth.StatementOfTruthCompletedBy;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;
import uk.gov.hmcts.reform.pcs.service.FeatureFlag;
import uk.gov.hmcts.reform.pcs.service.FeatureToggleService;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.bulkclaim.prepare.rental.RentalPreparerTestSupport.fixture;
import static uk.gov.hmcts.reform.pcs.bulkclaim.prepare.rental.RentalPreparerTestSupport.organisation;

class DefaultRentalBulkClaimPreparerTest {

    private final BulkClaimXmlCodec codec = new BulkClaimXmlCodec();
    private BulkPropertyEligibility eligibility;
    private BulkClaimantOrganisationResolver resolver;
    private FeatureToggleService featureToggleService;
    private DefaultRentalBulkClaimPreparer underTest;

    @BeforeEach
    void setUp() {
        eligibility = Mockito.mock(BulkPropertyEligibility.class);
        resolver = Mockito.mock(BulkClaimantOrganisationResolver.class);
        featureToggleService = Mockito.mock(FeatureToggleService.class);
        when(resolver.isPbaAccountValid(any(), any())).thenReturn(true);
        when(eligibility.check(any(), any())).thenReturn(
            new PropertyEligibility(null, LegislativeCountry.ENGLAND, 20262, 1));
        underTest = RentalPreparerTestSupport.preparer(eligibility, resolver, new BulkClaimProperties(),
                                                    featureToggleService);
    }

    private BulkClaimPreparationResult prepare(String fixtureName) {
        return prepare(fixtureName, claim -> { });
    }

    private BulkClaimPreparationResult prepare(String fixtureName, Consumer<RentalClaimSet.RentalClaim> tweak) {
        RentalClaimSet set = codec.parseRental(fixture(fixtureName));
        RentalClaimSet.RentalClaim claim = set.getRentalClaim().getFirst();
        tweak.accept(claim);
        return underTest.prepare(set, claim, organisation());
    }

    private PreparedBulkClaim prepared(String fixtureName) {
        BulkClaimPreparationResult result = prepare(fixtureName);
        assertThat(result).as(result.toString()).isInstanceOf(Prepared.class);
        return ((Prepared) result).claim();
    }

    @Test
    void shouldPrepareAssuredClaimWithRentArrears() {
        PreparedBulkClaim claim = prepared("rental-england-assured-arrears-valid.xml");
        PCSCase pcsCase = claim.pcsCase();

        assertThat(claim.pbaAccountNumber()).isEqualTo("PBA1234567");
        assertThat(pcsCase.getLegislativeCountry()).isEqualTo(LegislativeCountry.ENGLAND);
        assertThat(pcsCase.getCaseManagementLocationNumber()).isEqualTo(20262);
        assertThat(pcsCase.getRegionId()).isEqualTo(1);
        assertThat(pcsCase.getClaimantType().getValueCode()).isEqualTo("PROVIDER_OF_SOCIAL_HOUSING");
        assertThat(pcsCase.getClaimAgainstTrespassers()).isEqualTo(VerticalYesNo.NO);
        assertThat(pcsCase.getWantToUploadDocuments()).isEqualTo(VerticalYesNo.NO);
        assertThat(pcsCase.getCompletionNextStep()).isEqualTo(CompletionNextStep.SUBMIT_AND_PAY_NOW);

        assertThat(pcsCase.getTenancyLicenceDetails().getTypeOfTenancyLicence())
            .isEqualTo(TenancyLicenceType.ASSURED_TENANCY);
        assertThat(pcsCase.getTenancyLicenceDetails().getHasCopyOfTenancyLicence()).isEqualTo(VerticalYesNo.NO);
        assertThat(pcsCase.getTenancyLicenceDetails().getReasonsForNoTenancyLicenceDocuments())
            .isEqualTo("Not supplied via bulk interface");

        assertThat(pcsCase.getClaimDueToRentArrears()).isEqualTo(YesOrNo.YES);
        assertThat(pcsCase.getAssuredRentArrearsPossessionGrounds().getRentArrearsGrounds()).hasSize(2);
        assertThat(pcsCase.getHasOtherAdditionalGrounds()).isEqualTo(YesOrNo.YES);
        assertThat(pcsCase.getRentArrearsGroundsReasons().getOwnerOccupierReason())
            .isEqualTo("Landlord needs the home.");

        assertThat(pcsCase.getNoticeServed()).isEqualTo(YesOrNo.YES);
        assertThat(pcsCase.getNoticeServedDetails().getServiceMethod()).isEqualTo(NoticeServiceMethod.FIRST_CLASS_POST);
        assertThat(pcsCase.getNoticeServedDetails().getUnableToUploadReason())
            .isEqualTo("Not supplied via bulk interface");

        assertThat(pcsCase.getShowRentSectionPage()).isEqualTo(YesOrNo.YES);
        assertThat(pcsCase.getRentDetails().getFrequency()).isEqualTo(RentPaymentFrequency.MONTHLY);
        assertThat(pcsCase.getRentDetails().getCalculatedDailyCharge()).isEqualByComparingTo("26.28");
        assertThat(pcsCase.getRentDetails().getPerDayCorrect()).isEqualTo(VerticalYesNo.NO);
        assertThat(pcsCase.getRentDetails().getAmendedDailyCharge()).isEqualByComparingTo("26.00");
        assertThat(pcsCase.getRentArrears().getTotal()).isEqualByComparingTo("2400.50");
        assertThat(pcsCase.getRentArrears().getRecoveryAttempted()).isEqualTo(VerticalYesNo.YES);
        assertThat(pcsCase.getArrearsJudgmentWanted()).isEqualTo(VerticalYesNo.YES);

        assertThat(pcsCase.getDefendantCircumstances().getHasDefendantCircumstancesInfo()).isEqualTo(VerticalYesNo.YES);
        assertThat(pcsCase.getAdditionalReasonsForPossession().getHasReasons()).isEqualTo(VerticalYesNo.YES);
        assertThat(pcsCase.getHasUnderlesseeOrMortgagee()).isEqualTo(VerticalYesNo.YES);
        assertThat(pcsCase.getStatementOfTruth().getCompletedBy()).isEqualTo(StatementOfTruthCompletedBy.CLAIMANT);
        assertThat(pcsCase.getStatementOfTruth().getAgreementClaimant()).isNotEmpty();

        assertThat(claim.extras().noticeType()).isEqualTo("Section 8");
        assertThat(claim.extras().claimingCosts()).isTrue();
        assertThat(claim.extras().claimantDesignation()).isEqualTo("SocialLandlord");
        assertThat(claim.extras().legacyPcolClaimantReference()).isEqualTo("1234");
        assertThat(claim.extras().defendants()).hasSize(1);
        assertThat(claim.extras().occupants()).isEmpty();
    }

    @Test
    void shouldPrepareAssuredClaimWithoutRentArrears() {
        PCSCase pcsCase = prepared("rental-england-assured-no-arrears.xml").pcsCase();

        assertThat(pcsCase.getClaimDueToRentArrears()).isEqualTo(YesOrNo.NO);
        assertThat(pcsCase.getNoRentArrearsGroundsOptions().getMandatoryGrounds()).hasSize(1);
        assertThat(pcsCase.getNoRentArrearsGroundsOptions().getOtherGroundDescription())
            .isEqualTo("Breach of a special term.");
        assertThat(pcsCase.getNoRentArrearsGroundsReasons().getAntisocialBehaviour())
            .isEqualTo("Antisocial behaviour reported.");
        assertThat(pcsCase.getNoticeServed()).isEqualTo(YesOrNo.NO);
        assertThat(pcsCase.getShowRentSectionPage()).isEqualTo(YesOrNo.NO);
        assertThat(pcsCase.getRentArrears()).isNotNull();
        assertThat(pcsCase.getRentArrears().getTotal()).isNull();
    }

    @Test
    void shouldPrepareSecureClaimWithQuarterlyRentAndSuspension() {
        PCSCase pcsCase = prepared("rental-england-secure.xml").pcsCase();

        assertThat(pcsCase.getSecureOrFlexiblePossessionGrounds().getSecureAntisocialAdditionalGrounds()).hasSize(1);
        assertThat(pcsCase.getRentArrearsOrBreachOfTenancy()).hasSize(2);
        assertThat(pcsCase.getShowRentSectionPage()).isEqualTo(YesOrNo.YES);
        assertThat(pcsCase.getRentDetails().getFrequency()).isEqualTo(RentPaymentFrequency.OTHER);
        assertThat(pcsCase.getRentDetails().getOtherFrequency()).isEqualTo("Quarterly");
        assertThat(pcsCase.getRentDetails().getDailyCharge()).isEqualByComparingTo("3.29");
        assertThat(pcsCase.getRentArrears().getTotal()).isEqualByComparingTo("600.00");
        assertThat(pcsCase.getNoticeServedDetails().getPersonName()).isEqualTo("Ann Tenant");
        assertThat(pcsCase.getNoticeServedDetails().getHandedOverDateTime()).isNotNull();
        assertThat(pcsCase.getAlternativesToPossession()).hasSize(1);
        assertThat(pcsCase.getSuspensionOfRightToBuy().getShowHousingActsPage()).isEqualTo(YesOrNo.YES);
        assertThat(pcsCase.getMediationAttempted()).isEqualTo(VerticalYesNo.YES);
    }

    @Test
    void shouldPrepareFlexibleClaimWithBothAlternativesAndEmailNotice() {
        PCSCase pcsCase = prepared("rental-england-flexible.xml").pcsCase();

        assertThat(pcsCase.getTenancyLicenceDetails().getTypeOfTenancyLicence())
            .isEqualTo(TenancyLicenceType.FLEXIBLE_TENANCY);
        assertThat(pcsCase.getAlternativesToPossession()).hasSize(2);
        assertThat(pcsCase.getSuspensionOfRightToBuyDemotionOfTenancy().getSuspensionToBuyDemotionOfTenancyPages())
            .isEqualTo(YesOrNo.YES);
        assertThat(pcsCase.getDemotionOfTenancy().getStatementOfExpressTermsServed()).isEqualTo(VerticalYesNo.YES);
        assertThat(pcsCase.getNoticeServedDetails().getEmailAddress()).isEqualTo("ann@example.org");
        assertThat(pcsCase.getShowRentSectionPage()).isEqualTo(YesOrNo.NO);
    }

    @Test
    void shouldPrepareIntroductoryClaimWithWeeklyRent() {
        PCSCase pcsCase = prepared("rental-england-introductory.xml").pcsCase();

        assertThat(pcsCase.getIntroductoryDemotedOrOtherGroundsForPossession()
            .getHasIntroductoryDemotedOtherGroundsForPossession())
            .isEqualTo(VerticalYesNo.YES);
        assertThat(pcsCase.getRentDetails().getFrequency()).isEqualTo(RentPaymentFrequency.WEEKLY);
        assertThat(pcsCase.getRentDetails().getCalculatedDailyCharge()).isEqualByComparingTo("10.00");
        assertThat(pcsCase.getRentDetails().getPerDayCorrect()).isEqualTo(VerticalYesNo.YES);
        assertThat(pcsCase.getNoticeServedDetails().getDeliveredDate()).isNotNull();
    }

    @Test
    void shouldPrepareDemotedClaimWithNoGrounds() {
        PCSCase pcsCase = prepared("rental-england-demoted-no-grounds.xml").pcsCase();

        assertThat(pcsCase.getIntroductoryDemotedOrOtherGroundsForPossession()
            .getHasIntroductoryDemotedOtherGroundsForPossession())
            .isEqualTo(VerticalYesNo.NO);
        assertThat(pcsCase.getIntroductoryDemotedOtherGroundReason().getNoGrounds())
            .isEqualTo("No ground is relied on.");
        assertThat(pcsCase.getShowRentSectionPage()).isEqualTo(YesOrNo.NO);
        assertThat(pcsCase.getNoticeServedDetails().getOtherElectronicExplanation())
            .isEqualTo("Sent by text message.");
    }

    @Test
    void shouldPrepareOtherTenancyWithTwoDefendants() {
        PreparedBulkClaim claim = prepared("rental-england-other-tenancy.xml");
        PCSCase pcsCase = claim.pcsCase();

        assertThat(pcsCase.getTenancyLicenceDetails().getDetailsOfOtherTypeOfTenancyLicence())
            .isEqualTo("A licence agreement.");
        assertThat(pcsCase.getAddAnotherDefendant()).isEqualTo(VerticalYesNo.YES);
        assertThat(pcsCase.getAdditionalDefendants()).hasSize(1);
        assertThat(pcsCase.getDefendant1().getAddressSameAsPossession()).isEqualTo(VerticalYesNo.YES);
        assertThat(pcsCase.getAdditionalDefendants().getFirst().getValue().getCorrespondenceAddress().getPostCode())
            .isEqualTo("LS1 1AA");
        assertThat(claim.extras().defendants()).hasSize(2);
        assertThat(pcsCase.getNoticeServedDetails().getOtherExplanation()).isEqualTo("Left with a neighbour.");
        assertThat(pcsCase.getHasUnderlesseeOrMortgagee()).isEqualTo(VerticalYesNo.NO);
    }

    @Test
    void shouldCollectEveryValidationErrorForInvalidCombinations() {
        BulkClaimPreparationResult result = prepare("rental-england-invalid-combinations.xml");

        assertThat(result).isInstanceOf(Rejected.class);
        Rejected rejected = (Rejected) result;
        assertThat(rejected.reason()).isEqualTo(BulkFailReason.VALIDATION_FAILED);
        assertThat(rejected.errors()).extracting(error -> error.path()).contains(
            "/RentalClaimSet/RentalClaim[1]/ClaimParticulars/Grounds/Ground[1]/Code",
            "/RentalClaimSet/RentalClaim[1]/ClaimParticulars/Grounds/Ground[2]/Reason",
            "/RentalClaimSet/RentalClaim[1]/ClaimParticulars/Notice/ServiceMethod",
            "/RentalClaimSet/RentalClaim[1]/ClaimParticulars/DailyRent",
            "/RentalClaimSet/RentalClaim[1]/ClaimParticulars/TotalArrears");
    }

    @Test
    void shouldRejectOccupationContractTenancyTypeInEngland() {
        BulkClaimPreparationResult result = prepare("rental-england-assured-arrears-valid.xml", claim ->
            claim.getClaimParticulars().setTenancyType(
                uk.gov.hmcts.reform.pcs.bulkclaim.xml.generated.TenancyTypeCode.SECURE_CONTRACT));

        assertThat(((Rejected) result).errors()).extracting(error -> error.path())
            .contains("/RentalClaimSet/RentalClaim[1]/ClaimParticulars/TenancyType");
    }

    @Test
    void shouldRejectWalesPropertiesWhileTheWalesFlagIsOff() {
        when(featureToggleService.isEnabled(FeatureFlag.WALES_MAKE_A_CLAIM)).thenReturn(false);
        when(eligibility.check(any(), any())).thenReturn(
            new PropertyEligibility(null, LegislativeCountry.WALES, 20262, 1));

        BulkClaimPreparationResult result = prepare("rental-england-assured-arrears-valid.xml");

        assertThat(((Rejected) result).reason()).isEqualTo(BulkFailReason.CLAIM_KIND_NOT_ENABLED);
    }

    @Test
    void shouldRejectIneligibleProperty() {
        when(eligibility.check(any(), any())).thenReturn(
            new PropertyEligibility(BulkFailReason.NO_COURT_FOR_POSTCODE, null, null, null));

        Rejected rejected = (Rejected) prepare("rental-england-assured-arrears-valid.xml");

        assertThat(rejected.reason()).isEqualTo(BulkFailReason.NO_COURT_FOR_POSTCODE);
        assertThat(rejected.customerUniqueIdentifier()).isEqualTo("ASSURED-ARREARS");
    }

    @Test
    void shouldRejectPrivateLandlordInEngland() {
        Rejected rejected = (Rejected) prepare("rental-england-assured-arrears-valid.xml", claim ->
            claim.setClaimantDesignation(
                uk.gov.hmcts.reform.pcs.bulkclaim.xml.generated.ClaimantDesignationRentalType.PRIVATE_LANDLORD));

        assertThat(rejected.reason()).isEqualTo(BulkFailReason.CLAIMANT_TYPE_NOT_ELIGIBLE);
    }

    @Test
    void shouldRejectClaimantTypeThatIsNotEnabled() {
        BulkClaimProperties properties = new BulkClaimProperties();
        properties.setEnabledClaimantTypes(List.of(uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantType.MORTGAGE_LENDER));
        underTest = RentalPreparerTestSupport.preparer(eligibility, resolver, properties, featureToggleService);

        Rejected rejected = (Rejected) prepare("rental-england-assured-arrears-valid.xml");

        assertThat(rejected.reason()).isEqualTo(BulkFailReason.CLAIMANT_TYPE_NOT_ELIGIBLE);
    }

    @Test
    void shouldRejectInvalidPbaOverrideAndUseValidOne() {
        when(resolver.isPbaAccountValid(any(), Mockito.eq("PBA7654321"))).thenReturn(false);

        Rejected rejected = (Rejected) prepare("rental-england-assured-arrears-valid.xml",
                                               claim -> claim.setPaymentAccountNumber("PBA7654321"));
        assertThat(rejected.reason()).isEqualTo(BulkFailReason.PBA_OVERRIDE_NOT_VALID);
        assertThat(rejected.errors().getFirst().path())
            .isEqualTo("/RentalClaimSet/RentalClaim[1]/PaymentAccountNumber");

        when(resolver.isPbaAccountValid(any(), Mockito.eq("PBA7654321"))).thenReturn(true);
        PreparedBulkClaim prepared = ((Prepared) prepare("rental-england-assured-arrears-valid.xml",
            claim -> claim.setPaymentAccountNumber("PBA7654321"))).claim();
        assertThat(prepared.pbaAccountNumber()).isEqualTo("PBA7654321");
    }

    @Test
    void shouldFlagRentFieldsAsBigDecimalAmounts() {
        PCSCase pcsCase = prepared("rental-england-assured-arrears-valid.xml").pcsCase();

        assertThat(pcsCase.getRentDetails().getCurrentRent()).isEqualByComparingTo(new BigDecimal("800.00"));
    }
}
