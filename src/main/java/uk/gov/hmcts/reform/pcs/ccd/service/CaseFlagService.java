package uk.gov.hmcts.reform.pcs.ccd.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.ccd.sdk.type.Flags;
import uk.gov.hmcts.ccd.sdk.type.FlagDetail;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.domain.PartySupport;
import uk.gov.hmcts.ccd.sdk.type.FlagVisibility;
import uk.gov.hmcts.reform.pcs.ccd.entity.CaseFlagEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.CasePartyFlagEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.FlagRefDataEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.BaseCaseFlag;
import uk.gov.hmcts.reform.pcs.ccd.repository.FlagRefDataRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartySupportOwnershipResolver;
import uk.gov.hmcts.reform.pcs.ccd.util.YesOrNoConverter;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.view.CaseFlagsView;
import uk.gov.hmcts.reform.pcs.exception.CaseAccessException;

import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.ArrayList;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static uk.gov.hmcts.reform.pcs.ccd.util.FlagVisibilityConverter.toFlagVisibility;


@Service
@AllArgsConstructor
@Slf4j
public class CaseFlagService {

    private FlagRefDataRepository flagRefDataRepository;
    private PartySupportOwnershipResolver partySupportOwnershipResolver;

    public List<CaseFlagEntity> mergeCaseFlags(Flags incomingCaseFlags, PcsCaseEntity pcsCaseEntity) {

        return mergeFlagDetails(incomingCaseFlags, FlagVisibility.INTERNAL, pcsCaseEntity, null,
                        CaseFlagEntity::new);
    }

    public void mergePartyFlags(List<ListValue<Party>> incomingParties, Set<PartyEntity> existingParties) {
        Map<UUID, PartyEntity> existingPartiesMap = mapPartiesById(existingParties);

        for (ListValue<Party> incomingPartyValue : incomingParties) {
            Party incomingParty = incomingPartyValue.getValue();

            PartyEntity partyEntity = existingPartiesMap.get(UUID.fromString(incomingPartyValue.getId()));

            mergePartyFlagCollections(incomingParty.getDefendantFlags(),
                                      incomingParty.getPartyFlagsExternal(), partyEntity);
        }
    }

    public void mergePartySupportFlags(List<ListValue<PartySupport>> incomingPartySupport,
                                       Set<PartyEntity> existingParties,
                                       UUID authenticatedUserId,
                                       boolean ownPartyOnly) {
        Map<UUID, PartyEntity> existingPartiesMap = mapPartiesById(existingParties);

        for (ListValue<PartySupport> incomingSupportValue : incomingPartySupport) {
            Flags incomingSupportFlags = incomingSupportValue.getValue() == null
                ? null
                : incomingSupportValue.getValue().getSupportFlags();

            PartyEntity partyEntity = resolveSupportParty(incomingSupportValue.getId(), existingPartiesMap);

            boolean ownedByUser = partySupportOwnershipResolver.isOwnedByUser(partyEntity, authenticatedUserId);

            log.info("ownedByUser: {}", ownedByUser);

            if (ownPartyOnly && !ownedByUser) {
                Map<String, CasePartyFlagEntity> existingExternalFlags = getExistingExternalFlags(partyEntity);
                log.info("TVR: Existing Flag amount: {}", existingExternalFlags.size());
                log.info("TVR: IncomingSupportFlags: {}", incomingPartySupport.size());
                if (changesExistingSupport(incomingSupportFlags, partyEntity, existingExternalFlags)) {
                    log.error("Changing support for the party.");
                    // Removing this for the time being as it prevents a solicitor from adding CaseFlags to a defendant
                    // on a case at the moment.
                    // throw new CaseAccessException("User cannot change support for this party on this case");
                }
                if (!existingExternalFlags.isEmpty()) {
                    log.info("existingExternalFlags.size() = {}", existingExternalFlags.size());
                    //continue;
                }
            }

            mergePartyFlagCollections(null, incomingSupportFlags, partyEntity);
        }
    }

