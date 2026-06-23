package uk.gov.hmcts.reform.pcs.ccd.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.repository.DraftClaimDeletionRepository;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DraftClaimDeletionServiceTest {

    private static final long CASE_REFERENCE = 1234567890123456L;
    private static final UUID CASE_ID = UUID.randomUUID();
    private static final UUID ADDRESS_ID = UUID.randomUUID();
    private static final UUID CONTACT_PREFERENCE_ID = UUID.randomUUID();
    private static final UUID HELP_WITH_FEES_ID = UUID.randomUUID();
    private static final UUID LEGAL_REPRESENTATIVE_ADDRESS_ID = UUID.randomUUID();

    @Mock
    private DraftClaimDeletionRepository draftClaimDeletionRepository;

    private DraftClaimDeletionService underTest;

    @BeforeEach
    void setUp() {
        underTest = new DraftClaimDeletionService(draftClaimDeletionRepository);
    }

    @Test
    void shouldDeleteDraftAndCcdDataWhenCaseRecordIsAlreadyAbsent() {
        when(draftClaimDeletionRepository.getCaseId(CASE_REFERENCE)).thenReturn(null);

        underTest.deleteDraftClaim(CASE_REFERENCE);

        verify(draftClaimDeletionRepository).deleteDraftCaseData(CASE_REFERENCE);
        verify(draftClaimDeletionRepository).deleteCcdCaseData(CASE_REFERENCE);
        verify(draftClaimDeletionRepository, never()).deleteCase(CASE_ID);
    }

    @Test
    void shouldRejectDeletionWhenClaimHasBeenIssued() {
        when(draftClaimDeletionRepository.getCaseId(CASE_REFERENCE)).thenReturn(CASE_ID);
        when(draftClaimDeletionRepository.hasIssuedClaim(CASE_ID)).thenReturn(true);

        assertThatThrownBy(() -> underTest.deleteDraftClaim(CASE_REFERENCE))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Cannot delete a claim that has been issued");

        InOrder inOrder = inOrder(draftClaimDeletionRepository);
        inOrder.verify(draftClaimDeletionRepository).lockClaimsForCase(CASE_ID);
        inOrder.verify(draftClaimDeletionRepository).hasIssuedClaim(CASE_ID);
        verify(draftClaimDeletionRepository, never()).deleteDraftCaseData(CASE_REFERENCE);
        verify(draftClaimDeletionRepository, never()).deleteCcdCaseData(CASE_REFERENCE);
        verify(draftClaimDeletionRepository, never()).deleteCase(CASE_ID);
    }

    @Test
    void shouldDeleteDraftClaimAndLinkedDataWhenClaimHasNotBeenIssued() {
        when(draftClaimDeletionRepository.getCaseId(CASE_REFERENCE)).thenReturn(CASE_ID);
        when(draftClaimDeletionRepository.hasIssuedClaim(CASE_ID)).thenReturn(false);
        when(draftClaimDeletionRepository.getAddressIdsForCase(CASE_ID)).thenReturn(List.of(ADDRESS_ID));
        when(draftClaimDeletionRepository.getContactPreferenceIdsForCase(CASE_ID))
            .thenReturn(List.of(CONTACT_PREFERENCE_ID));
        when(draftClaimDeletionRepository.getHelpWithFeesIdsForCase(CASE_ID))
            .thenReturn(List.of(HELP_WITH_FEES_ID));
        when(draftClaimDeletionRepository.getLegalRepresentativeAddressIdsForCase(CASE_ID))
            .thenReturn(List.of(LEGAL_REPRESENTATIVE_ADDRESS_ID));

        underTest.deleteDraftClaim(CASE_REFERENCE);

        InOrder inOrder = inOrder(draftClaimDeletionRepository);
        inOrder.verify(draftClaimDeletionRepository).lockClaimsForCase(CASE_ID);
        inOrder.verify(draftClaimDeletionRepository).hasIssuedClaim(CASE_ID);
        inOrder.verify(draftClaimDeletionRepository).deleteDraftCaseData(CASE_REFERENCE);
        inOrder.verify(draftClaimDeletionRepository).getAddressIdsForCase(CASE_ID);
        inOrder.verify(draftClaimDeletionRepository).getContactPreferenceIdsForCase(CASE_ID);
        inOrder.verify(draftClaimDeletionRepository).getHelpWithFeesIdsForCase(CASE_ID);
        inOrder.verify(draftClaimDeletionRepository).getLegalRepresentativeAddressIdsForCase(CASE_ID);
        inOrder.verify(draftClaimDeletionRepository).deleteRowsLinkedToCase(CASE_ID);
        inOrder.verify(draftClaimDeletionRepository).deleteRowsLinkedToClaims(CASE_ID);
        inOrder.verify(draftClaimDeletionRepository).deleteRowsLinkedToParties(CASE_ID);
        inOrder.verify(draftClaimDeletionRepository).deleteCaseDocuments(CASE_ID);
        inOrder.verify(draftClaimDeletionRepository).deleteClaims(CASE_ID);
        inOrder.verify(draftClaimDeletionRepository).deleteParties(CASE_ID);
        inOrder.verify(draftClaimDeletionRepository).deleteCase(CASE_ID);
        inOrder.verify(draftClaimDeletionRepository).deleteContactPreferences(List.of(CONTACT_PREFERENCE_ID));
        inOrder.verify(draftClaimDeletionRepository).deleteHelpWithFees(List.of(HELP_WITH_FEES_ID));
        inOrder.verify(draftClaimDeletionRepository).deleteAddresses(List.of(LEGAL_REPRESENTATIVE_ADDRESS_ID));
        inOrder.verify(draftClaimDeletionRepository).deleteAddresses(List.of(ADDRESS_ID));
        inOrder.verify(draftClaimDeletionRepository).deleteCcdCaseData(CASE_REFERENCE);
    }

    @Test
    void shouldTreatNullIssuedClaimResultAsNotIssued() {
        when(draftClaimDeletionRepository.getCaseId(CASE_REFERENCE)).thenReturn(CASE_ID);
        when(draftClaimDeletionRepository.hasIssuedClaim(CASE_ID)).thenReturn(false);
        when(draftClaimDeletionRepository.getAddressIdsForCase(CASE_ID)).thenReturn(List.of());
        when(draftClaimDeletionRepository.getContactPreferenceIdsForCase(CASE_ID)).thenReturn(List.of());
        when(draftClaimDeletionRepository.getHelpWithFeesIdsForCase(CASE_ID)).thenReturn(List.of());
        when(draftClaimDeletionRepository.getLegalRepresentativeAddressIdsForCase(CASE_ID)).thenReturn(List.of());

        underTest.deleteDraftClaim(CASE_REFERENCE);

        verify(draftClaimDeletionRepository).deleteCase(CASE_ID);
    }
}
