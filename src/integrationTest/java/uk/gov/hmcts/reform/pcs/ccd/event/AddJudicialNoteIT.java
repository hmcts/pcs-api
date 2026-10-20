package uk.gov.hmcts.reform.pcs.ccd.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import uk.gov.hmcts.ccd.sdk.testing.CcdEventTestSupport;
import uk.gov.hmcts.ccd.sdk.testing.CcdEventTestSupport.Actor;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.config.AbstractPostgresContainerIT;
import uk.gov.hmcts.reform.pcs.config.IssuedCases;
import uk.gov.hmcts.reform.pcs.config.PcsCcdEventTest;
import uk.gov.hmcts.reform.pcs.service.FeatureFlag;
import uk.gov.hmcts.reform.pcs.service.FeatureToggleService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.addJudicialNote;

/**
 * Judges add judicial notes and read them in the judicial notes tab, only while make order is enabled.
 */
@DisplayName("Add a judicial note")
@PcsCcdEventTest
class AddJudicialNoteIT extends AbstractPostgresContainerIT {

    @Autowired
    private CcdEventTestSupport<PCSCase, State> events;
    @Autowired
    private IssuedCases cases;
    @MockitoSpyBean
    private FeatureToggleService featureToggleService;

    private long caseReference;
    private Actor judge;

    @BeforeEach
    void setUp() {
        caseReference = cases.issue();
        judge = events.registerActor("A", "Judge", "caseworker-pcs");
        makeOrderEnabled();
    }

    @Test
    @DisplayName("shows the judge the notes added to the case")
    void showsTheNotesAdded() {
        addNote("The rent schedule is incomplete").submitExpectingSuccess();

        assertThat(events.view(caseReference, judge).getJudicialNotesMarkdown())
            .contains("The rent schedule is incomplete");
    }

    @Test
    @DisplayName("refuses a note while make order is disabled")
    void refusesANoteWhileDisabled() {
        makeOrderDisabled();

        assertThat(addNote("Should not be saved").submitExpectingErrors().errors())
            .containsExactly("Judicial notes are not available");

        makeOrderEnabled();
        assertThat(events.view(caseReference, judge).getJudicialNotesMarkdown())
            .doesNotContain("Should not be saved");
    }

    @Test
    @DisplayName("does not render the notes while make order is disabled")
    void doesNotRenderNotesWhileDisabled() {
        addNote("Added while enabled").submitExpectingSuccess();

        makeOrderDisabled();

        assertThat(events.view(caseReference, judge).getJudicialNotesMarkdown()).isNull();
    }

    private CcdEventTestSupport<PCSCase, State>.EventSubmission addNote(String note) {
        return events.event(caseReference, addJudicialNote.name(), PCSCase.builder().judicialNote(note).build())
            .as(judge);
    }

    private void makeOrderDisabled() {
        doReturn(false).when(featureToggleService).isEnabled(FeatureFlag.MAKE_ORDER);
    }

    private void makeOrderEnabled() {
        doReturn(true).when(featureToggleService).isEnabled(FeatureFlag.MAKE_ORDER);
    }
}
