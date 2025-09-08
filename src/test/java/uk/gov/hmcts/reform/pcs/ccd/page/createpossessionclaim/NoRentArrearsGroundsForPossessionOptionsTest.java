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

public class NoRentArrearsGroundsForPossessionOptionsTest extends BasePageTest {
    private Event<PCSCase, UserRole, State> event;

    @BeforeEach
    void setUp() {
        event = buildPageInTestEvent(new NoRentArrearsGroundsForPossessionOptions());
    }

    @Test
    void shouldMapSelectedGroundsToEnums() {
        // Given: Mandatory and Discretionary are set
        CaseDetails<PCSCase, State> caseDetails = new CaseDetails<>();
        Set<NoRentArrearsMandatoryGrounds> expectedMandatory = Set.of(
            NoRentArrearsMandatoryGrounds.ANTISOCIAL_BEHAVIOUR,
            NoRentArrearsMandatoryGrounds.DEATH_OF_TENANT,
            NoRentArrearsMandatoryGrounds.SERIOUS_RENT_ARREARS);
        Set<NoRentArrearsDiscretionaryGrounds> expectedDiscretionary = Set.of(
            NoRentArrearsDiscretionaryGrounds.DOMESTIC_VIOLENCE,
            NoRentArrearsDiscretionaryGrounds.LANDLORD_EMPLOYEE,
            NoRentArrearsDiscretionaryGrounds.FALSE_STATEMENT);
        PCSCase caseData = PCSCase.builder()
                .noRentArrearsDiscretionaryGroundsOptions(expectedDiscretionary)
                .noRentArrearsMandatoryGroundsOptions(expectedMandatory)
                .build();

        caseDetails.setData(caseData);

        // When: Mid event is executed
        MidEvent<PCSCase, State> midEvent = getMidEventForPage(event, "noRentArrearsGroundsForPossessionOptions");
        midEvent.handle(caseDetails, null);

        // Then: Mandatory and Discretionary enum should exist in each set
        Set<NoRentArrearsMandatoryGrounds> selectedMandatory =
                caseDetails.getData().getNoRentArrearsMandatoryGroundsOptions();
        Set<NoRentArrearsDiscretionaryGrounds> selectedDiscretionary =
                caseDetails.getData().getNoRentArrearsDiscretionaryGroundsOptions();

        assertThat(selectedMandatory).containsExactlyInAnyOrderElementsOf(expectedMandatory);
        assertThat(selectedDiscretionary).containsExactlyInAnyOrderElementsOf(expectedDiscretionary);
    }
}
