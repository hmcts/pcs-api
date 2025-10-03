package uk.gov.hmcts.reform.pcs.ccd.event;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.DecentralisedConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.Event.EventBuilder;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;
import uk.gov.hmcts.reform.pcs.ccd.ShowConditions;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantType;
import uk.gov.hmcts.reform.pcs.ccd.domain.DefendantDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.PaymentStatus;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.page.builder.SavingPageBuilderFactory;
import uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim.AdditionalReasonsForPossession;
import uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim.ClaimantCircumstances;
import uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim.EntitledToClaimRelief;
import uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim.MoneyJudgment;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.AlternativesToPossessionOptions;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.CheckingNotice;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.ClaimTypeNotEligibleEngland;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.ClaimTypeNotEligibleWales;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.ClaimantInformation;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.ClaimantTypeNotEligibleEngland;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.ClaimantTypeNotEligibleWales;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.ClaimingCosts;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.CompletingClaim;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.ContactPreferences;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.DailyRentAmount;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.DefendantCircumstancesPage;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.DefendantsDetails;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.GeneralApplication;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.GroundForPossessionAdditionalGrounds;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.GroundForPossessionRentArrears;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.GroundsForPossession;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.IntroductoryDemotedOrOtherGroundsForPossession;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.IntroductoryDemotedOtherGroundsReasons;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.MediationAndSettlement;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.NoRentArrearsGroundsForPossessionOptions;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.NoRentArrearsGroundsForPossessionReason;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.NoticeDetails;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.PreActionProtocol;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.RentArrears;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.RentArrearsOrBreachOfTenancyGround;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.RentDetails;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.ResumeClaim;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.SecureOrFlexibleGroundsForPossession;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.SecureOrFlexibleGroundsForPossessionReasons;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.SelectClaimType;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.SelectClaimantType;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.SuspensionOfRightToBuyHousingActOptions;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.SuspensionOfRightToBuyOrderReason;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.TenancyLicenceDetails;
import uk.gov.hmcts.reform.pcs.ccd.service.ClaimService;
import uk.gov.hmcts.reform.pcs.ccd.service.PartyService;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.UnsubmittedCaseDataService;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringList;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringListElement;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static uk.gov.hmcts.reform.pcs.ccd.domain.State.AWAITING_FURTHER_CLAIM_DETAILS;
import static uk.gov.hmcts.reform.pcs.ccd.domain.State.AWAITING_SUBMISSION_TO_HMCTS;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.resumePossessionClaim;

@Slf4j
@Component
@AllArgsConstructor
public class ResumePossessionClaim implements CCDConfig<PCSCase, State, UserRole> {

    private final PcsCaseService pcsCaseService;
    private final SecurityContextService securityContextService;
    private final PartyService partyService;
    private final ClaimService claimService;
    private final SavingPageBuilderFactory savingPageBuilderFactory;
    private final ResumeClaim resumeClaim;
    private final UnsubmittedCaseDataService unsubmittedCaseDataService;
    private final NoticeDetails noticeDetails;

    private final TenancyLicenceDetails tenancyLicenceDetails;
    private final ContactPreferences contactPreferences;
    private final DefendantsDetails defendantsDetails;

    @Override
    public void configureDecentralised(DecentralisedConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        EventBuilder<PCSCase, UserRole, State> eventBuilder =
            configBuilder
                .decentralisedEvent(resumePossessionClaim.name(), this::submit, this::start)
                .forStateTransition(AWAITING_FURTHER_CLAIM_DETAILS, AWAITING_SUBMISSION_TO_HMCTS)
                .name("Make a claim")
                .showCondition(ShowConditions.NEVER_SHOW)
                .grant(Permission.CRUD, UserRole.PCS_SOLICITOR)
                .showSummary();

        savingPageBuilderFactory.create(eventBuilder)
            .add(resumeClaim)
            .add(new SelectClaimantType())
            .add(new ClaimantTypeNotEligibleEngland())
            .add(new ClaimantTypeNotEligibleWales())
            .add(new SelectClaimType())
            .add(new ClaimTypeNotEligibleEngland())
            .add(new ClaimTypeNotEligibleWales())
            .add(new ClaimantInformation())
            .add(contactPreferences)
            .add(defendantsDetails)
            .add(tenancyLicenceDetails)
            .add(new SecureOrFlexibleGroundsForPossession())
            .add(new RentArrearsOrBreachOfTenancyGround())
            .add(new SecureOrFlexibleGroundsForPossessionReasons())
            .add(new IntroductoryDemotedOrOtherGroundsForPossession())
            .add(new IntroductoryDemotedOtherGroundsReasons())
            .add(new GroundsForPossession())
            .add(new GroundForPossessionRentArrears())
            .add(new GroundForPossessionAdditionalGrounds())
            .add(new NoRentArrearsGroundsForPossessionOptions())
            .add(new NoRentArrearsGroundsForPossessionReason())
            .add(new PreActionProtocol())
            .add(new MediationAndSettlement())
            .add(new CheckingNotice())
            .add(noticeDetails)
            .add(new RentDetails())
            .add(new DailyRentAmount())
            .add(new RentArrears())
            .add(new MoneyJudgment())
            .add(new ClaimantCircumstances())
            .add(new DefendantCircumstancesPage())
            .add(new AlternativesToPossessionOptions())
            .add(new SuspensionOfRightToBuyHousingActOptions())
            .add(new SuspensionOfRightToBuyOrderReason())
            .add(new ClaimingCosts())
            .add(new AdditionalReasonsForPossession())
            .add(new EntitledToClaimRelief())
            .add(new GeneralApplication())
            .add(new CompletingClaim());

    }

