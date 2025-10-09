package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.DemotionOfTenancy;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.SuspensionOfRightToBuyDemotionOfTenancy;

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
                .mandatory(DemotionOfTenancy::getStatementOfExpressTermsServed,
                           "suspensionToBuyDemotionOfTenancyPages=\"No\"")
                .mandatory(DemotionOfTenancy::getStatementOfExpressTermsDetails,
                    "statementOfExpressTermsServed=\"YES\""
                        + " AND suspensionToBuyDemotionOfTenancyPages=\"No\"")
                .done()
                .complex(PCSCase::getSuspensionOfRightToBuyDemotionOfTenancy)
                .mandatory(SuspensionOfRightToBuyDemotionOfTenancy::getHasServedStatementExpressTerms,
                           "suspensionToBuyDemotionOfTenancyPages=\"Yes\"")
                .mandatory(SuspensionOfRightToBuyDemotionOfTenancy::getExpressTermsDetails,
                       "hasServedStatementExpressTerms=\"YES\""
                           + " AND suspensionToBuyDemotionOfTenancyPages=\"Yes\"")
                .done();
    }
}
