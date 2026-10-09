package uk.gov.hmcts.reform.pcs.ccd.page.claimantmakeanapplication;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.ShowConditions;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.XuiGenAppRequest;
import uk.gov.hmcts.reform.pcs.ccd.domain.statementoftruth.StatementOfTruthCompletedBy;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;

@Slf4j
@Component
@RequiredArgsConstructor
public class StatementOfTruth implements CcdPageConfiguration {

    private static final String CLAIMANT_SHOW_CONDITION = ShowConditions.fieldEquals(
        "xui_genapp_SotCompletedBy",
        StatementOfTruthCompletedBy.CLAIMANT
    );
    private static final String LEGAL_REP_SHOW_CONDITION = ShowConditions.fieldEquals(
        "xui_genapp_SotCompletedBy",
        StatementOfTruthCompletedBy.LEGAL_REPRESENTATIVE
    );

    private static final String INFO_MARKDOWN = """
        <p class="govuk-body">
          I understand that proceedings for contempt of court may be brought against anyone who makes, or causes to be
          made, a false statement in a document verified by a statement of truth without an honest belief in its truth.
        </p>
        """;

    private final PcsCaseService pcsCaseService;
    private final PartyService partyService;

    @Override
    public void addTo(PageBuilder pageBuilder) {

        pageBuilder
            .page("statementOfTruth", this::midEvent)
            .pageLabel("Statement of truth")
            .label("cma-statementOfTruth-lineSeparator", "---")
            .label("cma-statementOfTruth-info", INFO_MARKDOWN)
            .complex(PCSCase::getXuiGenAppRequest)
            .readonly(XuiGenAppRequest::getApplicantPartyId, ShowConditions.NEVER_SHOW, true)
            .mandatory(XuiGenAppRequest::getSotCompletedBy)
            .mandatory(XuiGenAppRequest::getAgreementClaimant, CLAIMANT_SHOW_CONDITION)
            .mandatory(XuiGenAppRequest::getAgreementClaimantLegalRep, LEGAL_REP_SHOW_CONDITION)
            .mandatory(XuiGenAppRequest::getSotFullName,
                       ShowConditions.or(CLAIMANT_SHOW_CONDITION, LEGAL_REP_SHOW_CONDITION))
            .mandatory(XuiGenAppRequest::getSotFirmName, LEGAL_REP_SHOW_CONDITION)
            .mandatory(XuiGenAppRequest::getSotPositionHeld,
                       ShowConditions.or(CLAIMANT_SHOW_CONDITION, LEGAL_REP_SHOW_CONDITION))
            .done();
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> detailsBefore) {

        long caseReference = details.getId();
        PCSCase caseData = details.getData();
        XuiGenAppRequest xuiGenAppRequest = caseData.getXuiGenAppRequest();

        PcsCaseEntity pcsCaseEntity = pcsCaseService.loadCase(caseReference);
        PartyEntity primaryClaimantParty = partyService.getPrimaryClaimantPartyEntity(pcsCaseEntity);

        xuiGenAppRequest.setApplicantPartyId(primaryClaimantParty.getId().toString());

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(caseData)
            .build();
    }

}
