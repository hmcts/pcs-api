package uk.gov.hmcts.reform.pcs.enforcement.notification;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import uk.gov.hmcts.reform.pcs.enforcement.domain.warrantreturn.WarrantReturnName;

import java.util.Arrays;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Driven by the HDPI-8424 notification tables. "Claimant is notified of the return" in
 * 11-record-warrant-return.feature gives examples of both.
 */
class WarrantReturnClaimantEmailsTest {

    private static final String REASON = "[reason from the additional details free textbox to be populated here]";

    @ParameterizedTest(name = "{0}: claimant {1} an email")
    @CsvSource(delimiter = '|', textBlock = """
        Warrant Withdrawn                                             | receives
        No reply to request for additional information                | receives
        Eviction unsuccessful - other                                 | receives
        No Claimant or agent in attendance for eviction               | receives
        Warrant  Issued In Error                                      | receives
        Reopen Warrant                                                | receives
        Claimant Advised Defendant in Breathing Space                 | receives
        Application Unsuccessful & Warrant returned to bailiff        | receives
        Request for additional information                            | receives
        Other Interim Return                                          | receives
        Defendant States they are in Breathing Space (Standard or Mental Health) | receives
        Warrant Expired                                               | receives
        Unable to gain access to the property                         | receives
        Claimant did not confirm eviction                             | receives
        No Locksmith in attendance (Claimant failed to arrange)       | receives
        No animal handler in attendance (Claimant failed to arrange)  | receives
        No animal handler in attendance (Parties unaware of animal)   | receives
        No Police Presence (Police refused to attend when asked to)   | receives
        Children Left unattended in Property                          | receives
        Crowds or protests.                                           | receives
        Obstruction by defendants                                     | receives
        Safeguarding or vulnerability concerns                        | receives
        Warrant Suspended by Order                                    | does not receive
        Possession Order set aside                                    | does not receive
        Eviction Successful                                           | does not receive
        """)
    void shouldNotifyTheClaimantAsTheFeatureSays(String returnName, String notified) {
        WarrantReturnName name = byLabel(returnName);
        boolean receives = "receives".equals(notified);

        assertThat(WarrantReturnClaimantEmails.notifiesClaimant(name)).isEqualTo(receives);
        assertThat(WarrantReturnClaimantEmails.body(name, "details").isPresent()).isEqualTo(receives);
    }

    @Test
    void shouldCoverEveryReturnName() {
        assertThat(Arrays.stream(WarrantReturnName.values()).filter(WarrantReturnClaimantEmails::notifiesClaimant))
            .hasSize(22);
    }

    // The HDPI-8424 notification table, word for word.
    @ParameterizedTest(name = "{0}")
    @CsvFileSource(resources = "/enforcement/warrant-return-claimant-emails.psv", delimiter = '|')
    void shouldUseTheBodyFromTheNotificationTable(String returnName, String body) {
        assertThat(WarrantReturnClaimantEmails.templates().get(byLabel(returnName)))
            .isEqualTo(body.replace(REASON, WarrantReturnClaimantEmails.REASON));
    }

    // "Free text is included in the claimant email".
    @Test
    void shouldIncludeTheAdditionalDetailsInTheOtherInterimReturnEmail() {
        assertThat(WarrantReturnClaimantEmails.body(WarrantReturnName.OTHER_INTERIM_RETURN,
                                                    "Occupant presented a court order"))
            .hasValueSatisfying(body -> assertThat(body)
                .contains("The reason provided by the bailiff is: Occupant presented a court order")
                .isEqualTo("The bailiff has made a return on the warrant. The reason provided by the bailiff is: "
                               + "Occupant presented a court order. The court will take the next action required "
                               + "on the warrant."));
    }

    @Test
    void shouldIncludeTheAdditionalDetailsInTheEvictionUnsuccessfulOtherEmail() {
        assertThat(WarrantReturnClaimantEmails.body(WarrantReturnName.EVICTION_UNSUCCESSFUL_OTHER,
                                                    "  because the locks had been changed. "))
            .contains("The bailiff has been unable to execute the warrant because the locks had been changed. "
                          + "The warrant has been filed away.");
    }

    @ParameterizedTest
    @NullAndEmptySource
    void shouldLeaveTheReasonEmptyWhenThereAreNoAdditionalDetails(String additionalDetails) {
        assertThat(WarrantReturnClaimantEmails.body(WarrantReturnName.OTHER_INTERIM_RETURN, additionalDetails))
            .hasValueSatisfying(body -> assertThat(body).doesNotContain(WarrantReturnClaimantEmails.REASON));
    }

    @Test
    void shouldNotNotifyWithoutAReturnName() {
        assertThat(WarrantReturnClaimantEmails.notifiesClaimant(null)).isFalse();
        assertThat(WarrantReturnClaimantEmails.body(null, "details")).isEmpty();
    }

    private static WarrantReturnName byLabel(String featureText) {
        String wanted = normalise(featureText);
        return Arrays.stream(WarrantReturnName.values())
            .filter(name -> normalise(name.getLabel()).equals(wanted))
            .findFirst()
            .orElseThrow(() -> new AssertionError("No return name " + featureText));
    }

    private static String normalise(String text) {
        String collapsed = text.strip().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
        return collapsed.endsWith(".") ? collapsed.substring(0, collapsed.length() - 1) : collapsed;
    }
}
