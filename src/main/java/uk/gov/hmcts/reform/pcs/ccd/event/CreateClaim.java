package uk.gov.hmcts.reform.pcs.ccd.event;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.ConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.UserRole;
import uk.gov.hmcts.reform.pcs.entity.PcsCase;
import uk.gov.hmcts.reform.pcs.repository.PCSCaseRepository;

@Component
public class CreateClaim implements CCDConfig<PCSCase, State, UserRole> {
    @Autowired
    private PCSCaseRepository repository;

    @Override
    public void configure(ConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        configBuilder
            .decentralisedEvent("createClaim", this::submit)
            .initialState(State.PreSubmission)
            .name("Create a claim")
            .grant(Permission.CRUD, UserRole.CASE_WORKER)
            .fields()
            .page("Preamble")
                .label("preamble", """
                    # Before you start
                    
                    You can use this online service if you're a private registered provider of social housing and the property you want to claim possession of is in Bedfordshire.
                    
                    We'll check your eligibility by asking for the property's address.
                    
                    The claim fee is £391.
                    
                    **What you'll need**
                    
                    Before you start, make sure you have the following information:
                    
                    * The address of the property
                    * Information about the tenancy
                    * Details of the people living in the property (if known)
                    * Your reasons for making a possession claim
                    * Copies of any relevant documents
                    
                    You'll be able to review and change your answers before you submit the claim and you can save your progress and return later if you need to.
                    """)

            .page("Submit", this::validateAddress)
            .label("whichAddress", """
                # What's the address of the property you're claiming possession of?

                The property must be located in Bedfordshire. 
                """)
                .mandatory(PCSCase::getPropertyAddress)
                .done();

    }

    private AboutToStartOrSubmitResponse<PCSCase, State> validateAddress(
        CaseDetails<PCSCase, State> d, CaseDetails<PCSCase, State> before) {
        var builder = AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(d.getData());
        if (!d.getData().getPropertyAddress().getPostTown().toUpperCase().contains("BEDFORD")) {
            builder.errors(List.of("The property address must be in Bedfordshire. " +
                "Please check the address and try again."));
        }
        return builder.build();
    }

    public void submit(EventPayload<PCSCase, State> p) {
        var c = PcsCase.builder()
            .reference(p.caseReference())
            .caseDescription("Possession claim for " + p.caseData().getPropertyAddress().getAddressLine1())
            .build();
        repository.save(c);
    }
}
