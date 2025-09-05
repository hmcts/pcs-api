package uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.Event;
import uk.gov.hmcts.ccd.sdk.api.callback.MidEvent;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.NoRentArrearsDiscretionaryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.NoRentArrearsMandatoryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class GroundsForPossessionTest extends BasePageTest {
    private Event<PCSCase, UserRole, State> event;

    @BeforeEach
    void setUp() {
        event = buildPageInTestEvent(new GroundsForPossession());
    }

    @Test
    void shouldClearMandatoryAndDiscretionaryGroundsOptions() {
        // Given: One mandatory and one discretionary is selected
        CaseDetails<PCSCase, State> caseDetails = new CaseDetails<>();
        PCSCase caseData = PCSCase.builder()
            .discretionaryGroundsOptionsList(Set.of(NoRentArrearsDiscretionaryGrounds.DOMESTIC_VIOLENCE))
            .mandatoryGroundsOptionsList(Set.of(NoRentArrearsMandatoryGrounds.ANTISOCIAL_BEHAVIOUR))
            .build();

        caseDetails.setData(caseData);

        // When: Mid event is executed
        MidEvent<PCSCase, State> midEvent = getMidEventForPage(event, "groundsForPossession");
        midEvent.handle(caseDetails, null);

        // Then: Set should be cleared
        assertThat(caseDetails.getData().getMandatoryGroundsOptionsList()).isEmpty();
        assertThat(caseDetails.getData().getDiscretionaryGroundsOptionsList()).isEmpty();

    }
}
