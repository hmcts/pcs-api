package uk.gov.hmcts.reform.pcs.ccd.event;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.ConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimType;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.PaymentStatus;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.service.ClaimService;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;

import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.createShellClaim;

@Component
@AllArgsConstructor
public class CreateShellClaim implements CCDConfig<PCSCase, State, UserRole> {

    private final PcsCaseService pcsCaseService;
    private final ClaimService claimService;

    @Override
    public void configure(ConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        configBuilder
            .decentralisedEvent(createShellClaim.name(), this::submit)
            .initialState(State.CASE_ISSUED)
            .name("Create shell claim")
            .grant(Permission.CRUD, UserRole.PCS_CASE_WORKER);

    }

    private void submit(EventPayload<PCSCase, State> eventPayload) {
        long caseReference = eventPayload.caseReference();
        PCSCase pcsCase = eventPayload.caseData();
        pcsCase.setPropertyAddress(AddressUK.builder()
                                       .addressLine1("123 Baker Street")
                                       .addressLine2("Marylebone")
                                       .postTown("London")
                                       .county("Greater London")
                                       .postCode("NW1 6XE")
                                       .build());
        pcsCase.setPaymentStatus(PaymentStatus.UNPAID);

        pcsCaseService.createCase(caseReference, pcsCase);
        claimService.createClaim(caseReference, ClaimType.MAIN_CLAIM, "Main claim");
    }

}
