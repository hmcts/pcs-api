package uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.writ;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.writ.WritDetails;
import uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.ShowConditionsWarrantOrWrit;

public class ConfirmHCEOfficerPage implements CcdPageConfiguration {

    public static final String HCEO_INFORMATION = """
                    <details class="govuk-details">
                        <summary class="govuk-details__summary">
                            <span class="govuk-details__summary-text">
                                I do not know if I need to hire a High Court enforcement officer
                            </span>
                        </summary>
                        <div class="govuk-details__text">
                            <p class="govuk-body">A High Court enforcement officer (HCEO) is a type of bailiff. If your
                            application for a writ is successful, a High Court enforcement officer will carry out the
                            eviction.
                            </p>
                            <p class="govuk-body">You can either choose your own HCEO, or we will choose one for you.
                            The cost is the same, but if you have not chosen one then you will need to send a copy of
                            your writ to the National Enforcement Office (we’ll send you an email explaining where to
                            send it to).
                            </p>
                            <p class="govuk-body govuk-!-font-weight-bold">If you want to choose your own High Court
                             enforcement officer
                            </p>
                            <p class="govuk-body">It usually costs around £1000.00 to hire a High Court enforcement
                             officer to do an eviction.
                            </p>
                            <p class="govuk-body govuk-!-margin-bottom-1">You may need to pay additional fees for things
                             like:</p>
                            <ul class="govuk-list govuk-list--bullet">
                              <li class="govuk-!-font-size-19">hiring a dog handler (if you are trying to evict someone
                               who owns an aggressive dog)
                              </li>
                              <li class="govuk-!-font-size-19 govuk-!-padding-bottom-1">hiring a locksmith to secure the
                               property
                              </li>
                            </ul>
                            <p class="govuk-body govuk-!-margin-bottom-1">You can either:
                            </p>
                            <ul class="govuk-list govuk-list--bullet">
                              <li class="govuk-!-font-size-19">
                               <a href="https://certificatedbailiffs.justice.gov.uk/searchPublic.do?search"
                               rel="noreferrer noopener" target="_blank" class="govuk-link">search for a High Court
                               enforcement officer using your postcode</a>
                              </li>
                              <li class="govuk-!-font-size-19">
                               <a href="https://www.hceoa.org.uk/choosing-a-hceo"
                               rel="noreferrer noopener" target="_blank" class="govuk-link">check the full list of High
                               Court enforcement officers</a> and choose one in your area
                              </li>
                              <li class="govuk-!-font-size-19 govuk-!-padding-bottom-1">search for an HCEO using a
                               search engine
                              </li>
                            </ul>
                            <p class="govuk-body">We’ll ask you to provide the name of your High Court enforcement
                            officer on the next page.
                            </p>
                        </div>
                    </details>
                    """;

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("confirmHCEOfficer")
            .pageLabel("Confirm if you have already hired a High Court enforcement officer")
            .showCondition(ShowConditionsWarrantOrWrit.WRIT_FLOW)
            .label("confirmHCEOfficer-line-separator", "---")
            .complex(PCSCase::getEnforcementOrder)
            .complex(EnforcementOrder::getWritDetails)
            .mandatory(WritDetails::getHasHiredHighCourtEnforcementOfficer)
            .done()
            .done()
            .label("confirmHCEOfficer-notice", HCEO_INFORMATION)
            .label("confirmHCEOfficer-saveAndReturn", CommonPageContent.SAVE_AND_RETURN);
    }
}
