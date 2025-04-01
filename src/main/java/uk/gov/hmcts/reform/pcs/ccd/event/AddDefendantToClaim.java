package uk.gov.hmcts.reform.pcs.ccd.event;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.ConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.ccd.sdk.type.DynamicListElement;
import uk.gov.hmcts.ccd.sdk.type.DynamicMultiSelectList;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PcsCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.UserRole;
import uk.gov.hmcts.reform.pcs.exception.ClaimNotFoundException;
import uk.gov.hmcts.reform.pcs.repository.ClaimRepository;
import uk.gov.hmcts.reform.pcs.service.ClaimService;
import uk.gov.hmcts.reform.pcs.service.PartyService;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static uk.gov.hmcts.reform.pcs.ccd.MultiSelectListUtils.getSelectedCodes;

@Component
public class AddDefendantToClaim implements CCDConfig<PcsCase, State, UserRole> {

    private static final String NEVER_SHOW = "[STATE]=\"NEVER_SHOW\"";

    private final PartyService partyService;
    private final ClaimService claimService;
    private final ClaimRepository claimRepository;

    public AddDefendantToClaim(PartyService partyService,
                               ClaimService claimService,
                               ClaimRepository claimRepository) {

        this.partyService = partyService;
        this.claimService = claimService;
        this.claimRepository = claimRepository;
    }

    @Override
    public void configure(ConfigBuilder<PcsCase, State, UserRole> configBuilder) {
        configBuilder
            .decentralisedEvent(EventId.addDefendantToClaim.name(), this::submit, this::start)
            .forAllStates()
            .name("Add defendant")
            .grant(Permission.CRUD, UserRole.CASE_WORKER)
            .showCondition(NEVER_SHOW)
            .fields()
            .page("defendants")
            .mandatory(PcsCase::getDefendantsToAdd)
            .label("no-claimants-label", "### There are no unassigned parties for this claim", "partyListEmpty=\"Yes\"")
            .readonly(PcsCase::getCurrentClaimId, NEVER_SHOW)
            .readonly(PcsCase::getPartyListEmpty, "[STATE]=\"NEVER_SHOW\"")
            .done();
    }

    private PcsCase start(EventPayload<PcsCase, State> payload) {
        PcsCase pcsCase = payload.caseData();

        String claimId = payload.urlParams().getFirst("claimId");

        uk.gov.hmcts.reform.pcs.entity.Claim claimEntity = claimRepository.findById(UUID.fromString(claimId))
            .orElseThrow(() -> new ClaimNotFoundException("Claim not found for ID " + claimId));

        Set<UUID> existingClaimPartyIds = claimEntity.getClaimParties().stream()
            .map(claimParty -> claimParty.getParty().getId())
            .collect(Collectors.toSet());

        List<DynamicListElement> defendantsOptionsList = partyService
            .getAllPartiesAsOptionsList(payload.caseReference())
            .stream()
            .filter(listElement -> !existingClaimPartyIds.contains(listElement.getCode()))
            .toList();

        DynamicMultiSelectList dynamicList = DynamicMultiSelectList.builder()
            .listItems(defendantsOptionsList)
            .build();

        pcsCase.setCurrentClaimId(claimId);
        pcsCase.setDefendantsToAdd(dynamicList);
        pcsCase.setPartyListEmpty(YesOrNo.from(defendantsOptionsList.isEmpty()));

        return pcsCase;
    }

    private void submit(EventPayload<PcsCase, State> eventPayload) {
        PcsCase pcsCase = eventPayload.caseData();

        UUID currentClaimId = UUID.fromString(pcsCase.getCurrentClaimId());
        List<UUID> partiesIdsToAdd = getSelectedCodes(pcsCase.getDefendantsToAdd());

        claimService.linkDefendants(currentClaimId, partiesIdsToAdd);
    }

}
