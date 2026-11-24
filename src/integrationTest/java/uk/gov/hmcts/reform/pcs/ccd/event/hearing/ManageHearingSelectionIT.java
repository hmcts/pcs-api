package uk.gov.hmcts.reform.pcs.ccd.event.hearing;

import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;
import uk.gov.hmcts.ccd.sdk.ConfigBuilderImpl;
import uk.gov.hmcts.ccd.sdk.ResolvedCCDConfig;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.api.callback.MidEvent;
import uk.gov.hmcts.ccd.sdk.testing.CcdEventTestSupport;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.hearing.Hearing;
import uk.gov.hmcts.reform.pcs.ccd.domain.hearing.ManageHearingOption;
import uk.gov.hmcts.reform.pcs.ccd.entity.hearing.HearingEntity;
import uk.gov.hmcts.reform.pcs.ccd.page.managehearing.ManageHearingPage;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.hearing.HearingService;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringListElement;
import uk.gov.hmcts.reform.pcs.config.AbstractPostgresContainerIT;
import uk.gov.hmcts.reform.pcs.config.IssuedCases;
import uk.gov.hmcts.reform.pcs.config.PcsCcdEventTest;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** The existing manage-hearing page selects any hearing; persistence changes only that hearing. */
@PcsCcdEventTest
class ManageHearingSelectionIT extends AbstractPostgresContainerIT {

    @Autowired
    private IssuedCases cases;
    @Autowired
    private HearingService hearings;
    @Autowired
    private PcsCaseService pcsCases;
    @Autowired
    private ManageHearingPage page;
    @Autowired
    private TransactionTemplate transaction;
    @Autowired
    private CcdEventTestSupport<PCSCase, State> events;

    private CcdEventTestSupport.Actor staff;

    private long reference;
    private int earlier;
    private int later;
    private int cancelled;
    private PCSCase form;

    @BeforeEach
    void setUp() {
        reference = cases.issue();
        earlier = cases.hearing(reference, LocalDateTime.of(2030, 1, 2, 10, 0), false);
        later = cases.hearing(reference, LocalDateTime.of(2030, 2, 3, 14, 30), false);
        cancelled = cases.hearing(reference, LocalDateTime.of(2030, 3, 4, 10, 0), true);
        staff = events.registerActor("Hearing", "Administrator", "caseworker-pcs");
        form = events.start(reference, "manageHearing").as(staff).startExpectingSuccess().caseData();
    }

    @Test
    void listsEveryAvailableHearingAndAllowsMoreToBeAdded() {
        assertThat(form.getHearingSelection().getValue()).isNull();
        assertThat(form.getHearingSelection().getListItems()).extracting(DynamicStringListElement::getCode)
            .containsExactly(String.valueOf(earlier), String.valueOf(later));
        assertThat(form.getHearingSelection().getListItems()).extracting(DynamicStringListElement::getLabel)
            .containsExactly("Possession first hearing — 2 January 2030 at 10:00am",
                             "Possession first hearing — 3 February 2030 at 2:30pm");

        select(later, ManageHearingOption.EDIT);
        // Adding reuses the existing form and creates a separate record on every submission.
        form.setManageHearingOption(ManageHearingOption.ADD);
        for (int i = 0; i < 5; i++) {
            assertThat(events.event(reference, "manageHearing", form).as(staff).submitExpectingSuccess()
                .changes("hearing")).hasSize(1);
        }

        assertThat(events.start(reference, "manageHearing").as(staff).startExpectingSuccess()
            .caseData().getHearingSelection().getListItems()).hasSize(7);
        int hearingCount = transaction.execute(status -> pcsCases.loadCase(reference).getHearings().size());
        assertThat(hearingCount).isEqualTo(8);
    }

    @Test
    void editsALaterHearingWithoutChangingTheNextHearing() {
        select(later, ManageHearingOption.EDIT);
        assertThat(form.getHearing().getDate()).isEqualTo(LocalDateTime.of(2030, 2, 3, 14, 30));
        form.getHearing().setNotes("Updated later hearing");
        var edited = events.event(reference, "manageHearing", form).as(staff).submitExpectingSuccess();
        assertThat(edited.changes("hearing")).singleElement()
            .satisfies(change -> assertThat(change.newValues().get("id").asInt()).isEqualTo(later));

        assertThat(stored(later).getNotes()).isEqualTo("Updated later hearing");
        assertThat(stored(earlier).getNotes()).isNull();
        assertThat(stored(cancelled).getCancelled()).isTrue();
    }

