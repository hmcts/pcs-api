package uk.gov.hmcts.reform.pcs.ccd.event;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.ConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.reform.pcs.ccd.domain.PcsCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;

import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.createPossessionClaim;

@Component
@AllArgsConstructor
public class CreatePossessionCase implements CCDConfig<PcsCase, State, UserRole> {

    private final PcsCaseService pcsCaseService;

    @Override
    public void configure(ConfigBuilder<PcsCase, State, UserRole> configBuilder) {
        configBuilder
            .decentralisedEvent(createPossessionClaim.name(), this::submit)
            .initialState(State.Draft)
            .name("Make a possession claim")
            .grant(Permission.CRUD, UserRole.APPLICANT_SOLICITOR)
            .grant(Permission.R, UserRole.CIVIL_CASE_WORKER)
            .showSummary(true)
            .endButtonLabel("Save Application")
            .fields()
            .page("claimAddress")
                .pageLabel("What is the address of the property you are claiming possession of?")
                .optional(PcsCase::getClaimAddress)
            .page("groundsForPossession")
                .pageLabel("Grounds for possession")
                .label("groundForPossessionLabel", getGroundsForPossessionLabel())
                .mandatory(PcsCase::getGroundsForPossession)
            .page("additionalInformation")
                .pageLabel("Additional Information")
                .optional(PcsCase::getGeneralNotes)
            .done();
    }

    private String getGroundsForPossessionLabel() {
        return """
                You should have already given the defendants notice of your intention to
                begin possession proceedings. On this notice, you should have written which
                grounds you are making your claim under. You should select these grounds here
                and select any extra grounds you would like to add to your claim, if you need to.
            """;
    }

    private void submit(EventPayload<PcsCase, State> eventPayload) {
        long caseReference = eventPayload.caseReference();
        PcsCase pcsCase = eventPayload.caseData();

        pcsCaseService.createCase(caseReference, pcsCase);

        // TODO: Assign role before or after case creation?
        // roleService.assignCaseRoleToCurrentUser(UserRole.APPLICANT_SOLICITOR, caseReference);

    }


}
