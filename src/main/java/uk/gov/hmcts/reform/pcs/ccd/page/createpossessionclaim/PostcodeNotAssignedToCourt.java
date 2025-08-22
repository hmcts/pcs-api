package uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.NEVER_SHOW;

@AllArgsConstructor
@Component
@Slf4j
public class PostcodeNotAssignedToCourt implements CcdPageConfiguration {

    private static final String SHOW_PAGE = "showPostcodeNotAssignedToCourt=\"Yes\"";
    private static final String SHOW_ENGLAND = SHOW_PAGE + " AND postcodeNotAssignedView=\"ENGLAND\"";
    private static final String SHOW_WALES = SHOW_PAGE + " AND postcodeNotAssignedView=\"WALES\"";
    private static final String SHOW_ALL = SHOW_PAGE + " AND postcodeNotAssignedView=\"ALL_COUNTRIES\"";

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("postcodeNotAssignedToCourt")
            .pageLabel("You cannot use this online service")
            .showCondition(SHOW_PAGE)
            .readonly(PCSCase::getShowPostcodeNotAssignedToCourt, NEVER_SHOW)
            .readonly(PCSCase::getPostcodeNotAssignedView, NEVER_SHOW)
            .readonly(PCSCase::getLegislativeCountry, NEVER_SHOW)
            .label(
                "postcodeNotAssignedToCourt-header",
                """
                ---
                <section tabindex="0">
                <p class="govuk-body">
                Based on the postcode you provided, we cannot determine which court your claim should be assigned to, 
                so you cannot use this online service.
                </p>

                <h3 class="govuk-heading-s govuk-!-font-size-19">What to do next</h3>
                """
            )
            .label(
                "postcodeNotAssignedToCourt-england",
                """
                <ul class="govuk-list govuk-list--bullet">
                    <li><span class="govuk-!-font-weight-bold">For rental or mortgage arrears claims</span> – 
                    use the %s.</li>
                    <li><span class="govuk-!-font-weight-bold">For other types of claims</span> – fill in form 
                    N5 and the correct particulars of claim form.</li>
                </ul>
                """.formatted(PCOL_LINK),
                SHOW_ENGLAND
            )
            .label(
                "postcodeNotAssignedToCourt-wales",
                """
                <p class="govuk-body">Use form N5 Wales and the correct particulars of claim form.</p>
                """,
                SHOW_WALES
            )
            .label(
                "postcodeNotAssignedToCourt-all",
                """
                <ul class="govuk-list govuk-list--bullet">
                    <li><span class="govuk-!-font-weight-bold">For rental or mortgage arrears claims in England</span> – 
                    use the %s.</li>
                    <li><span class="govuk-!-font-weight-bold">For other types of claims in England</span> – fill in form 
                    N5 and the correct particulars of claim form.</li>
                    <li><span class="govuk-!-font-weight-bold">For claims in Wales</span> - Use form N5 Wales and the 
                    correct particulars of claim form.</li>
                    <li><span class="govuk-!-font-weight-bold">For claims in Scotland</span> - use your 
                    <a href="https://www.scotcourts.gov.uk/home" rel="noreferrer noopener" target="_blank" 
                    class="govuk-link" aria-label="Go to Scottish Courts website (opens in new tab)">
                    local sheriff court (opens in new tab)</a>.</li>
                    <li><span class="govuk-!-font-weight-bold">For claims in Northern Ireland</span> - use the 
                    <a href="https://www.nidirect.gov.uk/articles/enforcement-civil-court-orders-northern-ireland" 
                    rel="noreferrer noopener" target="_blank" class="govuk-link" 
                    aria-label="Go to Enforcement of Judgments Office website (opens in new tab)">
                    Enforcement of Judgments Office (EJO) (opens in new tab)</a>.</li>
                </ul>
                """.formatted(PCOL_LINK),
                SHOW_ALL
            )
            .label(
                "postcodeNotAssignedToCourt-forms",
                FORMS_LINK
            )
            .label(
                "postcodeNotAssignedToCourt-footer",
                FOOTER            );
    }

    private static final String PCOL_LINK = """
            <a href="https://www.gov.uk/possession-claim-online-recover-property" 
            rel="noreferrer noopener" target="_blank" class="govuk-link" 
            aria-label="Go to Possession Claim Online service (opens in new tab)">
            Possession Claim Online (PCOL) service (opens in new tab)</a>""";

    private static final String FORMS_LINK = """
            <p class="govuk-body">
            <a href="https://www.gov.uk/government/collections/property-possession-forms" 
            rel="noreferrer noopener" target="_blank" class="govuk-link" 
            aria-label="View all property possession forms (opens in new tab)">
            View the full list of property possessions forms (opens in a new tab)</a>.
            </p>""";

    private static final String FOOTER = """
            <div class="govuk-warning-text" role="alert" aria-labelledby="warning-message">
                <span class="govuk-warning-text__icon" aria-hidden="true">!</span>
                <strong class="govuk-warning-text__text">
                    <span class="govuk-visually-hidden">Warning</span>
                    <span id="warning-message">To exit back to the case list, select 'Cancel'</span>
                </strong>
            </div>
            </section>
            """;

}
