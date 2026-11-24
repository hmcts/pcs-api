package uk.gov.hmcts.reform.pcs.bulkclaim.prepare.mortgage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.pcs.bulkclaim.config.BulkClaimProperties;
import uk.gov.hmcts.reform.pcs.bulkclaim.model.BulkClaimExtras;
import uk.gov.hmcts.reform.pcs.bulkclaim.model.BulkClaimPreparationResult;
import uk.gov.hmcts.reform.pcs.bulkclaim.model.BulkClaimPreparationResult.Prepared;
import uk.gov.hmcts.reform.pcs.bulkclaim.model.BulkClaimPreparationResult.Rejected;
import uk.gov.hmcts.reform.pcs.bulkclaim.model.BulkFailReason;
import uk.gov.hmcts.reform.pcs.bulkclaim.model.BulkValidationError;
import uk.gov.hmcts.reform.pcs.bulkclaim.model.PreparedBulkClaim;
import uk.gov.hmcts.reform.pcs.bulkclaim.org.BulkClaimantOrganisationResolver;
import uk.gov.hmcts.reform.pcs.bulkclaim.prepare.common.BulkPropertyEligibility;
import uk.gov.hmcts.reform.pcs.bulkclaim.prepare.common.BulkPropertyEligibility.PropertyEligibility;
import uk.gov.hmcts.reform.pcs.bulkclaim.xml.BulkClaimSchemaValidationException;
import uk.gov.hmcts.reform.pcs.bulkclaim.xml.BulkClaimXmlCodec;
import uk.gov.hmcts.reform.pcs.bulkclaim.xml.generated.AdditionalPaymentType;
import uk.gov.hmcts.reform.pcs.bulkclaim.xml.generated.MortgageClaimSet;
import uk.gov.hmcts.reform.pcs.bulkclaim.xml.generated.SigneeStatusType;
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantType;
import uk.gov.hmcts.reform.pcs.ccd.domain.CompletionNextStep;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.statementoftruth.StatementOfTruthCompletedBy;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;
import uk.gov.hmcts.reform.pcs.service.FeatureFlag;
import uk.gov.hmcts.reform.pcs.service.FeatureToggleService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.bulkclaim.prepare.mortgage.MortgagePreparerTestSupport.fixture;
import static uk.gov.hmcts.reform.pcs.bulkclaim.prepare.mortgage.MortgagePreparerTestSupport.organisation;

class DefaultMortgageBulkClaimPreparerTest {

    private final BulkClaimXmlCodec codec = new BulkClaimXmlCodec();
    private BulkPropertyEligibility eligibility;
    private BulkClaimantOrganisationResolver resolver;
    private FeatureToggleService featureToggleService;
    private BulkClaimProperties properties;
    private DefaultMortgageBulkClaimPreparer underTest;

    @BeforeEach
    void setUp() {
        eligibility = mock(BulkPropertyEligibility.class);
        resolver = mock(BulkClaimantOrganisationResolver.class);
        featureToggleService = mock(FeatureToggleService.class);
        properties = new BulkClaimProperties();
        when(resolver.isPbaAccountValid(any(), any())).thenReturn(true);
        when(featureToggleService.isEnabled(FeatureFlag.WALES_MAKE_A_CLAIM)).thenReturn(true);
        when(eligibility.check(any(), any())).thenReturn(eligible(LegislativeCountry.ENGLAND));
        underTest = MortgagePreparerTestSupport.preparer(eligibility, resolver, featureToggleService, properties);
    }

    private static PropertyEligibility eligible(LegislativeCountry country) {
        return new PropertyEligibility(null, country, 20262, 1);
    }

    private BulkClaimPreparationResult prepare(String fixtureName) {
        return prepare(fixtureName, claim -> { });
    }

    private BulkClaimPreparationResult prepare(String fixtureName, Consumer<MortgageClaimSet.MortgageClaim> tweak) {
        MortgageClaimSet set = codec.parseMortgage(fixture(fixtureName));
        MortgageClaimSet.MortgageClaim claim = set.getMortgageClaim().getFirst();
        tweak.accept(claim);
        return underTest.prepare(set, claim, organisation());
    }

    private PreparedBulkClaim prepared(String fixtureName) {
        BulkClaimPreparationResult result = prepare(fixtureName);
        assertThat(result).as(result.toString()).isInstanceOf(Prepared.class);
        return ((Prepared) result).claim();
    }

    private Rejected rejected(BulkClaimPreparationResult result) {
        assertThat(result).as(result.toString()).isInstanceOf(Rejected.class);
        return (Rejected) result;
    }

