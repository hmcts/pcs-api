package uk.gov.hmcts.reform.pcs.ccd.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.PaymentStatus;
import uk.gov.hmcts.reform.pcs.ccd.entity.AddressEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.exception.CaseNotFoundException;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class PcsCaseServiceTest {

    private static final long CASE_REFERENCE = 1234L;

    @Mock
    private PcsCaseRepository pcsCaseRepository;
    @Mock
    private SecurityContextService securityContextService;
    @Mock
    private ModelMapper modelMapper;
    @Captor
    private ArgumentCaptor<PcsCaseEntity> pcsCaseEntityCaptor;

    private PcsCaseService underTest;

    @BeforeEach
    void setUp() {
        underTest = new PcsCaseService(pcsCaseRepository, securityContextService, modelMapper);
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
        assertThat(savedEntity.getApplicantForename()).isNull();
        assertThat(savedEntity.getApplicantSurname()).isNull();
        assertThat(savedEntity.getPropertyAddress()).isNull();
    }

    @Test
    void shouldCreateCaseWithData() {
        // Given
        String expectedForename = "Test forename";
        String expectedSurname = "Test surname";

        PCSCase pcsCase = mock(PCSCase.class);
        AddressUK propertyAddress = mock(AddressUK.class);
        final AddressEntity propertyAddressEntity = stubAddressUKModelMapper(propertyAddress);

        when(pcsCase.getApplicantForename()).thenReturn(expectedForename);
        when(pcsCase.getApplicantSurname()).thenReturn(expectedSurname);
        when(pcsCase.getPropertyAddress()).thenReturn(propertyAddress);

        // When
        underTest.createCase(CASE_REFERENCE, pcsCase);

        // Then
        verify(pcsCaseRepository).save(pcsCaseEntityCaptor.capture());

        PcsCaseEntity savedEntity = pcsCaseEntityCaptor.getValue();
        assertThat(savedEntity.getApplicantForename()).isEqualTo(expectedForename);
        assertThat(savedEntity.getApplicantSurname()).isEqualTo(expectedSurname);
        assertThat(savedEntity.getPropertyAddress()).isEqualTo(propertyAddressEntity);
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
    void shouldLeaveFieldsUnchangedWhenPatchingCaseWithNoData() {
        // Given
        PCSCase pcsCase = mock(PCSCase.class);
        PcsCaseEntity existingPcsCaseEntity = mock(PcsCaseEntity.class);

        when(pcsCaseRepository.findByCaseReference(CASE_REFERENCE)).thenReturn(Optional.of(existingPcsCaseEntity));
        when(pcsCase.getCaseManagementLocation()).thenReturn(null);

        // When
        underTest.patchCase(CASE_REFERENCE, pcsCase);

        // Then
        verify(pcsCaseRepository).save(pcsCaseEntityCaptor.capture());

        PcsCaseEntity savedEntity = pcsCaseEntityCaptor.getValue();
        assertThat(savedEntity).isSameAs(existingPcsCaseEntity);
        verifyNoInteractions(existingPcsCaseEntity);
    }

    @Test
    void shouldChangeFieldsWhenPatchingCase() {
        // Given
        PCSCase pcsCase = mock(PCSCase.class);
        String updatedForename = "Updated forename";
        String updatedSurname = "Updated surname";

        AddressUK updatedPropertyAddress = mock(AddressUK.class);
        final AddressEntity updatedAddressEntity = stubAddressUKModelMapper(updatedPropertyAddress);

        PcsCaseEntity existingPcsCaseEntity = mock(PcsCaseEntity.class);
        when(pcsCase.getApplicantForename()).thenReturn(updatedForename);
        when(pcsCase.getApplicantSurname()).thenReturn(updatedSurname);
        when(pcsCase.getPropertyAddress()).thenReturn(updatedPropertyAddress);

        when(pcsCaseRepository.findByCaseReference(CASE_REFERENCE)).thenReturn(Optional.of(existingPcsCaseEntity));

        // When
        underTest.patchCase(CASE_REFERENCE, pcsCase);

        // Then
        verify(pcsCaseRepository).save(pcsCaseEntityCaptor.capture());

        PcsCaseEntity savedEntity = pcsCaseEntityCaptor.getValue();
        assertThat(savedEntity).isSameAs(existingPcsCaseEntity);
        verify(existingPcsCaseEntity).setApplicantForename(updatedForename);
        verify(existingPcsCaseEntity).setApplicantSurname(updatedSurname);
        verify(existingPcsCaseEntity).setPropertyAddress(updatedAddressEntity);
    }

    @Test
    void shouldCreatePartyWithPcqIdForCurrentUser() {
        UUID userId = UUID.randomUUID();
        UUID expectedPcqId = UUID.randomUUID();
        String expectedForename = "some forename";
        String expectedSurname = "some surname";

        PCSCase pcsCase = mock(PCSCase.class);
        when(pcsCase.getUserPcqId()).thenReturn(expectedPcqId.toString());

        UserInfo userDetails = mock(UserInfo.class);
        when(userDetails.getUid()).thenReturn(userId.toString());
        when(userDetails.getGivenName()).thenReturn(expectedForename);
        when(userDetails.getFamilyName()).thenReturn(expectedSurname);
        when(securityContextService.getCurrentUserDetails()).thenReturn(userDetails);

        PcsCaseEntity existingPcsCaseEntity = new PcsCaseEntity();

        when(pcsCaseRepository.findByCaseReference(CASE_REFERENCE)).thenReturn(Optional.of(existingPcsCaseEntity));

        // When
        underTest.patchCase(CASE_REFERENCE, pcsCase);

        // Then
        verify(pcsCaseRepository).save(pcsCaseEntityCaptor.capture());
        PcsCaseEntity savedEntity = pcsCaseEntityCaptor.getValue();
        assertThat(savedEntity.getParties())
            .hasSize(1)
            .allSatisfy(party -> {
                assertThat(party.getIdamId()).isEqualTo(userId);
                assertThat(party.getPcqId()).isEqualTo(expectedPcqId);
                assertThat(party.getForename()).isEqualTo(expectedForename);
                assertThat(party.getSurname()).isEqualTo(expectedSurname);
            });

    }

    @Test
    void shouldUpdateExistingPartyWithPcqIdForCurrentUser() {
        final UUID userId = UUID.randomUUID();
        final UUID expectedPcqId = UUID.randomUUID();
        final UUID existingPartyId = UUID.randomUUID();

        PCSCase pcsCase = mock(PCSCase.class);
        when(pcsCase.getUserPcqId()).thenReturn(expectedPcqId.toString());

        UserInfo userDetails = mock(UserInfo.class);
        when(userDetails.getUid()).thenReturn(userId.toString());
        when(securityContextService.getCurrentUserDetails()).thenReturn(userDetails);

        PcsCaseEntity existingPcsCaseEntity = new PcsCaseEntity();
        PartyEntity existingPartyEntity = new PartyEntity();
        existingPartyEntity.setId(existingPartyId);
        existingPartyEntity.setIdamId(userId);
        existingPcsCaseEntity.addParty(existingPartyEntity);

        when(pcsCaseRepository.findByCaseReference(CASE_REFERENCE)).thenReturn(Optional.of(existingPcsCaseEntity));

        // When
        underTest.patchCase(CASE_REFERENCE, pcsCase);

        // Then
        verify(pcsCaseRepository).save(pcsCaseEntityCaptor.capture());
        PcsCaseEntity savedEntity = pcsCaseEntityCaptor.getValue();
        assertThat(savedEntity.getParties())
            .hasSize(1)
            .allSatisfy(
                party -> {
                    assertThat(party.getId()).isEqualTo(existingPartyId);
                    assertThat(party.getPcqId()).isEqualTo(expectedPcqId);
                }
            );

    }

    @Test
    void shouldUpdatePaymentStatusWhenNotNull() {
        // Given
        PCSCase pcsCase = mock(PCSCase.class);
        PcsCaseEntity existingPcsCaseEntity = mock(PcsCaseEntity.class);
        PaymentStatus paymentStatus = PaymentStatus.PAID;

        when(pcsCase.getPaymentStatus()).thenReturn(paymentStatus);
        when(pcsCaseRepository.findByCaseReference(CASE_REFERENCE)).thenReturn(Optional.of(existingPcsCaseEntity));

        // When
        underTest.patchCase(CASE_REFERENCE, pcsCase);

        // Then
        verify(pcsCaseRepository).save(pcsCaseEntityCaptor.capture());
        verify(existingPcsCaseEntity).setPaymentStatus(paymentStatus);
        assertThat(pcsCaseEntityCaptor.getValue()).isSameAs(existingPcsCaseEntity);
    }

    @Test
    void shouldUpdateCaseManagementLocationWhenNotNull() {
        // Given
        PCSCase pcsCase = mock(PCSCase.class);
        PcsCaseEntity existingPcsCaseEntity = mock(PcsCaseEntity.class);
        Integer location = 13685;

        when(pcsCase.getCaseManagementLocation()).thenReturn(location);
        when(pcsCaseRepository.findByCaseReference(CASE_REFERENCE)).thenReturn(Optional.of(existingPcsCaseEntity));

        // When
        underTest.patchCase(CASE_REFERENCE, pcsCase);

        // Then
        verify(pcsCaseRepository).save(pcsCaseEntityCaptor.capture());
        verify(existingPcsCaseEntity).setCaseManagementLocation(location);
        assertThat(pcsCaseEntityCaptor.getValue()).isSameAs(existingPcsCaseEntity);
    }

    private AddressEntity stubAddressUKModelMapper(AddressUK addressUK) {
        AddressEntity addressEntity = mock(AddressEntity.class);
        when(modelMapper.map(addressUK, AddressEntity.class)).thenReturn(addressEntity);
        return addressEntity;
    }

    @Test
    void shouldAddDocumentToCase() {
        final String fileName = "test-document.pdf";
        final String filePath = "/documents/test-document.pdf";

        PcsCaseEntity existingPcsCaseEntity = new PcsCaseEntity();
        existingPcsCaseEntity.setCaseReference(CASE_REFERENCE);

        when(pcsCaseRepository.findByCaseReference(CASE_REFERENCE)).thenReturn(Optional.of(existingPcsCaseEntity));

        underTest.addDocumentToCase(CASE_REFERENCE, fileName, filePath);

        verify(pcsCaseRepository).save(pcsCaseEntityCaptor.capture());

        PcsCaseEntity savedEntity = pcsCaseEntityCaptor.getValue();

        assertThat(savedEntity).isNotNull();
        assertThat(savedEntity.getDocuments()).isNotNull();
        assertThat(savedEntity.getDocuments()).hasSize(1);

        if (!savedEntity.getDocuments().isEmpty()) {
            DocumentEntity savedDocument = savedEntity.getDocuments().iterator().next();
            System.out.println("Document: " + savedDocument);
            assertThat(savedDocument.getFileName()).isEqualTo(fileName);
        }
    }

    @Test
    void shouldThrowExceptionWhenAddingDocumentToUnknownCase() {
        final String fileName = "test-document.docx";
        final String filePath = "/documents/test-document.docx";

        when(pcsCaseRepository.findByCaseReference(CASE_REFERENCE)).thenReturn(Optional.empty());

        Throwable throwable = catchThrowable(() -> underTest.addDocumentToCase(CASE_REFERENCE, fileName, filePath));

        assertThat(throwable)
            .isInstanceOf(CaseNotFoundException.class)
            .hasMessage("No case found with reference %s", CASE_REFERENCE);
    }

    @Test
    void shouldAddMultipleDocumentsToCase() {
        final String fileName1 = "document1.docx";
        final String filePath1 = "/documents/document1.docx";
        final String fileName2 = "document2.docx";
        final String filePath2 = "/documents/document2.docx";

        PcsCaseEntity existingPcsCaseEntity = new PcsCaseEntity();
        existingPcsCaseEntity.setCaseReference(CASE_REFERENCE);

        when(pcsCaseRepository.findByCaseReference(CASE_REFERENCE)).thenReturn(Optional.of(existingPcsCaseEntity));

        underTest.addDocumentToCase(CASE_REFERENCE, fileName1, filePath1);
        underTest.addDocumentToCase(CASE_REFERENCE, fileName2, filePath2);

        verify(pcsCaseRepository, times(2)).save(pcsCaseEntityCaptor.capture());

        PcsCaseEntity finalSavedEntity = pcsCaseEntityCaptor.getValue();
        assertThat(finalSavedEntity.getDocuments()).hasSize(2);

        Set<String> fileNames = finalSavedEntity.getDocuments().stream()
            .map(DocumentEntity::getFileName)
            .collect(Collectors.toSet());

        assertThat(fileNames).containsExactlyInAnyOrder(fileName1, fileName2);
    }

}
