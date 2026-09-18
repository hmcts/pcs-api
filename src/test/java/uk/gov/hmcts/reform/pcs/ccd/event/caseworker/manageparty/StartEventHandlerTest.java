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
import uk.gov.hmcts.reform.pcs.ccd.domain.caseworker.UpdatePartyDetails;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.caseworker.manageparty.RemovePartyService;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.service.caseworker.manageparty.RemovePartyService.LAST_PARTY_ERROR;

@ExtendWith(MockitoExtension.class)
class StartEventHandlerTest {

    private static final long TEST_CASE_REFERENCE = 1234L;

    @Mock
    private PcsCaseService pcsCaseService;
    @Mock
    private PartyService partyService;
    @Mock
    private RemovePartyService removePartyService;

    private StartEventHandler underTest;

    @BeforeEach
    void setUp() {
        underTest = new StartEventHandler(pcsCaseService, partyService, removePartyService);
    }

    @Test
    void shouldBuildPartyRadioList() {
        // Given
        PartyEntity claimantParty = PartyEntity.builder()
            .id(UUID.randomUUID()).firstName("Jane").lastName("Doe").nameKnown(VerticalYesNo.YES).build();
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
        when(partyService.getPartyName(defendantParty)).thenReturn("Person unknown");
        when(partyService.getPartyLabel(mainClaim, claimantParty.getId())).thenReturn("Claimant 1");
        when(partyService.getPartyLabel(mainClaim, defendantParty.getId())).thenReturn("Defendant 1");
        when(partyService.isActive(claimantParty)).thenReturn(true);
        when(partyService.isActive(defendantParty)).thenReturn(true);
        when(removePartyService.getActiveClaimantsAndDefendants(mainClaim))
            .thenReturn(mainClaim.getClaimParties().subList(0, 2));

        PCSCase caseData = PCSCase.builder()
            .addPartyDetails(AddPartyDetails.builder().build())
            .updatePartyDetails(UpdatePartyDetails.builder().build())
            .build();
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

    @Test
    void shouldInitialiseMissingManagePartyDetailsAndBuildRemovablePartyList() {
        // Given
        PartyEntity firstDefendant = PartyEntity.builder()
            .id(UUID.randomUUID()).firstName("Danny").lastName("Defendant").nameKnown(VerticalYesNo.YES).build();
        PartyEntity secondDefendant = PartyEntity.builder()
            .id(UUID.randomUUID()).firstName("Daisy").lastName("Defendant").nameKnown(VerticalYesNo.YES).build();
        ClaimPartyEntity firstClaimParty = ClaimPartyEntity.builder()
            .party(firstDefendant)
            .role(PartyRole.DEFENDANT)
            .build();
        ClaimPartyEntity secondClaimParty = ClaimPartyEntity.builder()
            .party(secondDefendant)
            .role(PartyRole.DEFENDANT)
            .build();
        ClaimEntity mainClaim = ClaimEntity.builder()
            .claimParties(List.of(firstClaimParty, secondClaimParty))
            .build();
        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder().claims(List.of(mainClaim)).build();

        when(pcsCaseService.loadCase(TEST_CASE_REFERENCE)).thenReturn(pcsCaseEntity);
        when(partyService.isActive(firstDefendant)).thenReturn(true);
        when(partyService.isActive(secondDefendant)).thenReturn(true);
        when(removePartyService.getActiveClaimantsAndDefendants(mainClaim))
            .thenReturn(List.of(firstClaimParty, secondClaimParty));
        when(removePartyService.canSelectForRemoval(firstClaimParty, List.of(firstClaimParty, secondClaimParty)))
            .thenReturn(true);
        when(removePartyService.canSelectForRemoval(secondClaimParty, List.of(firstClaimParty, secondClaimParty)))
            .thenReturn(true);
        when(partyService.getPartyName(firstDefendant)).thenReturn("Danny Defendant");
        when(partyService.getPartyName(secondDefendant)).thenReturn("Daisy Defendant");
        when(partyService.getPartyLabel(mainClaim, firstDefendant.getId())).thenReturn("Defendant 1");
        when(partyService.getPartyLabel(mainClaim, secondDefendant.getId())).thenReturn("Defendant 2");

        PCSCase caseData = PCSCase.builder().build();
        EventPayload<PCSCase, State> eventPayload = new EventPayload<>(TEST_CASE_REFERENCE, caseData, null);

        // When
        PCSCase result = underTest.start(eventPayload);

        // Then
        assertThat(result.getAddPartyDetails()).isNotNull();
        assertThat(result.getUpdatePartyDetails()).isNotNull();
        assertThat(result.getRemovePartyDetails()).isNotNull();
        assertThat(result.getRemovePartyDetails().getPartyToRemove().getListItems())
            .extracting(DynamicListElement::getLabel)
            .containsExactly("Danny Defendant - Defendant 1", "Daisy Defendant - Defendant 2");
    }

    @Test
    void shouldShowReasonAboveUnremovableClaimantWhenDefendantsCanBeSelected() {
        // Given
        PartyEntity claimant = PartyEntity.builder().id(UUID.randomUUID()).build();
        PartyEntity firstDefendant = PartyEntity.builder().id(UUID.randomUUID()).build();
        PartyEntity secondDefendant = PartyEntity.builder().id(UUID.randomUUID()).build();

        ClaimPartyEntity claimantClaimParty = claimParty(claimant, PartyRole.CLAIMANT);
        ClaimPartyEntity firstDefendantClaimParty = claimParty(firstDefendant, PartyRole.DEFENDANT);
        ClaimPartyEntity secondDefendantClaimParty = claimParty(secondDefendant, PartyRole.DEFENDANT);
        List<ClaimPartyEntity> activeClaimantsAndDefendants = List.of(
            claimantClaimParty, firstDefendantClaimParty, secondDefendantClaimParty);
        ClaimEntity mainClaim = ClaimEntity.builder().claimParties(activeClaimantsAndDefendants).build();
        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder().claims(List.of(mainClaim)).build();

        when(pcsCaseService.loadCase(TEST_CASE_REFERENCE)).thenReturn(pcsCaseEntity);
        when(partyService.isActive(claimant)).thenReturn(true);
        when(partyService.isActive(firstDefendant)).thenReturn(true);
        when(partyService.isActive(secondDefendant)).thenReturn(true);
        when(removePartyService.getActiveClaimantsAndDefendants(mainClaim))
            .thenReturn(activeClaimantsAndDefendants);
        when(removePartyService.canSelectForRemoval(claimantClaimParty, activeClaimantsAndDefendants))
            .thenReturn(false);
        when(removePartyService.canSelectForRemoval(firstDefendantClaimParty, activeClaimantsAndDefendants))
            .thenReturn(true);
        when(removePartyService.canSelectForRemoval(secondDefendantClaimParty, activeClaimantsAndDefendants))
            .thenReturn(true);
        when(partyService.getPartyName(claimant)).thenReturn("Jane Claimant");
        when(partyService.getPartyName(firstDefendant)).thenReturn("Danny Defendant");
        when(partyService.getPartyName(secondDefendant)).thenReturn("Daisy Defendant");
        when(partyService.getPartyLabel(mainClaim, claimant.getId())).thenReturn("Claimant 1");
        when(partyService.getPartyLabel(mainClaim, firstDefendant.getId())).thenReturn("Defendant 1");
        when(partyService.getPartyLabel(mainClaim, secondDefendant.getId())).thenReturn("Defendant 2");

        // When
        PCSCase result = underTest.start(new EventPayload<>(TEST_CASE_REFERENCE, PCSCase.builder().build(), null));

        // Then
        assertThat(result.getRemovePartyDetails().getPartyToRemove().getListItems())
            .extracting(DynamicListElement::getLabel)
            .containsExactly("Danny Defendant - Defendant 1", "Daisy Defendant - Defendant 2");
        assertThat(result.getRemovePartyDetails().getUnremovablePartyList()).isEqualTo("""
            %s

            Jane Claimant - Claimant 1
            """.formatted(LAST_PARTY_ERROR));
    }

    @Test
    void shouldShowReasonAboveUnremovableDefendantWhenClaimantsCanBeSelected() {
        // Given
        PartyEntity firstClaimant = PartyEntity.builder().id(UUID.randomUUID()).build();
        PartyEntity secondClaimant = PartyEntity.builder().id(UUID.randomUUID()).build();
        PartyEntity defendant = PartyEntity.builder().id(UUID.randomUUID()).build();

        ClaimPartyEntity firstClaimantClaimParty = claimParty(firstClaimant, PartyRole.CLAIMANT);
        ClaimPartyEntity secondClaimantClaimParty = claimParty(secondClaimant, PartyRole.CLAIMANT);
        ClaimPartyEntity defendantClaimParty = claimParty(defendant, PartyRole.DEFENDANT);
        List<ClaimPartyEntity> activeClaimantsAndDefendants = List.of(
            firstClaimantClaimParty, secondClaimantClaimParty, defendantClaimParty);
        ClaimEntity mainClaim = ClaimEntity.builder().claimParties(activeClaimantsAndDefendants).build();
        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder().claims(List.of(mainClaim)).build();

        when(pcsCaseService.loadCase(TEST_CASE_REFERENCE)).thenReturn(pcsCaseEntity);
        when(partyService.isActive(firstClaimant)).thenReturn(true);
        when(partyService.isActive(secondClaimant)).thenReturn(true);
        when(partyService.isActive(defendant)).thenReturn(true);
        when(removePartyService.getActiveClaimantsAndDefendants(mainClaim))
            .thenReturn(activeClaimantsAndDefendants);
        when(removePartyService.canSelectForRemoval(firstClaimantClaimParty, activeClaimantsAndDefendants))
            .thenReturn(true);
        when(removePartyService.canSelectForRemoval(secondClaimantClaimParty, activeClaimantsAndDefendants))
            .thenReturn(true);
        when(removePartyService.canSelectForRemoval(defendantClaimParty, activeClaimantsAndDefendants))
            .thenReturn(false);
        when(partyService.getPartyName(firstClaimant)).thenReturn("Jane Claimant");
        when(partyService.getPartyName(secondClaimant)).thenReturn("Jack Claimant");
        when(partyService.getPartyName(defendant)).thenReturn("Danny Defendant");
        when(partyService.getPartyLabel(mainClaim, firstClaimant.getId())).thenReturn("Claimant 1");
        when(partyService.getPartyLabel(mainClaim, secondClaimant.getId())).thenReturn("Claimant 2");
        when(partyService.getPartyLabel(mainClaim, defendant.getId())).thenReturn("Defendant 1");

        // When
        PCSCase result = underTest.start(new EventPayload<>(TEST_CASE_REFERENCE, PCSCase.builder().build(), null));

        // Then
        assertThat(result.getRemovePartyDetails().getPartyToRemove().getListItems())
            .extracting(DynamicListElement::getLabel)
            .containsExactly("Jane Claimant - Claimant 1", "Jack Claimant - Claimant 2");
        assertThat(result.getRemovePartyDetails().getUnremovablePartyList()).isEqualTo("""
            %s

            Danny Defendant - Defendant 1
            """.formatted(LAST_PARTY_ERROR));
    }

    private ClaimPartyEntity claimParty(PartyEntity partyEntity, PartyRole role) {
        return ClaimPartyEntity.builder()
            .party(partyEntity)
            .role(role)
            .build();
    }

}
