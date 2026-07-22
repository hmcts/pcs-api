package uk.gov.hmcts.reform.pcs.ccd.event.legalrepcontactdetails;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.DecentralisedConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.Event;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.LegalRepresentativeDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.legalrepresentativedetails.LegalRepresentativeContactDetailsPage;
import uk.gov.hmcts.reform.pcs.ccd.service.legalrepresentative.LegalRepresentativePageService;
import uk.gov.hmcts.reform.pcs.reference.service.OrganisationService;

import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.legalRepresentativeContactDetails;

@Slf4j
@Component
@AllArgsConstructor
public class LegalRepresentativeContactDetails implements CCDConfig<PCSCase, State, UserRole> {

    private final LegalRepresentativeContactDetailsPage legalRepresentativeContactDetailsPage;
    private final OrganisationService organisationService;
    private final LegalRepresentativePageService legalRepresentativePageService;

    @Override
    public void configureDecentralised(DecentralisedConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        Event.EventBuilder<PCSCase, UserRole, State> eventBuilder =
            configBuilder
                .decentralisedEvent(legalRepresentativeContactDetails.name(), this::submit, this::start)
                .forAllStates()
                .name("Amend representative's details")
                .grant(Permission.CRUD, UserRole.DEFENDANT_SOLICITOR);

        new PageBuilder(eventBuilder)
            .add(legalRepresentativeContactDetailsPage);
    }

    private PCSCase start(EventPayload<PCSCase, State> eventPayload) {
        PCSCase pcsCase = eventPayload.caseData();
        Long caseReference = eventPayload.caseReference();

        String organisationId = organisationService.getOrganisationIdForCurrentUser();

        LegalRepresentativeDetails legalRepresentativeDetails =
            legalRepresentativePageService.retrieveLegalRepresentativeDetails(
                organisationId,
                caseReference,
                pcsCase.getLegalRepresentativeDetails()
        );

        pcsCase.setLegalRepresentativeDetails(legalRepresentativeDetails);

        return pcsCase;
    }

    private SubmitResponse<State> submit(EventPayload<PCSCase, State> eventPayload) {
        String organisationId = organisationService.getOrganisationIdForCurrentUser();
        PCSCase pcsCase = eventPayload.caseData();
        Long caseReference = eventPayload.caseReference();
        legalRepresentativePageService.save(organisationId, caseReference, pcsCase.getLegalRepresentativeDetails());
        return SubmitResponse.<State>builder()
            .confirmationBody(getUpdatedInformationConfirmationMarkdown())
            .build();
    }

    private String getUpdatedInformationConfirmationMarkdown() {
        return """
            ---
            <div class="govuk-panel govuk-panel--confirmation govuk-!-padding-top-3 govuk-!-padding-bottom-3">
            <span class="govuk-panel__title govuk-!-font-size-36">You have updated a defendant's
                    legal representative's information</span>
            </div>
            """;
    }

}
