package uk.gov.hmcts.reform.pcs.enforcement.ccd.page.warrantreturn;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseNameFormatter;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringList;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringListElement;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EvictionDetailsMarkupTest {

    private final EvictionDetailsMarkup underTest = new EvictionDetailsMarkup(new CaseNameFormatter());

    @Test
    void shouldShowTheCaseNameAndTheEvictionDetailsTable() {
        PCSCase pcsCase = PCSCase.builder()
            .claimantType(DynamicStringList.builder()
                              .value(DynamicStringListElement.builder()
                                         .code("PROVIDER_OF_SOCIAL_HOUSING")
                                         .label("Registered provider of social housing or local authority")
                                         .build())
                              .build())
            .allClaimants(wrap(Party.builder().orgName("Treetops Housing & Co").build()))
            .allDefendants(wrap(named("Sam", "Jones"), named("Alex", "Smith")))
            .build();

        String markup = underTest.build(pcsCase);

        assertThat(markup)
            .contains("Treetops Housing &amp; Co vs Sam Jones and Alex Smith")
            .contains("<h2 class=\"govuk-heading-m\">Eviction details</h2>")
            .contains("Claimant type</th>\n      <td class=\"govuk-table__cell\">"
                          + "Registered provider of social housing or local authority</td>")
            .contains("Claimant name</th>\n      <td class=\"govuk-table__cell\">Treetops Housing &amp; Co</td>")
            .contains("Defendant names</th>\n      <td class=\"govuk-table__cell\">Sam Jones<br>Alex Smith</td>")
            .doesNotContain("Warrant number");
    }

    @Test
    void shouldShowPersonsUnknownOnceForSeveralUnknownDefendants() {
        Party unknown = Party.builder().nameKnown(VerticalYesNo.NO).build();

        assertThat(EvictionDetailsMarkup.defendantNames(List.of(named("Sam", "Jones"), unknown, unknown)))
            .containsExactly("Sam Jones", "Person(s) unknown");
        assertThat(EvictionDetailsMarkup.defendantNames(List.of(unknown, unknown)))
            .containsExactly("Person(s) unknown");
    }

    @Test
    void shouldShowPersonsUnknownWithoutDefendants() {
        assertThat(EvictionDetailsMarkup.defendantNames(List.of())).containsExactly("Person(s) unknown");
        assertThat(EvictionDetailsMarkup.defendantNames(null)).containsExactly("Person(s) unknown");
    }

    @Test
    void shouldCopeWithAnEmptyCase() {
        String markup = underTest.build(PCSCase.builder().build());

        assertThat(markup).contains("Person(s) unknown").doesNotContain("null");
    }

    @Test
    void shouldUseTheClaimantsNameWhenTheyAreAPerson() {
        PCSCase pcsCase = PCSCase.builder()
            .allClaimants(wrap(named("Pat", "Lee")))
            .allDefendants(wrap(named("Sam", "Jones")))
            .build();

        assertThat(underTest.build(pcsCase)).contains("Pat Lee vs Sam Jones")
            .contains("Claimant name</th>\n      <td class=\"govuk-table__cell\">Pat Lee</td>");
    }

    private static Party named(String firstName, String lastName) {
        return Party.builder().nameKnown(VerticalYesNo.YES).firstName(firstName).lastName(lastName).build();
    }

    @SafeVarargs
    private static <T> List<ListValue<T>> wrap(T... values) {
        return java.util.Arrays.stream(values).map(value -> new ListValue<>(null, value)).toList();
    }
}
