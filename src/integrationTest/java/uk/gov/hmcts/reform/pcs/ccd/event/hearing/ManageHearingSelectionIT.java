package uk.gov.hmcts.reform.pcs.ccd.event.hearing;

import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.support.TransactionTemplate;
import uk.gov.hmcts.ccd.sdk.ConfigBuilderImpl;
import uk.gov.hmcts.ccd.sdk.ResolvedCCDConfig;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.api.callback.MidEvent;
import uk.gov.hmcts.ccd.sdk.testing.CcdEventTestSupport;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.hearing.Hearing;
import uk.gov.hmcts.reform.pcs.ccd.domain.hearing.ManageHearingOption;
import uk.gov.hmcts.reform.pcs.ccd.entity.hearing.HearingEntity;
import uk.gov.hmcts.reform.pcs.ccd.page.managehearing.CancelHearingPage;
import uk.gov.hmcts.reform.pcs.ccd.page.managehearing.ManageHearingPage;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.hearing.HearingService;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringList;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringListElement;
import uk.gov.hmcts.reform.pcs.config.AbstractPostgresContainerIT;
import uk.gov.hmcts.reform.pcs.config.IssuedCases;
import uk.gov.hmcts.reform.pcs.config.PcsCcdEventTest;
import uk.gov.hmcts.reform.pcs.service.FeatureFlag;
import uk.gov.hmcts.reform.pcs.service.FeatureToggleService;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.doReturn;

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
    private CancelHearingPage cancelPage;
    @Autowired
    private TransactionTemplate transaction;
    @Autowired
    private CcdEventTestSupport<PCSCase, State> events;

    @MockitoSpyBean
    private FeatureToggleService featureToggleService;

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
    void flagOffEditsAndCancelsTheNextHearingWithoutAPicker() {
        doReturn(false).when(featureToggleService).isEnabled(FeatureFlag.MAKE_ORDER);
        form = events.start(reference, "manageHearing").as(staff).startExpectingSuccess().caseData();
        assertThat(form.getHearingSelection()).isNull();
        form.setManageHearingOption(ManageHearingOption.EDIT);
        assertThat(continuePage().getErrors()).isNullOrEmpty();
        assertThat(form.getHearing().getHearingId()).isEqualTo(earlier);
        form.getHearing().setNotes("Next hearing edited");
        events.event(reference, "manageHearing", form).as(staff).submitExpectingSuccess();
        assertThat(stored(earlier).getNotes()).isEqualTo("Next hearing edited");

        form = events.start(reference, "manageHearing").as(staff).startExpectingSuccess().caseData();
        form.setManageHearingOption(ManageHearingOption.CANCEL);
        assertThat(continuePage().getErrors()).isNullOrEmpty();
        assertThat(form.getHearing().getHearingSummaryMarkdown())
            .contains("2 January 2030", "You’re viewing the next scheduled hearing for this case.");
        form.getHearing().setCancellationReason("No longer required");
        events.event(reference, "manageHearing", form).as(staff).submitExpectingSuccess();
        assertThat(stored(earlier).getCancelled()).isTrue();
        assertThat(stored(earlier).getCancellationReason()).isEqualTo("No longer required");
        assertThat(stored(earlier).getCaseManagementCancellationReasonKey()).isNull();
        assertThat(stored(later).getCancelled()).isFalse();
    }

    @Test
    void flagOffSubmissionChangesTheHearingStaffWereShownEvenIfTheNextHearingChanges() {
        doReturn(false).when(featureToggleService).isEnabled(FeatureFlag.MAKE_ORDER);
        form = events.start(reference, "manageHearing").as(staff).startExpectingSuccess().caseData();
        form.setManageHearingOption(ManageHearingOption.EDIT);
        assertThat(continuePage().getErrors()).isNullOrEmpty();
        assertThat(form.getHearing().getHearingId()).isEqualTo(earlier);

        // Another user lists a sooner hearing while this edit is open, as master allowed.
        int sooner = cases.hearing(reference, LocalDateTime.of(2029, 12, 1, 10, 0), false);
        form.getHearing().setNotes("Edited after a sooner hearing was listed");
        events.event(reference, "manageHearing", form).as(staff).submitExpectingSuccess();

        assertThat(stored(earlier).getNotes()).isEqualTo("Edited after a sooner hearing was listed");
        assertThat(stored(sooner).getNotes()).isNull();
    }

    @Test
    void disablingTheFlagAfterSelectionChangesOnlyTheChosenHearing() {
        select(later, ManageHearingOption.EDIT);
        doReturn(false).when(featureToggleService).isEnabled(FeatureFlag.MAKE_ORDER);
        form.getHearing().setNotes("Chosen before the flag was disabled");
        events.event(reference, "manageHearing", form).as(staff).submitExpectingSuccess();

        assertThat(stored(later).getNotes()).isEqualTo("Chosen before the flag was disabled");
        assertThat(stored(earlier).getNotes()).isNull();
    }

    @Test
    void cancellationOpenedWithThePreviousDefinitionCancelsOnlyAHearingOnTheCase() {
        doReturn(false).when(featureToggleService).isEnabled(FeatureFlag.MAKE_ORDER);
        form = events.start(reference, "manageHearing").as(staff).startExpectingSuccess().caseData();
        int foreign = cases.hearing(cases.issue(), LocalDateTime.of(2030, 1, 1, 10, 0), false);
        form.setManageHearingOption(ManageHearingOption.CANCEL);
        form.setHearingSelection(null);
        for (int id : new int[] {cancelled, foreign}) {
            form.setHearing(Hearing.builder().hearingId(id).cancellationReason("No longer required").build());
            assertThat(events.event(reference, "manageHearing", form).as(staff).submitExpectingErrors().errors())
                .containsExactly("Select an available hearing on this case");
        }
        assertThat(events.history(reference)).isEmpty();

        form.setHearing(Hearing.builder().hearingId(earlier).cancellationReason("No longer required").build());
        events.event(reference, "manageHearing", form).as(staff).submitExpectingSuccess();
        assertThat(stored(earlier).getCancelled()).isTrue();
        assertThat(stored(later).getCancelled()).isFalse();
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
            .contains("Central London County Court", "3 February 2030",
                "You’re viewing the selected hearing for this case.");
        var reasonSelection = form.getHearing().getCancellationReasonSelection();
        assertThat(reasonSelection.getValue()).isNull();
        assertThat(reasonSelection.getListItems())
            .extracting(DynamicStringListElement::getCode, DynamicStringListElement::getLabel)
            .contains(tuple("jodir", "Judicial direction"), tuple("settled", "Settled"));
        reasonSelection.setValue(new DynamicStringListElement("settled", "untrusted label"));
        assertThat(continueCancellationPage().getErrors()).isNullOrEmpty();
        assertThat(reasonSelection.getValue().getLabel()).isEqualTo("Settled");

        var cancelledEvent = events.event(reference, "manageHearing", form).as(staff).submitExpectingSuccess();
        assertThat(cancelledEvent.changes("hearing")).singleElement().satisfies(change -> {
            assertThat(change.newValues().get("id").asInt()).isEqualTo(later);
            assertThat(change.newValues().get("case_management_cancellation_reason_key").asText())
                .isEqualTo("settled");
        });

        assertThat(stored(later).getCancelled()).isTrue();
        assertThat(stored(later).getCaseManagementCancellationReasonKey()).isEqualTo("settled");
        assertThat(stored(earlier).getCancelled()).isFalse();
        assertThat(events.start(reference, "manageHearing").as(staff).startExpectingSuccess()
            .caseData().getHearingSelection().getValueCode()).isEqualTo(String.valueOf(earlier));
    }

    @Test
    void rejectsMissingAndUnknownCancellationReasons() {
        select(later, ManageHearingOption.CANCEL);
        var selection = form.getHearing().getCancellationReasonSelection();

        for (String code : new String[] {null, "not-a-cancellation-reason"}) {
            selection.setValue(code == null ? null : new DynamicStringListElement(code, "Settled"));
            assertThat(continueCancellationPage().getErrors())
                .containsExactly("Select a valid reason for cancellation");
            assertThat(events.event(reference, "manageHearing", form).as(staff).submitExpectingErrors().errors())
                .containsExactly("Select a valid reason for cancellation");
        }

        assertThat(stored(later).getCancelled()).isFalse();
        assertThat(events.history(reference)).isEmpty();
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
    void pastHearingsAreNotOfferedAndCannotBeEditedOrCancelled() {
        int past = cases.hearing(reference, LocalDateTime.of(2020, 5, 6, 10, 0), false);
        form = events.start(reference, "manageHearing").as(staff).startExpectingSuccess().caseData();
        assertThat(form.getHearingSelection().getListItems()).extracting(DynamicStringListElement::getCode)
            .containsExactly(String.valueOf(earlier), String.valueOf(later));

        form.getHearingSelection().setValue(DynamicStringListElement.builder().code(String.valueOf(past)).build());
        form.setManageHearingOption(ManageHearingOption.EDIT);
        assertThat(continuePage().getErrors()).containsExactly("Select an available hearing on this case");

        form.setHearing(Hearing.builder().notes("Changed").cancellationReason("No longer required").build());
        form.setSelectedHearingId(String.valueOf(past));
        for (ManageHearingOption option : new ManageHearingOption[] {
            ManageHearingOption.EDIT, ManageHearingOption.CANCEL}) {
            form.setManageHearingOption(option);
            assertThat(events.event(reference, "manageHearing", form).as(staff).submitExpectingErrors().errors())
                .containsExactly("Select an available hearing on this case");
        }
        assertThat(events.history(reference)).isEmpty();
        assertThat(stored(past).getCancelled()).isFalse();
    }

    @Test
    void caseWithOnlyPastHearingsGoesStraightToAddingAHearing() {
        long pastOnly = cases.issue();
        cases.hearing(pastOnly, LocalDateTime.of(2020, 5, 6, 10, 0), false);
        PCSCase started = events.start(pastOnly, "manageHearing").as(staff).startExpectingSuccess().caseData();

        assertThat(started.getHearingSelection().getListItems()).isEmpty();
        assertThat(started.getShowManageHearingPage()).isEqualTo(VerticalYesNo.NO);
        assertThat(started.getManageHearingOption()).isEqualTo(ManageHearingOption.ADD);
    }

    @Test
    void rejectsAHearingCancelledAfterItWasSelected() {
        select(later, ManageHearingOption.EDIT);
        PCSCase cancellation = events.start(reference, "manageHearing").as(staff)
            .startExpectingSuccess().caseData();
        cancellation.setSelectedHearingId(String.valueOf(later));
        cancellation.setManageHearingOption(ManageHearingOption.CANCEL);
        cancellation.setHearing(Hearing.builder().cancellationReasonSelection(
            DynamicStringList.builder().value(new DynamicStringListElement("jodir", "Judicial direction")).build()
        ).build());
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

    private AboutToStartOrSubmitResponse<PCSCase, State> continuePage() {
        return handlePage(page, page.getPageKey());
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> continueCancellationPage() {
        return handlePage(cancelPage, cancelPage.getPageKey());
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private AboutToStartOrSubmitResponse<PCSCase, State> handlePage(
        CcdPageConfiguration pageConfiguration, String pageKey) {
        var config = new ConfigBuilderImpl<>(new ResolvedCCDConfig<>(
            PCSCase.class, State.class, UserRole.class, Map.of(), ImmutableSet.of()));
        var event = config.decentralisedEvent("manageHearing", null).forAllStates();
        pageConfiguration.addTo(new PageBuilder(event));
        MidEvent<PCSCase, State> callback = config.build().getEvents().get("manageHearing")
            .getFields().getPagesToMidEvent().get(pageKey);
        return transaction.execute(status -> callback.handle(
            CaseDetails.<PCSCase, State>builder().id(reference).data(form).build(), null));
    }
}
