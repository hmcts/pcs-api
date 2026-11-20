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
import uk.gov.hmcts.reform.pcs.bulkclaim.xml.generated.ClaimantDesignationRentalType;
import uk.gov.hmcts.reform.pcs.bulkclaim.xml.generated.RentalClaimSet;
import uk.gov.hmcts.reform.pcs.bulkclaim.xml.generated.WalesNoticeType;
import uk.gov.hmcts.reform.pcs.bulkclaim.xml.generated.WalesType;
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantType;
import uk.gov.hmcts.reform.pcs.ccd.domain.NoticeServiceMethod;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentPaymentFrequency;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.DiscretionaryGroundWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.EstateManagementGroundsWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.MandatoryGroundWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.OccupationLicenceTypeWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.SecureContractDiscretionaryGroundsWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.SecureContractMandatoryGroundsWales;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;
import uk.gov.hmcts.reform.pcs.service.FeatureFlag;
import uk.gov.hmcts.reform.pcs.service.FeatureToggleService;

import java.time.LocalDate;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.bulkclaim.prepare.rental.RentalPreparerTestSupport.fixture;
import static uk.gov.hmcts.reform.pcs.bulkclaim.prepare.rental.RentalPreparerTestSupport.organisation;

class WalesRentalBulkClaimPreparerTest {

    private static final String CLAIM = "/RentalClaimSet/RentalClaim[1]/ClaimParticulars";

    private final BulkClaimXmlCodec codec = new BulkClaimXmlCodec();
    private BulkPropertyEligibility eligibility;
    private FeatureToggleService featureToggleService;
    private BulkClaimantOrganisationResolver resolver;
    private DefaultRentalBulkClaimPreparer underTest;

    @BeforeEach
    void setUp() {
        eligibility = Mockito.mock(BulkPropertyEligibility.class);
        resolver = Mockito.mock(BulkClaimantOrganisationResolver.class);
        featureToggleService = Mockito.mock(FeatureToggleService.class);
        when(resolver.isPbaAccountValid(any(), any())).thenReturn(true);
        when(featureToggleService.isEnabled(FeatureFlag.WALES_MAKE_A_CLAIM)).thenReturn(true);
        when(eligibility.check(any(), any())).thenReturn(
            new PropertyEligibility(null, LegislativeCountry.WALES, 20262, 1));
        underTest = RentalPreparerTestSupport.preparer(
            eligibility, resolver, new BulkClaimProperties(), featureToggleService);
    }

    private BulkClaimPreparationResult prepare(String fixtureName, Consumer<RentalClaimSet.RentalClaim> tweak) {
        RentalClaimSet set = codec.parseRental(fixture(fixtureName));
        RentalClaimSet.RentalClaim claim = set.getRentalClaim().getFirst();
        tweak.accept(claim);
        return underTest.prepare(set, claim, organisation());
    }

    private PreparedBulkClaim prepared(String fixtureName) {
        BulkClaimPreparationResult result = prepare(fixtureName, claim -> { });
        assertThat(result).as(result.toString()).isInstanceOf(Prepared.class);
        return ((Prepared) result).claim();
    }

    private List<String> errorPaths(String fixtureName, Consumer<RentalClaimSet.RentalClaim> tweak) {
        BulkClaimPreparationResult result = prepare(fixtureName, tweak);
        assertThat(result).isInstanceOf(Rejected.class);
        return ((Rejected) result).errors().stream().map(error -> error.path()).toList();
    }

