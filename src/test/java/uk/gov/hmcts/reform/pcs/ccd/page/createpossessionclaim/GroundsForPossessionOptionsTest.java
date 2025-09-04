package uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.Event;
import uk.gov.hmcts.ccd.sdk.api.callback.MidEvent;
import uk.gov.hmcts.ccd.sdk.type.DynamicListElement;
import uk.gov.hmcts.ccd.sdk.type.DynamicMultiSelectList;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.DiscretionaryGrounds;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.MandatoryGrounds;

import java.util.List;
import java.util.Set;
import java.util.UUID;

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
        MandatoryGrounds mandatoryGround = MandatoryGrounds.values()[0];
        DiscretionaryGrounds discretionaryGround = DiscretionaryGrounds.values()[0];

        // Build DynamicMultiSelectList with selected values
        DynamicListElement mandatoryItem = DynamicListElement.builder()
            .code(UUID.randomUUID())
            .label(mandatoryGround.getLabel())
            .build();

        DynamicMultiSelectList mandatoryList = DynamicMultiSelectList.builder()
            .value(List.of(mandatoryItem))
            .listItems(List.of(mandatoryItem))
            .build();


        DynamicListElement discretionaryItem = DynamicListElement.builder()
            .code(UUID.randomUUID())
            .label(discretionaryGround.getLabel())
            .build();

        DynamicMultiSelectList discretionaryList = DynamicMultiSelectList.builder()
            .value(List.of(discretionaryItem))
            .listItems(List.of(discretionaryItem))
            .build();

        PCSCase caseData = PCSCase.builder()
            .mandatoryGroundsOptionsList(mandatoryList)
            .discretionaryGroundsOptionsList(discretionaryList)
            .build();

        CaseDetails<PCSCase, State> caseDetails = new CaseDetails<>();
        caseDetails.setData(caseData);

        // When
        MidEvent<PCSCase, State> midEvent = getMidEventForPage(event, "groundsForPossessionOptions");
        midEvent.handle(caseDetails, null);

        // Then
        Set<MandatoryGrounds> selectedMandatory = caseDetails.getData().getSelectedMandatoryGrounds();
        Set<DiscretionaryGrounds> selectedDiscretionary = caseDetails.getData().getSelectedDiscretionaryGrounds();

        assertThat(selectedMandatory).containsExactly(mandatoryGround);
        assertThat(selectedDiscretionary).containsExactly(discretionaryGround);
    }
}
