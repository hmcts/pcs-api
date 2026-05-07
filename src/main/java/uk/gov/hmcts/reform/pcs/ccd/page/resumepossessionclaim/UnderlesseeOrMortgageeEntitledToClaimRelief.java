package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent;

public class UnderlesseeOrMortgageeEntitledToClaimRelief implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
                .page("underlesseeMortgageeEntitledToClaimRelief")
                .pageLabel("Underlessee or mortgagee entitled to claim relief against forfeiture")
                .label("underlesseeMortgageeEntitledToClaimRelief-info", """
                   ---
                   <p class="govuk-body-m">
                    You must tell us if there is an underlessee (a subtenant) or a mortgagee (a mortgage lender)
                    who has a legal right to ask the court to let a lease continue, even though the landlord has tried
                    to end it.
                   </p>
                   """)
            .mandatory(PCSCase::getHasUnderlesseeOrMortgagee)
            .label("underlesseeMortgageeEntitledToClaimRelief-saveAndReturn", CommonPageContent.SAVE_AND_RETURN);
    }
}
