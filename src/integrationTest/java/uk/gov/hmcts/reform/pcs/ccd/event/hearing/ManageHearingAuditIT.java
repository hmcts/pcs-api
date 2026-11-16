package uk.gov.hmcts.reform.pcs.ccd.event.hearing;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.ccd.sdk.testing.CcdEventTestSupport;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.hearing.Hearing;
import uk.gov.hmcts.reform.pcs.ccd.domain.hearing.HearingNoticeWording;
import uk.gov.hmcts.reform.pcs.ccd.domain.hearing.HearingType;
import uk.gov.hmcts.reform.pcs.ccd.domain.hearing.ManageHearingOption;
import uk.gov.hmcts.reform.pcs.config.AbstractPostgresContainerIT;
import uk.gov.hmcts.reform.pcs.config.IssuedCases;
import uk.gov.hmcts.reform.pcs.config.PcsCcdEventTest;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@PcsCcdEventTest
class ManageHearingAuditIT extends AbstractPostgresContainerIT {

    @Autowired
    private CcdEventTestSupport<PCSCase, State> events;
    @Autowired
    private IssuedCases cases;

    @Test
    void recordsHearingChangesAgainstTheStaffMembersEvent() {
        long reference = cases.issue();
        var staff = events.registerActor("Hearing", "Administrator", "caseworker-pcs");
        var form = PCSCase.builder()
            .showManageHearingPage(VerticalYesNo.YES)
            .manageHearingOption(ManageHearingOption.ADD)
            .hearing(Hearing.builder().type(HearingType.POSSESSION).noticeWording(HearingNoticeWording.TPL)
                .date(LocalDateTime.of(2030, 1, 2, 10, 0)).durationDays(0).durationHours(1f)
                .durationMinutes(0f).issueNotice(VerticalYesNo.NO).notes("Original hearing").build())
            .build();

        var added = events.event(reference, "manageHearing", form).as(staff).submitExpectingSuccess();
        assertThat(added.changes("hearing")).hasSize(1);
        var inserted = added.changes("hearing").getFirst();
        int hearingId = inserted.newValues().get("id").asInt();
        assertThat(inserted.newValues().get("notes").asText()).isEqualTo("Original hearing");

        form.setSelectedHearingId(String.valueOf(hearingId));
        form.setManageHearingOption(ManageHearingOption.EDIT);
        form.getHearing().setNotes("Updated hearing");
        var edited = events.event(reference, "manageHearing", form).as(staff).submitExpectingSuccess();
        assertThat(edited.changes("hearing")).hasSize(1);
        var update = edited.changes("hearing").getFirst();
        assertThat(update.oldValues().get("notes").asText()).isEqualTo("Original hearing");
        assertThat(update.newValues().get("notes").asText()).isEqualTo("Updated hearing");

        form.setManageHearingOption(ManageHearingOption.CANCEL);
        form.getHearing().setCancellationReason("No longer required");
        var cancelled = events.event(reference, "manageHearing", form).as(staff).submitExpectingSuccess();
        assertThat(cancelled.changes("hearing")).hasSize(1);
        var cancellation = cancelled.changes("hearing").getFirst();
        assertThat(cancellation.newValues().get("id").asInt()).isEqualTo(hearingId);
        assertThat(cancellation.newValues().get("cancelled").asBoolean()).isTrue();
        assertThat(cancellation.newValues().get("cancellation_reason").asText()).isEqualTo("No longer required");
        assertThat(cancelled.audit().eventId()).isEqualTo("manageHearing");
        assertThat(cancelled.audit().userId()).isEqualTo(staff.uid())
            .isEqualTo(edited.audit().userId()).isEqualTo(added.audit().userId());
    }
}
