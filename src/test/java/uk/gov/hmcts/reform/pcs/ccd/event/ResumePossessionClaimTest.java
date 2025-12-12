package uk.gov.hmcts.reform.pcs.ccd.event;

import com.github.kagkarlsson.scheduler.SchedulerClient;
import com.github.kagkarlsson.scheduler.task.SchedulableInstance;
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
import uk.gov.hmcts.reform.idam.client.models.UserInfo;
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantCircumstances;
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantInformation;
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantType;
import uk.gov.hmcts.reform.pcs.ccd.domain.CompletionNextStep;
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantContactPreferences;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PartyEntity;
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
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.RentArrearsGroundsForPossession;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.RentArrearsGroundsForPossessionReasons;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.ResumeClaim;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.SecureOrFlexibleGroundsForPossessionReasons;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.SelectClaimantType;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.StatementOfExpressTerms;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.SuspensionOfRightToBuyOrderReason;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.SuspensionToBuyDemotionOfTenancyOrderReasons;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.TenancyLicenceDetails;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.UnderlesseeOrMortgageeDetailsPage;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.UploadAdditionalDocumentsDetails;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.WalesCheckingNotice;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.wales.ASBQuestionsWales;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.wales.GroundsForPossessionWales;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.wales.OccupationLicenceDetailsWalesPage;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.wales.ProhibitedConductWales;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.wales.ReasonsForPossessionWales;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.wales.SecureContractGroundsForPossessionWalesPage;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseAssignmentService;
import uk.gov.hmcts.reform.pcs.ccd.service.ClaimService;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftCaseDataService;
import uk.gov.hmcts.reform.pcs.ccd.service.PartyService;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringListElement;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressFormatter;
import uk.gov.hmcts.reform.pcs.ccd.util.FeeFormatter;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeeDetails;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeeTypes;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mock.Strictness.LENIENT;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
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
    private TenancyLicenceDetails tenancyLicenceDetails;
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
    private GroundsForPossessionWales groundsForPossessionWales;
    @Mock
    private SecureContractGroundsForPossessionWalesPage secureContractGroundsForPossessionWales;
    @Mock
    private ReasonsForPossessionWales reasonsForPossessionWales;
    @Mock
    private AddressFormatter addressFormatter;
    @Mock
    private RentArrearsGroundsForPossession rentArrearsGroundsForPossession;
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
            rentArrearsGroundsForPossession, rentArrearsGroundForPossessionAdditionalGrounds,
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
            assertThat(updatedCaseData.getClaimantInformation().getOrganisationName()).isEqualTo(expectedClaimantEmail);
            assertThat(updatedCaseData.getContactPreferencesDetails().getClaimantContactEmail())
                .isEqualTo(expectedClaimantEmail);
            assertThat(updatedCaseData.getContactPreferencesDetails().getFormattedClaimantContactAddress())
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

        @Test
        void shouldMergeCaseData() {
            // Given
            PcsCaseEntity pcsCaseEntity = mock(PcsCaseEntity.class);
            when(pcsCaseService.loadCase(TEST_CASE_REFERENCE)).thenReturn(pcsCaseEntity);

            stubPartyCreation();
            stubFeeService();

            PCSCase caseData = PCSCase.builder()
                .legislativeCountry(WALES)
                .claimantCircumstances(mock(ClaimantCircumstances.class))
                .claimingCostsWanted(VerticalYesNo.YES)
                .completionNextStep(CompletionNextStep.SUBMIT_AND_PAY_NOW)
                .build();

            when(claimService.createMainClaimEntity(eq(caseData), any())).thenReturn(ClaimEntity.builder().build());

            // When
            callSubmitHandler(caseData);

            // Then
            InOrder inOrder = inOrder(pcsCaseService);
            inOrder.verify(pcsCaseService).mergeCaseData(pcsCaseEntity, caseData);
            inOrder.verify(pcsCaseService).save(pcsCaseEntity);
        }

        @Test
        void shouldCreateClaimantParty() {
            // Given
            AddressUK propertyAddress = mock(AddressUK.class);
            String claimantName = "Test Claimant";
            String claimantContactEmail = "claimant@test.com";
            String claimantContactPhoneNumber = "01234 567890";
            String claimantCircumstances = "Some circumstances";

            PcsCaseEntity pcsCaseEntity = mock(PcsCaseEntity.class);
            when(pcsCaseService.loadCase(TEST_CASE_REFERENCE)).thenReturn(pcsCaseEntity);

            stubFeeService();

            PCSCase caseData = PCSCase.builder()
                .propertyAddress(propertyAddress)
                .legislativeCountry(WALES)
                .claimantInformation(
                    ClaimantInformation.builder()
                        .claimantName(claimantName)
                        .build()
                )
                .contactPreferencesDetails(
                    ClaimantContactPreferences.builder()
                        .claimantContactEmail(claimantContactEmail)
                        .claimantContactPhoneNumber(claimantContactPhoneNumber)
                        .build()
                )
                .claimantCircumstances(ClaimantCircumstances.builder()
                                           .claimantCircumstancesDetails(claimantCircumstances)
                                           .build())
                .claimingCostsWanted(VerticalYesNo.YES)
                .completionNextStep(CompletionNextStep.SUBMIT_AND_PAY_NOW)
                .build();

            // When
            callSubmitHandler(caseData);

            // Then
            verify(partyService).createPartyEntity(
                USER_ID,
                claimantName,
                null,
                claimantName,
                claimantContactEmail,
                propertyAddress,
                claimantContactPhoneNumber
            );
        }

        @Test
        void shouldCreateMainClaim() {
            // Given
            PcsCaseEntity pcsCaseEntity = mock(PcsCaseEntity.class);
            when(pcsCaseService.loadCase(TEST_CASE_REFERENCE)).thenReturn(pcsCaseEntity);

            stubClaimCreation();
            stubFeeService();
            PartyEntity partyEntity = stubPartyCreation();

            PCSCase caseData = PCSCase.builder()
                .completionNextStep(CompletionNextStep.SUBMIT_AND_PAY_NOW)
                .build();

            // When
            callSubmitHandler(caseData);

            // Then
            verify(claimService).createMainClaimEntity(caseData, partyEntity);
        }

        @Test
        void shouldSchedulePaymentTask() {
            // Given
            PcsCaseEntity pcsCaseEntity = mock(PcsCaseEntity.class);
            when(pcsCaseService.loadCase(TEST_CASE_REFERENCE)).thenReturn(pcsCaseEntity);

            stubPartyCreation();
            stubClaimCreation();
            final FeeDetails feeDetails = stubFeeService();

            PCSCase caseData = PCSCase.builder()
                .claimantInformation(
                    ClaimantInformation.builder()
                        .organisationName("Org Ltd")
                        .build()
                )
                .completionNextStep(CompletionNextStep.SUBMIT_AND_PAY_NOW)
                .build();

            // When
            callSubmitHandler(caseData);

            // Then
            @SuppressWarnings("unchecked")
            ArgumentCaptor<SchedulableInstance<FeesAndPayTaskData>> schedulableInstanceCaptor
                = ArgumentCaptor.forClass(SchedulableInstance.class);

            verify(schedulerClient,atLeastOnce()).scheduleIfNotExists(schedulableInstanceCaptor.capture());

            FeesAndPayTaskData taskData = schedulableInstanceCaptor.getValue().getTaskInstance().getData();

            assertThat(taskData.getFeeType()).isEqualTo(FeeTypes.CASE_ISSUE_FEE.getCode());
            assertThat(taskData.getFeeDetails()).isEqualTo(feeDetails);
        }

        @Test
        void shouldScheduleAccessCodeTask() {
            // Given
            PcsCaseEntity pcsCaseEntity = mock(PcsCaseEntity.class);
            when(pcsCaseService.loadCase(TEST_CASE_REFERENCE)).thenReturn(pcsCaseEntity);
            stubPartyCreation();
            stubClaimCreation();
            stubFeeService();

            PCSCase caseData = PCSCase.builder()
                .completionNextStep(CompletionNextStep.SUBMIT_AND_PAY_NOW)
                .build();

            // When
            callSubmitHandler(caseData);

            // Then
            @SuppressWarnings("unchecked")
            ArgumentCaptor<SchedulableInstance<AccessCodeTaskData>> captor
                = ArgumentCaptor.forClass(SchedulableInstance.class);

            verify(schedulerClient, atLeastOnce()).scheduleIfNotExists(captor.capture());

            SchedulableInstance<AccessCodeTaskData> accessCodeTaskInstance = captor.getAllValues().stream()
                .filter(s -> s.getTaskInstance().getData() instanceof AccessCodeTaskData)
                .map(s -> (SchedulableInstance<AccessCodeTaskData>) s)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Access code task was not scheduled"));

            AccessCodeTaskData taskData = (AccessCodeTaskData) accessCodeTaskInstance.getTaskInstance().getData();

            assertThat(taskData.getCaseReference()).isEqualTo(String.valueOf(TEST_CASE_REFERENCE));
        }

        private void stubClaimCreation() {
            ClaimEntity claimEntity = mock(ClaimEntity.class);
            when(claimService.createMainClaimEntity(
                any(PCSCase.class),
                any(PartyEntity.class)
            )).thenReturn(claimEntity);
        }

        @Test
        void shouldReturnConfirmationScreen() {
            // Given
            PcsCaseEntity pcsCaseEntity = mock(PcsCaseEntity.class);
            when(pcsCaseService.loadCase(TEST_CASE_REFERENCE)).thenReturn(pcsCaseEntity);

            stubPartyCreation();
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

        @Test
        void shouldGetClaimantInformation() {

            PCSCase caseData = PCSCase.builder()
                .claimantInformation(
                    ClaimantInformation.builder()
                        .claimantName("TestName")
                        .organisationName("Org Ltd")
                        .build()
                )
                .build();

            // When
            callSubmitHandler(caseData);

            // Then
            assertThat(caseData.getClaimantInformation().getClaimantName())
                .isEqualTo("TestName");

            assertThat(caseData.getClaimantInformation().getOrganisationName())
                .isEqualTo("Org Ltd");
        }

        private FeeDetails stubFeeService() {
            FeeDetails feeDetails = FeeDetails.builder().feeAmount(CLAIM_FEE_AMOUNT).build();
            when(feeService.getFee(FeeTypes.CASE_ISSUE_FEE.getCode()))
                .thenReturn(feeDetails);

            return feeDetails;
        }

        private PartyEntity stubPartyCreation() {
            PartyEntity partyEntity = mock(PartyEntity.class);
            when(partyService.createPartyEntity(eq(USER_ID), any(), any(), any(), any(), any(), any()))
                .thenReturn(partyEntity);

            return partyEntity;
        }
    }

}