    @Test
    void shouldPrepareStandardContractWithRentArrears() {
        PreparedBulkClaim claim = prepared("rental-wales-standard-arrears.xml");
        PCSCase pcsCase = claim.pcsCase();

        assertThat(pcsCase.getLegislativeCountry()).isEqualTo(LegislativeCountry.WALES);
        assertThat(pcsCase.getClaimantType().getValueCode()).isEqualTo("COMMUNITY_LANDLORD");
        assertThat(claim.extras().claimantDesignation()).isEqualTo("SocialLandlord");

        assertThat(pcsCase.getOccupationLicenceDetailsWales().getOccupationLicenceTypeWales())
            .isEqualTo(OccupationLicenceTypeWales.STANDARD_CONTRACT);
        assertThat(pcsCase.getOccupationLicenceDetailsWales().getLicenceStartDate())
            .isEqualTo(LocalDate.of(2022, 3, 1));
        assertThat(pcsCase.getTenancyLicenceDetails()).isNull();

        assertThat(pcsCase.getGroundsForPossessionWales().getDiscretionaryGrounds())
            .containsExactly(DiscretionaryGroundWales.RENT_ARREARS_S157);
        assertThat(pcsCase.getGroundsForPossessionWales().getMandatoryGrounds())
            .containsExactly(MandatoryGroundWales.SERIOUS_ARREARS_PERIODIC_S181);
        assertThat(pcsCase.getShowASBQuestionsPageWales()).isEqualTo(YesOrNo.NO);
        assertThat(pcsCase.getShowReasonsForGroundsPageWales()).isEqualTo(YesOrNo.NO);

        assertThat(pcsCase.getWalesNoticeDetails().getNoticeServed()).isEqualTo(YesOrNo.YES);
        assertThat(pcsCase.getWalesNoticeDetails().getTypeOfNoticeServed()).isEqualTo("RHW20");
        assertThat(pcsCase.getNoticeServedDetails().getServiceMethod()).isEqualTo(NoticeServiceMethod.FIRST_CLASS_POST);
        assertThat(pcsCase.getNoticeServed()).isNull();
        assertThat(claim.extras().noticeType()).isEqualTo("RHW20");

        assertThat(pcsCase.getShowRentSectionPage()).isEqualTo(YesOrNo.YES);
        assertThat(pcsCase.getRentDetails().getFrequency()).isEqualTo(RentPaymentFrequency.MONTHLY);
        assertThat(pcsCase.getRentArrears().getTotal()).isEqualByComparingTo("2400.50");
        assertThat(pcsCase.getArrearsJudgmentWanted()).isEqualTo(VerticalYesNo.YES);

        assertThat(pcsCase.getIsExemptLandlord()).isEqualTo(VerticalYesNo.NO);
        assertThat(pcsCase.getProhibitedConductWalesClaim()).isEqualTo(VerticalYesNo.NO);
        assertThat(pcsCase.getRequiredDocumentsWales().getHasEnergyPerformanceCertificate())
            .isEqualTo(VerticalYesNo.YES);
        assertThat(pcsCase.getPreActionProtocolCompleted()).isEqualTo(VerticalYesNo.YES);
        assertThat(pcsCase.getAlternativesToPossession()).isNull();
    }

    @Test
    void shouldPrepareSecureContractWithAntisocialBehaviour() {
        PCSCase pcsCase = prepared("rental-wales-secure-asb.xml").pcsCase();

        assertThat(pcsCase.getOccupationLicenceDetailsWales().getOccupationLicenceTypeWales())
            .isEqualTo(OccupationLicenceTypeWales.SECURE_CONTRACT);
        assertThat(pcsCase.getSecureContractGroundsForPossessionWales().getDiscretionaryGrounds())
            .containsExactly(SecureContractDiscretionaryGroundsWales.ANTISOCIAL_BEHAVIOUR_S157);
        assertThat(pcsCase.getGroundsForPossessionWales()).isNull();
        assertThat(pcsCase.getShowASBQuestionsPageWales()).isEqualTo(YesOrNo.YES);
        assertThat(pcsCase.getShowReasonsForGroundsPageWales()).isEqualTo(YesOrNo.NO);

        assertThat(pcsCase.getAsbQuestionsWales().getAntisocialBehaviour()).isEqualTo(VerticalYesNo.YES);
        assertThat(pcsCase.getAsbQuestionsWales().getAntisocialBehaviourDetails())
            .isEqualTo("Loud parties and threats to neighbours.");
        assertThat(pcsCase.getAsbQuestionsWales().getIllegalPurposesUse()).isEqualTo(VerticalYesNo.NO);
        assertThat(pcsCase.getAsbQuestionsWales().getOtherProhibitedConduct()).isEqualTo(VerticalYesNo.YES);

        assertThat(pcsCase.getProhibitedConductWalesClaim()).isEqualTo(VerticalYesNo.YES);
        assertThat(pcsCase.getProhibitedConductWalesClaimDetails())
            .isEqualTo("The defendant has repeatedly harassed neighbours.");
        assertThat(pcsCase.getPeriodicContractTermsWales().getDetailsOfTerms()).isEqualTo("Pets are not allowed.");

        assertThat(pcsCase.getWalesNoticeDetails().getNoticeServed()).isEqualTo(YesOrNo.NO);
        assertThat(pcsCase.getWalesNoticeDetails().getNoticeStatement()).startsWith("I am a landlord");
        assertThat(pcsCase.getShowRentSectionPage()).isEqualTo(YesOrNo.NO);
        assertThat(pcsCase.getRentArrears().getTotal()).isNull();
    }

