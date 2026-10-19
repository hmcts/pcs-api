package uk.gov.hmcts.reform.pcs.enforcement.ccd.page.writ;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;

import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementOrder;

import static uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent.SAVE_AND_RETURN;

import uk.gov.hmcts.reform.pcs.enforcement.domain.common.StatementOfTruthDetailsEnforcement;
import uk.gov.hmcts.reform.pcs.enforcement.domain.writ.WritDetails;
import uk.gov.hmcts.reform.pcs.enforcement.ccd.page.ShowConditionsEnforcementType;

/**
 * Shown in the writ of possession and writ of restitution journeys, which record the statement of truth in the
 * same fields. Only the document the claimant asks the High Court to enforce by differs.
 */
public class StatementOfTruthWritPage implements CcdPageConfiguration {

    private static final String WRIT_COMPLETED_BY_CLAIMANT = "writCompletedBy=\"CLAIMANT\"";
    private static final String WRIT_COMPLETED_BY_LEGAL_REPRESENTATIVE = "writCompletedBy=\"LEGAL_REPRESENTATIVE\"";

    private static final String CERTIFICATION_DETAILS = """
        <div class="govuk-!-margin-left-6 govuk-!-margin-top-0">
            <ul class="govuk-list govuk-list--bullet govuk-!-margin-top-0">
                <li>
                    The details I have given are correct and that to my knowledge
                    there is no application or other procedure pending.
                </li>
                <li>
                    I request an order for enforcement in the High Court by
                    %s.
                </li>
                <li>
                    I believe that the facts stated in this form are true.
                </li>
            </ul>
        </div>
        """;

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("statementOfTruthWrit")
            .pageLabel("Statement of truth")
            .showCondition(ShowConditionsEnforcementType.WRIT_OR_RESTITUTION_FLOW)
            .label("statementOfTruthWrit-line-separator", "---")
            .complex(PCSCase:: getEnforcementOrder)
            .complex(EnforcementOrder::getWritDetails)
            .complex(WritDetails::getStatementOfTruth)
            .mandatoryNoSummary(StatementOfTruthDetailsEnforcement::getCertification)
            .label(
                "statementOfTruthWrit-certification-details",
                CERTIFICATION_DETAILS.formatted("Writ of Possession"),
                ShowConditionsEnforcementType.WRIT_FLOW
            )
            .label(
                "statementOfTruthWrit-certification-details-restitution",
                CERTIFICATION_DETAILS.formatted("Writ of Restitution"),
                ShowConditionsEnforcementType.WRIT_OF_RESTITUTION_FLOW
            )
            .label(
                "statementOfTruthWrit-completedBy-heading",
                """
                <p class="govuk-body govuk-!-font-weight-bold">
                    Completed by
                </p>
                """
            )
            .mandatory(StatementOfTruthDetailsEnforcement::getCompletedBy,null,
                       null,
                       " ")
            .mandatoryNoSummary(StatementOfTruthDetailsEnforcement::getAgreementClaimant,
                       WRIT_COMPLETED_BY_CLAIMANT, null
            )
            .mandatory(StatementOfTruthDetailsEnforcement::getFullNameParty,
                       WRIT_COMPLETED_BY_CLAIMANT
            )
            .mandatory(StatementOfTruthDetailsEnforcement::getPositionParty,
                       WRIT_COMPLETED_BY_CLAIMANT
            )
            .mandatoryNoSummary(StatementOfTruthDetailsEnforcement::getAgreementClaimantLegalRep,
                       WRIT_COMPLETED_BY_LEGAL_REPRESENTATIVE, null
            )
            .mandatory(StatementOfTruthDetailsEnforcement::getFullNameLegalRep,
                       WRIT_COMPLETED_BY_LEGAL_REPRESENTATIVE
            )
            .mandatory(StatementOfTruthDetailsEnforcement::getFirmNameLegalRep,
                       WRIT_COMPLETED_BY_LEGAL_REPRESENTATIVE
            )
            .mandatory(StatementOfTruthDetailsEnforcement::getPositionLegalRep,
                       WRIT_COMPLETED_BY_LEGAL_REPRESENTATIVE
            )
            .done()
            .done()
            .done()
            .label("statementOfTruthWrit-saveAndReturn", SAVE_AND_RETURN);
    }
}
