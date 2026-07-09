package uk.gov.hmcts.reform.pcs.ccd.event.entergenapp;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.DynamicListElement;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.EnterGenAppRequest;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.event.BaseEventTest;
import uk.gov.hmcts.reform.pcs.ccd.page.entergenapp.ApplicationDetails;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnterGenAppTest extends BaseEventTest {

    @Mock
    private PcsCaseService pcsCaseService;
    @Mock
    private PartyService partyService;
    @Mock
    private PcsCaseEntity pcsCaseEntity;
    @Mock
    private ClaimEntity claimEntity;
    @Mock
    private ApplicationDetails applicationDetails;

    private EnterGenApp enterGenApp;

    @BeforeEach
    void setUp() {
        enterGenApp = new EnterGenApp(pcsCaseService, partyService, applicationDetails);
        setEventUnderTest(enterGenApp);
    }

    @Test
    void shouldBuildApplicantPartyListFromClaimantAndDefendants() {
        // Given
        UUID claimantId = UUID.randomUUID();
        UUID defendant1Id = UUID.randomUUID();
        UUID defendant2Id = UUID.randomUUID();
        UUID underlesseeId = UUID.randomUUID();

        PartyEntity claimant = PartyEntity.builder()
            .id(claimantId)
            .firstName("John")
            .lastName("Smith")
            .nameKnown(VerticalYesNo.YES)
            .build();

        PartyEntity defendant1 = PartyEntity.builder()
            .id(defendant1Id)
            .firstName("Jane")
            .lastName("Doe")
            .nameKnown(VerticalYesNo.YES)
            .build();

        PartyEntity defendant2 = PartyEntity.builder()
            .id(defendant2Id)
            .nameKnown(VerticalYesNo.NO)
            .build();

        PartyEntity underlesseeOrMortgagee = PartyEntity.builder()
            .id(underlesseeId)
            .orgName("Bank Ltd")
            .build();

        when(pcsCaseService.loadCase(TEST_CASE_REFERENCE)).thenReturn(pcsCaseEntity);
        when(pcsCaseEntity.getClaims()).thenReturn(List.of(claimEntity));
        when(claimEntity.getClaimParties()).thenReturn(List.of(
            ClaimPartyEntity.builder().party(claimant).role(PartyRole.CLAIMANT).rank(1).build(),
            ClaimPartyEntity.builder().party(defendant1).role(PartyRole.DEFENDANT).rank(1).build(),
            ClaimPartyEntity.builder().party(defendant2).role(PartyRole.DEFENDANT).rank(2).build(),
            ClaimPartyEntity.builder().party(underlesseeOrMortgagee).role(PartyRole.UNDERLESSEE_OR_MORTGAGEE)
                .rank(1).build()
        ));

        when(partyService.getPartyName(claimant)).thenReturn("John Smith");
        when(partyService.getPartyName(defendant1)).thenReturn("Jane Doe");
        when(partyService.getPartyLabel(claimEntity, claimantId)).thenReturn("Claimant 1");
        when(partyService.getPartyLabel(claimEntity, defendant1Id)).thenReturn("Defendant 1");
        when(partyService.getPartyLabel(claimEntity, defendant2Id)).thenReturn("Defendant 2");

        PCSCase caseData = PCSCase.builder()
            .enterGenAppRequest(EnterGenAppRequest.builder().build())
            .build();

        // When
        PCSCase result = callStartHandler(caseData);

        // Then
        List<DynamicListElement> listItems = result.getEnterGenAppRequest().getApplicantParty().getListItems();

        assertThat(listItems).containsExactly(
            DynamicListElement.builder().code(claimantId).label("John Smith - Claimant 1").build(),
            DynamicListElement.builder().code(defendant1Id).label("Jane Doe - Defendant 1").build(),
            DynamicListElement.builder().code(defendant2Id).label("Person unknown - Defendant 2").build()
        );
    }

}
