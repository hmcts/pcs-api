package uk.gov.hmcts.reform.pcs.ccd.event;

import com.github.kagkarlsson.scheduler.SchedulerClient;
import com.github.kagkarlsson.scheduler.task.SchedulableInstance;
import com.github.kagkarlsson.scheduler.task.TaskDescriptor;
import com.github.kagkarlsson.scheduler.task.TaskInstance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantCircumstances;
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantContactPreferences;
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantInformation;
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantType;
import uk.gov.hmcts.reform.pcs.ccd.domain.CompletionNextStep;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.model.AccessCodeTaskData;
import uk.gov.hmcts.reform.pcs.ccd.page.builder.SavingPageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.page.builder.SavingPageBuilderFactory;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.AdditionalReasonsForPossession;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.CheckingNotice;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.ClaimantCircumstancesPage;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.ClaimantDetailsWalesPage;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.ContactPreferences;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.DefendantCircumstancesPage;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.DefendantsDetails;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.DemotionOfTenancyOrderReason;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.IntroductoryDemotedOrOtherGroundsForPossession;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.IntroductoryDemotedOtherGroundsReasons;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.MediationAndSettlement;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.NoRentArrearsGroundsForPossessionOptions;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.NoRentArrearsGroundsForPossessionReason;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.NoticeDetails;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.RentArrearsGroundForPossessionAdditionalGrounds;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.RentArrearsGroundsForPossessionPage;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.RentArrearsGroundsForPossessionReasons;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.ResumeClaim;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.SecureOrFlexibleGroundsForPossessionReasons;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.SelectClaimantType;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.StatementOfExpressTerms;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.SuspensionOfRightToBuyOrderReason;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.SuspensionToBuyDemotionOfTenancyOrderReasons;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.TenancyLicenceDetailsPage;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.UnderlesseeOrMortgageeDetailsPage;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.UploadAdditionalDocumentsDetails;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.WalesCheckingNotice;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.wales.ASBQuestionsWales;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.wales.GroundsForPossessionWalesPage;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.wales.OccupationLicenceDetailsWalesPage;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.wales.ProhibitedConductWales;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.wales.ReasonsForPossessionWales;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.wales.SecureContractGroundsForPossessionWalesPage;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseAssignmentService;
import uk.gov.hmcts.reform.pcs.ccd.service.ClaimService;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftCaseDataService;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringListElement;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressFormatter;
import uk.gov.hmcts.reform.pcs.ccd.util.FeeFormatter;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeeDetails;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeeType;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeesAndPayTaskData;
import uk.gov.hmcts.reform.pcs.feesandpay.service.FeeService;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;
import uk.gov.hmcts.reform.pcs.reference.service.OrganisationService;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mock.Strictness.LENIENT;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.domain.CompletionNextStep.SUBMIT_AND_PAY_NOW;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.resumePossessionClaim;
import static uk.gov.hmcts.reform.pcs.ccd.task.AccessCodeGenerationComponent.ACCESS_CODE_TASK_DESCRIPTOR;
import static uk.gov.hmcts.reform.pcs.feesandpay.task.FeesAndPayTaskComponent.FEE_CASE_ISSUED_TASK_DESCRIPTOR;
import static uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry.ENGLAND;
import static uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry.SCOTLAND;
import static uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry.WALES;

