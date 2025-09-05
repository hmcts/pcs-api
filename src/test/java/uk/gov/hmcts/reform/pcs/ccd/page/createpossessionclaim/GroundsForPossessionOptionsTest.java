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

public class GroundsForPossessionOptionsTest extends BasePageTest {
    private Event<PCSCase, UserRole, State> event;

    @BeforeEach
    void setUp() {
        event = buildPageInTestEvent(new GroundsForPossessionOptions());
    }

    @Test
    void shouldMapSelectedGroundsToEnums() {
        // Given: pick one mandatory and one discretionary enum

        CaseDetails<PCSCase, State> caseDetails = new CaseDetails<>();
        NoRentArrearsMandatoryGrounds dummyMandatory = NoRentArrearsMandatoryGrounds.ANTISOCIAL_BEHAVIOUR;
        NoRentArrearsDiscretionaryGrounds dummyDiscretionary = NoRentArrearsDiscretionaryGrounds.DOMESTIC_VIOLENCE;
        PCSCase caseData = PCSCase.builder()
                .discretionaryGroundsOptionsList(Set.of(dummyDiscretionary))
                .mandatoryGroundsOptionsList(Set.of(dummyMandatory))
                .build();

        caseDetails.setData(caseData);

        // When
        MidEvent<PCSCase, State> midEvent = getMidEventForPage(event, "groundsForPossessionOptions");
        midEvent.handle(caseDetails, null);

        // Then
        Set<NoRentArrearsMandatoryGrounds> selectedMandatory = caseDetails.getData().getSelectedNoRentArrearsMandatoryGrounds();
        Set<NoRentArrearsDiscretionaryGrounds> selectedDiscretionary = caseDetails.getData().getSelectedNoRentArrearsDiscretionaryGrounds();

        assertThat(selectedMandatory).containsExactly(dummyMandatory);
        assertThat(selectedDiscretionary).containsExactly(dummyDiscretionary);
    }
}
