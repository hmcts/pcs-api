package uk.gov.hmcts.reform.pcs.ccd.event.hearing;

import com.google.common.collect.ImmutableSet;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.ccd.sdk.ConfigBuilderImpl;
import uk.gov.hmcts.ccd.sdk.ResolvedCCDConfig;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.api.callback.MidEvent;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.hearing.ManageHearingOption;
import uk.gov.hmcts.reform.pcs.ccd.entity.hearing.HearingEntity;
import uk.gov.hmcts.reform.pcs.ccd.page.managehearing.ManageHearingPage;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.hearing.HearingService;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringListElement;
import uk.gov.hmcts.reform.pcs.config.AbstractPostgresContainerIT;
import uk.gov.hmcts.reform.pcs.config.IssuedCases;
import uk.gov.hmcts.reform.pcs.config.PcsCcdEventTest;
import uk.gov.hmcts.reform.pcs.exception.HearingNotFoundException;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** The existing manage-hearing page selects any hearing; persistence changes only that hearing. */
@PcsCcdEventTest
@Transactional
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
    private EntityManager entityManager;

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
        form = PCSCase.builder().hearingLocation("Test court")
            .hearingSelection(hearings.buildHearingSelection(pcsCases.loadCase(reference))).build();
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
            hearings.addHearing(reference, form);
        }

        entityManager.flush();
        entityManager.clear();
        assertThat(hearings.buildHearingSelection(pcsCases.loadCase(reference)).getListItems()).hasSize(7);
        assertThat(pcsCases.loadCase(reference).getHearings()).hasSize(8);
    }

    @Test
    void editsALaterHearingWithoutChangingTheNextHearing() {
        select(later, ManageHearingOption.EDIT);
        assertThat(form.getHearing().getDate()).isEqualTo(LocalDateTime.of(2030, 2, 3, 14, 30));
        form.getHearing().setNotes("Updated later hearing");
        hearings.updateHearing(reference, form);
        entityManager.flush();
        entityManager.clear();

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
        select(later, ManageHearingOption.CANCEL);
        assertThat(form.getHearing().getHearingSummaryMarkdown()).contains("3 February 2030");
        form.getHearing().setCancellationReason("No longer required");
        hearings.cancelHearing(reference, form);
        entityManager.flush();
        entityManager.clear();

        assertThat(stored(later).getCancelled()).isTrue();
        assertThat(stored(later).getCancellationReason()).isEqualTo("No longer required");
        assertThat(stored(earlier).getCancelled()).isFalse();
        assertThat(hearings.buildHearingSelection(pcsCases.loadCase(reference)).getValueCode())
            .isEqualTo(String.valueOf(earlier));
    }

    @Test
    void rejectsCancelledMissingAndOtherCasesHearings() {
        int foreign = cases.hearing(cases.issue(), LocalDateTime.of(2030, 1, 1, 10, 0), false);
        for (String id : new String[] {null, String.valueOf(cancelled), String.valueOf(foreign)}) {
            form.getHearingSelection().setValue(DynamicStringListElement.builder().code(id).build());
            form.setManageHearingOption(ManageHearingOption.EDIT);
            assertThat(continuePage().getErrors()).containsExactly("Select an available hearing on this case");
        }
        form.setSelectedHearingId(String.valueOf(foreign));
        assertThatThrownBy(() -> hearings.cancelHearing(reference, form)).isInstanceOf(HearingNotFoundException.class);
        assertThatThrownBy(() -> hearings.updateHearing(reference, form)).isInstanceOf(HearingNotFoundException.class);
    }

    @Test
    void rejectsAHearingCancelledAfterItWasSelected() {
        select(later, ManageHearingOption.EDIT);
        stored(later).setCancelled(true);

        assertThatThrownBy(() -> hearings.updateHearing(reference, form)).isInstanceOf(HearingNotFoundException.class);
        assertThatThrownBy(() -> hearings.cancelHearing(reference, form)).isInstanceOf(HearingNotFoundException.class);
    }

    private void select(int id, ManageHearingOption option) {
        form.setManageHearingOption(option);
        form.getHearingSelection().setValue(DynamicStringListElement.builder().code(String.valueOf(id)).build());
        assertThat(continuePage().getErrors()).isNullOrEmpty();
    }

    private HearingEntity stored(int id) {
        return pcsCases.loadCase(reference).getHearings().stream()
            .filter(hearing -> hearing.getId() == id).findFirst().orElseThrow();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private AboutToStartOrSubmitResponse<PCSCase, State> continuePage() {
        var config = new ConfigBuilderImpl<>(new ResolvedCCDConfig<>(
            PCSCase.class, State.class, UserRole.class, Map.of(), ImmutableSet.of()));
        var event = config.decentralisedEvent("manageHearing", null).forAllStates();
        page.addTo(new PageBuilder(event));
        MidEvent<PCSCase, State> callback = config.build().getEvents().get("manageHearing")
            .getFields().getPagesToMidEvent().get(page.getPageKey());
        return callback.handle(CaseDetails.<PCSCase, State>builder().id(reference).data(form).build(), null);
    }
}
