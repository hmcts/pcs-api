package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.DemotionOfTenancy;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;

public class StatementOfExpressTerms implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("statementOfExpressTerms")
            .pageLabel("Statement of express terms")
            .showCondition("showDemotionOfTenancyHousingActsPage=\"Yes\""
                               + " OR suspensionToBuyDemotionOfTenancyPages=\"Yes\"")
            .label("statementOfExpressTerms-info", "---")
                .complex(PCSCase::getDemotionOfTenancy)
                .mandatory(DemotionOfTenancy::getStatementOfExpressTermsServed)
                .mandatory(DemotionOfTenancy::getStatementOfExpressTermsDetails,
                           "statementOfExpressTermsServed=\"YES\"")
                .done();
    }
}
