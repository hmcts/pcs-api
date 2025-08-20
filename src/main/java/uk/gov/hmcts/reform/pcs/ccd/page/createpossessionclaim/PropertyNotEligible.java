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

/**
 * CCD page configuration for postcode not eligible.
 * This page is shown when a postcode is not eligible for the Possessions Service.
 */
@AllArgsConstructor
@Component
@Slf4j
public class PropertyNotEligible implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("propertyNotEligible", this::midEvent)
            .pageLabel("Property not eligible for this online service")
            .showCondition("showPropertyNotEligiblePage=\"Yes\"")
            .readonly(PCSCase::getShowPropertyNotEligiblePage, NEVER_SHOW)

            // England and Wales guidance section
            .label("propertyNotEligible-england-wales", """
                    <section tabindex="0">
                    <hr class="govuk-section-break govuk-section-break--l govuk-section-break--visible">
                    <h2 class="govuk-heading-m">What to do next</h2>
                    <p class="govuk-body">
                      This service is currently only available for claimants claiming possession of a property
                      in Bedfordshire.
                    </p>
                    <p class="govuk-body">If you're making a claim in another area:</p>

                    <ul class="govuk-list govuk-list--bullet">
                      <li class="govuk-!-font-size-19">
                        <span class="govuk-!-font-weight-bold">For rental or mortgage arrears claims in England</span> –
                        use the <a class="govuk-link" href="https://www.gov.uk/possession-claim-online-recover-property" target="_blank" rel="noopener noreferrer">
                        Possession Claim Online (PCOL) service (opens in new tab)</a>.
                      </li>
                      <li class="govuk-!-font-size-19">
                        <span class="govuk-!-font-weight-bold">For other types of claims in England</span> –
                        use form N5 and the correct particulars of claim form.
                      </li>
                      <li class="govuk-!-font-size-19">
                        <span class="govuk-!-font-weight-bold">For claims in Wales</span> –
                        use form N5 Wales and the correct particulars of claim form.
                      </li>
                    </ul>

                    <p class="govuk-body">
                      <a class="govuk-link" href="https://www.gov.uk/government/collections/property-possession-forms" target="_blank" rel="noopener noreferrer">
                        View the full list of property possessions forms (opens in new tab)
                      </a>
                    </p>

                    <div class="govuk-warning-text">
                      <span class="govuk-warning-text__icon" aria-hidden="true">!</span>
                      <strong class="govuk-warning-text__text">
                        <span class="govuk-visually-hidden">Warning</span>
                          To exit back to the case list, select 'Cancel'
                      </strong>
                    </div>
                    </section>
                    """, "legislativeCountry=\"England\" OR legislativeCountry=\"Wales\"")


            // Scotland-specific guidance section
            .label("propertyNotEligible-scotland", """
                <section tabindex="0">
                <hr class="govuk-section-break govuk-section-break--l govuk-section-break--visible">
                <h2 class="govuk-heading-m">What to do next</h2>
                <p class="govuk-body">
                  This service is only available for claimants claiming possession of a property in England or Wales.
                </p>
                <p class="govuk-body">
                  To make a claim in Scotland, you can use your
                  <a class="govuk-link" href="https://www.scotcourts.gov.uk/home" target="_blank" rel="noopener noreferrer">
                    local sheriff court (opens in new tab)
                  </a>.
                </p>
                <div class="govuk-warning-text">
                  <span class="govuk-warning-text__icon" aria-hidden="true">!</span>
                  <strong class="govuk-warning-text__text">
                    <span class="govuk-visually-hidden">Warning</span>
                      To exit back to the case list, select 'Cancel'
                  </strong>
                </div>
                </section>
                """, "legislativeCountry=\"Scotland\"")

            // Northern Ireland guidance section
            .label("propertyNotEligible-northern-ireland", """
                <section tabindex="0">
                <hr class="govuk-section-break govuk-section-break--l govuk-section-break--visible">
                <h2 class="govuk-heading-m">What to do next</h2>
                <p class="govuk-body">
                  This service is only available for claimants claiming possession of a property in England or Wales.
                </p>
                <p class="govuk-body">
                  To make a claim in Northern Ireland, you can use the
                  <a class="govuk-link" href="https://www.nidirect.gov.uk/articles/enforcement-civil-court-orders-northern-ireland" target="_blank" rel="noopener noreferrer">
                    Enforcement of Judgments Office (EJO) (opens in new tab)
                  </a>.
                </p>
                <div class="govuk-warning-text">
                  <span class="govuk-warning-text__icon" aria-hidden="true">!</span>
                  <strong class="govuk-warning-text__text">
                    <span class="govuk-visually-hidden">Warning</span>
                      To exit back to the case list, select 'Cancel'
                  </strong>
                </div>
                </section>
                """, "legislativeCountry=\"Northern Ireland\"")

            // Channel Islands and Isle of Man guidance section
            .label("propertyNotEligible-channel-islands-iom", """
                <section tabindex="0">
                <p class="govuk-body">
                  This service is only available for claimants claiming possession of a property in England or Wales.
                </p>
                <div class="govuk-warning-text">
                  <span class="govuk-warning-text__icon" aria-hidden="true">!</span>
                  <strong class="govuk-warning-text__text">
                    <span class="govuk-visually-hidden">Warning</span>
                      To exit back to the case list, select 'Cancel'
                  </strong>
                </div>
                </section>
                """, "legislativeCountry=\"Channel Islands\" OR legislativeCountry=\"Isle of Man\"");
    }


    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                   CaseDetails<PCSCase, State> detailsBefore) {
        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .errors(List.of("Property not eligible for this online service"))
            .build();
    }

}
