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
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantType;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.AddressEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringList;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringListElement;
import uk.gov.hmcts.reform.pcs.config.MapperConfig;
import uk.gov.hmcts.reform.pcs.exception.CaseNotFoundException;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;

import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.junit.jupiter.params.provider.Arguments.arguments;
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
            partyDocumentsService
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
