package uk.gov.hmcts.reform.pcs.ccd.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;

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
}
