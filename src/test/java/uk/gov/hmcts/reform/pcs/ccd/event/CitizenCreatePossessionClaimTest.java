package uk.gov.hmcts.reform.pcs.ccd.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantType;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenClaimEligibility;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenPossessionClaim;
import uk.gov.hmcts.reform.pcs.ccd.service.citizenclaim.CitizenPossessionClaimCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.citizenclaim.CitizenPossessionClaimValidator;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;
import uk.gov.hmcts.reform.pcs.service.FeatureFlag;
import uk.gov.hmcts.reform.pcs.service.FeatureToggleService;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.event.CitizenCreatePossessionClaim.SERVICE_NOT_AVAILABLE;

@ExtendWith(MockitoExtension.class)
class CitizenCreatePossessionClaimTest extends BaseEventTest {

    private static final AddressUK PROPERTY_ADDRESS = AddressUK.builder()
        .addressLine1("1 Test Street").postTown("London").postCode("SW1A 1AA").build();

    @Mock
    private FeatureToggleService featureToggleService;
    @Mock
    private CitizenPossessionClaimCaseService caseService;

    @BeforeEach
    void setUp() {
        setEventUnderTest(new CitizenCreatePossessionClaim(
            featureToggleService, caseService, new CitizenPossessionClaimValidator()));
    }

    @Test
    void shouldBeAHiddenCitizenEventWithAShortName() {
        assertConfiguredAsNeverShow();
        assertGrants(UserRole.CITIZEN, Permission.CRU);
        assertThat(configuredEvent.getName()).hasSizeLessThanOrEqualTo(30);
    }

    @Test
    void shouldRefuseToStartWhenFlagIsOff() {
        when(featureToggleService.isEnabled(FeatureFlag.PRIVATE_LANDLORD_MAKE_A_CLAIM)).thenReturn(false);

        AboutToStartOrSubmitResponse<PCSCase, State> response = callAboutToStart(PCSCase.builder().build());

        assertThat(response.getErrors()).containsExactly(SERVICE_NOT_AVAILABLE);
    }

    @Test
    void shouldStartWhenFlagIsOn() {
        when(featureToggleService.isEnabled(FeatureFlag.PRIVATE_LANDLORD_MAKE_A_CLAIM)).thenReturn(true);
        PCSCase caseData = PCSCase.builder().build();

        AboutToStartOrSubmitResponse<PCSCase, State> response = callAboutToStart(caseData);

        assertThat(response.getErrors()).isNullOrEmpty();
        assertThat(response.getData()).isSameAs(caseData);
    }

    @Test
    void shouldRefuseToSubmitWhenFlagIsOff() {
        when(featureToggleService.isEnabled(FeatureFlag.PRIVATE_LANDLORD_MAKE_A_CLAIM)).thenReturn(false);

        SubmitResponse<State> response = callSubmitHandler(caseData(ClaimantType.PRIVATE_LANDLORD));

        assertThat(response.getErrors()).containsExactly(SERVICE_NOT_AVAILABLE);
        verifyNoInteractions(caseService);
    }

    @Test
    void shouldRefuseClaimantTypesOtherThanPrivateLandlord() {
        when(featureToggleService.isEnabled(FeatureFlag.PRIVATE_LANDLORD_MAKE_A_CLAIM)).thenReturn(true);

        SubmitResponse<State> response = callSubmitHandler(caseData(ClaimantType.PROVIDER_OF_SOCIAL_HOUSING));

        assertThat(response.getErrors()).containsExactly(CitizenPossessionClaimValidator.CLAIMANT_TYPE_NOT_ELIGIBLE);
        verify(caseService, never()).createCase(anyLong(), any(), any());
    }