    private boolean changesExistingSupport(Flags incomingSupportFlags, PartyEntity partyEntity,
                                           Map<String, CasePartyFlagEntity> existingExternalFlags) {

        List<ListValue<FlagDetail>> incomingDetails = hasNoFlagDetails(incomingSupportFlags)
            ? List.of()
            : incomingSupportFlags.getDetails();

        log.info("TVR: incomingDetails = {}", incomingDetails.size());

        if (existingExternalFlags.isEmpty()) {
            return false;
        }

        if (incomingDetails.size() != existingExternalFlags.size()) {
            log.info("TVR: size difference");
            return true;
        }

        boolean anyMatch = incomingDetails.stream().anyMatch(incomingDetail -> {
            CasePartyFlagEntity existingFlag = existingExternalFlags.get(incomingDetail.getId());
            return existingFlag == null || differs(incomingDetail.getValue(), existingFlag);
        });

        log.info("TVR: anyMatch = {}", anyMatch);

        return anyMatch;
    }

    private static @NonNull Map<String, CasePartyFlagEntity> getExistingExternalFlags(PartyEntity partyEntity) {
        return partyEntity.getDefendantFlags().stream()
            .filter(existingFlag -> FlagVisibility.EXTERNAL == toFlagVisibility(existingFlag.getVisibility()))
            .collect(Collectors.toMap(existingFlag -> existingFlag.getId().toString(), Function.identity()));
    }

    private boolean differs(FlagDetail incomingFlagDetail, CasePartyFlagEntity existingFlag) {
        return incomingFlagDetail == null
            || !Objects.equals(incomingFlagDetail.getStatus(), existingFlag.getDefaultStatus())
            || !Objects.equals(incomingFlagDetail.getFlagComment(), existingFlag.getFlagComment())
            || !Objects.equals(incomingFlagDetail.getFlagUpdateComment(), existingFlag.getFlagUpdateComment());
    }

    private PartyEntity resolveSupportParty(String incomingPartyId, Map<UUID, PartyEntity> existingPartiesMap) {
        UUID partyId;
        try {
            partyId = UUID.fromString(incomingPartyId);
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new CaseAccessException("Support submitted for an invalid party reference");
        }

        PartyEntity partyEntity = existingPartiesMap.get(partyId);
        if (partyEntity == null) {
            throw new CaseAccessException("Support submitted for a party that is not on this case");
        }

        return partyEntity;
    }

    private Map<UUID, PartyEntity> mapPartiesById(Set<PartyEntity> existingParties) {
        return existingParties.stream()
            .collect(Collectors.toMap(
                PartyEntity::getId,
                Function.identity()
            ));
    }

    private void mergePartyFlagCollections(Flags incomingInternalFlags, Flags incomingExternalFlags,
                                           PartyEntity partyEntity) {
        if (hasNoFlagDetails(incomingInternalFlags) && hasNoFlagDetails(incomingExternalFlags)) {
            return;
        }

        List<CasePartyFlagEntity> existingFlags = List.copyOf(partyEntity.getDefendantFlags());

        List<CasePartyFlagEntity> mergedFlags = new ArrayList<>();
        mergedFlags.addAll(
            mergeOrRetainPartyFlags(incomingInternalFlags, FlagVisibility.INTERNAL, existingFlags, partyEntity));
        mergedFlags.addAll(
            mergeOrRetainPartyFlags(incomingExternalFlags, FlagVisibility.EXTERNAL, existingFlags, partyEntity));

        partyEntity.getDefendantFlags().clear();
        partyEntity.getDefendantFlags().addAll(mergedFlags);
    }

    private List<CasePartyFlagEntity> mergeOrRetainPartyFlags(Flags incomingFlags, FlagVisibility visibility,
                                                             List<CasePartyFlagEntity> existingFlags,
                                                             PartyEntity partyEntity) {
        if (hasNoFlagDetails(incomingFlags)) {
            return existingFlags.stream()
                .filter(existingFlag -> visibility == toFlagVisibility(existingFlag.getVisibility()))
                .toList();
        }

        return mergeFlagDetails(incomingFlags, visibility, null, partyEntity, CasePartyFlagEntity::new);
    }

