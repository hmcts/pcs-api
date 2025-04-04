package uk.gov.hmcts.reform.pcs.ccd.event;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.ConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.DynamicListElement;
import uk.gov.hmcts.ccd.sdk.type.DynamicMultiSelectList;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.Claim;
import uk.gov.hmcts.reform.pcs.ccd.domain.PcsCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.UserRole;
import uk.gov.hmcts.reform.pcs.entity.Party;
import uk.gov.hmcts.reform.pcs.entity.PartyRole;
import uk.gov.hmcts.reform.pcs.exception.CaseNotFoundException;
import uk.gov.hmcts.reform.pcs.repository.ClaimRepository;
import uk.gov.hmcts.reform.pcs.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.repository.PartyRepository;
import uk.gov.hmcts.reform.pcs.service.CcdMoneyFieldFormatter;
import uk.gov.hmcts.reform.pcs.service.PartyService;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.NEVER_SHOW;

@Component
public class CreateClaim implements CCDConfig<PcsCase, State, UserRole> {

    private final PartyService partyService;
    private final PcsCaseRepository pcsCaseRepository;
    private final ClaimRepository claimRepository;
    private final PartyRepository partyRepository;
    private final CcdMoneyFieldFormatter ccdMoneyFieldFormatter;

    public CreateClaim(PartyService partyService,
                       PcsCaseRepository pcsCaseRepository,
                       ClaimRepository claimRepository,
                       PartyRepository partyRepository,
                       CcdMoneyFieldFormatter ccdMoneyFieldFormatter) {

        this.partyService = partyService;
        this.pcsCaseRepository = pcsCaseRepository;
        this.claimRepository = claimRepository;
        this.partyRepository = partyRepository;
        this.ccdMoneyFieldFormatter = ccdMoneyFieldFormatter;
    }

    @Override
    public void configure(ConfigBuilder<PcsCase, State, UserRole> configBuilder) {
        configBuilder
            .decentralisedEvent(EventId.createClaim.name(), this::submit)
            .forAllStates()
            .name("Create claim")
            .showSummary()
            .aboutToStartCallback(this::start)
            .grant(Permission.CRUD, UserRole.CASE_WORKER)
            .fields()
            .page("summary")
            .mandatory(PcsCase::getClaimToAdd)
            .page("claimants", this::claimantsSelected)
            .mandatory(PcsCase::getClaimantsToAdd)
            .page("defendants", this::defendantsSelected)
            .mandatory(PcsCase::getDefendantsToAdd)
            .page("interested-parties")
            .showCondition("ipEmpty=\"No\"")
            .optional(PcsCase::getInterestedPartiesToAdd)
            .readonly(PcsCase::getIpEmpty, NEVER_SHOW)
            .done();
    }

    private AboutToStartOrSubmitResponse<PcsCase, State> start(CaseDetails<PcsCase, State> caseDetails) {
        PcsCase pcsCase = caseDetails.getData();

        List<DynamicListElement> claimantsOptionsList = partyService.getAllPartiesAsOptionsList(caseDetails.getId());

        DynamicMultiSelectList dynamicList = DynamicMultiSelectList.builder()
            .listItems(claimantsOptionsList)
            .build();
        pcsCase.setClaimantsToAdd(dynamicList);

        return AboutToStartOrSubmitResponse.<PcsCase, State>builder()
            .data(caseDetails.getData())
            .build();
    }

    private AboutToStartOrSubmitResponse<PcsCase, State> claimantsSelected(CaseDetails<PcsCase, State> details,
                                                                           CaseDetails<PcsCase, State> detailsBefore) {

        PcsCase pcsCase = details.getData();

        DynamicMultiSelectList claimantsMultiSelectList = pcsCase.getClaimantsToAdd();
        List<DynamicListElement> selectedClaimants = claimantsMultiSelectList.getValue();
        List<DynamicListElement> defendantOptionsList = new ArrayList<>(claimantsMultiSelectList.getListItems());

        defendantOptionsList.removeAll(selectedClaimants);

        DynamicMultiSelectList defendantsMultiSelectList = DynamicMultiSelectList.builder()
            .listItems(defendantOptionsList)
            .build();

        pcsCase.setDefendantsToAdd(defendantsMultiSelectList);

        return AboutToStartOrSubmitResponse.<PcsCase, State>builder()
            .data(details.getData())
            .build();
    }