    @Test
    void shouldPrepareEnglandMortgageClaim() {
        PreparedBulkClaim claim = prepared("mortgage-england-valid.xml");
        PCSCase pcsCase = claim.pcsCase();

        assertThat(claim.pbaAccountNumber()).isEqualTo("PBA1234567");
        assertThat(pcsCase.getLegislativeCountry()).isEqualTo(LegislativeCountry.ENGLAND);
        assertThat(pcsCase.getClaimantType().getValueCode()).isEqualTo(ClaimantType.MORTGAGE_LENDER.name());
        assertThat(pcsCase.getClaimAgainstTrespassers()).isEqualTo(VerticalYesNo.NO);
        assertThat(pcsCase.getWantToUploadDocuments()).isEqualTo(VerticalYesNo.NO);
        assertThat(pcsCase.getCompletionNextStep()).isEqualTo(CompletionNextStep.SUBMIT_AND_PAY_NOW);
        assertThat(pcsCase.getPreActionProtocolCompleted()).isEqualTo(VerticalYesNo.YES);
        assertThat(pcsCase.getAdditionalDefendants()).hasSize(1);

        assertThat(pcsCase.getTenancyLicenceDetails()).isNull();
        assertThat(pcsCase.getNoticeServed()).isNull();
        assertThat(pcsCase.getRentArrears()).isNull();
        assertThat(pcsCase.getClaimDueToRentArrears()).isNull();

        assertThat(pcsCase.getStatementOfTruth().getCompletedBy()).isEqualTo(StatementOfTruthCompletedBy.CLAIMANT);
        assertThat(pcsCase.getStatementOfTruth().getPositionParty()).isEqualTo("Possessions Manager");
        assertThat(pcsCase.getStatementOfTruth().getSignedDate()).isEqualTo(LocalDate.of(2026, 11, 3));
    }

    @Test
    void shouldMapMortgageParticularsToExtras() {
        BulkClaimExtras extras = prepared("mortgage-england-valid.xml").extras();

        assertThat(extras.claimantDesignation()).isEqualTo("Commercial");
        assertThat(extras.legacyPcolClaimantReference()).isEqualTo("555");
        assertThat(extras.noticeType()).isNull();
        assertThat(extras.paymentHistory()).hasSize(2);

        BulkClaimExtras.MortgageParticulars mortgage = extras.mortgageParticulars();
        assertThat(mortgage.mortgageDate()).isEqualTo(LocalDate.of(2015, 6, 1));
        assertThat(mortgage.defaultNoticeDate()).isEqualTo(LocalDate.of(2026, 10, 5));
        assertThat(mortgage.loanAmount()).isEqualByComparingTo("200000.00");
        assertThat(mortgage.currentRepaymentTerms()).isEqualTo("Monthly repayments of 1000.00.");
        assertThat(mortgage.fullRepaymentDate()).isEqualTo(LocalDate.of(2040, 6, 1));
        assertThat(mortgage.fullRepaymentAmount()).isEqualByComparingTo("180000.00");
        assertThat(mortgage.costsAndAdministration()).isEqualByComparingTo("250.00");
        assertThat(mortgage.outstandingAmount()).isEqualByComparingTo("175000.00");
        assertThat(mortgage.startInterestRate()).isEqualByComparingTo("3.5");
        assertThat(mortgage.preArrearsInterestRate()).isEqualByComparingTo("4.25");
        assertThat(mortgage.claimStartInterestRate()).isEqualByComparingTo("5");
    }

    @Test
    void shouldMapMortgagePartiesStepsAndAdditionalPayments() {
        BulkClaimExtras.MortgageParticulars mortgage =
            prepared("mortgage-england-valid.xml").extras().mortgageParticulars();

        assertThat(mortgage.interestedParty()).isEqualTo("Example Building Society");
        assertThat(mortgage.tenancyDate()).isEqualTo(LocalDate.of(2020, 1, 1));
        assertThat(mortgage.tenantNoticeServedDate()).isEqualTo(LocalDate.of(2026, 10, 20));
        assertThat(mortgage.recoverySteps()).isEqualTo("Letters and telephone calls to the borrowers.");
        assertThat(mortgage.moneyJudgmentWanted()).isTrue();
        assertThat(mortgage.originalProviderName()).isEqualTo("First Lender Ltd");
        assertThat(mortgage.originalProviderBoughtDate()).isEqualTo(LocalDate.of(2019, 4, 1));
        assertThat(mortgage.originalProviderDefendantNotifiedDate()).isEqualTo(LocalDate.of(2019, 5, 1));
        assertThat(mortgage.additionalPayments()).hasSize(2);
        assertThat(mortgage.additionalPayments().getFirst().reason()).isEqualTo("Insurance premium");
        assertThat(mortgage.additionalPayments().getFirst().arrears()).isEqualByComparingTo("50.00");
        assertThat(mortgage.additionalPayments().getLast().arrears()).isNull();
    }

