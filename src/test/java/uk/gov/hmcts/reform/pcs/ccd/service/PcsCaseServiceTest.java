package uk.gov.hmcts.reform.pcs.ccd.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.DefendantDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.AddressEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.model.Defendant;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.config.MapperConfig;
import uk.gov.hmcts.reform.pcs.exception.CaseNotFoundException;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
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

    private PcsCaseService underTest;

    @BeforeEach
    void setUp() {
        MapperConfig config = new MapperConfig();
        modelMapper = spy(config.modelMapper());

        underTest = new PcsCaseService(pcsCaseRepository, pcsCaseMergeService, modelMapper, tenancyLicenceService);
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
        assertThat(actualPcsCaseEntity).isEqualTo(expectedPcsCaseEntity);
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

    @Test
    void shouldMapFromDefendantDetailsToDefendantPojo() {
        // Given
        AddressUK correspondenceAddress = AddressUK.builder()
            .addressLine1("125 Broadway")
            .postCode("W5 8DG")
            .build();
        DefendantDetails details = DefendantDetails.builder()
            .nameKnown(VerticalYesNo.YES)
            .firstName("John")
            .lastName("Doe")
            .addressKnown(VerticalYesNo.YES)
            .addressSameAsPossession(VerticalYesNo.YES)
            .correspondenceAddress(correspondenceAddress)
            .emailKnown(VerticalYesNo.NO)
            .build();

        ListValue<DefendantDetails> listValue = new ListValue<>("123", details);

        // When
        List<Defendant> result = underTest.mapFromDefendantDetails(List.of(listValue));

        // Then
        assertThat(result).hasSize(1);
        Defendant mappedDefendant = result.getFirst();

        assertThat(mappedDefendant.getId()).isEqualTo("123");
        assertThat(mappedDefendant.getNameKnown()).isTrue();
        assertThat(mappedDefendant.getFirstName()).isEqualTo("John");
        assertThat(mappedDefendant.getLastName()).isEqualTo("Doe");
        assertThat(mappedDefendant.getAddressKnown()).isTrue();
        assertThat(mappedDefendant.getAddressSameAsPossession()).isTrue();
        assertThat(mappedDefendant.getCorrespondenceAddress().getAddressLine1())
            .isEqualTo(correspondenceAddress.getAddressLine1());
        assertThat(mappedDefendant.getEmailKnown()).isFalse();
    }

    @Test
    void shouldMapDefendantPojoToDefendantDetails() {
        // Given
        Defendant defendant = Defendant.builder()
            .id("456")
            .nameKnown(true)
            .firstName("Jane")
            .lastName("Smith")
            .addressKnown(false)
            .addressSameAsPossession(false)
            .emailKnown(true)
            .email("jane.smith@email.com")
            .build();

        // When
        List<ListValue<DefendantDetails>> result = underTest.mapToDefendantDetails(List.of(defendant));

        // Then
        ListValue<DefendantDetails> listValue = result.getFirst();
        DefendantDetails mappedDefendantDetails = listValue.getValue();

        assertThat(result).hasSize(1);
        assertThat(listValue.getId()).isEqualTo("456");
        assertThat(mappedDefendantDetails.getNameKnown()).isEqualTo(VerticalYesNo.YES);
        assertThat(mappedDefendantDetails.getFirstName()).isEqualTo("Jane");
        assertThat(mappedDefendantDetails.getLastName()).isEqualTo("Smith");
        assertThat(mappedDefendantDetails.getAddressKnown()).isEqualTo(VerticalYesNo.NO);
        assertThat(mappedDefendantDetails.getAddressSameAsPossession()).isEqualTo(VerticalYesNo.NO);
        assertThat(mappedDefendantDetails.getEmailKnown()).isEqualTo(VerticalYesNo.YES);
        assertThat(mappedDefendantDetails.getEmail()).isEqualTo("jane.smith@email.com");
    }

    @Test
    void shouldClearHiddenDefendantDetailsFields() {
        // Given
        DefendantDetails defendantWithHiddenFields = DefendantDetails.builder()
            .nameKnown(VerticalYesNo.NO)
            .firstName("John")
            .lastName("Doe")
            .addressKnown(VerticalYesNo.NO)
            .correspondenceAddress(AddressUK.builder()
                                       .addressLine1("123 Test Street")
                                       .postTown("Test Town")
                                       .postCode("TE1 1ST")
                                       .build())
            .addressSameAsPossession(VerticalYesNo.NO)
            .emailKnown(VerticalYesNo.NO)
            .email("test@example.com")
            .build();

        List<ListValue<DefendantDetails>> defendantsList = List.of(
            new ListValue<>("1", defendantWithHiddenFields));

        // When
        underTest.clearHiddenDefendantDetailsFields(defendantsList);

        // Then
        DefendantDetails clearedDefendant = defendantsList.getFirst().getValue();
        assertThat(clearedDefendant.getFirstName()).isNull();
        assertThat(clearedDefendant.getLastName()).isNull();
        assertThat(clearedDefendant.getCorrespondenceAddress()).isNull();
        assertThat(clearedDefendant.getAddressSameAsPossession()).isNull();
        assertThat(clearedDefendant.getEmail()).isNull();
    }

    private AddressEntity stubAddressUKModelMapper(AddressUK addressUK) {
        AddressEntity addressEntity = mock(AddressEntity.class);
        when(modelMapper.map(addressUK, AddressEntity.class)).thenReturn(addressEntity);
        return addressEntity;
    }
}
