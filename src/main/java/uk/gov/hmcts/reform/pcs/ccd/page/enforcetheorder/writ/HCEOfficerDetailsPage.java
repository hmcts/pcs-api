package uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.writ;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.writ.WritDetails;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.ShowConditionsWarrantOrWrit;

import static uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent.SAVE_AND_RETURN;

@AllArgsConstructor
@Component
public class HCEOfficerDetailsPage implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("hCEOfficerDetails")
            .pageLabel("Your High Court enforcement officer")
            .showCondition(ShowConditionsWarrantOrWrit.WRIT_FLOW
                               + " AND writHasHiredHighCourtEnforcementOfficer=\"YES\"")
            .label("hCEOfficerDetails-line-separator", "---")
            .label(
                "hCEOfficerDetails-information-text", """
                    <p class="govuk-body govuk-!-font-weight-bold govuk-!-font-size-24">Who have you hired to carry out
                    your eviction?</p>
                    """
            )
            .complex(PCSCase::getEnforcementOrder)
            .complex(EnforcementOrder::getWritDetails)
            .mandatory(WritDetails::getHceoDetails)
            .done()
            .done()
            .label("hCEOfficerDetails-saveAndReturn", SAVE_AND_RETURN);
    }
}