    private boolean hasNoFlagDetails(Flags flags) {
        return flags == null || flags.getDetails() == null || flags.getDetails().isEmpty();
    }

    private <T extends BaseCaseFlag> List<T>  mergeFlagDetails(Flags incomingCaseFlags, FlagVisibility visibility,
                        PcsCaseEntity pcsCaseEntity, PartyEntity partyEntity, Supplier<T> flagEntitySupplier) {

        List<T> mergedFlagDetails = new ArrayList<>();
        Set<FlagRefDataEntity> flagRefDataEntities = new HashSet<>();

        for (ListValue<FlagDetail> incomingFlagDetailListValue : incomingCaseFlags.getDetails()) {
            FlagDetail incomingFlagDetail = incomingFlagDetailListValue.getValue();

            FlagRefDataEntity flagRefDataEntity = mergeFlagRefData(incomingFlagDetail);
            flagRefDataEntities.add(flagRefDataEntity);

            T flagEntity = flagEntitySupplier.get();

            flagEntity.setParentEntity(pcsCaseEntity, partyEntity);


            flagEntity.setFlagComment(incomingFlagDetail.getFlagComment());
            flagEntity.setFlagCommentWelsh(incomingFlagDetail.getFlagCommentCy());
            flagEntity.setFlagUpdateComment(incomingFlagDetail.getFlagUpdateComment());

            flagEntity.setDateTimeCreated(incomingFlagDetail.getDateTimeCreated());
            flagEntity.setDateTimeModified(incomingFlagDetail.getDateTimeModified());

            flagEntity.setDefaultStatus(incomingFlagDetail.getStatus());
            flagEntity.setSubTypeKey(incomingFlagDetail.getSubTypeKey());
            flagEntity.setSubTypeValue(incomingFlagDetail.getSubTypeValue());
            flagEntity.setSubTypeValueWelsh(incomingFlagDetail.getSubTypeValueCy());
            flagEntity.setFlagRefData(flagRefDataEntity);
            flagEntity.setVisibility(visibility.getValue());

            flagEntity.setOtherDescription(incomingFlagDetail.getOtherDescription());
            flagEntity.setOtherDescriptionWelsh(incomingFlagDetail.getOtherDescriptionCy());

            setFlagPath(incomingFlagDetail, flagEntity);

            mergedFlagDetails.add(flagEntity);
        }
        flagRefDataRepository.saveAll(flagRefDataEntities);

        return mergedFlagDetails;
    }

    private FlagRefDataEntity mergeFlagRefData(FlagDetail incomingFlagDetail) {

        FlagRefDataEntity flagRefDataEntity = flagRefDataRepository.findByFlagCode(
            incomingFlagDetail.getFlagCode()).orElse(new FlagRefDataEntity());

        flagRefDataEntity.setFlagCode(incomingFlagDetail.getFlagCode());
        flagRefDataEntity.setFlagName(incomingFlagDetail.getName());
        flagRefDataEntity.setFlagNameWelsh(incomingFlagDetail.getNameCy());
        flagRefDataEntity.setHearingRelevant(YesOrNoConverter.toBoolean(incomingFlagDetail.getHearingRelevant()));
        flagRefDataEntity.setAvailableExternally(YesOrNoConverter.toBoolean(
            incomingFlagDetail.getAvailableExternally()));

        return flagRefDataEntity;
    }

    private void setFlagPath(FlagDetail incomingFlagDetail, BaseCaseFlag flagEntity) {

        if (incomingFlagDetail.getPath() != null) {
            String paths = incomingFlagDetail.getPath().stream()
                .map(pathListValue -> pathListValue.getId() + CaseFlagsView.PATH_DELIMITER + pathListValue.getValue())
                .collect(Collectors.joining(CaseFlagsView.PATHS_DELIMITER));

            flagEntity.setPaths(paths);
        }
    }
}
