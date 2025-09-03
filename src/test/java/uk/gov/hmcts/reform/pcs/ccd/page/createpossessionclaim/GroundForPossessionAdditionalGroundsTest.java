package uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.api.Event;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;

import static org.assertj.core.api.Assertions.assertThat;

class GroundForPossessionAdditionalGroundsTest extends BasePageTest {

    private Event<PCSCase, UserRole, State> event;

    @BeforeEach
    void setUp() {
        event = buildPageInTestEvent(new GroundForPossessionAdditionalGrounds());
    }

    @Test
    void shouldBuildPageConfigurationSuccessfully() {
        // Given & When
        // The page is built in setUp() method

        // Then
        assertThat(event).isNotNull();
        assertThat(event.getFields()).isNotNull();
    }

    @Test
    void shouldNotHaveMidEventCallback() {
        // Given & When
        // The page is built in setUp() method

        // Then
        // Verify that there's no midEvent callback for this page
        // (since we removed it and moved the logic to GroundForPossessionRentArrears)
        var midEvents = event.getFields().getPagesToMidEvent();
        assertThat(midEvents).doesNotContainKey("groundForPossessionAdditionalGrounds");
    }

    @Test
    void shouldHaveCorrectEventConfiguration() {
        // Given & When
        // The page is built in setUp() method

        // Then
        assertThat(event).isNotNull();
        assertThat(event.getFields()).isNotNull();
    }
}
