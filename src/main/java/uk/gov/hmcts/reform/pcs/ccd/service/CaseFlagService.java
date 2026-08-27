package uk.gov.hmcts.reform.pcs.ccd.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import uk.gov.hmcts.ccd.sdk.type.FlagDetail;
import uk.gov.hmcts.ccd.sdk.type.FlagVisibility;
import uk.gov.hmcts.ccd.sdk.type.Flags;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.camunda.CamundaService;
import uk.gov.hmcts.reform.pcs.camunda.TaskType;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.domain.PartySupport;
import uk.gov.hmcts.reform.pcs.ccd.entity.BaseCaseFlag;
import uk.gov.hmcts.reform.pcs.ccd.entity.CaseFlagEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.CasePartyFlagEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.FlagRefDataEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.FlagRefDataRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartySupportOwnershipResolver;
import uk.gov.hmcts.reform.pcs.ccd.service.workallocation.TaskDescriptionService;
import uk.gov.hmcts.reform.pcs.ccd.service.workallocation.TranslationWAService;
import uk.gov.hmcts.reform.pcs.ccd.util.YesOrNoConverter;
import uk.gov.hmcts.reform.pcs.ccd.view.CaseFlagsView;
import uk.gov.hmcts.reform.pcs.exception.CaseAccessException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNullElse;
import static uk.gov.hmcts.reform.pcs.ccd.util.FlagVisibilityConverter.toFlagVisibility;

@Service
@AllArgsConstructor
@Slf4j
public class CaseFlagService {

    private static final String WELSH_COMMUNICATIONS_FLAG_CODE = "PF0026";
    private static final String ACTIVE_STATUS = "Active";
    private static final String RA_FLAG_CODE_PREFIX = "RA";
    private static final String SUPPORT_NOT_REPRESENTED_MESSAGE =
        "User cannot change support for this party on this case";

    private FlagRefDataRepository flagRefDataRepository;
    private CamundaService camundaService;
    private TaskDescriptionService taskDescriptionService;
    private PartySupportOwnershipResolver partySupportOwnershipResolver;
    private TranslationWAService translationWAService;

    public List<CaseFlagEntity> mergeCaseFlags(Flags incomingCaseFlags, PcsCaseEntity pcsCaseEntity) {

        return mergeFlagDetails(incomingCaseFlags, FlagVisibility.INTERNAL, pcsCaseEntity, null,
                                CaseFlagEntity::new, RefDataPolicy.UPDATE_FROM_PAYLOAD,
                                pcsCaseEntity.getCaseFlags());
    }

    /**
     * Applies the reasonable adjustment flags a defendant supplied via the cui-ra microsite to their
     * party. Only RA flags are accepted and replaced. Caseworker flags arrive through
     * {@link #mergePartyFlags(List, Set)} instead, which is not restricted in this way.
     */
    public void saveReasonableAdjustmentFlags(PartyEntity partyEntity, Flags incomingFlags, long caseReference) {
        if (incomingFlags == null || CollectionUtils.isEmpty(incomingFlags.getDetails())) {
            return;
        }

        List<ListValue<FlagDetail>> reasonableAdjustmentDetails = incomingFlags.getDetails().stream()
            .filter(detail -> isReasonableAdjustmentCode(detail.getValue().getFlagCode()))
            .toList();

        int ignored = incomingFlags.getDetails().size() - reasonableAdjustmentDetails.size();
        if (ignored > 0) {
            log.warn("Ignoring {} supplied flag(s) for party {} that are not reasonable adjustments",
                     ignored, partyEntity.getId());
        }

        if (reasonableAdjustmentDetails.isEmpty()) {
            return;
        }

        List<String> activeFlags = reasonableAdjustmentDetails.stream()
            .map(ListValue::getValue)
            .filter(CaseFlagService::isCaseFlagActive)
            .map(FlagDetail::getName)
            .toList();

        if (!CollectionUtils.isEmpty(activeFlags)) {
            String taskDescription = taskDescriptionService
                .createReviewCaseFlagDescription(caseReference, activeFlags);
            camundaService.createTask(caseReference, TaskType.REVIEW_CASE_FLAG, taskDescription);
        }

        Flags reasonableAdjustmentFlags = Flags.builder()
            .visibility(incomingFlags.getVisibility())
            .details(reasonableAdjustmentDetails)
            .build();

        List<CasePartyFlagEntity> casePartyFlags = mergeFlagDetails(
            reasonableAdjustmentFlags, null, null, partyEntity, CasePartyFlagEntity::new,
            RefDataPolicy.CREATE_IF_ABSENT, List.of());

        partyEntity.getDefendantFlags().removeIf(CaseFlagService::isReasonableAdjustmentFlag);
        partyEntity.getDefendantFlags().addAll(casePartyFlags);
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

        boolean welshCommsAlreadyActive = hasActiveWelshCommunicationsFlag(partyEntity.getDefendantFlags());
        partyEntity.getDefendantFlags().clear();
        partyEntity.getDefendantFlags().addAll(mergedFlags);
        fireOnActiveWelshFlags(partyEntity, mergedFlags, welshCommsAlreadyActive);
    }

