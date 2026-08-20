package uk.gov.hmcts.reform.pcs.ccd.event.caseworker.manageparty;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.type.DynamicListElement;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.caseworker.AddPartyDetails;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StartEventHandlerTest {

    private static final long TEST_CASE_REFERENCE = 1234L;

    @Mock
    private PcsCaseService pcsCaseService;
    @Mock
    private PartyService partyService;

    private StartEventHandler underTest;

    @BeforeEach
    void setUp() {
        underTest = new StartEventHandler(pcsCaseService, partyService);
    }

    @Test
    void shouldBuildPartyRadioList() {
        // Given
        PartyEntity claimantParty = PartyEntity.builder()
            .id(UUID.randomUUID()).nameKnown(VerticalYesNo.YES).build();
        PartyEntity defendantParty = PartyEntity.builder()
            .id(UUID.randomUUID()).nameKnown(VerticalYesNo.NO).build();
        PartyEntity litigationFriendParty = PartyEntity.builder().id(UUID.randomUUID()).build();

        ClaimEntity mainClaim = ClaimEntity.builder()
            .claimParties(List.of(
                ClaimPartyEntity.builder().party(claimantParty).role(PartyRole.CLAIMANT).build(),
                ClaimPartyEntity.builder().party(defendantParty).role(PartyRole.DEFENDANT).build(),
                ClaimPartyEntity.builder().party(litigationFriendParty).role(PartyRole.LITIGATION_FRIEND).build()
            ))
            .build();

        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder().claims(List.of(mainClaim)).build();
        when(pcsCaseService.loadCase(TEST_CASE_REFERENCE)).thenReturn(pcsCaseEntity);

        when(partyService.getPartyName(claimantParty)).thenReturn("Jane Doe");
        when(partyService.getPartyLabel(mainClaim, claimantParty.getId())).thenReturn("Claimant 1");
        when(partyService.getPartyLabel(mainClaim, defendantParty.getId())).thenReturn("Defendant 1");

        PCSCase caseData = PCSCase.builder().addPartyDetails(AddPartyDetails.builder().build()).build();
        EventPayload<PCSCase, State> eventPayload = new EventPayload<>(TEST_CASE_REFERENCE, caseData, null);

        // When
        PCSCase result = underTest.start(eventPayload);

        // Then
        List<DynamicListElement> listItems = result.getAddPartyDetails().getPartyRadioList().getListItems();
        assertThat(listItems).hasSize(2);
        assertThat(listItems.get(0).getLabel()).isEqualTo("Jane Doe - Claimant 1");
        assertThat(listItems.get(1).getLabel()).isEqualTo("Person unknown - Defendant 1");
        verify(partyService, never()).getPartyName(litigationFriendParty);
    }

}