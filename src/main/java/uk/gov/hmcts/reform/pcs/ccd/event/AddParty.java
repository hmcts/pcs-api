package uk.gov.hmcts.reform.pcs.ccd.event;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.ConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.UserRole;
import uk.gov.hmcts.reform.pcs.entity.Party;
import uk.gov.hmcts.reform.pcs.repository.PCSCaseRepository;
import uk.gov.hmcts.reform.pcs.repository.PartyRepository;

@Component
public class AddParty implements CCDConfig<PCSCase, State, UserRole> {
    @Autowired
    private PCSCaseRepository cases;

    @Autowired
    private PartyRepository parties;

    @Override
    public void configure(ConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        configBuilder
            .event("addParty")
            .forState(State.Open)
            .name("Add a party")
            .aboutToSubmitCallback(this::aboutToSubmit)
            .grant(Permission.CRUD, UserRole.CASE_WORKER)
            .fields()
            .page("Party details")
                .mandatory(PCSCase::getPartyFirstName)
                .mandatory(PCSCase::getPartyLastName)
            .done();
    }

    // Save the new party details to the database (instead of CCD's JSON blob)
    public AboutToStartOrSubmitResponse<PCSCase, State> aboutToSubmit(
        CaseDetails<PCSCase, State> details, CaseDetails<PCSCase, State> beforeDetails) {
        var data = details.getData();

        var c = cases.getReferenceById(details.getId());
        var p = Party.builder()
            .forename(data.getPartyFirstName())
            .surname(data.getPartyLastName())
            .pcsCase(c)
            .build();

        parties.save(p);

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(details.getData())
            .build();
    }
}
