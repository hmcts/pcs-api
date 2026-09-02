package uk.gov.hmcts.reform.pcs.ccd.event.caseworker.entercounterclaim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.DynamicList;
import uk.gov.hmcts.ccd.sdk.type.DynamicListElement;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.event.BaseEventTest;
import uk.gov.hmcts.reform.pcs.ccd.page.caseworker.entercounterclaim.CounterClaimAmount;
import uk.gov.hmcts.reform.pcs.ccd.page.caseworker.entercounterclaim.CourtPermission;
import uk.gov.hmcts.reform.pcs.ccd.page.caseworker.entercounterclaim.HelpWithFees;
import uk.gov.hmcts.reform.pcs.ccd.page.caseworker.entercounterclaim.PartyCounterClaimAgainst;
import uk.gov.hmcts.reform.pcs.ccd.page.caseworker.entercounterclaim.TypeOfCounterClaim;
import uk.gov.hmcts.reform.pcs.ccd.page.caseworker.entercounterclaim.UploadCounterClaimForm;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnterCounterClaimTest extends BaseEventTest {

    @Mock
    private PcsCaseService pcsCaseService;
    @Mock
    private PartyService partyService;
    @Mock
    private SubmitEventHandler submitEventHandler;
    @Mock
    private PcsCaseEntity pcsCaseEntity;
    @Mock
    private ClaimEntity claimEntity;
    @Mock
    private CourtPermission courtPermission;
    @Mock
    private TypeOfCounterClaim typeOfCounterClaim;
    @Mock
    private CounterClaimAmount counterClaimAmount;
    @Mock
    private HelpWithFees helpWithFees;
    @Mock
    private PartyCounterClaimAgainst partyCounterClaimAgainst;
    @Mock
    private UploadCounterClaimForm uploadCounterClaimForm;

    @BeforeEach
    void setUp() {
        EnterCounterClaim enterCounterClaim = new EnterCounterClaim(
            pcsCaseService, partyService, submitEventHandler,
            courtPermission, typeOfCounterClaim, counterClaimAmount, helpWithFees,
            partyCounterClaimAgainst, uploadCounterClaimForm);
        setEventUnderTest(enterCounterClaim);
    }

    @Test
    void shouldPopulateSubmittingPartyListFromDefendantsOnly() {
        // Given
        when(pcsCaseService.loadCase(TEST_CASE_REFERENCE)).thenReturn(pcsCaseEntity);
        when(pcsCaseEntity.getClaims()).thenReturn(List.of(claimEntity));

        DynamicList partyList = DynamicList.builder()
            .listItems(List.of(DynamicListElement.builder().code(UUID.randomUUID()).label("Jane Doe - Defendant 1")
                .build()))
            .build();
        when(partyService.buildPartyDynamicList(claimEntity, PartyRole.DEFENDANT))
            .thenReturn(partyList);

        PCSCase caseData = PCSCase.builder().build();

        // When
        PCSCase result = callStartHandler(caseData);

        // Then
        assertThat(result.getPartyRadioList()).isEqualTo(partyList);
    }
}
