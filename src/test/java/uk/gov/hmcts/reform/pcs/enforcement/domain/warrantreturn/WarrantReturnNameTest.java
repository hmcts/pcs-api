package uk.gov.hmcts.reform.pcs.enforcement.domain.warrantreturn;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Driven by the tables in 11-record-warrant-return.feature and the Jira tickets they come from. The feature
 * writes return names with varying capitals, so they are matched ignoring case.
 */
class WarrantReturnNameTest {

    private static final String FINAL_NOT_SUCCESSFUL_NAMES = "Warrant withdrawn, Warrant suspended by Order, "
        + "Possession Order set aside, Unable to gain access to the property, Claimant did not confirm eviction, "
        + "No locksmith in attendance (Claimant failed to arrange), "
        + "No animal handler in attendance (Claimant failed to arrange), "
        + "No Claimant or agent in attendance for eviction, No reply to request for additional information, "
        + "Eviction unsuccessful - other, Warrant expired, Warrant issued in error";

    private static final List<String> INTERIM_NAMES = List.of(
        "Application unsuccessful & warrant returned to bailiff",
        "Request for additional information",
        "No animal handler in attendance (Parties unaware of animal)",
        "No Police presence (Police refused to attend when asked to)",
        "Children left unattended in property",
        "Crowds or protests",
        "Obstruction by Defendants",
        "Safeguarding or vulnerability concerns",
        "Defendant states they are in Breathing Space (Standard or Mental Health)",
        "Claimant advised Defendant in Breathing Space",
        "Other interim return",
        "Reopen warrant"
    );

    // "Return name moves the enforcement status" (HDPI-8424).
    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource(delimiter = '|', textBlock = """
        Eviction successful                                            | Closed
        Warrant Suspended by Order                                     | Closed
        Possession Order set aside                                     | Closed
        Warrant Withdrawn                                              | Closed
        No reply to request for additional information                 | Closed
        Eviction unsuccessful - other                                  | Closed
        No Claimant or agent in attendance for eviction                | Closed
        Warrant Issued In Error                                        | Closed
        Warrant Expired                                                | Closed
        Unable to gain access to the property                          | Closed
        Claimant did not confirm eviction                              | Closed
        No Locksmith in attendance (Claimant failed to arrange)        | Closed
        No animal handler in attendance (Claimant failed to arrange)   | Closed
        Claimant Advised Defendant in Breathing Space                  | Breathing space
        Defendant States they are in Breathing Space (Standard or Mental Health) | Breathing space
        No animal handler in attendance (Parties unaware of animal)    | Awaiting scheduling
        No Police Presence (Police refused to attend when asked to)    | Awaiting scheduling
        Children Left unattended in Property                           | Awaiting scheduling
        Crowds or protests                                             | Awaiting scheduling
        Obstruction by defendants                                      | Awaiting scheduling
        Safeguarding or vulnerability concerns                         | Awaiting scheduling
        Reopen Warrant                                                 | Awaiting bailiff allocation
        Application Unsuccessful & Warrant returned to bailiff         | unchanged
        Request for additional information                             | unchanged
        Other Interim Return                                           | unchanged
        """)
    void shouldMoveTheEnforcementStatusAsTheFeatureSays(String returnName, String status) {
        WarrantReturnName name = byLabel(returnName);

        if ("unchanged".equals(status)) {
            assertThat(name.statusAfterReturn()).isEmpty();
        } else {
            assertThat(name.statusAfterReturn()).map(EnforcementStatus::getLabel).contains(status);
        }
    }

    // "Additional details are mandatory for some return names": the feature's examples plus the rest of the
    // HDPI-7347 lists, so every return name is covered.
    @ParameterizedTest(name = "{0}: {1}")
    @CsvSource(delimiter = '|', textBlock = """
        Warrant withdrawn                                            | mandatory
        Unable to gain access to the property                        | mandatory
        Request for additional information                           | mandatory
        Eviction unsuccessful - other                                | mandatory
        Safeguarding or vulnerability concerns                       | mandatory
        Other interim return                                         | mandatory
        Reopen warrant                                               | mandatory
        Eviction successful                                          | optional
        Warrant suspended by Order                                   | optional
        Obstruction by Defendants                                    | optional
        Claimant advised Defendant in Breathing Space                | optional
        No animal handler in attendance (Claimant failed to arrange) | mandatory
        No animal handler in attendance (Parties unaware of animal)  | mandatory
        No reply to request for additional information               | mandatory
        Warrant issued in error                                      | mandatory
        Possession Order set aside                                   | optional
        Claimant did not confirm eviction                            | optional
        No locksmith in attendance (Claimant failed to arrange)      | optional
        Application unsuccessful & warrant returned to bailiff       | optional
        No Police presence (Police refused to attend when asked to)  | optional
        Children left unattended in property                         | optional
        Crowds or protests                                           | optional
        No Claimant or agent in attendance for eviction              | optional
        Warrant expired                                              | optional
        Defendant states they are in Breathing Space (Standard or Mental Health) | optional
        """)
    void shouldRequireAdditionalDetailsAsTheFeatureSays(String returnName, String required) {
        assertThat(byLabel(returnName).isAdditionalDetailsRequired()).isEqualTo("mandatory".equals(required));
    }

