package uk.gov.hmcts.reform.pcs.ccd3.event;

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
import uk.gov.hmcts.reform.pcs.ccd3.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd3.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd3.domain.DefendantDetails;
import uk.gov.hmcts.reform.pcs.ccd3.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd3.domain.PaymentStatus;
import uk.gov.hmcts.reform.pcs.ccd3.domain.State;
import uk.gov.hmcts.reform.pcs.ccd3.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd3.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd3.entity.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd3.entity.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd3.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd3.page.createpossessionclaim.CheckingNotice;
import uk.gov.hmcts.reform.pcs.ccd3.page.createpossessionclaim.ClaimTypeNotEligibleEngland;
import uk.gov.hmcts.reform.pcs.ccd3.page.createpossessionclaim.ClaimTypeNotEligibleWales;
import uk.gov.hmcts.reform.pcs.ccd3.page.createpossessionclaim.ClaimantInformation;
import uk.gov.hmcts.reform.pcs.ccd3.page.createpossessionclaim.ClaimantTypeNotEligibleEngland;
import uk.gov.hmcts.reform.pcs.ccd3.page.createpossessionclaim.ClaimantTypeNotEligibleWales;
import uk.gov.hmcts.reform.pcs.ccd3.page.createpossessionclaim.ContactPreferences;
import uk.gov.hmcts.reform.pcs.ccd3.page.createpossessionclaim.CrossBorderPostcodeSelection;
import uk.gov.hmcts.reform.pcs.ccd3.page.createpossessionclaim.DefendantsDetails;
import uk.gov.hmcts.reform.pcs.ccd3.page.createpossessionclaim.EnterPropertyAddress;
import uk.gov.hmcts.reform.pcs.ccd3.page.createpossessionclaim.GroundsForPossession;
import uk.gov.hmcts.reform.pcs.ccd3.page.createpossessionclaim.MediationAndSettlement;
import uk.gov.hmcts.reform.pcs.ccd3.page.createpossessionclaim.NoticeDetails;
import uk.gov.hmcts.reform.pcs.ccd3.page.createpossessionclaim.PostcodeNotAssignedToCourt;
import uk.gov.hmcts.reform.pcs.ccd3.page.createpossessionclaim.PreActionProtocol;
import uk.gov.hmcts.reform.pcs.ccd3.page.createpossessionclaim.PropertyNotEligible;
import uk.gov.hmcts.reform.pcs.ccd3.page.createpossessionclaim.RentDetails;
import uk.gov.hmcts.reform.pcs.ccd3.page.createpossessionclaim.RentArrears;
import uk.gov.hmcts.reform.pcs.ccd3.page.createpossessionclaim.SelectClaimType;
import uk.gov.hmcts.reform.pcs.ccd3.page.createpossessionclaim.SelectClaimantType;
import uk.gov.hmcts.reform.pcs.ccd3.page.createpossessionclaim.SelectLegislativeCountry;
import uk.gov.hmcts.reform.pcs.ccd3.page.createpossessionclaim.StartTheService;
import uk.gov.hmcts.reform.pcs.ccd3.service.ClaimService;
import uk.gov.hmcts.reform.pcs.ccd3.service.PartyService;
import uk.gov.hmcts.reform.pcs.ccd3.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static feign.Util.isNotBlank;
import static uk.gov.hmcts.reform.pcs.ccd3.event.EventId.createPossessionClaim;


@Slf4j
@Component
@AllArgsConstructor
public class CreatePossessionClaim implements CCDConfig<PCSCase, State, UserRole> {

    private final PcsCaseService pcsCaseService;
    private final SecurityContextService securityContextService;
    private final PartyService partyService;
    private final ClaimService claimService;
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
            .add(enterPropertyAddress)
            .add(crossBorderPostcodeSelection)
            .add(propertyNotEligible)
            .add(new PostcodeNotAssignedToCourt())
            .add(new SelectLegislativeCountry())
            .add(new SelectClaimantType())
            .add(new ClaimantTypeNotEligibleEngland())
            .add(new ClaimantTypeNotEligibleWales())
            .add(new SelectClaimType())
            .add(new ClaimTypeNotEligibleEngland())
            .add(new ClaimTypeNotEligibleWales())
            .add(new ClaimantInformation())
            .add(new ContactPreferences())
            .add(new DefendantsDetails())
            .add(new GroundsForPossession())
            .add(new PreActionProtocol())
            .add(new MediationAndSettlement())
            .add(new CheckingNotice())
            .add(new NoticeDetails())
            .add(new RentDetails())
            .add(new RentArrears());
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