    @Test
    void shouldPrepareOtherContractWithEstateManagementGroundsWithTheirOwnReasons() {
        PreparedBulkClaim claim = prepared("rental-wales-estate-management.xml");
        PCSCase pcsCase = claim.pcsCase();

        assertThat(pcsCase.getOccupationLicenceDetailsWales().getOccupationLicenceTypeWales())
            .isEqualTo(OccupationLicenceTypeWales.OTHER);
        assertThat(pcsCase.getOccupationLicenceDetailsWales().getOtherLicenceTypeDetails())
            .isEqualTo("A licence agreement.");
        assertThat(pcsCase.getGroundsForPossessionWales().getDiscretionaryGrounds()).containsExactlyInAnyOrder(
            DiscretionaryGroundWales.ESTATE_MANAGEMENT_GROUNDS_S160,
            DiscretionaryGroundWales.OTHER_BREACH_OF_CONTRACT_S157);
        assertThat(pcsCase.getGroundsForPossessionWales().getEstateManagementGrounds()).containsExactlyInAnyOrder(
            EstateManagementGroundsWales.BUILDING_WORKS, EstateManagementGroundsWales.REDEVELOPMENT_SCHEMES);
        assertThat(pcsCase.getGroundsForPossessionWales().getMandatoryGrounds())
            .containsExactly(MandatoryGroundWales.LANDLORD_NOTICE_PERIODIC_S178);
        assertThat(pcsCase.getGroundsReasonsWales().getBuildingWorksReason())
            .isEqualTo("The block is being redeveloped.");
        assertThat(pcsCase.getGroundsReasonsWales().getRedevelopmentSchemesReason())
            .isEqualTo("The estate is part of a regeneration scheme.");
        assertThat(pcsCase.getGroundsReasonsWales().getOtherBreachSection157Reason())
            .isEqualTo("Sub-letting without consent.");
        assertThat(pcsCase.getGroundsReasonsWales().getLandlordNoticePeriodicS178Reason())
            .isEqualTo("Notice was given and has expired.");
        assertThat(pcsCase.getShowReasonsForGroundsPageWales()).isEqualTo(YesOrNo.YES);
        assertThat(pcsCase.getShowASBQuestionsPageWales()).isEqualTo(YesOrNo.NO);
        assertThat(pcsCase.getAsbQuestionsWales()).isNull();
        assertThat(pcsCase.getShowRentSectionPage()).isEqualTo(YesOrNo.NO);
    }

    @Test
    void shouldPrepareExemptLandlordOnASecureContract() {
        PCSCase pcsCase = prepared("rental-wales-exempt-landlord.xml").pcsCase();

        assertThat(pcsCase.getIsExemptLandlord()).isEqualTo(VerticalYesNo.YES);
        assertThat(pcsCase.getSecureContractGroundsForPossessionWales().getMandatoryGrounds())
            .containsExactly(SecureContractMandatoryGroundsWales.FAILURE_TO_GIVE_UP_POSSESSION_S170);
        assertThat(pcsCase.getSecureContractGroundsForPossessionWales().getEstateManagementGrounds())
            .containsExactly(EstateManagementGroundsWales.DISABLED_SUITABLE_DWELLING);
        assertThat(pcsCase.getGroundsReasonsWales().getSecureDisabledSuitableDwellingReason())
            .isEqualTo("The dwelling is needed for a disabled person.");
        assertThat(pcsCase.getGroundsReasonsWales().getSecureFailureToGiveUpPossessionSection170Reason())
            .isEqualTo("The contract-holder did not leave.");
        assertThat(pcsCase.getPeriodicContractTermsWales().getAgreedTermsOfPeriodicContract())
            .isEqualTo(VerticalYesNo.NO);
    }

