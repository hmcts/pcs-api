package uk.gov.hmcts.reform.pcs.ccd.event;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.ConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;

import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.createTestApplication;

@Component
@AllArgsConstructor
public class CreateTestCase implements CCDConfig<PCSCase, State, UserRole> {

    private final PcsCaseService pcsCaseService;

    @Override
    public void configure(ConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        configBuilder
            .decentralisedEvent(createTestApplication.name(), this::submit, this::start)
            .initialState(State.CASE_ISSUED)
            .name("Make a claim")
            .grant(Permission.CRUD, UserRole.PCS_CASE_WORKER)
            .fields()
            .page("Make a claim")
                .pageLabel("What is the address of the property you're claiming possession of?")
                .label("lineSeparator", "---")
                .mandatory(PCSCase::getPropertyAddress)
            .page("landing page")
                .pageLabel("Make a housing possession claim")
                .label("BeforeYouStartMainBody",
                        """
                                You can use this online service if you're a registered provider of social housing or \
                                a community landlord and the property you want to claim possession of is in England \
                                or Wales.
                                <br>
                                <br>
                                This service is also available in Welsh (Cymraeg).
                                <br>
                                <br>
                                We’ll check your eligibility by asking for the property’s address.
                                <br>
                                <br>
                                The claim fee is £404.
                                <br>
                                <h2> What you'll need </h2>
                                Before you start, make sure you have the following information:
                                <br>
                                <ul>
                                    <li>details of the tenancy, contract, licence or mortgage agreement</li>
                                    <li>the defendants’ details (the people you’re making the claim against)</li>
                                    <li>copies of any relevant documents. You can either upload documents now or \
                                    closer to the hearing date. Any documents you upload now will be included in the \
                                    pack of documents that a judge will receive before the hearing (the bundle)</li>
                                </ul>
                                Once you’ve finished answering the questions, you can either:
                                <br>
                                <ul>
                                    <li>sign, submit and pay for your claim now, or</li>
                                    <li>save it as a draft. You can then sign, submit and pay at a later date</li>
                                </ul>
                            """)
            .page("claimant types")
                .pageLabel("Claimant type")
                .label("lineSeparator", "---")
                .label("claimantTypeMainBody", """
                    A claimant is the person or organisation who is making the possession claim.
                    """)
                .mandatoryWithLabel(PCSCase::getClaimantType, "Which type of claimant are you?")
            .page("claimant information")
                .pageLabel("Please enter applicant's name")
                .mandatory(PCSCase::getApplicantForename)
            .done();
    }

    private PCSCase start(EventPayload<PCSCase, State> eventPayload) {
        PCSCase caseData = eventPayload.caseData();
        caseData.setApplicantForename("Preset value");
        return caseData;
    }

    private void submit(EventPayload<PCSCase, State> eventPayload) {
        long caseReference = eventPayload.caseReference();
        PCSCase pcsCase = eventPayload.caseData();

        pcsCaseService.createCase(caseReference, pcsCase);
    }

}
