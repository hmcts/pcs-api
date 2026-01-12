package uk.gov.hmcts.reform.pcs.ccd.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantType;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.AddressEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringList;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringListElement;
import uk.gov.hmcts.reform.pcs.config.MapperConfig;
import uk.gov.hmcts.reform.pcs.exception.CaseNotFoundException;
import uk.gov.hmcts.reform.pcs.factory.ClaimantPartyFactory;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;

import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PcsCaseServiceTest {

    private static final long CASE_REFERENCE = 1234L;

    private ModelMapper modelMapper;
    @Mock
    private PcsCaseRepository pcsCaseRepository;
    @Mock
    private PcsCaseMergeService pcsCaseMergeService;
    @Mock
    private TenancyLicenceService tenancyLicenceService;
    @Mock
    private ClaimantPartyFactory claimantPartyFactory;
    @Mock
    private ClaimService claimService;
    @Captor
    private ArgumentCaptor<PcsCaseEntity> pcsCaseEntityCaptor;

    @Mock
    private PartyDocumentsService partyDocumentsService;

    private PcsCaseService underTest;

    @BeforeEach
    void setUp() {
        MapperConfig config = new MapperConfig();
        modelMapper = spy(config.modelMapper());
        tenancyLicenceService = mock(TenancyLicenceService.class);
        underTest = new PcsCaseService(
            pcsCaseRepository,
            pcsCaseMergeService,
            modelMapper,
            tenancyLicenceService,
            partyDocumentsService,
            claimantPartyFactory,
            claimService
        );
    }

    @Test
    void shouldCreateCaseWithAddressAndLegislativeCountry() {
        // Given
        AddressUK propertyAddress = mock(AddressUK.class);
        AddressEntity propertyAddressEntity = mock(AddressEntity.class);
        LegislativeCountry legislativeCountry = mock(LegislativeCountry.class);

        when(modelMapper.map(propertyAddress, AddressEntity.class)).thenReturn(propertyAddressEntity);

        // When
        underTest.createCase(CASE_REFERENCE, propertyAddress, legislativeCountry);

        // Then
        verify(pcsCaseRepository).save(pcsCaseEntityCaptor.capture());
        PcsCaseEntity savedEntity = pcsCaseEntityCaptor.getValue();
        assertThat(savedEntity.getCaseReference()).isEqualTo(CASE_REFERENCE);
        assertThat(savedEntity.getPropertyAddress()).isEqualTo(propertyAddressEntity);
        assertThat(savedEntity.getLegislativeCountry()).isEqualTo(legislativeCountry);
    }

    @Test
    void shouldCreateCaseWithNoData() {
        // Given
        PCSCase pcsCase = mock(PCSCase.class);

        // When
        underTest.createCase(CASE_REFERENCE, pcsCase);

        // Then
        verify(pcsCaseRepository).save(pcsCaseEntityCaptor.capture());

        PcsCaseEntity savedEntity = pcsCaseEntityCaptor.getValue();
        assertThat(savedEntity.getCaseManagementLocation()).isNull();
        assertThat(savedEntity.getPropertyAddress()).isNull();
        assertThat(savedEntity.getPreActionProtocolCompleted()).isNull();
    }

    @Test
    void shouldCreateCaseWithData() {
        // Given
        VerticalYesNo preActionProtocolCompleted = VerticalYesNo.YES;

        PCSCase pcsCase = mock(PCSCase.class);
        AddressUK propertyAddress = mock(AddressUK.class);
        final AddressEntity propertyAddressEntity = stubAddressUKModelMapper(propertyAddress);

        when(pcsCase.getPropertyAddress()).thenReturn(propertyAddress);
        when(pcsCase.getPreActionProtocolCompleted()).thenReturn(preActionProtocolCompleted);

        // When
        underTest.createCase(CASE_REFERENCE, pcsCase);

        // Then
        verify(pcsCaseRepository).save(pcsCaseEntityCaptor.capture());

        PcsCaseEntity savedEntity = pcsCaseEntityCaptor.getValue();
        assertThat(savedEntity.getPropertyAddress()).isEqualTo(propertyAddressEntity);
        assertThat(savedEntity.getPreActionProtocolCompleted()).isEqualTo(preActionProtocolCompleted.toBoolean());
    }

    @Test
    void shouldPatchCaseEntityAndSave() {
        // Given
        PCSCase pcsCase = mock(PCSCase.class);
        PcsCaseEntity pcsCaseEntity = mock(PcsCaseEntity.class);
        when(pcsCaseRepository.findByCaseReference(CASE_REFERENCE)).thenReturn(Optional.of(pcsCaseEntity));

        // When
        underTest.patchCase(CASE_REFERENCE, pcsCase);

        // Then
        InOrder inOrder = inOrder(pcsCaseRepository, pcsCaseMergeService);
        inOrder.verify(pcsCaseRepository).findByCaseReference(CASE_REFERENCE);
        inOrder.verify(pcsCaseMergeService).mergeCaseData(pcsCaseEntity, pcsCase);
        inOrder.verify(pcsCaseRepository).save(pcsCaseEntity);
    }

    @Test
    void shouldMergeCaseData() {
        // Given
        PCSCase pcsCase = mock(PCSCase.class);
        PcsCaseEntity pcsCaseEntity = mock(PcsCaseEntity.class);

        // When
        underTest.mergeCaseData(pcsCaseEntity, pcsCase);

        // Then
        verify(pcsCaseMergeService).mergeCaseData(pcsCaseEntity, pcsCase);
    }

    @Test
    void shouldThrowExceptionPatchingUnknownCaseReference() {
        // Given
        PCSCase pcsCase = mock(PCSCase.class);
        when(pcsCaseRepository.findByCaseReference(CASE_REFERENCE)).thenReturn(Optional.empty());

        // When
        Throwable throwable = catchThrowable(() -> underTest.patchCase(CASE_REFERENCE, pcsCase));

        // Then
        assertThat(throwable)
            .isInstanceOf(CaseNotFoundException.class)
            .hasMessage("No case found with reference %s", CASE_REFERENCE);
    }

    @Test
    void shouldLoadCaseFromRepository() {
        // Given
        PcsCaseEntity expectedPcsCaseEntity = mock(PcsCaseEntity.class);
        when(pcsCaseRepository.findByCaseReference(CASE_REFERENCE)).thenReturn(Optional.of(expectedPcsCaseEntity));

        // When
        PcsCaseEntity actualPcsCaseEntity = underTest.loadCase(CASE_REFERENCE);

        // Then
        verify(pcsCaseRepository).findByCaseReference(CASE_REFERENCE);
        assertThat(actualPcsCaseEntity).isSameAs(expectedPcsCaseEntity);
    }

    @Test
    void shouldThrowExceptionLoadingUnknownCaseReference() {
        // Given
        when(pcsCaseRepository.findByCaseReference(CASE_REFERENCE)).thenReturn(Optional.empty());

        // When
        Throwable throwable = catchThrowable(() -> underTest.loadCase(CASE_REFERENCE));

        // Then
        assertThat(throwable)
            .isInstanceOf(CaseNotFoundException.class)
            .hasMessage("No case found with reference %s", CASE_REFERENCE);
    }

    @Test
    void shouldSaveCaseToRepository() {
        // Given
        PcsCaseEntity pcsCaseEntity = mock(PcsCaseEntity.class);

        // When
        underTest.save(pcsCaseEntity);

        // Then
        verify(pcsCaseRepository).save(pcsCaseEntity);
    }

    private AddressEntity stubAddressUKModelMapper(AddressUK addressUK) {
        AddressEntity addressEntity = mock(AddressEntity.class);
        when(modelMapper.map(addressUK, AddressEntity.class)).thenReturn(addressEntity);
        return addressEntity;
    }

    @Test
    void shouldPersistClaimantTypeWhenCreatingCase() {
        // Given
        PCSCase pcsCase = mock(PCSCase.class);
        AddressUK propertyAddress = mock(AddressUK.class);

        DynamicStringList claimantTypeList = DynamicStringList.builder()
            .value(DynamicStringListElement.builder()
                .code(ClaimantType.PRIVATE_LANDLORD.name())
                .label(ClaimantType.PRIVATE_LANDLORD.getLabel())
                .build())
            .build();

        when(pcsCase.getPropertyAddress()).thenReturn(propertyAddress);
        when(pcsCase.getClaimantType()).thenReturn(claimantTypeList);

        // When
        underTest.createCase(CASE_REFERENCE, pcsCase);

        // Then
        verify(pcsCaseRepository).save(pcsCaseEntityCaptor.capture());

        PcsCaseEntity savedEntity = pcsCaseEntityCaptor.getValue();
        assertThat(savedEntity.getClaimantType()).isEqualTo(ClaimantType.PRIVATE_LANDLORD);
    }

    @Test
    void shouldSkipClaimantTypeWhenNull() {
        // Given
        PCSCase pcsCase = mock(PCSCase.class);
        AddressUK propertyAddress = mock(AddressUK.class);

        when(pcsCase.getPropertyAddress()).thenReturn(propertyAddress);
        when(pcsCase.getClaimantType()).thenReturn(null);

        // When
        underTest.createCase(CASE_REFERENCE, pcsCase);

        // Then
        verify(pcsCaseRepository).save(pcsCaseEntityCaptor.capture());

        PcsCaseEntity savedEntity = pcsCaseEntityCaptor.getValue();
        assertThat(savedEntity.getClaimantType()).isNull();
    }

    @Test
    void shouldSkipClaimantTypeWhenValueCodeIsNull() {
        // Given
        PCSCase pcsCase = mock(PCSCase.class);
        AddressUK propertyAddress = mock(AddressUK.class);

        DynamicStringList claimantTypeList = mock(DynamicStringList.class);
        when(claimantTypeList.getValueCode()).thenReturn(null);
        when(pcsCase.getPropertyAddress()).thenReturn(propertyAddress);
        when(pcsCase.getClaimantType()).thenReturn(claimantTypeList);

        // When
        underTest.createCase(CASE_REFERENCE, pcsCase);

        // Then
        verify(pcsCaseRepository).save(pcsCaseEntityCaptor.capture());

        PcsCaseEntity savedEntity = pcsCaseEntityCaptor.getValue();
        assertThat(savedEntity.getClaimantType()).isNull();
    }

    @ParameterizedTest
    @MethodSource("claimantTypeScenarios")
    void shouldPersistAllClaimantTypes(ClaimantType claimantType) {
        // Given
        PCSCase pcsCase = mock(PCSCase.class);
        AddressUK propertyAddress = mock(AddressUK.class);

        DynamicStringList claimantTypeList = DynamicStringList.builder()
            .value(DynamicStringListElement.builder()
                .code(claimantType.name())
                .label(claimantType.getLabel())
                .build())
            .build();

        when(pcsCase.getPropertyAddress()).thenReturn(propertyAddress);
        when(pcsCase.getClaimantType()).thenReturn(claimantTypeList);

        // When
        underTest.createCase(CASE_REFERENCE, pcsCase);

        // Then
        verify(pcsCaseRepository).save(pcsCaseEntityCaptor.capture());

        PcsCaseEntity savedEntity = pcsCaseEntityCaptor.getValue();
        assertThat(savedEntity.getClaimantType()).isEqualTo(claimantType);
    }

    @Test
    void shouldAddClaimantPartyAndClaimSuccessfully() {
        // Given
        PCSCase pcsCase = mock(PCSCase.class);
        UserInfo userDetails = mock(UserInfo.class);
        UUID userId = UUID.randomUUID();
        String userEmail = "test@example.com";

        PartyEntity claimantPartyEntity = mock(PartyEntity.class);
        ClaimEntity claimEntity = mock(ClaimEntity.class);

        when(userDetails.getUid()).thenReturn(userId.toString());
        when(userDetails.getSub()).thenReturn(userEmail);
        when(claimantPartyFactory.createAndPersistClaimantParty(eq(pcsCase),
                                                                any(ClaimantPartyFactory.ClaimantPartyContext.class)))
            .thenReturn(claimantPartyEntity);
        when(claimService.createMainClaimEntity(pcsCase, claimantPartyEntity)).thenReturn(claimEntity);
        PcsCaseEntity pcsCaseEntity = mock(PcsCaseEntity.class);

        // When
        underTest.addClaimantPartyAndClaim(pcsCaseEntity, pcsCase, userDetails);

        // Then
        InOrder inOrder = inOrder(claimantPartyFactory, pcsCaseEntity, claimService, pcsCaseRepository);
        inOrder.verify(claimantPartyFactory).createAndPersistClaimantParty(eq(pcsCase),
            any(ClaimantPartyFactory.ClaimantPartyContext.class));
        inOrder.verify(pcsCaseEntity).addParty(claimantPartyEntity);
        inOrder.verify(claimService).createMainClaimEntity(pcsCase, claimantPartyEntity);
        inOrder.verify(pcsCaseEntity).addClaim(claimEntity);
        inOrder.verify(pcsCaseRepository).save(pcsCaseEntity);
    }

    @Test
    void shouldCreateClaimantPartyContextWithCorrectUserDetails() {
        // Given
        PCSCase pcsCase = mock(PCSCase.class);
        UserInfo userDetails = mock(UserInfo.class);
        UUID userId = UUID.randomUUID();
        String userEmail = "claimant@test.com";

        PartyEntity claimantPartyEntity = mock(PartyEntity.class);
        ClaimEntity claimEntity = mock(ClaimEntity.class);

        when(userDetails.getUid()).thenReturn(userId.toString());
        when(userDetails.getSub()).thenReturn(userEmail);
        when(claimantPartyFactory.createAndPersistClaimantParty(eq(pcsCase),
                                                                any(ClaimantPartyFactory.ClaimantPartyContext.class)))
            .thenReturn(claimantPartyEntity);
        when(claimService.createMainClaimEntity(pcsCase, claimantPartyEntity)).thenReturn(claimEntity);

        ArgumentCaptor<ClaimantPartyFactory.ClaimantPartyContext> contextCaptor =
            ArgumentCaptor.forClass(ClaimantPartyFactory.ClaimantPartyContext.class);
        PcsCaseEntity pcsCaseEntity = mock(PcsCaseEntity.class);

        // When
        underTest.addClaimantPartyAndClaim(pcsCaseEntity, pcsCase, userDetails);

        // Then
        verify(claimantPartyFactory).createAndPersistClaimantParty(eq(pcsCase), contextCaptor.capture());
        ClaimantPartyFactory.ClaimantPartyContext capturedContext = contextCaptor.getValue();
        assertThat(capturedContext.userId()).isEqualTo(userId);
        assertThat(capturedContext.userEmail()).isEqualTo(userEmail);
    }

    @Test
    void shouldSavePcsCaseEntityAfterAddingPartyAndClaim() {
        // Given
        PCSCase pcsCase = mock(PCSCase.class);
        UserInfo userDetails = mock(UserInfo.class);
        UUID userId = UUID.randomUUID();

        PartyEntity claimantPartyEntity = mock(PartyEntity.class);
        ClaimEntity claimEntity = mock(ClaimEntity.class);

        when(userDetails.getUid()).thenReturn(userId.toString());
        when(userDetails.getSub()).thenReturn("test@example.com");
        when(claimantPartyFactory.createAndPersistClaimantParty(eq(pcsCase),
                                                                any(ClaimantPartyFactory.ClaimantPartyContext.class)))
            .thenReturn(claimantPartyEntity);
        when(claimService.createMainClaimEntity(pcsCase, claimantPartyEntity)).thenReturn(claimEntity);
        PcsCaseEntity pcsCaseEntity = mock(PcsCaseEntity.class);

        // When
        underTest.addClaimantPartyAndClaim(pcsCaseEntity, pcsCase, userDetails);

        // Then
        verify(pcsCaseRepository).save(pcsCaseEntity);
    }

    @Test
    void shouldAddPartyBeforeCreatingClaim() {
        // Given
        PCSCase pcsCase = mock(PCSCase.class);
        UserInfo userDetails = mock(UserInfo.class);
        UUID userId = UUID.randomUUID();

        PartyEntity claimantPartyEntity = mock(PartyEntity.class);
        ClaimEntity claimEntity = mock(ClaimEntity.class);

        when(userDetails.getUid()).thenReturn(userId.toString());
        when(userDetails.getSub()).thenReturn("test@example.com");
        when(claimantPartyFactory.createAndPersistClaimantParty(eq(pcsCase),
                                                                any(ClaimantPartyFactory.ClaimantPartyContext.class)))
            .thenReturn(claimantPartyEntity);
        when(claimService.createMainClaimEntity(pcsCase, claimantPartyEntity)).thenReturn(claimEntity);
        PcsCaseEntity pcsCaseEntity = mock(PcsCaseEntity.class);

        // When
        underTest.addClaimantPartyAndClaim(pcsCaseEntity, pcsCase, userDetails);

        // Then
        InOrder inOrder = inOrder(pcsCaseEntity, claimService);
        inOrder.verify(pcsCaseEntity).addParty(claimantPartyEntity);
        inOrder.verify(claimService).createMainClaimEntity(pcsCase, claimantPartyEntity);
    }

    @Test
    void shouldAddClaimAfterCreatingIt() {
        // Given
        PCSCase pcsCase = mock(PCSCase.class);
        UserInfo userDetails = mock(UserInfo.class);
        UUID userId = UUID.randomUUID();

        PartyEntity claimantPartyEntity = mock(PartyEntity.class);
        ClaimEntity claimEntity = mock(ClaimEntity.class);

        when(userDetails.getUid()).thenReturn(userId.toString());
        when(userDetails.getSub()).thenReturn("test@example.com");
        when(claimantPartyFactory.createAndPersistClaimantParty(eq(pcsCase),
                                                                any(ClaimantPartyFactory.ClaimantPartyContext.class)))
            .thenReturn(claimantPartyEntity);
        when(claimService.createMainClaimEntity(pcsCase, claimantPartyEntity)).thenReturn(claimEntity);
        PcsCaseEntity pcsCaseEntity = mock(PcsCaseEntity.class);

        // When
        underTest.addClaimantPartyAndClaim(pcsCaseEntity, pcsCase, userDetails);

        // Then
        InOrder inOrder = inOrder(claimService, pcsCaseEntity);
        inOrder.verify(claimService).createMainClaimEntity(pcsCase, claimantPartyEntity);
        inOrder.verify(pcsCaseEntity).addClaim(claimEntity);
    }

    @Test
    void shouldPassCorrectPcsCaseAndPartyToClaimService() {
        // Given
        PCSCase pcsCase = mock(PCSCase.class);
        UserInfo userDetails = mock(UserInfo.class);
        UUID userId = UUID.randomUUID();

        PartyEntity claimantPartyEntity = mock(PartyEntity.class);
        ClaimEntity claimEntity = mock(ClaimEntity.class);

        when(userDetails.getUid()).thenReturn(userId.toString());
        when(userDetails.getSub()).thenReturn("test@example.com");
        when(claimantPartyFactory.createAndPersistClaimantParty(eq(pcsCase),
                                                                any(ClaimantPartyFactory.ClaimantPartyContext.class)))
            .thenReturn(claimantPartyEntity);
        when(claimService.createMainClaimEntity(pcsCase, claimantPartyEntity)).thenReturn(claimEntity);
        PcsCaseEntity pcsCaseEntity = mock(PcsCaseEntity.class);

        // When
        underTest.addClaimantPartyAndClaim(pcsCaseEntity, pcsCase, userDetails);

        // Then
        verify(claimService).createMainClaimEntity(pcsCase, claimantPartyEntity);
    }

    private static Stream<Arguments> claimantTypeScenarios() {
        return Stream.of(
            arguments(ClaimantType.PRIVATE_LANDLORD),
            arguments(ClaimantType.PROVIDER_OF_SOCIAL_HOUSING),
            arguments(ClaimantType.COMMUNITY_LANDLORD),
            arguments(ClaimantType.MORTGAGE_LENDER),
            arguments(ClaimantType.OTHER)
        );
    }

}