    @Test
    void switchingHearingsDoesNotCarryThePreviousHearingsDraftIntoTheNewSelection() {
        select(later, ManageHearingOption.EDIT);
        form.getHearing().setNotes("Draft for later hearing");
        hearings.storeDraftHearingForm(form);
        select(later, ManageHearingOption.EDIT);
        assertThat(form.getHearing().getNotes()).isEqualTo("Draft for later hearing");

        select(earlier, ManageHearingOption.EDIT);
        assertThat(form.getHearing().getHearingId()).isEqualTo(earlier);
        assertThat(form.getHearing().getNotes()).isNull();
        select(earlier, ManageHearingOption.EDIT);
        assertThat(form.getHearing().getNotes()).isNull();
        assertThat(form.getHearing().getHearingId()).isEqualTo(earlier);
        assertThat(form.getHearing().getHearingSummaryMarkdown()).contains("2 January 2030");
    }

    @Test
    void cancelsTheChosenHearingWithoutCancellingTheNextHearing() {
        form.setHearingSummaryLocation("Central London County Court");
        select(later, ManageHearingOption.CANCEL);
        assertThat(form.getHearing().getHearingSummaryMarkdown())
            .contains("Central London County Court", "3 February 2030");
        form.getHearing().setCancellationReason("No longer required");
        var cancelledEvent = events.event(reference, "manageHearing", form).as(staff).submitExpectingSuccess();
        assertThat(cancelledEvent.changes("hearing")).singleElement()
            .satisfies(change -> assertThat(change.newValues().get("id").asInt()).isEqualTo(later));

        assertThat(stored(later).getCancelled()).isTrue();
        assertThat(stored(later).getCancellationReason()).isEqualTo("No longer required");
        assertThat(stored(earlier).getCancelled()).isFalse();
        assertThat(events.start(reference, "manageHearing").as(staff).startExpectingSuccess()
            .caseData().getHearingSelection().getValueCode()).isEqualTo(String.valueOf(earlier));
    }

    @Test
    void rejectsCancelledMissingAndOtherCasesHearings() {
        int foreign = cases.hearing(cases.issue(), LocalDateTime.of(2030, 1, 1, 10, 0), false);
        for (String id : new String[] {null, String.valueOf(cancelled), String.valueOf(foreign)}) {
            form.getHearingSelection().setValue(DynamicStringListElement.builder().code(id).build());
            form.setManageHearingOption(ManageHearingOption.EDIT);
            assertThat(continuePage().getErrors()).containsExactly("Select an available hearing on this case");
        }
        for (String id : new String[] {null, String.valueOf(cancelled), String.valueOf(foreign), "2147483647"}) {
            for (ManageHearingOption option : new ManageHearingOption[] {
                ManageHearingOption.EDIT, ManageHearingOption.CANCEL}) {
                form.setSelectedHearingId(id);
                form.setManageHearingOption(option);
                assertThat(events.event(reference, "manageHearing", form).as(staff).submitExpectingErrors().errors())
                    .containsExactly("Select an available hearing on this case");
            }
        }
        assertThat(events.history(reference)).isEmpty();
        assertThat(stored(earlier).getCancelled()).isFalse();
    }

    @Test
    void rejectsAHearingCancelledAfterItWasSelected() {
        select(later, ManageHearingOption.EDIT);
        PCSCase cancellation = events.start(reference, "manageHearing").as(staff)
            .startExpectingSuccess().caseData();
        cancellation.setSelectedHearingId(String.valueOf(later));
        cancellation.setManageHearingOption(ManageHearingOption.CANCEL);
        cancellation.setHearing(Hearing.builder().cancellationReason("Cancelled by another user").build());
        events.event(reference, "manageHearing", cancellation).as(staff).submitExpectingSuccess();

        assertThat(events.event(reference, "manageHearing", form).as(staff).submitExpectingErrors().errors())
            .containsExactly("Select an available hearing on this case");
        assertThat(events.history(reference)).hasSize(1);
    }

    private void select(int id, ManageHearingOption option) {
        form.setManageHearingOption(option);
        form.getHearingSelection().setValue(DynamicStringListElement.builder().code(String.valueOf(id)).build());
        assertThat(continuePage().getErrors()).isNullOrEmpty();
    }

    private HearingEntity stored(int id) {
        return transaction.execute(status -> pcsCases.loadCase(reference).getHearings().stream()
            .filter(hearing -> hearing.getId() == id).findFirst().orElseThrow());
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private AboutToStartOrSubmitResponse<PCSCase, State> continuePage() {
        var config = new ConfigBuilderImpl<>(new ResolvedCCDConfig<>(
            PCSCase.class, State.class, UserRole.class, Map.of(), ImmutableSet.of()));
        var event = config.decentralisedEvent("manageHearing", null).forAllStates();
        page.addTo(new PageBuilder(event));
        MidEvent<PCSCase, State> callback = config.build().getEvents().get("manageHearing")
            .getFields().getPagesToMidEvent().get(page.getPageKey());
        return transaction.execute(status -> callback.handle(
            CaseDetails.<PCSCase, State>builder().id(reference).data(form).build(), null));
    }
}