    private AboutToStartOrSubmitResponse<PcsCase, State> defendantsSelected(CaseDetails<PcsCase, State> details,
                                                                            CaseDetails<PcsCase, State> detailsBefore) {

        PcsCase pcsCase = details.getData();

        DynamicMultiSelectList defendantsMultiSelectList = pcsCase.getDefendantsToAdd();
        List<DynamicListElement> selectedDefendants = defendantsMultiSelectList.getValue();
        List<DynamicListElement> interestedPartiesOptionsList
            = new ArrayList<>(defendantsMultiSelectList.getListItems());

        interestedPartiesOptionsList.removeAll(selectedDefendants);

        DynamicMultiSelectList interestedPartiesMultiSelectList = DynamicMultiSelectList.builder()
            .listItems(interestedPartiesOptionsList)
            .build();

        pcsCase.setInterestedPartiesToAdd(interestedPartiesMultiSelectList);
        pcsCase.setIpEmpty(YesOrNo.from(interestedPartiesOptionsList.isEmpty()));

        return AboutToStartOrSubmitResponse.<PcsCase, State>builder()
            .data(details.getData())
            .build();
    }

    private void submit(EventPayload<PcsCase, State> eventPayload) {
        PcsCase pcsCase = eventPayload.caseData();
        Claim claimToAdd = pcsCase.getClaimToAdd();

        uk.gov.hmcts.reform.pcs.entity.Claim claimEntity = uk.gov.hmcts.reform.pcs.entity.Claim.builder()
            .summary(claimToAdd.getSummary())
            .amount(ccdMoneyFieldFormatter.parsePenceString(claimToAdd.getAmountInPence()))
            .build();

        List<UUID> claimantUuids = getSelectedItemCodes(pcsCase.getClaimantsToAdd());
        List<UUID> defendantUuids = getSelectedItemCodes(pcsCase.getDefendantsToAdd());
        List<UUID> interestedPartyUuids = getSelectedItemCodes(pcsCase.getInterestedPartiesToAdd());

        // TODO: Optimise this DB update
        claimantUuids.forEach(
            claimantUuid -> {
                Party partyReference = partyRepository.getReferenceById(claimantUuid);
                claimEntity.addParty(partyReference, PartyRole.CLAIMANT);
            }
        );

        defendantUuids.forEach(
            defendantUuid -> {
                Party partyReference = partyRepository.getReferenceById(defendantUuid);
                claimEntity.addParty(partyReference, PartyRole.DEFENDANT);
            }
        );

        interestedPartyUuids.forEach(
            claimantUuid -> {
                Party interestedPartyUuid = partyRepository.getReferenceById(claimantUuid);
                claimEntity.addParty(interestedPartyUuid, PartyRole.INTERESTED_PARTY);
            }
        );

        long caseReference = eventPayload.caseReference();
        uk.gov.hmcts.reform.pcs.entity.PcsCase pcsCaseEntity = pcsCaseRepository.findByCaseReference(caseReference)
            .orElseThrow(() -> new CaseNotFoundException("Case not found for " + caseReference));

        pcsCaseEntity.addClaim(claimEntity);
        claimRepository.save(claimEntity);
    }

    @NonNull
    private static List<UUID> getSelectedItemCodes(DynamicMultiSelectList multiSelectList) {
        if (multiSelectList != null) {
            return multiSelectList.getValue().stream()
                .map(DynamicListElement::getCode)
                .toList();
        } else {
            return List.of();
        }
    }

}
