package uk.gov.hmcts.reform.pcs.ccd.event.caseworker.entergenapp;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.DynamicList;
import uk.gov.hmcts.ccd.sdk.type.DynamicListElement;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.caseworker.EnterGenAppRequest;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.event.BaseEventTest;
import uk.gov.hmcts.reform.pcs.ccd.page.caseworker.entergenapp.ApplicationDetails;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.genapp.GenAppService;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressFormatter;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppState.GEN_APP_ISSUED;

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
    @Mock
    private GenAppService genAppService;
    @Mock
    private AddressFormatter addressFormatter;

    @BeforeEach
    void setUp() {
        EnterGenApp enterGenApp = new EnterGenApp(pcsCaseService, partyService, genAppService, applicationDetails,
                                                  addressFormatter);
        setEventUnderTest(enterGenApp);
    }

    @Test
    void shouldPopulatePartyRadioListFromClaimantsAndDefendants() {
        // Given
        when(pcsCaseService.loadCase(TEST_CASE_REFERENCE)).thenReturn(pcsCaseEntity);
        when(pcsCaseEntity.getClaims()).thenReturn(List.of(claimEntity));

        DynamicList partyList = DynamicList.builder()
            .listItems(List.of(
                DynamicListElement.builder().code(UUID.randomUUID()).label("John Smith - Claimant 1").build(),
                DynamicListElement.builder().code(UUID.randomUUID()).label("Jane Doe - Defendant 1").build()))
            .build();
        when(partyService.buildPartyDynamicList(claimEntity, PartyRole.CLAIMANT, PartyRole.DEFENDANT))
            .thenReturn(partyList);

        PCSCase caseData = PCSCase.builder()
            .enterGenAppRequest(EnterGenAppRequest.builder().build())
            .build();

        // When
        PCSCase result = callStartHandler(caseData);

        // Then
        assertThat(result.getPartyRadioList()).isEqualTo(partyList);
    }

    @Test
    void shouldCreateGenAppEntityOnSubmit() {
        // Given
        UUID applicantPartyId = UUID.randomUUID();
        PartyEntity applicantParty = mock(PartyEntity.class);

        when(pcsCaseService.loadCase(TEST_CASE_REFERENCE)).thenReturn(pcsCaseEntity);
        when(partyService.getPartyEntityByEntityId(applicantPartyId, TEST_CASE_REFERENCE))
            .thenReturn(applicantParty);

        EnterGenAppRequest enterGenAppRequest = EnterGenAppRequest.builder().build();

        DynamicList partyRadioList = DynamicList.builder()
            .value(DynamicListElement.builder().code(applicantPartyId).build())
            .build();

        PCSCase caseData = PCSCase.builder()
            .enterGenAppRequest(enterGenAppRequest)
            .partyRadioList(partyRadioList)
            .caseNameHmctsInternal("Smith v Doe")
            .build();

        // When
        SubmitResponse<State> response = callSubmitHandler(caseData);

        // Then
        verify(genAppService)
            .createGenAppEntity(caseData, pcsCaseEntity, applicantParty, GEN_APP_ISSUED);
        assertThat(response.getConfirmationBody()).contains("Smith v Doe");
    }

}
