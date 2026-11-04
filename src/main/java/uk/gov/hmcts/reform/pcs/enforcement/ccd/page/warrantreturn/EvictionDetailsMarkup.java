package uk.gov.hmcts.reform.pcs.enforcement.ccd.page.warrantreturn;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseNameFormatter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.springframework.web.util.HtmlUtils.htmlEscape;
import static uk.gov.hmcts.reform.pcs.ccd.util.ListValueUtils.unwrapListItems;

@Component
@RequiredArgsConstructor
public class EvictionDetailsMarkup {

    static final String PERSONS_UNKNOWN = "Person(s) unknown";

    private final CaseNameFormatter caseNameFormatter;

    public String build(PCSCase pcsCase) {
        List<Party> claimants = unwrapListItems(pcsCase.getAllClaimants());
        List<Party> defendants = unwrapListItems(pcsCase.getAllDefendants());
        String claimantType = pcsCase.getClaimantType() == null || pcsCase.getClaimantType().getValue() == null
            ? ""
            : pcsCase.getClaimantType().getValue().getLabel();
        String caseName = claimants.isEmpty() ? "" : caseNameFormatter.formatCaseName(claimants, defendants);

        return """
            <p class="govuk-body govuk-!-font-size-24">%s</p>
            <h2 class="govuk-heading-m">Eviction details</h2>
            <table class="govuk-table">
              <tbody class="govuk-table__body">
                <tr class="govuk-table__row">
                  <th scope="row" class="govuk-table__header">Claimant type</th>
                  <td class="govuk-table__cell">%s</td>
                </tr>
                <tr class="govuk-table__row">
                  <th scope="row" class="govuk-table__header">Claimants</th>
                  <td class="govuk-table__cell">%s</td>
                </tr>
                <tr class="govuk-table__row">
                  <th scope="row" class="govuk-table__header">Defendants</th>
                  <td class="govuk-table__cell">%s</td>
                </tr>
              </tbody>
            </table>
            """.formatted(
                htmlEscape(caseName),
                htmlEscape(claimantType),
                htmlEscape(claimantName(claimants)),
                defendantNames(defendants).stream().map(name -> htmlEscape(name)).collect(Collectors.joining("<br>")));
    }

    private static String claimantName(List<Party> claimants) {
        if (claimants == null || claimants.isEmpty()) {
            return "";
        }
        Party claimant = claimants.getFirst();
        return claimant.getOrgName() != null ? claimant.getOrgName() : fullName(claimant);
    }

    static List<String> defendantNames(List<Party> defendants) {
        List<String> names = new ArrayList<>();
        boolean anyUnknown = defendants == null || defendants.isEmpty();
        if (defendants != null) {
            for (Party defendant : defendants) {
                if (defendant.getNameKnown() == VerticalYesNo.YES) {
                    names.add(fullName(defendant));
                } else {
                    anyUnknown = true;
                }
            }
        }
        if (anyUnknown) {
            names.add(PERSONS_UNKNOWN);
        }
        return names;
    }

    private static String fullName(Party party) {
        return Stream.of(party.getFirstName(), party.getLastName())
            .filter(Objects::nonNull)
            .collect(Collectors.joining(" "));
    }
}