@ExtendWith(MockitoExtension.class)
class ResumePossessionClaimTest extends BaseEventTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final BigDecimal CLAIM_FEE_AMOUNT = new BigDecimal("123.40");

    @Mock
    private PcsCaseService pcsCaseService;
    @Mock(strictness = LENIENT)
    private SecurityContextService securityContextService;
    @Mock
    private PartyService partyService;
    @Mock
    private ClaimService claimService;
    @Mock
    private SavingPageBuilderFactory savingPageBuilderFactory;
    @Mock
    private ResumeClaim resumeClaim;
    @Mock
    private SelectClaimantType selectClaimantType;
    @Mock
    private ContactPreferences contactPreferences;
    @Mock
    private DefendantsDetails defendantsDetails;
    @Mock
    private NoticeDetails noticeDetails;
    @Mock(strictness = LENIENT)
    private UserInfo userDetails;
    @Mock
    private TenancyLicenceDetailsPage tenancyLicenceDetails;
    @Mock
    private UploadAdditionalDocumentsDetails uploadAdditionalDocumentsDetails;
    @Mock
    private NoRentArrearsGroundsForPossessionReason noRentArrearsGroundsForPossessionReason;
    @Mock
    private AdditionalReasonsForPossession additionalReasonsForPossession;
    @Mock
    private SecureOrFlexibleGroundsForPossessionReasons secureOrFlexibleGroundsForPossessionReasons;
    @Mock
    private MediationAndSettlement mediationAndSettlement;
    @Mock
    private ClaimantCircumstancesPage claimantCircumstancesPage;
    @Mock
    private IntroductoryDemotedOtherGroundsReasons introductoryDemotedOtherGroundsReasons;
    @Mock
    private IntroductoryDemotedOrOtherGroundsForPossession introductoryDemotedOrOtherGroundsForPossession;
    @Mock
    private RentArrearsGroundsForPossessionReasons rentArrearsGroundsForPossessionReasons;
    @Mock
    private SuspensionToBuyDemotionOfTenancyOrderReasons suspensionToBuyDemotionOfTenancyOrderReasons;
    @Mock
    private DefendantCircumstancesPage defendantCircumstancesPage;
    @Mock
    private SuspensionOfRightToBuyOrderReason suspensionOfRightToBuyOrderReason;
    @Mock
    private StatementOfExpressTerms statementOfExpressTerms;
    @Mock
    private DemotionOfTenancyOrderReason demotionOfTenancyOrderReason;
    @Mock
    private OrganisationService organisationService;
    @Mock
    private ClaimantDetailsWalesPage claimantDetailsWalesPage;
    @Mock
    private UnderlesseeOrMortgageeDetailsPage underlesseeOrMortgageePage;
    @Mock
    private ProhibitedConductWales prohibitedConductWalesPage;
    @Mock
    private SchedulerClient schedulerClient;
    @Mock
    private DraftCaseDataService draftCaseDataService;
    @Mock
    private OccupationLicenceDetailsWalesPage occupationLicenceDetailsWalesPage;
    @Mock
    private GroundsForPossessionWalesPage groundsForPossessionWales;
    @Mock
    private SecureContractGroundsForPossessionWalesPage secureContractGroundsForPossessionWales;
    @Mock
    private ReasonsForPossessionWales reasonsForPossessionWales;
    @Mock
    private AddressFormatter addressFormatter;
    @Mock
    private RentArrearsGroundsForPossessionPage rentArrearsGroundsForPossessionPage;
    @Mock
    private RentArrearsGroundForPossessionAdditionalGrounds rentArrearsGroundForPossessionAdditionalGrounds;
    @Mock
    private NoRentArrearsGroundsForPossessionOptions noRentArrearsGroundsForPossessionOptions;
    @Mock
    private CheckingNotice checkingNotice;
    @Mock
    private WalesCheckingNotice walesCheckingNotice;
    @Mock
    private ASBQuestionsWales asbQuestionsWales;
    @Mock
    private FeeService feeService;
    @Mock
    private FeeFormatter feeFormatter;
    @Mock
    private CaseAssignmentService caseAssignmentService;

    @BeforeEach
    void setUp() {
        SavingPageBuilder savingPageBuilder = mock(SavingPageBuilder.class);
        when(savingPageBuilderFactory.create(any(), any(EventId.class))).thenReturn(savingPageBuilder);
        when(savingPageBuilder.add(any())).thenReturn(savingPageBuilder);

        when(securityContextService.getCurrentUserDetails()).thenReturn(userDetails);
        when(userDetails.getUid()).thenReturn(USER_ID.toString());

        ResumePossessionClaim underTest = new ResumePossessionClaim(
            pcsCaseService, securityContextService,
            partyService, claimService,
            savingPageBuilderFactory, resumeClaim,
            selectClaimantType, noticeDetails,
            uploadAdditionalDocumentsDetails, tenancyLicenceDetails, contactPreferences,
            defendantsDetails, noRentArrearsGroundsForPossessionReason, additionalReasonsForPossession,
            secureOrFlexibleGroundsForPossessionReasons, mediationAndSettlement, claimantCircumstancesPage,
            introductoryDemotedOtherGroundsReasons, introductoryDemotedOrOtherGroundsForPossession,
            rentArrearsGroundsForPossessionReasons, suspensionToBuyDemotionOfTenancyOrderReasons,
            defendantCircumstancesPage, suspensionOfRightToBuyOrderReason, statementOfExpressTerms,
            demotionOfTenancyOrderReason, organisationService, claimantDetailsWalesPage, prohibitedConductWalesPage,
            schedulerClient, draftCaseDataService, occupationLicenceDetailsWalesPage, groundsForPossessionWales,
            secureContractGroundsForPossessionWales, reasonsForPossessionWales, addressFormatter,
            rentArrearsGroundsForPossessionPage, rentArrearsGroundForPossessionAdditionalGrounds,
            noRentArrearsGroundsForPossessionOptions, checkingNotice, walesCheckingNotice, asbQuestionsWales,
            underlesseeOrMortgageePage, feeService, feeFormatter, caseAssignmentService
        );

        setEventUnderTest(underTest);
    }

    @Nested
    @DisplayName("Start callback tests")
    class StartCallbackTests {

        @Test
        void shouldThrowExceptionIfPropertyAddressNotSet() {
            // Given
            PCSCase caseData = PCSCase.builder()
                .legislativeCountry(WALES)
                .build();

            // When
            Throwable throwable = catchThrowable(() -> callStartHandler(caseData));

            // Then
            assertThat(throwable)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Cannot resume claim without property address already set");
        }

        @Test
        void shouldThrowExceptionIfLegislativeCountryNotSet() {
            // Given
            PCSCase caseData = PCSCase.builder()
                .propertyAddress(mock(AddressUK.class))
                .build();

            // When
            Throwable throwable = catchThrowable(() -> callStartHandler(caseData));

            // Then
            assertThat(throwable)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Cannot resume claim without legislative country already set");
        }

        @Test
        void shouldSetOrganisationNameFromOrganisationServiceWhenAvailable() {
            // Given
            String userEmail = "user@test.com";
            String orgName = "ACME Org Ltd";

            when(userDetails.getSub()).thenReturn(userEmail);
            when(organisationService.getOrganisationNameForCurrentUser()).thenReturn(orgName);

            AddressUK organisationAddress = mock(AddressUK.class);
            when(organisationService.getOrganisationAddressForCurrentUser()).thenReturn(organisationAddress);
            when(addressFormatter.formatMediumAddress(organisationAddress, AddressFormatter.BR_DELIMITER))
                .thenReturn("formatted org address");

            PCSCase caseData = PCSCase.builder()
                .propertyAddress(mock(AddressUK.class))
                .legislativeCountry(WALES)
                .build();

            // When
            PCSCase updatedCaseData = callStartHandler(caseData);

            // Then
            assertThat(updatedCaseData.getClaimantInformation().getOrganisationName()).isEqualTo(orgName);
        }

        @Test
        void shouldCreateContactPreferencesWhenNull() {
            // Given
            String userEmail = "user@test.com";
            when(userDetails.getSub()).thenReturn(userEmail);
            when(organisationService.getOrganisationNameForCurrentUser()).thenReturn(null);

            AddressUK organisationAddress = mock(AddressUK.class);
            when(organisationService.getOrganisationAddressForCurrentUser()).thenReturn(organisationAddress);
            when(addressFormatter.formatMediumAddress(organisationAddress, AddressFormatter.BR_DELIMITER))
                .thenReturn("formatted org address");

            PCSCase caseData = PCSCase.builder()
                .propertyAddress(mock(AddressUK.class))
                .legislativeCountry(WALES)
                .claimantContactPreferences(null)
                .build();

            // When
            PCSCase updatedCaseData = callStartHandler(caseData);

            // Then
            assertThat(updatedCaseData.getClaimantContactPreferences()).isNotNull();
            assertThat(updatedCaseData.getClaimantContactPreferences().getClaimantContactEmail()).isEqualTo(userEmail);
        }

        @Test
        void shouldSetClaimantDetails() {
            // Given
            String expectedClaimantEmail = "user@test.com";
            String expectedClaimantAddress = "formatted claimant address";

            AddressUK claimantAddress = mock(AddressUK.class);
            when(userDetails.getSub()).thenReturn(expectedClaimantEmail);
            when(organisationService.getOrganisationNameForCurrentUser()).thenReturn(null);
            when(organisationService.getOrganisationAddressForCurrentUser()).thenReturn(claimantAddress);
            when(addressFormatter.formatMediumAddress(claimantAddress, AddressFormatter.BR_DELIMITER))
                .thenReturn(expectedClaimantAddress);

            PCSCase caseData = PCSCase.builder()
                .propertyAddress(mock(AddressUK.class))
                .legislativeCountry(WALES)
                .build();

            // When
            PCSCase updatedCaseData = callStartHandler(caseData);

            // Then
            assertThat(updatedCaseData.getClaimantInformation().getClaimantName())
                .isEqualTo(expectedClaimantEmail); // HDPI-3582 will fix this
            assertThat(updatedCaseData.getClaimantContactPreferences().getClaimantContactEmail())
                .isEqualTo(expectedClaimantEmail);
            assertThat(updatedCaseData.getClaimantContactPreferences().getFormattedClaimantContactAddress())
                .isEqualTo("formatted claimant address");
        }

        @ParameterizedTest
        @MethodSource("claimantTypeScenarios")
        void shouldSetClaimantTypeList(LegislativeCountry legislativeCountry,
                                       List<ClaimantType> expectedClaimantTypes) {
            // Given
            PCSCase caseData = PCSCase.builder()
                .propertyAddress(mock(AddressUK.class))
                .legislativeCountry(legislativeCountry)
                .build();

            // When
            PCSCase updatedCaseData = callStartHandler(caseData);

            // Then
            List<String> actualClaimantTypeCodes = updatedCaseData.getClaimantType().getListItems().stream()
                .map(DynamicStringListElement::getCode)
                .toList();

            List<String> expectedClaimantTypeCodes = expectedClaimantTypes.stream()
                .map(ClaimantType::name)
                .toList();

            assertThat(actualClaimantTypeCodes).isEqualTo(expectedClaimantTypeCodes);
        }

        private static Stream<Arguments> claimantTypeScenarios() {
            return Stream.of(
                arguments(
                    ENGLAND, List.of(
                        ClaimantType.PRIVATE_LANDLORD,
                        ClaimantType.PROVIDER_OF_SOCIAL_HOUSING,
                        ClaimantType.MORTGAGE_LENDER,
                        ClaimantType.OTHER
                    )
                ),

                arguments(
                    WALES, List.of(
                        ClaimantType.PRIVATE_LANDLORD,
                        ClaimantType.COMMUNITY_LANDLORD,
                        ClaimantType.MORTGAGE_LENDER,
                        ClaimantType.OTHER
                    )
                ),

                arguments(SCOTLAND, List.of())
            );
        }

        @ParameterizedTest
        @MethodSource("unsubmittedDataFlagScenarios")
        void shouldSetFlagForUnsubmittedData(boolean hasUnsubmittedData, YesOrNo expectedCaseDataFlag) {
            // Given
            when(draftCaseDataService.hasUnsubmittedCaseData(TEST_CASE_REFERENCE, resumePossessionClaim))
                .thenReturn(hasUnsubmittedData);

            PCSCase caseData = PCSCase.builder()
                .propertyAddress(mock(AddressUK.class))
                .legislativeCountry(ENGLAND)
                .build();

            // When
            PCSCase updatedCaseData = callStartHandler(caseData);

            // Then
            assertThat(updatedCaseData.getHasUnsubmittedCaseData()).isEqualTo(expectedCaseDataFlag);
            verify(draftCaseDataService).hasUnsubmittedCaseData(TEST_CASE_REFERENCE, resumePossessionClaim);
        }

        private static Stream<Arguments> unsubmittedDataFlagScenarios() {
            return Stream.of(
                // unsubmitted case data available, expected case data flag
                arguments(false, YesOrNo.NO),
                arguments(true, YesOrNo.YES)
            );
        }

    }


    @Nested
    @DisplayName("Save for later submit callback tests")
    class SaveForLaterTests {

        @Test
        void shouldReturnConfirmationScreen() {
            // Given
            PCSCase caseData = PCSCase.builder()
                .completionNextStep(CompletionNextStep.SAVE_IT_FOR_LATER)
                .build();

            // When
            SubmitResponse<State> submitResponse = callSubmitHandler(caseData);

            // Then
            assertThat(submitResponse.getConfirmationBody()).contains("A draft of your claim has been saved");
        }

        @Test
        void shouldNotClearDraftData() {
            // Given
            PCSCase caseData = PCSCase.builder()
                .completionNextStep(CompletionNextStep.SAVE_IT_FOR_LATER)
                .build();

            // When
            callSubmitHandler(caseData);

            // Then
            verify(draftCaseDataService, never()).deleteUnsubmittedCaseData(anyLong(), any());
        }
    }

    @Nested
    @DisplayName("Submit and pay callback tests")
    class SubmitAndPayTests {

        private PcsCaseEntity pcsCaseEntity;

        @BeforeEach
        void setUp() {
            pcsCaseEntity = mock(PcsCaseEntity.class);
            when(pcsCaseService.loadCase(TEST_CASE_REFERENCE)).thenReturn(pcsCaseEntity);
        }

        @Test
        void shouldMergeCaseData() {
            // Given
            stubFeeService();

            PCSCase caseData = PCSCase.builder()
                .legislativeCountry(WALES)
                .claimantCircumstances(mock(ClaimantCircumstances.class))
                .claimingCostsWanted(VerticalYesNo.YES)
                .completionNextStep(SUBMIT_AND_PAY_NOW)
                .build();

            // When
            callSubmitHandler(caseData);

            // Then
            InOrder inOrder = inOrder(pcsCaseService);
            inOrder.verify(pcsCaseService).mergeCaseData(pcsCaseEntity, caseData);
        }

        @Test
        void shouldCreatePartiesInSubmitCallback() {
            // Given
            AddressUK propertyAddress = mock(AddressUK.class);
            AddressUK organisationAddress = mock(AddressUK.class);

            String claimantName = "Test Claimant";
            String claimantContactEmail = "claimant@test.com";
            String claimantContactPhoneNumber = "01234 567890";
            String claimantCircumstances = "Some circumstances";

            stubFeeService();

            PCSCase caseData = PCSCase.builder()
                .propertyAddress(propertyAddress)
                .legislativeCountry(WALES)
                .completionNextStep(SUBMIT_AND_PAY_NOW)
                .claimantInformation(
                    ClaimantInformation.builder()
                        .claimantName(claimantName)
                        .build()
                )
                .claimantContactPreferences(
                    ClaimantContactPreferences.builder()
                        .claimantContactEmail(claimantContactEmail)
                        .claimantContactPhoneNumber(claimantContactPhoneNumber)
                        .organisationAddress(organisationAddress)
                        .build()
                )
                .claimantCircumstances(ClaimantCircumstances.builder()
                                           .claimantCircumstancesDetails(claimantCircumstances)
                                           .build())
                .claimingCostsWanted(VerticalYesNo.YES)
                .build();

            ClaimEntity claimEntity = stubClaimCreation();

            // When
            callSubmitHandler(caseData);

            // Then
            verify(partyService).createAllParties(caseData, pcsCaseEntity, claimEntity);
        }

        @Test
        void shouldUseOverriddenClaimantNameWhenProvided() {
            // Given
            stubFeeService();

            AddressUK organisationAddress = mock(AddressUK.class);

            PCSCase caseData = PCSCase.builder()
                .legislativeCountry(WALES)
                .claimantInformation(
                    ClaimantInformation.builder()
                        .claimantName("Original Name")
                        .overriddenClaimantName("Overridden Name")
                        .build()
                )
                .claimantContactPreferences(
                    ClaimantContactPreferences.builder()
                        .claimantContactEmail("claimant@test.com")
                        .claimantContactPhoneNumber("01234 567890")
                        .organisationAddress(organisationAddress)
                        .build()
                )
                .completionNextStep(CompletionNextStep.SUBMIT_AND_PAY_NOW)
                .build();

            // When
            callSubmitHandler(caseData);

            // Then
            verify(partyService).createPartyEntity(
                USER_ID,
                "Overridden Name",
                null,
                "Overridden Name",
                "claimant@test.com",
                organisationAddress,
                "01234 567890"
            );
        }

        @Test
        void shouldUseOverriddenClaimantContactEmailWhenProvided() {
            // Given
            stubFeeService();

            AddressUK organisationAddress = mock(AddressUK.class);

            PCSCase caseData = PCSCase.builder()
                .legislativeCountry(WALES)
                .claimantInformation(
                    ClaimantInformation.builder()
                        .claimantName("Test Claimant")
                        .build()
                )
                .claimantContactPreferences(
                    ClaimantContactPreferences.builder()
                        .claimantContactEmail("original@test.com")
                        .overriddenClaimantContactEmail("override@test.com")
                        .claimantContactPhoneNumber("01234 567890")
                        .organisationAddress(organisationAddress)
                        .build()
                )
                .completionNextStep(CompletionNextStep.SUBMIT_AND_PAY_NOW)
                .build();

            // When
            callSubmitHandler(caseData);

            // Then
            verify(partyService).createPartyEntity(
                USER_ID,
                "Test Claimant",
                null,
                "Test Claimant",
                "override@test.com",
                organisationAddress,
                "01234 567890"
            );
        }

        @Test
        void shouldUseOverriddenContactAddressWhenProvided() {
            // Given
            stubFeeService();

            AddressUK overriddenAddress = mock(AddressUK.class);
            AddressUK organisationAddress = mock(AddressUK.class);

            PCSCase caseData = PCSCase.builder()
                .legislativeCountry(WALES)
                .claimantInformation(
                    ClaimantInformation.builder()
                        .claimantName("Test Claimant")
                        .build()
                )
                .claimantContactPreferences(
                    ClaimantContactPreferences.builder()
                        .claimantContactEmail("claimant@test.com")
                        .claimantContactPhoneNumber("01234 567890")
                        .overriddenClaimantContactAddress(overriddenAddress)
                        .organisationAddress(organisationAddress)
                        .build()
                )
                .completionNextStep(CompletionNextStep.SUBMIT_AND_PAY_NOW)
                .build();

            // When
            callSubmitHandler(caseData);

            // Then
            verify(partyService).createPartyEntity(
                USER_ID,
                "Test Claimant",
                null,
                "Test Claimant",
                "claimant@test.com",
                overriddenAddress,
                "01234 567890"
            );
        }

        @Test
        void shouldUseOrganisationAddressWhenOverriddenAndMissingAreNull() {
            // Given
            AddressUK propertyAddress = mock(AddressUK.class);
            AddressUK organisationAddress = mock(AddressUK.class);

            stubFeeService();

            PCSCase caseData = PCSCase.builder()
                .propertyAddress(propertyAddress)
                .legislativeCountry(WALES)
                .claimantInformation(
                    ClaimantInformation.builder()
                        .claimantName("Test Claimant")
                        .build()
                )
                .claimantContactPreferences(
                    ClaimantContactPreferences.builder()
                        .claimantContactEmail("claimant@test.com")
                        .claimantContactPhoneNumber("01234 567890")
                        .organisationAddress(organisationAddress)
                        .build()
                )
                .completionNextStep(CompletionNextStep.SUBMIT_AND_PAY_NOW)
                .build();

            // When
            callSubmitHandler(caseData);

            // Then
            verify(partyService).createPartyEntity(
                USER_ID,
                "Test Claimant",
                null,
                "Test Claimant",
                "claimant@test.com",
                organisationAddress,
                "01234 567890"
            );
        }

        @Test
        void shouldCreateMainClaim() {
            // Given
            stubFeeService();

            PCSCase caseData = PCSCase.builder()
                .completionNextStep(SUBMIT_AND_PAY_NOW)
                .build();

            // When
            callSubmitHandler(caseData);

            // Then
            verify(claimService).createMainClaimEntity(caseData);
        }

        @Test
        void shouldSchedulePaymentTask() {
            // Given
            final FeeDetails feeDetails = stubFeeService();

            PCSCase caseData = PCSCase.builder()
                .completionNextStep(SUBMIT_AND_PAY_NOW)
                .build();

            // When
            callSubmitHandler(caseData);

            // Then
            FeesAndPayTaskData taskData = getScheduledTaskData(FEE_CASE_ISSUED_TASK_DESCRIPTOR);
            assertThat(taskData.getFeeType()).isEqualTo(FeeType.CASE_ISSUE_FEE.getCode());
            assertThat(taskData.getFeeDetails()).isEqualTo(feeDetails);
        }

        @Test
        void shouldScheduleAccessCodeTask() {
            // Given
            stubClaimCreation();
            stubFeeService();

            PCSCase caseData = PCSCase.builder()
                .completionNextStep(SUBMIT_AND_PAY_NOW)
                .build();

            // When
            callSubmitHandler(caseData);

            // Then
            AccessCodeTaskData taskData = getScheduledTaskData(ACCESS_CODE_TASK_DESCRIPTOR);
            assertThat(taskData.getCaseReference()).isEqualTo(String.valueOf(TEST_CASE_REFERENCE));
        }

        private ClaimEntity stubClaimCreation() {
            ClaimEntity claimEntity = mock(ClaimEntity.class);
            when(claimService.createMainClaimEntity(any(PCSCase.class))).thenReturn(claimEntity);
            return claimEntity;
        }

        @Test
        void shouldReturnConfirmationScreen() {
            // Given
            stubClaimCreation();
            stubFeeService();

            String formattedFee = "some formatted fee";
            when(feeFormatter.formatFee(CLAIM_FEE_AMOUNT)).thenReturn(formattedFee);

            PCSCase caseData = PCSCase.builder()
                .completionNextStep(CompletionNextStep.SUBMIT_AND_PAY_NOW)
                .build();

            // When
            SubmitResponse<State> submitResponse = callSubmitHandler(caseData);

            // Then
            assertThat(submitResponse.getConfirmationBody()).contains("You must pay the claim fee of " + formattedFee);
        }

        private FeeDetails stubFeeService() {
            FeeDetails feeDetails = FeeDetails.builder().feeAmount(CLAIM_FEE_AMOUNT).build();
            when(feeService.getFee(FeeType.CASE_ISSUE_FEE))
                .thenReturn(feeDetails);

            return feeDetails;
        }

        @SuppressWarnings("unchecked")
        private <T> T getScheduledTaskData(TaskDescriptor<T> taskDescriptor) {
            ArgumentCaptor<SchedulableInstance<?>> captor = ArgumentCaptor.forClass(SchedulableInstance.class);

            verify(schedulerClient, atLeastOnce()).scheduleIfNotExists(captor.capture());
            List<SchedulableInstance<?>> allTasks = captor.getAllValues();

            String taskName = taskDescriptor.getTaskName();
            Class<T> taskDataClass = taskDescriptor.getDataClass();

            List<T> taskDataList = allTasks.stream()
                .filter(t -> t.getTaskInstance().getTaskName().equals(taskName))
                .map(SchedulableInstance::getTaskInstance)
                .map(TaskInstance::getData)
                .map(taskDataClass::cast)
                .toList();

            if (taskDataList.isEmpty()) {
                throw new AssertionError("No scheduled task found with name " + taskName);
            } else if (taskDataList.size() > 1) {
                throw new AssertionError("Multiple scheduled tasks found with name " + taskName);
            }

            return taskDataList.getFirst();
        }


    }

}
