package uk.gov.hmcts.reform.pcs.ccd.event;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.ConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.UserRole;
import uk.gov.hmcts.reform.pcs.entity.Party;
import uk.gov.hmcts.reform.pcs.repository.PCSCaseRepository;
import uk.gov.hmcts.reform.pcs.repository.PartyRepository;

@Component
public class EditDraft implements CCDConfig<PCSCase, State, UserRole> {
    @Autowired
    private PCSCaseRepository cases;

    @Autowired
    private PartyRepository parties;

    // Smoke and mirrors super hack
    private static PCSCase draft;

    @Override
    public void configure(ConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        configBuilder
            .decentralisedEvent("editDraft", this::aboutToSubmit, this::rehydrate)
            .forState(State.PreSubmission)
            .name("Resume drafting")
            .grant(Permission.CRUD, UserRole.CASE_WORKER)
            .fields()
            .page("Organisation name", this::mid)
            .label("edit", """
                # Organisation name
                
                """)
            .readonly(PCSCase::getOrganisationName)
                .mandatory(PCSCase::getIsNameCorrect)
            .optional(PCSCase::getEditOrganisationName, "isNameCorrect=\"No\"")
            .page("Contact preferences")
            .label("contactPreferences", """
                # Contact preferences
                
                Your current logged in email is
                
                housingofficer@lutonlocalhousing.org
                
                """)
                .mandatory(PCSCase::getUseSameEmail)
            .done();
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> mid(CaseDetails<PCSCase, State> pcsCaseStateCaseDetails,
                                                             CaseDetails<PCSCase, State> pcsCaseStateCaseDetails1) {

        draft = pcsCaseStateCaseDetails.getData();
        draft.setOrganisationName(draft.getEditOrganisationName());
        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(pcsCaseStateCaseDetails.getData())
            .build();
    }

    private PCSCase rehydrate(EventPayload<PCSCase, State> pcsCaseStateEventPayload) {
        if (draft != null) {
            draft.setIsNameCorrect(YesOrNo.YES);
        }
        return draft != null
            ? draft
            : pcsCaseStateEventPayload.caseData();
    }

    private void aboutToSubmit(EventPayload<PCSCase, State> pcsCaseStateEventPayload) {

    }
}