    private void fireOnActiveWelshFlags(PartyEntity partyEntity, List<CasePartyFlagEntity> mergedFlags,
                                        boolean welshCommsAlreadyActive) {
        // Only fire when the flag just became active, to avoid triggering duplicate tasks for the given party
        if (!welshCommsAlreadyActive && hasActiveWelshCommunicationsFlag(mergedFlags)) {
            translationWAService.triggerTranslationTasksForFlaggingParty(partyEntity);
        }
    }

    private List<CasePartyFlagEntity> mergeOrRetainPartyFlags(Flags incomingFlags, FlagVisibility visibility,
                                                              List<CasePartyFlagEntity> existingFlags,
                                                              PartyEntity partyEntity) {
        // Merge candidates are scoped to this visibility, so an incoming flag can only ever update a stored
        // flag of the same visibility even when the ids match.
        List<CasePartyFlagEntity> existingFlagsForVisibility = existingFlags.stream()
            .filter(existingFlag -> visibility == toFlagVisibility(existingFlag.getVisibility()))
            .toList();

        if (hasNoFlagDetails(incomingFlags)) {
            return existingFlagsForVisibility;
        }

        return mergeFlagDetails(incomingFlags, visibility, null, partyEntity, CasePartyFlagEntity::new,
                                RefDataPolicy.UPDATE_FROM_PAYLOAD, existingFlagsForVisibility);
    }

    private boolean hasNoFlagDetails(Flags flags) {
        return flags == null || flags.getDetails() == null || flags.getDetails().isEmpty();
    }

    private <T extends BaseCaseFlag> List<T>  mergeFlagDetails(Flags incomingCaseFlags, FlagVisibility visibility,
                                                               PcsCaseEntity pcsCaseEntity, PartyEntity partyEntity,
                                                               Supplier<T> flagEntitySupplier,
                                                               RefDataPolicy refDataPolicy,
                                                               List<T> existingFlags) {

        List<T> mergedFlagDetails = new ArrayList<>();
        Set<FlagRefDataEntity> flagRefDataEntities = new HashSet<>();
        Map<UUID, T> unmatchedExistingFlags = indexFlagsById(existingFlags);

        // Caseworker events state the visibility explicitly; flags supplied from outside CCD carry it
        // on the payload instead, and default to internal when absent.
        FlagVisibility effectiveVisibility = visibility != null
            ? visibility
            : requireNonNullElse(incomingCaseFlags.getVisibility(), FlagVisibility.INTERNAL);

        for (ListValue<FlagDetail> incomingFlagDetailListValue : incomingCaseFlags.getDetails()) {
            FlagDetail incomingFlagDetail = incomingFlagDetailListValue.getValue();

            FlagRefDataEntity flagRefDataEntity =
                mergeFlagRefData(incomingFlagDetail, effectiveVisibility.getValue(), refDataPolicy);
            flagRefDataEntities.add(flagRefDataEntity);

            T flagEntity = findExistingFlag(incomingFlagDetailListValue, unmatchedExistingFlags)
                .orElseGet(flagEntitySupplier);

            flagEntity.setParentEntity(pcsCaseEntity, partyEntity);

            applyEditedFlagFields(flagEntity, incomingFlagDetail);
            applyCarriedThroughFlagFields(flagEntity, incomingFlagDetail);

            flagEntity.setFlagRefData(flagRefDataEntity);
            flagEntity.setVisibility(effectiveVisibility.getValue());

            mergedFlagDetails.add(flagEntity);
        }
        flagRefDataRepository.saveAll(flagRefDataEntities);

        return mergedFlagDetails;
    }

    private <T extends BaseCaseFlag> Map<UUID, T> indexFlagsById(List<T> existingFlags) {
        Map<UUID, T> flagsById = new HashMap<>();
        existingFlags.stream()
            .filter(existingFlag -> existingFlag.getId() != null)
            .forEach(existingFlag -> flagsById.put(existingFlag.getId(), existingFlag));

        return flagsById;
    }

    private <T extends BaseCaseFlag> Optional<T> findExistingFlag(ListValue<FlagDetail> incomingFlagDetailListValue,
                                                                  Map<UUID, T> unmatchedExistingFlags) {
        String listValueId = incomingFlagDetailListValue.getId();
        if (listValueId == null || listValueId.isBlank() || unmatchedExistingFlags.isEmpty()) {
            return Optional.empty();
        }

        try {
            return Optional.ofNullable(unmatchedExistingFlags.remove(UUID.fromString(listValueId)));
        } catch (IllegalArgumentException e) {
            log.debug("Flag list id {} is not a stored flag id", listValueId);
            return Optional.empty();
        }
    }

