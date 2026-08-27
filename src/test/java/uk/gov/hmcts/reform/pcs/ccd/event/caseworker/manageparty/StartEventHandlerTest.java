package uk.gov.hmcts.reform.pcs.ccd.event.caseworker.manageparty;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.type.DynamicList;
import uk.gov.hmcts.ccd.sdk.type.DynamicListElement;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.caseworker.AddPartyDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.caseworker.UpdatePartyDetails;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
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
    void shouldBuildPartyRadioListFromDefendantsAndClaimants() {
        // Given
        ClaimEntity mainClaim = ClaimEntity.builder().claimParties(List.of()).build();
        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder().claims(List.of(mainClaim)).build();
        when(pcsCaseService.loadCase(TEST_CASE_REFERENCE)).thenReturn(pcsCaseEntity);

        DynamicList partyList = DynamicList.builder()
            .listItems(List.of(
                DynamicListElement.builder().code(UUID.randomUUID()).label("Jane Doe - Claimant 1").build(),
                DynamicListElement.builder().code(UUID.randomUUID()).label("John Smith - Defendant 1").build()))
            .build();
        when(partyService.buildPartyDynamicList(mainClaim, PartyRole.CLAIMANT, PartyRole.DEFENDANT))
            .thenReturn(partyList);

        PCSCase caseData = PCSCase.builder()
            .addPartyDetails(AddPartyDetails.builder().build())
            .updatePartyDetails(UpdatePartyDetails.builder().build())
            .build();
        EventPayload<PCSCase, State> eventPayload = new EventPayload<>(TEST_CASE_REFERENCE, caseData, null);

        // When
        PCSCase result = underTest.start(eventPayload);

        // Then
        assertThat(result.getAddPartyDetails().getPartyRadioList()).isEqualTo(partyList);
        assertThat(result.getUpdatePartyDetails().getPartyToUpdate()).isEqualTo(partyList);
    }

}
