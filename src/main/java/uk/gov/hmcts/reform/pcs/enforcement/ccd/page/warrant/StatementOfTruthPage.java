package uk.gov.hmcts.reform.pcs.enforcement.ccd.page.warrant;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.enforcement.domain.common.StatementOfTruthDetailsEnforcement;
import uk.gov.hmcts.reform.pcs.enforcement.domain.warrant.WarrantDetails;
import uk.gov.hmcts.reform.pcs.enforcement.ccd.page.ShowConditionsEnforcementType;

import static uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent.SAVE_AND_RETURN;

public class StatementOfTruthPage implements CcdPageConfiguration {

    private static final String WARRANT_COMPLETED_BY_CLAIMANT = "warrantCompletedBy=\"CLAIMANT\"";
    private static final String WARRANT_COMPLETED_BY_LEGAL_REP = "warrantCompletedBy=\"LEGAL_REPRESENTATIVE\"";

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("statementOfTruth")
            .pageLabel("Statement of truth")
            .showCondition(ShowConditionsEnforcementType.WARRANT_FLOW)
            .complex(PCSCase::getEnforcementOrder)
                .complex(EnforcementOrder::getWarrantDetails)
                    .label("statementOfTruth-lineSeparator", "---")
                    .complex(WarrantDetails::getStatementOfTruth)
                        .mandatoryNoSummary(StatementOfTruthDetailsEnforcement::getCertification)
                        .mandatory(StatementOfTruthDetailsEnforcement::getCompletedBy)
                        .mandatoryNoSummary(StatementOfTruthDetailsEnforcement::getAgreementClaimant,
                            WARRANT_COMPLETED_BY_CLAIMANT, null)
                        .mandatory(StatementOfTruthDetailsEnforcement::getFullNameParty,
                            WARRANT_COMPLETED_BY_CLAIMANT)
                        .mandatory(StatementOfTruthDetailsEnforcement::getPositionParty,
                            WARRANT_COMPLETED_BY_CLAIMANT)
                        .mandatoryNoSummary(StatementOfTruthDetailsEnforcement::getAgreementClaimantLegalRep,
                            WARRANT_COMPLETED_BY_LEGAL_REP, null)
                        .mandatory(StatementOfTruthDetailsEnforcement::getFullNameLegalRep,
                            WARRANT_COMPLETED_BY_LEGAL_REP)
                        .mandatory(StatementOfTruthDetailsEnforcement::getFirmNameLegalRep,
                            WARRANT_COMPLETED_BY_LEGAL_REP)
                        .mandatory(StatementOfTruthDetailsEnforcement::getPositionLegalRep,
                            WARRANT_COMPLETED_BY_LEGAL_REP)
                    .done()
                .done()
            .done()
            .label("statementOfTruth-saveAndReturn", SAVE_AND_RETURN);
    }

}