    @Test
    void shouldRefusePropertiesOutsideEngland() {
        when(featureToggleService.isEnabled(FeatureFlag.PRIVATE_LANDLORD_MAKE_A_CLAIM)).thenReturn(true);
        when(caseService.checkPropertyInEngland(PROPERTY_ADDRESS, null))
            .thenReturn(Optional.of(CitizenPossessionClaimCaseService.PROPERTY_NOT_IN_ENGLAND));

        SubmitResponse<State> response = callSubmitHandler(caseData(ClaimantType.PRIVATE_LANDLORD));

        assertThat(response.getErrors()).containsExactly("PROPERTY_NOT_IN_ENGLAND");
        verify(caseService, never()).createCase(anyLong(), any(), any());
    }

    @Test
    void shouldRefuseAPropertyAddressLongerThanItsColumns() {
        when(featureToggleService.isEnabled(FeatureFlag.PRIVATE_LANDLORD_MAKE_A_CLAIM)).thenReturn(true);
        PCSCase caseData = caseData(ClaimantType.PRIVATE_LANDLORD);
        caseData.getCitizenPossessionClaim().setPropertyAddress(
            AddressUK.builder().addressLine1("x".repeat(101)).postTown("London").postCode("SW1A 1AA").build());

        SubmitResponse<State> response = callSubmitHandler(caseData);

        assertThat(response.getErrors()).containsExactly("TOO_LONG:propertyAddress.AddressLine1");
        verify(caseService, never()).createCase(anyLong(), any(), any());
    }

    @Test
    void shouldNeedThePropertyAddressFirstLineAndPostcode() {
        when(featureToggleService.isEnabled(FeatureFlag.PRIVATE_LANDLORD_MAKE_A_CLAIM)).thenReturn(true);
        PCSCase caseData = caseData(ClaimantType.PRIVATE_LANDLORD);
        caseData.getCitizenPossessionClaim().setPropertyAddress(AddressUK.builder().postTown("London").build());

        SubmitResponse<State> response = callSubmitHandler(caseData);

        assertThat(response.getErrors())
            .containsExactly("REQUIRED:propertyAddress.AddressLine1", "REQUIRED:propertyAddress.PostCode");
        verify(caseService, never()).createCase(anyLong(), any(), any());
    }

    @Test
    void shouldPassTheChosenCountryForCrossBorderPostcodes() {
        when(featureToggleService.isEnabled(FeatureFlag.PRIVATE_LANDLORD_MAKE_A_CLAIM)).thenReturn(true);
        PCSCase caseData = caseData(ClaimantType.PRIVATE_LANDLORD);
        caseData.getCitizenPossessionClaim().getEligibility().setLegislativeCountry(LegislativeCountry.ENGLAND);
        when(caseService.checkPropertyInEngland(PROPERTY_ADDRESS, LegislativeCountry.ENGLAND))
            .thenReturn(Optional.empty());

        SubmitResponse<State> response = callSubmitHandler(caseData);

        assertThat(response.getErrors()).isNullOrEmpty();
    }

    @Test
    void shouldCreateTheCaseForAnEligibleClaim() {
        when(featureToggleService.isEnabled(FeatureFlag.PRIVATE_LANDLORD_MAKE_A_CLAIM)).thenReturn(true);
        when(caseService.checkPropertyInEngland(PROPERTY_ADDRESS, null)).thenReturn(Optional.empty());
        PCSCase caseData = caseData(ClaimantType.PRIVATE_LANDLORD);

        SubmitResponse<State> response = callSubmitHandler(caseData);

        assertThat(response.getErrors()).isNullOrEmpty();
        verify(caseService).createCase(TEST_CASE_REFERENCE, PROPERTY_ADDRESS,
                                       caseData.getCitizenPossessionClaim().getEligibility());
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> callAboutToStart(PCSCase caseData) {
        return configuredEvent.getAboutToStartCallback().handle(
            CaseDetails.<PCSCase, State>builder().data(caseData).build());
    }

    private static PCSCase caseData(ClaimantType claimantType) {
        return PCSCase.builder()
            .citizenPossessionClaim(CitizenPossessionClaim.builder()
                                        .propertyAddress(PROPERTY_ADDRESS)
                                        .eligibility(CitizenClaimEligibility.builder()
                                                         .claimantType(claimantType)
                                                         .build())
                                        .build())
            .build();
    }

}
