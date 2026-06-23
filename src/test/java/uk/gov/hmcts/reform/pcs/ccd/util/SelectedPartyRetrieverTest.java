package uk.gov.hmcts.reform.pcs.ccd.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.party.LegalRepForDefendantAccessValidator;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SelectedPartyRetrieverTest {

    @InjectMocks
    private SelectedPartyRetriever selectedPartyRetriever;

    @Mock
    private ClientContextRetriever clientContextRetriever;

    @Mock
    private ClientContext clientContext;

    @Mock
    private PcsCaseEntity pcsCaseEntity;

    @Mock
    private PartyEntity partyEntity;

    @Mock
    private PartyEntity partyEntity2;

    @Mock
    private PcsCaseService pcsCaseService;

    @Mock
    private LegalRepForDefendantAccessValidator legalRepForDefendantAccessValidator;

    @Mock
    private SecurityContextService securityContextService;

    @Test
    void getSelectedPartyId_WithSingleDefendantAndCaseReference_ReturnsPartyId() {
        // given
        UUID userId = UUID.randomUUID();
        UUID partyId = UUID.randomUUID();
        long caseRef = 1L;
        when(pcsCaseService.loadCase(caseRef)).thenReturn(pcsCaseEntity);
        when(securityContextService.getCurrentUserId()).thenReturn(userId);
        when(legalRepForDefendantAccessValidator.validateAndGetDefendants(pcsCaseEntity, userId))
            .thenReturn(List.of(partyEntity));
        when(partyEntity.getId()).thenReturn(partyId);

        // when
        Optional<UUID> result = selectedPartyRetriever.getSelectedPartyId(caseRef);

        // then
        assertTrue(result.isPresent());
        assertEquals(partyId, result.get());
        verify(clientContextRetriever, never()).getClientContext();
    }

    @Test
    void getSelectedPartyId_WithMultipleDefendantsAndCaseReference_ReturnClientContextSelectedPartyId() {
        // given
        UUID userId = UUID.randomUUID();
        UUID partyId = UUID.randomUUID();
        long caseRef = 1L;
        when(pcsCaseService.loadCase(caseRef)).thenReturn(pcsCaseEntity);
        when(securityContextService.getCurrentUserId()).thenReturn(userId);
        when(legalRepForDefendantAccessValidator.validateAndGetDefendants(pcsCaseEntity, userId))
            .thenReturn(List.of(partyEntity, partyEntity2));
        when(clientContextRetriever.getClientContext()).thenReturn(clientContext);
        when(clientContext.getSelectedPartyId()).thenReturn(partyId.toString());

        // when
        Optional<UUID> result = selectedPartyRetriever.getSelectedPartyId(caseRef);

        // then
        assertTrue(result.isPresent());
        assertEquals(partyId, result.get());
    }

    @Test
    void getSelectedPartyId_WithSingleDefendant_ReturnsPartyId() {
        // given
        UUID partyID = UUID.randomUUID();
        Party party = Party.builder()
            .build();

        List<ListValue<Party>> defendantList = new ArrayList<>();
        defendantList.add(ListValue.<Party>builder().value(party).id(partyID.toString()).build());

        PCSCase pcsCase =  PCSCase.builder().build();
        pcsCase.setAllDefendants(defendantList);

        // when
        Optional<UUID> result = selectedPartyRetriever.getSelectedPartyId(pcsCase);

        // then
        assertTrue(result.isPresent());
        assertEquals(partyID, result.get());
        verify(clientContextRetriever, never()).getClientContext();
    }

    @Test
    void getSelectedPartyId_WithMultipleDefendants_ReturnClientContextSelectedPartyId() {
        // given
        Party party = Party.builder()
            .build();
        List<ListValue<Party>> defendantList = new ArrayList<>();
        defendantList.add(ListValue.<Party>builder().value(party).build());
        defendantList.add(ListValue.<Party>builder().value(party).build());

        PCSCase pcsCase = PCSCase.builder().build();
        pcsCase.setAllDefendants(defendantList);
        UUID partyId = UUID.randomUUID();

        when(clientContextRetriever.getClientContext()).thenReturn(clientContext);
        when(clientContext.getSelectedPartyId()).thenReturn(partyId.toString());

        // when
        Optional<UUID> result = selectedPartyRetriever.getSelectedPartyId(pcsCase);

        // then
        assertTrue(result.isPresent());
        assertEquals(partyId, result.get());
    }

    @Test
    void getRequiredPartyId_WithInvalidSelectedPartyId_ThrowsException() {
        // given
        Party party = Party.builder()
            .build();
        List<ListValue<Party>> defendantList = new ArrayList<>();
        defendantList.add(ListValue.<Party>builder().value(party).build());
        defendantList.add(ListValue.<Party>builder().value(party).build());

        PCSCase pcsCase =  PCSCase.builder().build();
        pcsCase.setAllDefendants(defendantList);

        when(clientContextRetriever.getClientContext()).thenReturn(clientContext);
        when(clientContext.getSelectedPartyId()).thenReturn("invalid-id");

        // when / then
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            selectedPartyRetriever.getSelectedPartyId(pcsCase);
        });
        assertEquals("Invalid selected responding party id for respond to claim", exception.getMessage());
    }

    @Test
    void getRequiredPartyId_WithMissingClientContext_ReturnsEmpty() {
        // given
        Party party = Party.builder()
            .build();
        List<ListValue<Party>> defendantList = new ArrayList<>();
        defendantList.add(ListValue.<Party>builder().value(party).build());
        defendantList.add(ListValue.<Party>builder().value(party).build());

        PCSCase pcsCase =  PCSCase.builder().build();
        pcsCase.setAllDefendants(defendantList);

        when(clientContextRetriever.getClientContext()).thenReturn(null);

        // when
        Optional<UUID> selectedPartyId = selectedPartyRetriever.getSelectedPartyId(pcsCase);

        // then
        assertTrue(selectedPartyId.isEmpty());
    }

    @Test
    void getRequiredPartyId_WithBlankPartyId_ReturnsOptional() {
        // given
        Party party = Party.builder()
            .build();
        List<ListValue<Party>> defendantList = new ArrayList<>();
        defendantList.add(ListValue.<Party>builder().value(party).build());
        defendantList.add(ListValue.<Party>builder().value(party).build());

        PCSCase pcsCase =  PCSCase.builder().build();
        pcsCase.setAllDefendants(defendantList);

        when(clientContextRetriever.getClientContext()).thenReturn(clientContext);
        when(clientContext.getSelectedPartyId()).thenReturn("");

        // when
        Optional<UUID> result = selectedPartyRetriever.getSelectedPartyId(pcsCase);

        // then
        assertFalse(result.isPresent());
    }

    @Test
    void getCurrentRepresentedPartyId_WithSingleDefendant_ReturnsPartyId() {
        // given
        UUID partyID = UUID.randomUUID();
        Party party = Party.builder()
            .build();

        List<ListValue<Party>> defendantList = new ArrayList<>();
        defendantList.add(ListValue.<Party>builder().value(party).id(partyID.toString()).build());

        PCSCase pcsCase =  PCSCase.builder().build();
        pcsCase.setAllDefendants(defendantList);

        // when
        Optional<UUID> result = selectedPartyRetriever.getCurrentRepresentedPartyId(pcsCase);

        // then
        assertTrue(result.isPresent());
        assertEquals(partyID, result.get());
        verify(clientContextRetriever, never()).getClientContext();
    }

    @Test
    void getCurrentRepresentedPartyId_WithMultipleDefendantAndNoCurrentPartyId_ReturnsOptional() {
        // given
        UUID partyID = UUID.randomUUID();
        Party party = Party.builder()
            .build();

        List<ListValue<Party>> defendantList = new ArrayList<>();
        defendantList.add(ListValue.<Party>builder().value(party).id(partyID.toString()).build());
        defendantList.add(ListValue.<Party>builder().value(party).build());

        PCSCase pcsCase = PCSCase.builder().build();
        pcsCase.setAllDefendants(defendantList);

        // when
        Optional<UUID> result = selectedPartyRetriever.getCurrentRepresentedPartyId(pcsCase);

        // then
        assertFalse(result.isPresent());
    }

    @Test
    void getCurrentRepresentedPartyId_WithMultipleDefendantAndCurrentPartyId_ReturnsPartyId() {
        // given
        UUID partyID = UUID.randomUUID();
        Party party = Party.builder()
            .build();

        List<ListValue<Party>> defendantList = new ArrayList<>();
        defendantList.add(ListValue.<Party>builder().value(party).id(partyID.toString()).build());
        defendantList.add(ListValue.<Party>builder().value(party).build());

        PCSCase pcsCase = PCSCase.builder()
            .currentRepresentedPartyId(partyID.toString())
            .allDefendants(defendantList)
            .build();

        // when
        Optional<UUID> result = selectedPartyRetriever.getCurrentRepresentedPartyId(pcsCase);

        // then
        assertTrue(result.isPresent());
        assertEquals(partyID, result.get());
    }
}
