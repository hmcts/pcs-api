package uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;

import java.util.List;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.NEVER_SHOW;

@AllArgsConstructor
@Component
@Slf4j
public class PostcodeNotAssignedToCourt implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("postcodeNotAssignedToCourt", this::midEvent)
            .pageLabel("You cannot use this online service")
            .showCondition("showPostcodeNotAssignedToCourt=\"Yes\"")
            .readonly(PCSCase::getShowPostcodeNotAssignedToCourt, NEVER_SHOW)
            .readonly(PCSCase::getPostcodeNotAssignedView, NEVER_SHOW)
            .readonly(PCSCase::getSelectedLegislativeCountry, NEVER_SHOW)
            .label("postcodeNotAssignedToCourt-info", ALL_COUNTRIES_CONTENT);
    }

    protected String generateContent(PCSCase caseData) {
        String view = caseData.getPostcodeNotAssignedView();
        
        if (view == null) {
            return generateAllCountriesContent();
        }
        
        return switch (view) {
            case "ALL_COUNTRIES" -> generateAllCountriesContent();
            case "ENGLAND" -> generateEnglandContent();
            case "WALES" -> generateWalesContent();
            default -> generateAllCountriesContent();
        };
    }

    private static final String ALL_COUNTRIES_CONTENT = """
            ---
            <section tabindex="0">
            <p class="govuk-body">
            Based on the postcode you provided, we cannot determine which court your claim should be assigned to, 
            so you cannot use this online service.
            </p>

            <h3 class="govuk-heading-s govuk-!-font-size-19">What to do next</h3>

            <ul class="govuk-list govuk-list--bullet">
                <li><span class="govuk-!-font-weight-bold">For rental or mortgage arrears claims in England</span> – 
                use the <a href="https://www.gov.uk/possession-claim-online-recover-property" 
                rel="noreferrer noopener" target="_blank" class="govuk-link" 
                aria-label="Go to Possession Claim Online service (opens in new tab)">
                Possession Claim Online (PCOL) service (opens in new tab)</a>.</li>
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
                rel="noreferrer noop    ener" target="_blank" class="govuk-link" 
                aria-label="Go to Enforcement of Judgments Office website (opens in new tab)">
                Enforcement of Judgments Office (EJO) (opens in new tab)</a>.</li>
            </ul>

            <p class="govuk-body">
            <a href="https://www.gov.uk/government/collections/property-possession-forms" 
            rel="noreferrer noopener" target="_blank" class="govuk-link" 
            aria-label="View all property possession forms (opens in new tab)">
            View the full list of property possessions forms (opens in a new tab)</a>.
            </p>

            <div class="govuk-warning-text" role="alert" aria-labelledby="warning-message">
                <span class="govuk-warning-text__icon" aria-hidden="true">!</span>
                <strong class="govuk-warning-text__text">
                    <span class="govuk-visually-hidden">Warning</span>
                    <span id="warning-message">To exit back to the case list, select 'Cancel'</span>
                </strong>
            </div>
            </section>
            """;

    private static final String ENGLAND_CONTENT = """
            ---
            <section tabindex="0">
            <p class="govuk-body">
            Based on the postcode you provided, we cannot determine which court your claim should be assigned to, 
            so you cannot use this online service.
            </p>

            <h3 class="govuk-heading-s govuk-!-font-size-19">What to do next</h3>

            <ul class="govuk-list govuk-list--bullet">
                <li><span class="govuk-!-font-weight-bold">For rental or mortgage arrears claims</span> – 
                use the <a href="https://www.gov.uk/possession-claim-online-recover-property" 
                rel="noreferrer noopener" target="_blank" class="govuk-link" 
                aria-label="Go to Possession Claim Online service (opens in new tab)">
                Possession Claim Online (PCOL) service (opens in new tab)</a>.</li>
                <li><span class="govuk-!-font-weight-bold">For other types of claims</span> – fill in form 
                N5 and the correct particulars of claim form.</li>
            </ul>

            <p class="govuk-body">
            <a href="https://www.gov.uk/government/collections/property-possession-forms" 
            rel="noreferrer noopener" target="_blank" class="govuk-link" 
            aria-label="View all property possession forms (opens in new tab)">
            View the full list of property possessions forms (opens in a new tab)</a>.
            </p>

            <div class="govuk-warning-text" role="alert" aria-labelledby="warning-message">
                <span class="govuk-warning-text__icon" aria-hidden="true">!</span>
                <strong class="govuk-warning-text__text">
                    <span class="govuk-visually-hidden">Warning</span>
                    <span id="warning-message">To exit back to the case list, select 'Cancel'</span>
                </strong>
            </div>
            </section>
            """;

    private static final String WALES_CONTENT = """
            ---
            <section tabindex="0">
            <p class="govuk-body">
            Based on the postcode you provided, we cannot determine which court your claim should be assigned to, 
            so you cannot use this online service.
            </p>

            <h3 class="govuk-heading-s govuk-!-font-size-19">What to do next</h3>

            <p class="govuk-body">Use form N5 Wales and the correct particulars of claim form.</p>

            <p class="govuk-body">
            <a href="https://www.gov.uk/government/collections/property-possession-forms" 
            rel="noreferrer noopener" target="_blank" class="govuk-link" 
            aria-label="View all property possession forms (opens in new tab)">
            View the full list of property possessions forms (opens in a new tab)</a>.
            </p>

            <div class="govuk-warning-text" role="alert" aria-labelledby="warning-message">
                <span class="govuk-warning-text__icon" aria-hidden="true">!</span>
                <strong class="govuk-warning-text__text">
                    <span class="govuk-visually-hidden">Warning</span>
                    <span id="warning-message">To exit back to the case list, select 'Cancel'</span>
                </strong>
            </div>
            </section>
            """;

    private String generateAllCountriesContent() {
        return ALL_COUNTRIES_CONTENT;
    }

    private String generateEnglandContent() {
        return ENGLAND_CONTENT;
    }

    private String generateWalesContent() {
        return WALES_CONTENT;
    }

    protected AboutToStartOrSubmitResponse<PCSCase, State> midEvent(
        CaseDetails<PCSCase, State> details,
        CaseDetails<PCSCase, State> detailsBefore) {

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .errors(List.of("You're not eligible for this online service"))
            .build();
    }
}