    @Test
    void shouldUseStatedTotalArrearsWhenSupplied() {
        PreparedBulkClaim claim = prepared("mortgage-claim.xml");

        assertThat(claim.extras().mortgageParticulars().totalArrears()).isEqualByComparingTo("5000.00");
    }

    @Test
    void shouldDeriveTotalArrearsFromLatestPaymentLineWhenNotStated() {
        PreparedBulkClaim claim = prepared("mortgage-england-valid.xml");

        assertThat(claim.extras().mortgageParticulars().totalArrears()).isEqualByComparingTo("1750.00");
    }

    @Test
    void shouldMapOccupants() {
        List<BulkClaimExtras.Occupant> occupants = prepared("mortgage-england-valid.xml").extras().occupants();

        assertThat(occupants).hasSize(2);
        assertThat(occupants.getFirst())
            .isEqualTo(new BulkClaimExtras.Occupant("Mr", "Sam", "Occupier", "Jay", "SJ"));
        assertThat(occupants.getLast().firstName()).isEqualTo("Alex");
    }

    @Test
    void shouldMapDefendantOccupantFlags() {
        BulkClaimExtras extras = prepared("mortgage-england-valid.xml").extras();

        assertThat(extras.defendants()).extracting(BulkClaimExtras.PartyExtras::isOccupant)
            .containsExactly(false, true);
        assertThat(extras.defendants().getLast().title()).isEqualTo("Ms");
    }

    @Test
    void shouldPrepareWalesMortgageClaimWhenFlagEnabled() {
        when(eligibility.check(any(), any())).thenReturn(eligible(LegislativeCountry.WALES));

        PreparedBulkClaim claim = prepared("mortgage-wales-valid.xml");

        assertThat(claim.pcsCase().getLegislativeCountry()).isEqualTo(LegislativeCountry.WALES);
        assertThat(claim.pcsCase().getClaimantType().getValueCode()).isEqualTo("MORTGAGE_LENDER");
        assertThat(claim.pcsCase().getPreActionProtocolCompleted()).isEqualTo(VerticalYesNo.YES);
        assertThat(claim.pcsCase().getPreActionProtocolIncompleteExplanation()).isNull();
        assertThat(claim.extras().mortgageParticulars()).isNotNull();
    }

    @Test
    void shouldRejectWalesWhenFlagDisabled() {
        when(eligibility.check(any(), any())).thenReturn(eligible(LegislativeCountry.WALES));
        when(featureToggleService.isEnabled(FeatureFlag.WALES_MAKE_A_CLAIM)).thenReturn(false);

        Rejected rejected = rejected(prepare("mortgage-wales-valid.xml"));

        assertThat(rejected.reason()).isEqualTo(BulkFailReason.CLAIM_KIND_NOT_ENABLED);
        assertThat(rejected.errors()).extracting(BulkValidationError::path)
            .containsExactly("/MortgageClaimSet/MortgageClaim[1]/Property");
    }

    @Test
    void shouldRejectWhenMortgageLenderIsNotAnEnabledClaimantType() {
        properties.setEnabledClaimantTypes(List.of(ClaimantType.PROVIDER_OF_SOCIAL_HOUSING));

        Rejected rejected = rejected(prepare("mortgage-england-valid.xml"));

        assertThat(rejected.reason()).isEqualTo(BulkFailReason.CLAIMANT_TYPE_NOT_ELIGIBLE);
        assertThat(rejected.errors()).extracting(BulkValidationError::path)
            .containsExactly("/MortgageClaimSet/MortgageClaim[1]/ClaimantDesignation");
    }

    @Test
    void shouldRejectIneligibleProperty() {
        when(eligibility.check(any(), any())).thenReturn(
            new PropertyEligibility(BulkFailReason.PROPERTY_NOT_ELIGIBLE, null, null, null));

        assertThat(rejected(prepare("mortgage-england-valid.xml")).reason())
            .isEqualTo(BulkFailReason.PROPERTY_NOT_ELIGIBLE);
    }

    @Test
    void shouldRejectInvalidPbaOverride() {
        when(resolver.isPbaAccountValid(any(), any())).thenReturn(false);

        Rejected rejected = rejected(prepare("mortgage-england-valid.xml",
                                             claim -> claim.setPaymentAccountNumber("PBA9999999")));

        assertThat(rejected.reason()).isEqualTo(BulkFailReason.PBA_OVERRIDE_NOT_VALID);
    }

    @Test
    void shouldRejectMissingPaymentHistory() {
        Rejected rejected = rejected(prepare("mortgage-england-valid.xml",
                                             claim -> claim.getParticularsOfClaim().setPaymentHistory(null)));

        assertThat(rejected.reason()).isEqualTo(BulkFailReason.VALIDATION_FAILED);
        assertThat(rejected.errors()).extracting(BulkValidationError::path)
            .contains("/MortgageClaimSet/MortgageClaim[1]/ParticularsOfClaim/PaymentHistory");
    }

