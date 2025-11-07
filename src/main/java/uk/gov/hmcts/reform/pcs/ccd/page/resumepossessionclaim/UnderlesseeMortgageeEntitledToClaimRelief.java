package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.UnderlesseeMortgagee;

import static uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent.SAVE_AND_RETURN;

public class UnderlesseeMortgageeEntitledToClaimRelief implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
                .page("underlesseeMortgageeEntitledToClaimRelief")
                .pageLabel("Underlessee or mortgagee entitled to claim relief against forfeiture")
                .label("underlesseeMortgageeEntitledToClaimRelief-info", """
                   ---
                   <p class="govuk-body-m" tabindex="0">
                    You must tell us if there is an underlessee (a subtenant) or a mortgagee (a mortgage lender)
                    who has a legal right to ask the court to let a lease continue, even though the landlord has tried
                    to end it.
                   </p>
                   """)
            .complex(PCSCase::getUnderlesseeMortgagee)
            .mandatory(UnderlesseeMortgagee::getHasUnderlesseeOrMortgagee)
            .done()
            .label("underlesseeMortgageeEntitledToClaimRelief-save-and-return", SAVE_AND_RETURN);
    }
}
