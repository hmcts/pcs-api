package uk.gov.hmcts.reform.pcs.ccd.event;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.ConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.Event.EventBuilder;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.PaymentStatus;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.page.createtestcase.ClaimantInformation;
import uk.gov.hmcts.reform.pcs.ccd.page.createtestcase.ContactPreferences;
import uk.gov.hmcts.reform.pcs.ccd.page.createtestcase.EnterPropertyAddress;
import uk.gov.hmcts.reform.pcs.ccd.service.ClaimService;
import uk.gov.hmcts.reform.pcs.ccd.service.PartyService;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.util.UUID;

import static feign.Util.isNotBlank;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.createTestApplication;

@Component
@AllArgsConstructor
public class CreateTestCase implements CCDConfig<PCSCase, State, UserRole> {

    private final PcsCaseService pcsCaseService;
    private final SecurityContextService securityContextService;
    private final PartyService partyService;
    private final ClaimService claimService;

    @Override
    public void configure(ConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        EventBuilder<PCSCase, UserRole, State> eventBuilder =
            configBuilder
                .decentralisedEvent(createTestApplication.name(), this::submit, this::start)
                .initialState(State.CASE_ISSUED)
                .name("Make a claim")
                .grant(Permission.CRUD, UserRole.PCS_CASE_WORKER);

        new PageBuilder(eventBuilder)
            .add(new EnterPropertyAddress())
            .add(new ContactPreferences())
            .add(new ClaimantInformation());
    }

    private PCSCase start(EventPayload<PCSCase, State> eventPayload) {
        PCSCase caseData = eventPayload.caseData();
        String userDetails = securityContextService.getCurrentUserDetails().getSub();
        caseData.setClaimantName(userDetails);
        caseData.setContactEmail(userDetails);

        return caseData;
    }

    private void submit(EventPayload<PCSCase, State> eventPayload) {

        long caseReference = eventPayload.caseReference();
        PCSCase pcsCase = eventPayload.caseData();
        pcsCase.setPaymentStatus(PaymentStatus.UNPAID);
        pcsCase.setContactAddress(pcsCase.getPropertyAddress());

        UUID userID = UUID.fromString(securityContextService.getCurrentUserDetails().getUid());

        String claimantName = isNotBlank(pcsCase.getOverriddenClaimantName())
            ? pcsCase.getOverriddenClaimantName() : pcsCase.getClaimantName();

        PcsCaseEntity pcsCaseEntity = pcsCaseService.createCase(caseReference, pcsCase);
        PartyEntity party = partyService.createAndLinkParty(
            pcsCaseEntity,
            userID,
            claimantName,
            null,
            true);

        ClaimEntity claimEntity = claimService.createAndLinkClaim(
            pcsCaseEntity,
            party,
            "Main Claim",
            PartyRole.CLAIMANT);

        claimService.saveClaim(claimEntity);
    }

}