    private PCSCase start(EventPayload<PCSCase, State> eventPayload) {
        PCSCase caseData = eventPayload.caseData();

        String userDetails = securityContextService.getCurrentUserDetails().getSub();
        caseData.setClaimantName(userDetails);
        caseData.setClaimantContactEmail(userDetails);

        AddressUK propertyAddress = caseData.getPropertyAddress();
        if (propertyAddress == null) {
            throw new IllegalStateException("Cannot resume claim without property address already set");
        }

        LegislativeCountry legislativeCountry = caseData.getLegislativeCountry();
        if (legislativeCountry == null) {
            throw new IllegalStateException("Cannot resume claim without legislative country already set");
        }

        List<DynamicStringListElement> listItems = Arrays.stream(ClaimantType.values())
            .filter(value -> value.isApplicableFor(legislativeCountry))
            .map(value -> DynamicStringListElement.builder().code(value.name()).label(value.getLabel())
                .build())
            .toList();

        DynamicStringList claimantTypeList = DynamicStringList.builder()
            .listItems(listItems)
            .build();
        caseData.setClaimantType(claimantTypeList);

        String formattedAddress = String.format(
            "%s<br>%s<br>%s",
            propertyAddress.getAddressLine1(),
            propertyAddress.getPostTown(),
            propertyAddress.getPostCode()
        );
        caseData.setFormattedClaimantContactAddress(formattedAddress);

        return caseData;
    }

    private SubmitResponse submit(EventPayload<PCSCase, State> eventPayload) {
        long caseReference = eventPayload.caseReference();
        PCSCase pcsCase = eventPayload.caseData();

        pcsCase.setPaymentStatus(PaymentStatus.UNPAID);

        List<ListValue<DefendantDetails>> defendantsList = new ArrayList<>();
        if (pcsCase.getDefendant1() != null) {
            if (VerticalYesNo.YES == pcsCase.getDefendant1().getAddressSameAsPossession()) {
                pcsCase.getDefendant1().setCorrespondenceAddress(pcsCase.getPropertyAddress());
            }
            defendantsList.add(new ListValue<>(UUID.randomUUID().toString(), pcsCase.getDefendant1()));
            pcsCaseService.clearHiddenDefendantDetailsFields(defendantsList);
            pcsCase.setDefendants(defendantsList);
        }

        PcsCaseEntity pcsCaseEntity = pcsCaseService.loadCase(caseReference);

        pcsCaseService.mergeCaseData(pcsCaseEntity, pcsCase);

        PartyEntity claimantPartyEntity = createClaimantPartyEntity(pcsCase);
        pcsCaseEntity.addParty(claimantPartyEntity);

        ClaimEntity claimEntity = claimService.createMainClaimEntity(pcsCase, claimantPartyEntity);
        pcsCaseEntity.addClaim(claimEntity);

        pcsCaseService.save(pcsCaseEntity);

        unsubmittedCaseDataService.deleteUnsubmittedCaseData(caseReference);

        return SubmitResponse.builder().build();
    }

    private PartyEntity createClaimantPartyEntity(PCSCase pcsCase) {
        UserInfo userDetails = securityContextService.getCurrentUserDetails();
        UUID userID = UUID.fromString(userDetails.getUid());

        String claimantName = isNotBlank(pcsCase.getOverriddenClaimantName())
            ? pcsCase.getOverriddenClaimantName() : pcsCase.getClaimantName();

        AddressUK contactAddress = pcsCase.getOverriddenClaimantContactAddress() != null
            ? pcsCase.getOverriddenClaimantContactAddress() : pcsCase.getPropertyAddress();

        String contactEmail = isNotBlank(pcsCase.getOverriddenClaimantContactEmail())
            ? pcsCase.getOverriddenClaimantContactEmail() : pcsCase.getClaimantContactEmail();

        return partyService.createPartyEntity(
            userID,
            claimantName,
            null,
            contactEmail,
            contactAddress,
            pcsCase.getClaimantContactPhoneNumber()
        );
    }
}
