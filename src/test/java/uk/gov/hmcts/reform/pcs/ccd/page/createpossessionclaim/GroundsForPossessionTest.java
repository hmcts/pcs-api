package uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.Event;
import uk.gov.hmcts.ccd.sdk.api.callback.MidEvent;
import uk.gov.hmcts.ccd.sdk.type.DynamicListElement;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.DiscretionaryGrounds;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.MandatoryGrounds;

import static org.assertj.core.api.Assertions.assertThat;

public class GroundsForPossessionTest extends BasePageTest {
    private Event<PCSCase, UserRole, State> event;

    @BeforeEach
    void setUp() {
        event = buildPageInTestEvent(new GroundsForPossession());
    }

    @Test
    void shouldPopulateMandatoryAndDiscretionaryGroundsOptions() {
        // Given
        CaseDetails<PCSCase, State> caseDetails = new CaseDetails<>();
        PCSCase caseData = PCSCase.builder()
            .discretionaryGroundsOptionsList(null)
            .mandatoryGroundsOptionsList(null)
            .build();

        caseDetails.setData(caseData);

        // When
        MidEvent<PCSCase, State> midEvent = getMidEventForPage(event, "groundsForPossession");
        midEvent.handle(caseDetails, null);

        // Then - lists should be populated
        assertThat(caseDetails.getData().getMandatoryGroundsOptionsList()).isNotNull();
        assertThat(caseDetails.getData().getDiscretionaryGroundsOptionsList()).isNotNull();

        assertThat(caseDetails.getData().getMandatoryGroundsOptionsList().getListItems())
            .isNotEmpty();
        assertThat(caseDetails.getData().getDiscretionaryGroundsOptionsList().getListItems())
            .isNotEmpty();

        // Verify that at least one label matches a known enum value
        assertThat(caseDetails.getData().getMandatoryGroundsOptionsList().getListItems()
                       .stream().map(DynamicListElement::getLabel).toList())
            .contains(MandatoryGrounds.values()[0].getLabel());

        assertThat(caseDetails.getData().getDiscretionaryGroundsOptionsList().getListItems()
                       .stream().map(DynamicListElement::getLabel).toList())
            .contains(DiscretionaryGrounds.values()[0].getLabel());
    }
}
