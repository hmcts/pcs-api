package uk.gov.hmcts.reform.pcs.ccd.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantType;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.page.builder.SavingPageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.page.builder.SavingPageBuilderFactory;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.ContactPreferences;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.DefendantsDetails;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.NoticeDetails;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.ResumeClaim;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.TenancyLicenceDetails;
import uk.gov.hmcts.reform.pcs.ccd.service.ClaimService;
import uk.gov.hmcts.reform.pcs.ccd.service.PartyService;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.UnsubmittedCaseDataService;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringListElement;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mock.Strictness.LENIENT;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry.ENGLAND;
import static uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry.SCOTLAND;
import static uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry.WALES;

@ExtendWith(MockitoExtension.class)
class ResumePossessionClaimTest extends BaseEventTest {

    public static final UUID USER_ID = UUID.randomUUID();
    @Mock
    private PcsCaseService pcsCaseService;
    @Mock
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
    private UnsubmittedCaseDataService unsubmittedCaseDataService;
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

    @BeforeEach
    void setUp() {
        SavingPageBuilder savingPageBuilder = mock(SavingPageBuilder.class);
        when(savingPageBuilderFactory.create(any())).thenReturn(savingPageBuilder);
        when(savingPageBuilder.add(any())).thenReturn(savingPageBuilder);

        when(securityContextService.getCurrentUserDetails()).thenReturn(userDetails);
        when(userDetails.getUid()).thenReturn(USER_ID.toString());

        ResumePossessionClaim underTest = new ResumePossessionClaim(
            pcsCaseService, securityContextService,
            partyService, claimService,
            savingPageBuilderFactory, resumeClaim,
            unsubmittedCaseDataService, noticeDetails,
            tenancyLicenceDetails, contactPreferences,
            defendantsDetails
        );

        setEventUnderTest(underTest);
    }

    @Test
    void shouldThrowExceptionInStartCallbackIfPropertyAddressNotSet() {
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
    void shouldThrowExceptionInStartCallbackIfLegislativeCountryNotSet() {
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
    void shouldSetClaimantDetailsInStartCallback() {
        // Given
        String expectedUserEmail = "user@test.com";
        when(userDetails.getSub()).thenReturn(expectedUserEmail);

        AddressUK propertyAddress = AddressUK.builder()
            .addressLine1("10 High Street")
            .addressLine2("address line 2")
            .addressLine3("address line 3")
            .postTown("London")
            .postCode("W1 2BC")
            .country("United Kingdom")
            .build();

        PCSCase caseData = PCSCase.builder()
            .propertyAddress(propertyAddress)
            .legislativeCountry(WALES)
            .build();

        // When
        PCSCase updatedCaseData = callStartHandler(caseData);

        // Then
        assertThat(updatedCaseData.getClaimantName()).isEqualTo(expectedUserEmail);
        assertThat(updatedCaseData.getClaimantContactEmail()).isEqualTo(expectedUserEmail);
        assertThat(updatedCaseData.getFormattedClaimantContactAddress())
            .isEqualTo("10 High Street<br>London<br>W1 2BC");
    }

    @ParameterizedTest
    @MethodSource("claimantTypeScenarios")
    void shouldSetClaimantTypeListInStartCallback(LegislativeCountry legislativeCountry,
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

    @Test
    void shouldMergeCaseDataInSubmitCallback() {
        // Given
        PCSCase caseData = PCSCase.builder()
            .legislativeCountry(WALES)
            .build();

        PcsCaseEntity pcsCaseEntity = mock(PcsCaseEntity.class);
        when(pcsCaseService.loadCase(TEST_CASE_REFERENCE)).thenReturn(pcsCaseEntity);

        PartyEntity partyEntity = mock(PartyEntity.class);
        when(partyService.createPartyEntity(eq(USER_ID), any(), any(), any(), any(), any()))
            .thenReturn(partyEntity);

        when(claimService.createMainClaimEntity(eq(caseData), any())).thenReturn(ClaimEntity.builder().build());

        // When
        callSubmitHandler(caseData);

        // Then
        InOrder inOrder = inOrder(pcsCaseService);
        inOrder.verify(pcsCaseService).mergeCaseData(pcsCaseEntity, caseData);
        inOrder.verify(pcsCaseService).save(pcsCaseEntity);
    }

    @Test
    void shouldCreatePartyInSubmitCallback() {
        // Given
        AddressUK propertyAddress = mock(AddressUK.class);
        String claimantName = "Test Claimant";
        String claimantContactEmail = "claimant@test.com";
        String claimantContactPhoneNumber = "01234 567890";

        PcsCaseEntity pcsCaseEntity = mock(PcsCaseEntity.class);
        when(pcsCaseService.loadCase(TEST_CASE_REFERENCE)).thenReturn(pcsCaseEntity);

        PCSCase caseData = PCSCase.builder()
            .propertyAddress(propertyAddress)
            .legislativeCountry(WALES)
            .claimantName(claimantName)
            .claimantContactEmail(claimantContactEmail)
            .claimantContactPhoneNumber(claimantContactPhoneNumber)
            .build();

        // When
        callSubmitHandler(caseData);

        // Then
        verify(partyService).createPartyEntity(
            eq(USER_ID),
            eq(claimantName),
            eq(null),
            eq(claimantContactEmail),
            eq(propertyAddress),
            eq(claimantContactPhoneNumber)
        );
    }

    @Test
    void shouldCreateMainClaimInSubmitCallback() {
        // Given

        PcsCaseEntity pcsCaseEntity = mock(PcsCaseEntity.class);
        when(pcsCaseService.loadCase(TEST_CASE_REFERENCE)).thenReturn(pcsCaseEntity);

        PartyEntity partyEntity = mock(PartyEntity.class);
        when(partyService.createPartyEntity(eq(USER_ID), any(), any(), any(), any(), any()))
            .thenReturn(partyEntity);

        ClaimEntity claimEntity = mock(ClaimEntity.class);
        when(claimService.createMainClaimEntity(any(PCSCase.class), any(PartyEntity.class))).thenReturn(claimEntity);

        PCSCase caseData = mock(PCSCase.class);

        // When
        callSubmitHandler(caseData);

        // Then
        verify(claimService).createMainClaimEntity(caseData, partyEntity);
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