    @Test
    void shouldPrepareCertificatesThatAreNotProvided() {
        PCSCase pcsCase = prepared("rental-wales-certificates-not-provided.xml").pcsCase();

        assertThat(pcsCase.getRequiredDocumentsWales().getHasEnergyPerformanceCertificate())
            .isEqualTo(VerticalYesNo.NO);
        assertThat(pcsCase.getRequiredDocumentsWales().getNoEpcReason())
            .isEqualTo("The certificate is held by the previous owner.");
        assertThat(pcsCase.getRequiredDocumentsWales().getNoGasReportReason()).isEqualTo("There is no gas supply.");
        assertThat(pcsCase.getRequiredDocumentsWales().getNoEicrReason()).isEqualTo("The inspection is booked.");
        assertThat(pcsCase.getGroundsForPossessionWales().getMandatoryGrounds())
            .containsExactly(MandatoryGroundWales.SERIOUS_ARREARS_FIXED_TERM_S187);
        assertThat(pcsCase.getShowRentSectionPage()).isEqualTo(YesOrNo.YES);
    }

    @Test
    void shouldRejectWalesWhileTheFeatureIsOff() {
        when(featureToggleService.isEnabled(FeatureFlag.WALES_MAKE_A_CLAIM)).thenReturn(false);

        Rejected rejected = (Rejected) prepare("rental-wales-standard-arrears.xml", claim -> { });

        assertThat(rejected.reason()).isEqualTo(BulkFailReason.CLAIM_KIND_NOT_ENABLED);
    }

    @Test
    void shouldAcceptPrivateLandlordInWales() {
        Prepared prepared = (Prepared) prepare("rental-wales-standard-arrears.xml", claim ->
            claim.setClaimantDesignation(ClaimantDesignationRentalType.PRIVATE_LANDLORD));

        assertThat(prepared.claim().pcsCase().getClaimantType().getValueCode()).isEqualTo("PRIVATE_LANDLORD");
    }

    @Test
    void shouldRejectPrivateLandlordInWalesWhenNotEnabled() {
        BulkClaimProperties properties = new BulkClaimProperties();
        properties.setEnabledClaimantTypes(List.of(ClaimantType.COMMUNITY_LANDLORD));
        underTest = RentalPreparerTestSupport.preparer(eligibility, resolver, properties, featureToggleService);

        Rejected rejected = (Rejected) prepare("rental-wales-standard-arrears.xml", claim ->
            claim.setClaimantDesignation(ClaimantDesignationRentalType.PRIVATE_LANDLORD));

        assertThat(rejected.reason()).isEqualTo(BulkFailReason.CLAIMANT_TYPE_NOT_ELIGIBLE);
    }

    @Test
    void shouldRejectCommunityLandlordThatIsNotEnabled() {
        BulkClaimProperties properties = new BulkClaimProperties();
        properties.setEnabledClaimantTypes(List.of(ClaimantType.PROVIDER_OF_SOCIAL_HOUSING));
        underTest = RentalPreparerTestSupport.preparer(eligibility, resolver, properties, featureToggleService);

        Rejected rejected = (Rejected) prepare("rental-wales-standard-arrears.xml", claim -> { });

        assertThat(rejected.reason()).isEqualTo(BulkFailReason.CLAIMANT_TYPE_NOT_ELIGIBLE);
    }