    @Test
    void shouldRejectEmptyPaymentHistory() {
        Rejected rejected = rejected(prepare("mortgage-england-valid.xml",
                                             claim -> claim.getParticularsOfClaim().getPaymentHistory()
                                                 .getPaymentLine().clear()));

        assertThat(rejected.errors()).extracting(BulkValidationError::code).contains("REQUIRED");
    }

    @Test
    void shouldCollectEveryValidationError() {
        Rejected rejected = rejected(prepare("mortgage-invalid.xml"));

        assertThat(rejected.reason()).isEqualTo(BulkFailReason.VALIDATION_FAILED);
        String base = "/MortgageClaimSet/MortgageClaim[1]/";
        assertThat(rejected.errors()).extracting(BulkValidationError::path).contains(
            base + "ParticularsOfClaim/MortgageDate",
            base + "ParticularsOfClaim/DefaultNoticeDate",
            base + "ParticularsOfClaim/CurrentRepaymentTerms",
            base + "ParticularsOfClaim/AdditionalPayments[2]/AdditionalPaymentReason",
            base + "ParticularsOfClaim/OriginalMortgageProvider/MortgageBoughtDate",
            base + "ParticularsOfClaim/OriginalMortgageProvider/DateDefendantNotified",
            base + "ParticularsOfClaim/ClaimStatementOfTruth/SignedDate",
            base + "ParticularsOfClaim/ClaimStatementOfTruth/Position",
            base + "Occupant[2]/FirstName"
        );
    }

    @Test
    void shouldRejectNegativeMoneyAndOutOfRangeRates() {
        Rejected rejected = rejected(prepare("mortgage-england-valid.xml", claim -> {
            var particulars = claim.getParticularsOfClaim();
            particulars.setLoanAmount(new BigDecimal("-1.00"));
            particulars.setStartInterestRate(new BigDecimal("100.0001"));
            particulars.setPreArrearsInterestRate(new BigDecimal("-0.5"));
        }));

        String base = "/MortgageClaimSet/MortgageClaim[1]/ParticularsOfClaim/";
        assertThat(rejected.errors()).extracting(BulkValidationError::path)
            .contains(base + "LoanAmount", base + "StartInterestRate", base + "PreArrearsInterestRate");
    }

    @Test
    void shouldRejectOverlongTextFields() {
        Rejected rejected = rejected(prepare("mortgage-england-valid.xml", claim -> {
            var particulars = claim.getParticularsOfClaim();
            particulars.setCurrentRepaymentTerms("x".repeat(951));
            particulars.setInterestedParty("x".repeat(251));
            particulars.getAdditionalPayments().getFirst().setAdditionalPaymentReason("x".repeat(251));
            particulars.getOriginalMortgageProvider().setMortgageProviderName("x".repeat(101));
        }));

        assertThat(rejected.errors()).extracting(BulkValidationError::code).containsOnly("TOO_LONG");
        assertThat(rejected.errors()).hasSize(4);
    }

    @Test
    void shouldRejectMoreThanThreeAdditionalPayments() {
        Rejected rejected = rejected(prepare("mortgage-england-valid.xml", claim -> {
            List<AdditionalPaymentType> payments = claim.getParticularsOfClaim().getAdditionalPayments();
            payments.add(payments.getFirst());
            payments.add(payments.getFirst());
        }));

        assertThat(rejected.errors()).extracting(BulkValidationError::path)
            .containsExactly("/MortgageClaimSet/MortgageClaim[1]/ParticularsOfClaim/AdditionalPayments[4]");
    }

    @Test
    void shouldRejectMoreThanThreeAdditionalPaymentsInTheSchema() {
        byte[] xml = fixture("mortgage-too-many-additional-payments.xml");

        assertThatThrownBy(() -> codec.parseMortgage(xml)).isInstanceOf(BulkClaimSchemaValidationException.class);
    }

    @Test
    void shouldAllowFutureFullRepaymentDate() {
        prepared("mortgage-england-valid.xml");
    }

    @Test
    void shouldMapSignedDateForLegalRepresentative() {
        PreparedBulkClaim claim = ((Prepared) prepare("mortgage-england-valid.xml", mortgage -> {
            var statement = mortgage.getParticularsOfClaim().getClaimStatementOfTruth();
            statement.setSigneeStatus(SigneeStatusType.LEGAL_REPRESENTATIVE);
            statement.setFirmName("Firm LLP");
        })).claim();

        assertThat(claim.pcsCase().getStatementOfTruth().getCompletedBy())
            .isEqualTo(StatementOfTruthCompletedBy.LEGAL_REPRESENTATIVE);
        assertThat(claim.pcsCase().getStatementOfTruth().getSignedDate()).isEqualTo(LocalDate.of(2026, 11, 3));
    }
}
