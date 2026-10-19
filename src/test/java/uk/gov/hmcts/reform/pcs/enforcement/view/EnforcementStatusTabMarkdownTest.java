package uk.gov.hmcts.reform.pcs.enforcement.view;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringList;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringListElement;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus;
import uk.gov.hmcts.reform.pcs.enforcement.domain.SelectEnforcementType;
import uk.gov.hmcts.reform.pcs.enforcement.domain.warrantreturn.WarrantReturnName;
import uk.gov.hmcts.reform.pcs.enforcement.domain.warrantreturn.WarrantReturnOutcome;
import uk.gov.hmcts.reform.pcs.enforcement.domain.warrantreturn.WarrantReturnType;
import uk.gov.hmcts.reform.pcs.enforcement.entity.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.enforcement.entity.WarrantReturnEntity;
import uk.gov.hmcts.reform.pcs.enforcement.view.EnforcementStatusTabMarkdown.Entry;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EnforcementStatusTabMarkdownTest {

    @Test
    void shouldHeadAWarrantWithItsNumberAndShowEverySectionInOrder() {
        String markdown = render(warrant("WP-00000001", EnforcementStatus.AWAITING_SCHEDULING).build(), null);

        assertThat(markdown)
            .contains("<h2 class=\"govuk-heading-l\">WP-00000001</h2>")
            .contains(row("Type", "Warrant of possession"))
            .contains(row("Status", "Awaiting scheduling"));
        assertThat(markdown.indexOf(">Eviction notice</h3>"))
            .isPositive()
            .isLessThan(markdown.indexOf(">Scheduled eviction</h3>"));
        assertThat(markdown.indexOf(">Scheduled eviction</h3>"))
            .isLessThan(markdown.indexOf(">Claimant eviction confirmation</h3>"));
        assertThat(markdown.indexOf(">Claimant eviction confirmation</h3>"))
            .isLessThan(markdown.indexOf(">Eviction outcome</h3>"));
    }

    @Test
    void shouldShowPlaceholdersBeforeAnythingHasHappened() {
        String markdown = render(warrant("WP-00000001", EnforcementStatus.AWAITING_SCHEDULING).build(), null);

        assertThat(markdown)
            .contains("<p class=\"govuk-body\">Not yet served</p>")
            .contains("<p class=\"govuk-body\">Not yet scheduled</p>")
            .contains("<p class=\"govuk-body\">Not yet confirmed</p>")
            .contains("<p class=\"govuk-body\">No return recorded yet</p>");
    }

    @Test
    void shouldShowTheScheduledEvictionAndTheServedNotice() {
        EnforcementOrderEntity enforcement = warrant("WP-00000001", EnforcementStatus.AWAITING_CLAIMANT_CONFIRMATION)
            .bailiffDate(LocalDateTime.of(2027, 6, 30, 9, 30))
            .dispenseWithService(false)
            .noticeServedAt(LocalDateTime.of(2026, 3, 15, 8, 0))
            .noticeServedHow("by hand to the occupant")
            .build();

        String markdown = render(enforcement, null);

        assertThat(markdown)
            .contains(row("Date served", "15 March 2026"))
            .contains(row("Time served", "08:00 AM"))
            .contains(row("How it was served", "by hand to the occupant"))
            .contains(row("Eviction date", "30 June 2027"))
            .contains(row("Eviction time", "09:30 AM"));
    }

    @Test
    void shouldSayAWithoutNoticeEvictionHasNoNoticeToServe() {
        EnforcementOrderEntity enforcement = warrant("WP-00000001", EnforcementStatus.AWAITING_CLAIMANT_CONFIRMATION)
            .bailiffDate(LocalDateTime.of(2027, 6, 30, 14, 0))
            .dispenseWithService(true)
            .build();

        String markdown = render(enforcement, null);

        assertThat(markdown)
            .contains(row("Service", "Without notice: the court has given permission to dispense with service"))
            .contains(row("Date served", "Not applicable"))
            .contains(row("Time served", "Not applicable"))
            .contains(row("How it was served", "Not applicable"))
            .contains(row("Eviction time", "02:00 PM"));
    }

    @Test
    void shouldShowTheClaimantsAttendanceAnswerForTheCurrentDate() {
        EnforcementOrderEntity enforcement = warrant("WP-00000001", EnforcementStatus.CLAIMANT_UNABLE_TO_ATTEND)
            .bailiffDate(LocalDateTime.of(2027, 6, 30, 9, 30))
            .claimantCanAttend(false)
            .build();

        String markdown = render(enforcement, null);

        assertThat(markdown).contains(
            row("Can you (or someone from your organisation) attend on 30 June 2027 at 09:30 AM?", "No"));
    }

    @Test
    void shouldShowTheLatestReturn() {
        WarrantReturnEntity latestReturn = WarrantReturnEntity.builder()
            .returnType(WarrantReturnType.FINAL)
            .outcome(WarrantReturnOutcome.EVICTION_SUCCESSFUL)
            .returnName(WarrantReturnName.EVICTION_SUCCESSFUL)
            .evictionAt(Instant.parse("2026-07-08T08:15:00Z"))
            .additionalDetails("Keys handed over")
            .build();

        String markdown = render(warrant("WP-00000001", EnforcementStatus.CLOSED).build(), latestReturn);

        assertThat(markdown)
            .contains(row("Return", "Final"))
            .contains(row("Return name", "Eviction successful"))
            .contains(row("Outcome", "Eviction successful"))
            .contains(row("Eviction date", "8 July 2026"))
            .contains(row("Eviction time", "09:15 AM"))
            .contains(row("Additional details", "Keys handed over"));
    }

    @Test
    void shouldShowAWritsTransferDateAndNoBailiffSections() {
        EnforcementOrderEntity writ = EnforcementOrderEntity.builder()
            .enforcementNumber("HC-00000001")
            .status(EnforcementStatus.TRANSFERRED_TO_HIGH_COURT)
            .approvedAt(Instant.parse("2026-05-11T23:30:00Z"))
            .enforcementOrder(order(SelectEnforcementType.WRIT))
            .build();

        String markdown = render(writ, null);

        assertThat(markdown)
            .contains("<h2 class=\"govuk-heading-l\">HC-00000001</h2>")
            .contains(row("Type", "Writ of possession"))
            .contains(row("Date of transfer to the High Court", "12/05/2026"))
            .doesNotContain("Eviction notice", "Scheduled eviction", "Claimant eviction confirmation",
                            "Eviction outcome");
    }

    @Test
    void shouldListTheEntriesInTheOrderGivenWithABreakBetweenThem() {
        String markdown = EnforcementStatusTabMarkdown.render(List.of(
            new Entry(warrant("WR-00000002", EnforcementStatus.AWAITING_SCHEDULING).build(), null),
            new Entry(warrant("WP-00000001", EnforcementStatus.CLOSED).build(), null)
        ));

        assertThat(markdown.indexOf("WR-00000002")).isLessThan(markdown.indexOf("WP-00000001"));
        assertThat(markdown).containsOnlyOnce("<hr ");
    }

    @Test
    void shouldEscapeFreeTextAndKeepTheMarkupOneHtmlBlock() {
        EnforcementOrderEntity enforcement = warrant("WP-00000001", EnforcementStatus.AWAITING_CLAIMANT_CONFIRMATION)
            .bailiffDate(LocalDateTime.of(2027, 6, 30, 9, 30))
            .dispenseWithService(false)
            .noticeServedAt(LocalDateTime.of(2026, 3, 15, 8, 0))
            .noticeServedHow("<script>alert(1)</script>\n\n${caseTitleMarkdown}")
            .build();

        String markdown = render(enforcement, null);

        assertThat(markdown)
            .doesNotContain("<script>", "${", "\n\n")
            .contains("&lt;script&gt;alert(1)&lt;/script&gt;<br><br>&#36;{caseTitleMarkdown}");
    }

    @Test
    void shouldLeaveTheClaimantsSectionOutForDefendants() {
        EnforcementOrderEntity enforcement = warrant("WP-00000001", EnforcementStatus.CLAIMANT_UNABLE_TO_ATTEND)
            .bailiffDate(LocalDateTime.of(2027, 6, 30, 9, 30))
            .noticeServedAt(LocalDateTime.of(2027, 6, 1, 10, 0))
            .claimantCanAttend(false)
            .build();

        String markdown = EnforcementStatusTabMarkdown.renderForDefendants(List.of(new Entry(enforcement, null)));

        assertThat(markdown)
            .contains("<h2 class=\"govuk-heading-l\">WP-00000001</h2>")
            .contains(">Eviction notice</h3>")
            .contains(">Scheduled eviction</h3>")
            .contains(">Eviction outcome</h3>")
            .doesNotContain("Claimant eviction confirmation")
            .doesNotContain("(or someone from your organisation) attend on");
    }

    private static String render(EnforcementOrderEntity enforcement, WarrantReturnEntity latestReturn) {
        return EnforcementStatusTabMarkdown.render(List.of(new Entry(enforcement, latestReturn)));
    }

    private static EnforcementOrderEntity.EnforcementOrderEntityBuilder warrant(String number,
                                                                               EnforcementStatus status) {
        return EnforcementOrderEntity.builder()
            .enforcementNumber(number)
            .status(status)
            .enforcementOrder(order(SelectEnforcementType.WARRANT));
    }

    private static EnforcementOrder order(SelectEnforcementType type) {
        DynamicStringListElement element = DynamicStringListElement.builder()
            .code(type.name())
            .label(type.getLabel())
            .build();
        return EnforcementOrder.builder()
            .chooseEnforcementType(DynamicStringList.builder().value(element).build())
            .build();
    }

    private static String row(String key, String value) {
        return "<dt class=\"govuk-summary-list__key\">" + key + "</dt>"
            + "<dd class=\"govuk-summary-list__value\">" + value + "</dd>";
    }
}