    private void applyEditedFlagFields(BaseCaseFlag flagEntity, FlagDetail incomingFlagDetail) {
        flagEntity.setDefaultStatus(incomingFlagDetail.getStatus());
        flagEntity.setFlagComment(incomingFlagDetail.getFlagComment());
        flagEntity.setFlagCommentWelsh(incomingFlagDetail.getFlagCommentCy());
        flagEntity.setFlagUpdateComment(incomingFlagDetail.getFlagUpdateComment());
        flagEntity.setDateTimeModified(incomingFlagDetail.getDateTimeModified());
    }

    private void applyCarriedThroughFlagFields(BaseCaseFlag flagEntity, FlagDetail incomingFlagDetail) {
        setIfSupplied(incomingFlagDetail.getOtherDescription(), flagEntity::setOtherDescription);
        setIfSupplied(incomingFlagDetail.getOtherDescriptionCy(), flagEntity::setOtherDescriptionWelsh);
        setIfSupplied(incomingFlagDetail.getSubTypeKey(), flagEntity::setSubTypeKey);
        setIfSupplied(incomingFlagDetail.getSubTypeValue(), flagEntity::setSubTypeValue);
        setIfSupplied(incomingFlagDetail.getSubTypeValueCy(), flagEntity::setSubTypeValueWelsh);

        if (flagEntity.getDateTimeCreated() == null) {
            flagEntity.setDateTimeCreated(incomingFlagDetail.getDateTimeCreated());
        }

        setFlagPath(incomingFlagDetail, flagEntity);
    }

    private void setIfSupplied(String incomingValue, Consumer<String> setter) {
        if (incomingValue != null && !incomingValue.isBlank()) {
            setter.accept(incomingValue);
        }
    }

    private FlagRefDataEntity mergeFlagRefData(FlagDetail incomingFlagDetail,
                                               String visibility,
                                               RefDataPolicy refDataPolicy) {

        Optional<FlagRefDataEntity> existingFlagRefData =
            flagRefDataRepository.findByFlagCode(incomingFlagDetail.getFlagCode());

        // Reference data is shared by every case using this flag code, so a party-supplied flag
        // references the existing row as it stands rather than rewriting it from the payload.
        if (existingFlagRefData.isPresent() && refDataPolicy == RefDataPolicy.CREATE_IF_ABSENT) {
            return existingFlagRefData.get();
        }

        FlagRefDataEntity flagRefDataEntity = existingFlagRefData.orElseGet(FlagRefDataEntity::new);

        flagRefDataEntity.setFlagCode(incomingFlagDetail.getFlagCode());
        flagRefDataEntity.setFlagName(incomingFlagDetail.getName());
        flagRefDataEntity.setFlagNameWelsh(incomingFlagDetail.getNameCy());
        flagRefDataEntity.setVisibility(visibility);
        flagRefDataEntity.setHearingRelevant(YesOrNoConverter.toBoolean(incomingFlagDetail.getHearingRelevant()));
        flagRefDataEntity.setAvailableExternally(YesOrNoConverter.toBoolean(
            incomingFlagDetail.getAvailableExternally()));

        return flagRefDataEntity;
    }

    private static boolean isReasonableAdjustmentFlag(BaseCaseFlag flag) {
        return flag.getFlagRefData() != null
            && isReasonableAdjustmentCode(flag.getFlagRefData().getFlagCode());
    }

    private static boolean isReasonableAdjustmentCode(String flagCode) {
        return flagCode != null && flagCode.startsWith(RA_FLAG_CODE_PREFIX);
    }

    private void setFlagPath(FlagDetail incomingFlagDetail, BaseCaseFlag flagEntity) {

        if (!CollectionUtils.isEmpty(incomingFlagDetail.getPath())) {
            String paths = incomingFlagDetail.getPath().stream()
                .map(pathListValue -> requireNonNullElse(pathListValue.getId(), "")
                    + CaseFlagsView.PATH_DELIMITER + pathListValue.getValue())
                .collect(Collectors.joining(CaseFlagsView.PATHS_DELIMITER));

            flagEntity.setPaths(paths);
        }
    }

    private boolean hasActiveWelshCommunicationsFlag(List<CasePartyFlagEntity> flags) {
        return flags.stream().anyMatch(this::isWelshCommunicationsPreference);
    }

    public boolean isWelshCommunicationsPreference(BaseCaseFlag flagEntity) {
        return flagEntity.getFlagRefData() != null
            && WELSH_COMMUNICATIONS_FLAG_CODE.equals(flagEntity.getFlagRefData().getFlagCode())
            && ACTIVE_STATUS.equals(flagEntity.getDefaultStatus());
    }


