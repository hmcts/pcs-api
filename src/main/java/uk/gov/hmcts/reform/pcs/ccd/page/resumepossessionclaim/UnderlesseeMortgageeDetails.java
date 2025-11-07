package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.UnderlesseeMortgagee;

import static uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent.SAVE_AND_RETURN;

public class UnderlesseeMortgageeDetails  implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("underlesseeMortgageeDetails")
            .pageLabel("Underlessee or mortgagee details")
            .showCondition("hasUnderlesseeOrMortgagee=\"YES\"")
            .complex(PCSCase::getUnderlesseeMortgagee)
            .label("underlesseeMortgagee-name", """
                ---
                <h2 class="govuk-heading-m"> Underlessee or mortgagee name </h2>
                """)
            .mandatory(UnderlesseeMortgagee::getUnderlesseeOrMortgageeNameKnown)

            .label("underlesseeMortgagee-address", """
                ---
                <h2 class="govuk-heading-m">Underlessee or mortgagee address </h2>
                """)
            .mandatory(UnderlesseeMortgagee::getUnderlesseeOrMortgageeAddressKnown)

            .label("underlesseeMortgagee-additional", """
                ---
                <h2 class="govuk-heading-m">Additional underlessee or mortgagee?</h2>
                """)
            .mandatory(UnderlesseeMortgagee::getAddAdditionalUnderlesseeOrMortgagee)
            .done()
            .label("underlesseeMortgageeDetails-save-and-return", SAVE_AND_RETURN);
    }
}
