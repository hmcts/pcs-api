package uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim;

import static uk.gov.hmcts.ccd.sdk.api.ShowCondition.when;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.Event.EventBuilder;
import uk.gov.hmcts.ccd.sdk.api.ShowCondition;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.dto.CreateClaimData;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;

/**
 * CCD page configuration for postcode not eligible.
 * This page is shown when a postcode is not eligible for the Possessions Service.
 */
@AllArgsConstructor
@Component
@Slf4j
public class PropertyNotEligible {

    public void addTo(EventBuilder<CreateClaimData, UserRole, State> eventBuilder) {
        ShowCondition showEnglandOrWales = when(CreateClaimData::getLegislativeCountry)
            .isAnyOf(LegislativeCountry.ENGLAND, LegislativeCountry.WALES);
        ShowCondition showScotland = when(CreateClaimData::getLegislativeCountry)
            .is(LegislativeCountry.SCOTLAND);
        ShowCondition showNorthernIreland = when(CreateClaimData::getLegislativeCountry)
            .is(LegislativeCountry.NORTHERN_IRELAND);
        ShowCondition showChannelIslandsOrIom = when(CreateClaimData::getLegislativeCountry)
            .isAnyOf(LegislativeCountry.CHANNEL_ISLANDS, LegislativeCountry.ISLE_OF_MAN);

        eventBuilder.fields()
            .page("propertyNotEligible", this::midEvent)
            .pageLabel("Property not eligible for this online service")
            .showCondition(when(CreateClaimData::getShowPropertyNotEligiblePage).is(YesOrNo.YES))
            .hidden(CreateClaimData::getShowPropertyNotEligiblePage)

            // England and Wales guidance section
            .label("propertyNotEligible-england-wales", """
                    <section tabindex="0">
                    <hr class="govuk-section-break govuk-section-break--l govuk-section-break--visible">
                    <h2 class="govuk-heading-m">What to do next</h2>
                    <p class="govuk-body">
                      This service is currently only available for claimants claiming possession of a property
                      in Bedfordshire.
                    </p>
                    <p class="govuk-body">If you’re making a claim in another area:</p>

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
                          To exit back to the case list, select ‘Cancel’
                      </strong>
                    </div>
                    </section>
                    """, showEnglandOrWales)


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
                  <a class="govuk-link" href="https://www.scotcourts.gov.uk/home" target="_blank" rel="noopener noreferrer">local sheriff court (opens in new tab)</a>.
                </p>
                <div class="govuk-warning-text">
                  <span class="govuk-warning-text__icon" aria-hidden="true">!</span>
                  <strong class="govuk-warning-text__text">
                    <span class="govuk-visually-hidden">Warning</span>
                      To exit back to the case list, select ‘Cancel’
                  </strong>
                </div>
                </section>
                """, showScotland)

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
                  <a class="govuk-link" href="https://www.nidirect.gov.uk/articles/enforcement-civil-court-orders-northern-ireland" target="_blank" rel="noopener noreferrer">Enforcement of Judgments Office (EJO) (opens in new tab)</a>.
                </p>
                <div class="govuk-warning-text">
                  <span class="govuk-warning-text__icon" aria-hidden="true">!</span>
                  <strong class="govuk-warning-text__text">
                    <span class="govuk-visually-hidden">Warning</span>
                      To exit back to the case list, select ‘Cancel’
                  </strong>
                </div>
                </section>
                """, showNorthernIreland)

            // Channel Islands and Isle of Man guidance section
            .label("propertyNotEligible-channel-islands-iom", """
                <section tabindex="0">
                <hr class="govuk-section-break govuk-section-break--l govuk-section-break--visible">
                <p class="govuk-body">
                  This service is only available for claimants claiming possession of a property in England or Wales.
                </p>
                <div class="govuk-warning-text">
                  <span class="govuk-warning-text__icon" aria-hidden="true">!</span>
                  <strong class="govuk-warning-text__text">
                    <span class="govuk-visually-hidden">Warning</span>
                      To exit back to the case list, select ‘Cancel’
                  </strong>
                </div>
                </section>
                """, showChannelIslandsOrIom);
    }


    private AboutToStartOrSubmitResponse<CreateClaimData, State> midEvent(
        CaseDetails<CreateClaimData, State> details,
        CaseDetails<CreateClaimData, State> detailsBefore) {
        return AboutToStartOrSubmitResponse.<CreateClaimData, State>builder()
            .errors(List.of("Property not eligible for this online service"))
            .build();
    }

}