    public void mergePartySupportFlags(List<ListValue<PartySupport>> incomingPartySupport,
                                       Set<PartyEntity> existingParties,
                                       UUID authenticatedUserId) {
        Map<UUID, PartyEntity> existingPartiesMap = mapPartiesById(existingParties);

        for (ListValue<PartySupport> incomingSupportValue : incomingPartySupport) {
            Flags incomingSupportFlags = incomingSupportValue.getValue() == null
                ? null
                : incomingSupportValue.getValue().getSupportFlags();

            PartyEntity partyEntity = resolveSupportParty(incomingSupportValue.getId(), existingPartiesMap);

            if (!partySupportOwnershipResolver.isOwnedByUser(partyEntity, authenticatedUserId)) {
                if (changesSupport(incomingSupportFlags, partyEntity)) {
                    throw new CaseAccessException(SUPPORT_NOT_REPRESENTED_MESSAGE);
                }

                continue;
            }

            mergePartyFlagCollections(null, incomingSupportFlags, partyEntity);
        }
    }


    private boolean changesSupport(Flags incomingSupportFlags, PartyEntity partyEntity) {
        if (incomingSupportFlags == null || incomingSupportFlags.getDetails() == null) {
            return false;
        }

        Map<String, CasePartyFlagEntity> existingExternalFlags = getExistingExternalFlags(partyEntity);
        List<ListValue<FlagDetail>> incomingDetails = incomingSupportFlags.getDetails();

        if (incomingDetails.size() != existingExternalFlags.size()) {
            return true;
        }

        return incomingDetails.stream().anyMatch(incomingDetail -> {
            CasePartyFlagEntity existingFlag = existingExternalFlags.get(incomingDetail.getId());
            return existingFlag == null || differs(incomingDetail.getValue(), existingFlag);
        });
    }

    private static @NonNull Map<String, CasePartyFlagEntity> getExistingExternalFlags(PartyEntity partyEntity) {
        return partyEntity.getDefendantFlags().stream()
            .filter(existingFlag -> FlagVisibility.EXTERNAL == toFlagVisibility(existingFlag.getVisibility()))
            .collect(Collectors.toMap(existingFlag -> existingFlag.getId().toString(), Function.identity()));
    }

    private boolean differs(FlagDetail incomingFlagDetail, CasePartyFlagEntity existingFlag) {
        if (incomingFlagDetail == null) {
            return true;
        }

        boolean editedFieldsDiffer =
            !Objects.equals(incomingFlagDetail.getStatus(), existingFlag.getDefaultStatus())
                || !Objects.equals(incomingFlagDetail.getFlagComment(), existingFlag.getFlagComment())
                || !Objects.equals(incomingFlagDetail.getFlagCommentCy(), existingFlag.getFlagCommentWelsh())
                || !Objects.equals(incomingFlagDetail.getFlagUpdateComment(), existingFlag.getFlagUpdateComment());

        return editedFieldsDiffer
            || suppliedAndDiffers(incomingFlagDetail.getFlagCode(), existingFlagCode(existingFlag))
            || suppliedAndDiffers(incomingFlagDetail.getOtherDescription(), existingFlag.getOtherDescription())
            || suppliedAndDiffers(incomingFlagDetail.getOtherDescriptionCy(),
                                  existingFlag.getOtherDescriptionWelsh())
            || suppliedAndDiffers(incomingFlagDetail.getSubTypeKey(), existingFlag.getSubTypeKey())
            || suppliedAndDiffers(incomingFlagDetail.getSubTypeValue(), existingFlag.getSubTypeValue())
            || suppliedAndDiffers(incomingFlagDetail.getSubTypeValueCy(), existingFlag.getSubTypeValueWelsh());
    }

    private static String existingFlagCode(CasePartyFlagEntity existingFlag) {
        return existingFlag.getFlagRefData() == null ? null : existingFlag.getFlagRefData().getFlagCode();
    }

    private static boolean suppliedAndDiffers(String incomingValue, String existingValue) {
        return incomingValue != null && !incomingValue.isBlank()
            && !Objects.equals(incomingValue, existingValue);
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

    private static boolean isCaseFlagActive(FlagDetail flagDetail) {
        return Objects.equals(flagDetail.getStatus(), "Active");
    }

    /**
     * Whether the shared {@code flag_ref_data} row for a flag code may be rewritten from the incoming
     * payload. Caseworker events own that reference data; party-supplied flags may only reference it,
     * or create it where the code has not been seen before.
     */
    private enum RefDataPolicy {
        UPDATE_FROM_PAYLOAD,
        CREATE_IF_ABSENT
    }
}