    @Test
    void shouldIgnoreThePreActionProtocolExplanationInWales() {
        PCSCase pcsCase = ((Prepared) prepare("rental-wales-standard-arrears.xml", claim -> {
            claim.getClaimParticulars().setPreActionProtocolCompleted(false);
            claim.getClaimParticulars().setPreActionProtocolIncompleteExplanation("Not needed");
        })).claim().pcsCase();

        assertThat(pcsCase.getPreActionProtocolCompleted()).isEqualTo(VerticalYesNo.NO);
        assertThat(pcsCase.getPreActionProtocolIncompleteExplanation()).isNull();
    }

    @Test
    void shouldRejectEnglandTenancyTypeAndEnglandOnlyElementsInWales() {
        List<String> paths = errorPaths("rental-wales-invalid-tenancy.xml", claim -> { });

        assertThat(paths).contains(
            CLAIM + "/TenancyType",
            CLAIM + "/Notice",
            CLAIM + "/SuspensionOfRightToBuy",
            CLAIM + "/WalesNotice",
            CLAIM + "/Wales");
    }

    @Test
    void shouldReportEveryInvalidWalesCombination() {
        BulkClaimPreparationResult result = prepare("rental-wales-invalid-combinations.xml", claim -> { });

        assertThat(result).isInstanceOf(Rejected.class);
        Rejected rejected = (Rejected) result;
        assertThat(rejected.reason()).isEqualTo(BulkFailReason.VALIDATION_FAILED);
        assertThat(rejected.errors()).extracting(error -> error.path()).contains(
            CLAIM + "/TenancyCommenceDate",
            CLAIM + "/Grounds/RentArrearsOrBreach",
            CLAIM + "/Grounds/Ground[2]/EstateManagementGround",
            CLAIM + "/Grounds/Ground[3]/Reason",
            CLAIM + "/Grounds/Ground[5]/Code",
            CLAIM + "/Grounds/Ground[6]/Code",
            CLAIM + "/Grounds",
            CLAIM + "/WalesNotice/TypeOfNoticeServed",
            CLAIM + "/WalesNotice/ServiceMethod",
            CLAIM + "/Wales/IsExemptLandlord",
            CLAIM + "/Wales/ProhibitedConduct/Details",
            CLAIM + "/Wales/ProhibitedConduct/AgreedTermsOfPeriodicContract",
            CLAIM + "/Wales/AntisocialBehaviour/Details",
            CLAIM + "/Wales/IllegalPurposesUse",
            CLAIM + "/Wales/OtherProhibitedConduct",
            CLAIM + "/Wales/EnergyPerformanceCertificate/NotProvidedReason",
            CLAIM + "/Wales/GasSafetyReport",
            CLAIM + "/Wales/ElectricalInstallationConditionReport");
    }

    @Test
    void shouldRequireWalesNoticeAndWalesBlock() {
        List<String> paths = errorPaths("rental-wales-standard-arrears.xml", claim -> {
            claim.getClaimParticulars().setWalesNotice(null);
            claim.getClaimParticulars().setWales(null);
        });

        assertThat(paths).containsExactlyInAnyOrder(CLAIM + "/WalesNotice", CLAIM + "/Wales");
    }

    @Test
    void shouldRejectWalesElementsForAPropertyInEngland() {
        when(eligibility.check(any(), any())).thenReturn(
            new PropertyEligibility(null, LegislativeCountry.ENGLAND, 20262, 1));

        List<String> paths = errorPaths("rental-england-assured-arrears-valid.xml", claim -> {
            WalesNoticeType notice = new WalesNoticeType();
            notice.setServed(true);
            notice.setTypeOfNoticeServed("RHW20");
            claim.getClaimParticulars().setWalesNotice(notice);
            claim.getClaimParticulars().setWales(new WalesType());
        });

        assertThat(paths).containsExactlyInAnyOrder(CLAIM + "/WalesNotice", CLAIM + "/Wales");
    }

    @Test
    void shouldRequireTheEnglandNoticeForAPropertyInEngland() {
        when(eligibility.check(any(), any())).thenReturn(
            new PropertyEligibility(null, LegislativeCountry.ENGLAND, 20262, 1));

        List<String> paths = errorPaths("rental-england-assured-arrears-valid.xml", claim ->
            claim.getClaimParticulars().setNotice(null));

        assertThat(paths).containsExactly(CLAIM + "/Notice");
    }
}