    @Test
    void shouldCoverEveryReturnNameInTheStatusAndAdditionalDetailsTables() {
        // 13 closing, 2 breathing space, 6 awaiting scheduling, 1 reopen, 3 unchanged
        assertThat(WarrantReturnName.values()).hasSize(25);
        assertThat(Arrays.stream(WarrantReturnName.values()).filter(WarrantReturnName::isAdditionalDetailsRequired))
            .hasSize(11);
    }

    // "Final return names depend on the outcome".
    @Test
    void shouldOfferOnlyEvictionSuccessfulForASuccessfulEviction() {
        assertThat(labels(Arrays.stream(SuccessfulWarrantReturnName.values())
                              .map(SuccessfulWarrantReturnName::getLabel)))
            .containsExactly("Eviction successful");
    }

    @Test
    void shouldOfferTheFeatureListForAnUnsuccessfulOrCancelledEviction() {
        assertThat(labels(Arrays.stream(UnsuccessfulWarrantReturnName.values())
                              .map(UnsuccessfulWarrantReturnName::getLabel)))
            .containsExactlyElementsOf(List.of(FINAL_NOT_SUCCESSFUL_NAMES.split(", ")));
    }

    // "Interim return names".
    @Test
    void shouldOfferTheFeatureListForAnInterimReturn() {
        assertThat(labels(Arrays.stream(InterimWarrantReturnName.values()).map(InterimWarrantReturnName::getLabel)))
            .containsExactlyElementsOf(INTERIM_NAMES);
    }

    @Test
    void shouldOfferEveryReturnNameInExactlyOneDropDown() {
        Set<WarrantReturnName> offered = EnumSet.noneOf(WarrantReturnName.class);
        Arrays.stream(SuccessfulWarrantReturnName.values()).map(SuccessfulWarrantReturnName::getReturnName)
            .forEach(offered::add);
        Arrays.stream(UnsuccessfulWarrantReturnName.values()).map(UnsuccessfulWarrantReturnName::getReturnName)
            .forEach(offered::add);
        Arrays.stream(InterimWarrantReturnName.values()).map(InterimWarrantReturnName::getReturnName)
            .forEach(offered::add);

        assertThat(offered).containsExactlyInAnyOrder(WarrantReturnName.values());
        assertThat(SuccessfulWarrantReturnName.values().length + UnsuccessfulWarrantReturnName.values().length
                       + InterimWarrantReturnName.values().length).isEqualTo(WarrantReturnName.values().length);
    }

    @Test
    void shouldUseTheReturnNameConstantNameAsTheDropDownCode() {
        for (SuccessfulWarrantReturnName option : SuccessfulWarrantReturnName.values()) {
            assertThat(option.name()).isEqualTo(option.getReturnName().name());
        }
        for (UnsuccessfulWarrantReturnName option : UnsuccessfulWarrantReturnName.values()) {
            assertThat(option.name()).isEqualTo(option.getReturnName().name());
        }
        for (InterimWarrantReturnName option : InterimWarrantReturnName.values()) {
            assertThat(option.name()).isEqualTo(option.getReturnName().name());
        }
    }

    @Test
    void shouldOfferTheFeatureReturnAndOutcomeOptions() {
        assertThat(Arrays.stream(WarrantReturnType.values()).map(WarrantReturnType::getLabel))
            .containsExactly("Final", "Interim");
        assertThat(Arrays.stream(WarrantReturnOutcome.values()).map(WarrantReturnOutcome::getLabel))
            .containsExactly("Eviction successful", "Eviction unsuccessful", "Eviction cancelled");
    }

    static WarrantReturnName byLabel(String featureText) {
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

    private static List<String> labels(Stream<String> labels) {
        return labels.toList();
    }
}
