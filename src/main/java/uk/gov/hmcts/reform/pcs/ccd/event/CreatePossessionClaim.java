package uk.gov.hmcts.reform.pcs.ccd.event;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.ConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.Event.EventBuilder;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.DefendantDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.PaymentStatus;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim.CrossBorderPostcodeSelection;
import uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim.EnterPropertyAddress;
import uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim.PropertyNotEligible;
import uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim.StartTheService;
import uk.gov.hmcts.reform.pcs.ccd.service.ClaimService;
import uk.gov.hmcts.reform.pcs.ccd.service.PartyService;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.SendLetterService;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static feign.Util.isNotBlank;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.createPossessionClaim;


@Slf4j
@Component
@AllArgsConstructor
public class CreatePossessionClaim implements CCDConfig<PCSCase, State, UserRole> {

    private final PcsCaseService pcsCaseService;
    private final SecurityContextService securityContextService;
    private final PartyService partyService;
    private final ClaimService claimService;
    private final SendLetterService sendLetterService;
    private final EnterPropertyAddress enterPropertyAddress;
    private final CrossBorderPostcodeSelection crossBorderPostcodeSelection;
    private final PropertyNotEligible propertyNotEligible;


    @Override
    public void configure(ConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        EventBuilder<PCSCase, UserRole, State> eventBuilder =
            configBuilder
                .decentralisedEvent(createPossessionClaim.name(), this::submit, this::start)
                .initialState(State.CASE_ISSUED)
                .name("Make a claim")
                .grant(Permission.CRUD, UserRole.PCS_CASE_WORKER)
                .showSummary();

        new PageBuilder(eventBuilder)
            .add(new StartTheService())
            .add(enterPropertyAddress);
    }

    private PCSCase start(EventPayload<PCSCase, State> eventPayload) {
        PCSCase caseData = eventPayload.caseData();
        String userDetails = securityContextService.getCurrentUserDetails().getSub();
        caseData.setClaimantName(userDetails);
        caseData.setClaimantContactEmail(userDetails);

        return caseData;
    }

    private void submit(EventPayload<PCSCase, State> eventPayload) {
        PCSCase pcsCase = eventPayload.caseData();
        pcsCase.setPaymentStatus(PaymentStatus.UNPAID);
        pcsCase.setClaimantContactAddress(pcsCase.getPropertyAddress());

        UUID userID = UUID.fromString(securityContextService.getCurrentUserDetails().getUid());

        String claimantName = isNotBlank(pcsCase.getOverriddenClaimantName())
            ? pcsCase.getOverriddenClaimantName() : pcsCase.getClaimantName();

        AddressUK contactAddress = pcsCase.getOverriddenClaimantContactAddress() != null
            ? pcsCase.getOverriddenClaimantContactAddress() : pcsCase.getClaimantContactAddress();

        String contactEmail = isNotBlank(pcsCase.getOverriddenClaimantContactEmail())
            ? pcsCase.getOverriddenClaimantContactEmail() : pcsCase.getClaimantContactEmail();

        List<ListValue<DefendantDetails>> defendantsList = new ArrayList<>();
        if (pcsCase.getDefendant1() != null) {
            if (VerticalYesNo.YES == pcsCase.getDefendant1().getAddressSameAsPossession()) {
                pcsCase.getDefendant1().setCorrespondenceAddress(pcsCase.getPropertyAddress());
            }
            defendantsList.add(new ListValue<>(UUID.randomUUID().toString(), pcsCase.getDefendant1()));
            pcsCaseService.clearHiddenDefendantDetailsFields(defendantsList);
            pcsCase.setDefendants(defendantsList);
        }
        long caseReference = eventPayload.caseReference();
        PcsCaseEntity pcsCaseEntity = pcsCaseService.createCase(caseReference, pcsCase);
        PartyEntity party = partyService.createAndLinkParty(
            pcsCaseEntity,
            userID,
            claimantName,
            null,
            contactEmail,
            contactAddress,
            pcsCase.getClaimantContactPhoneNumber(),
            true);

        ClaimEntity claimEntity = claimService.createAndLinkClaim(
            pcsCaseEntity,
            party,
            "Main Claim",
            PartyRole.CLAIMANT);

        claimService.saveClaim(claimEntity);
    }

}
