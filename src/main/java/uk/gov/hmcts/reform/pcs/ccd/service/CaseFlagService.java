package uk.gov.hmcts.reform.pcs.ccd.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import uk.gov.hmcts.ccd.sdk.type.FlagDetail;
import uk.gov.hmcts.ccd.sdk.type.FlagVisibility;
import uk.gov.hmcts.ccd.sdk.type.Flags;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.camunda.CamundaService;
import uk.gov.hmcts.reform.pcs.camunda.TaskType;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.entity.BaseCaseFlag;
import uk.gov.hmcts.reform.pcs.ccd.entity.CaseFlagEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.CasePartyFlagEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.FlagRefDataEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.FlagRefDataRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.workallocation.TaskDescriptionService;
import uk.gov.hmcts.reform.pcs.ccd.service.workallocation.TranslationWAService;
import uk.gov.hmcts.reform.pcs.ccd.util.YesOrNoConverter;
import uk.gov.hmcts.reform.pcs.ccd.view.CaseFlagsView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNullElse;


@Service
@Slf4j
@AllArgsConstructor
public class CaseFlagService {

    private static final String WELSH_COMMUNICATIONS_FLAG_CODE = "PF0026";
    private static final String ACTIVE_STATUS = "Active";
    private static final String REQUESTED_STATUS = "Requested";
    private static final String RA_FLAG_CODE_PREFIX = "RA";

    private FlagRefDataRepository flagRefDataRepository;
    private CamundaService camundaService;
    private TaskDescriptionService taskDescriptionService;
    private TranslationWAService translationWAService;

    public List<CaseFlagEntity> mergeCaseFlags(Flags incomingCaseFlags, PcsCaseEntity pcsCaseEntity) {
        List<CaseFlagEntity> mergedFlagDetails = mergeFlagDetails(
            incomingCaseFlags, pcsCaseEntity, null, CaseFlagEntity::new, RefDataPolicy.UPDATE_FROM_PAYLOAD
        );

        createReviewCaseFlagRequestTask(
            pcsCaseEntity.getCaseReference(),
            getRequestedFlagNames(incomingCaseFlags.getDetails())
        );

        return mergedFlagDetails;
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

        createReviewCaseFlagRequestTask(caseReference, getRequestedFlagNames(reasonableAdjustmentDetails));

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
            reasonableAdjustmentFlags, null, partyEntity, CasePartyFlagEntity::new,
            RefDataPolicy.CREATE_IF_ABSENT);

        partyEntity.getDefendantFlags().removeIf(CaseFlagService::isReasonableAdjustmentFlag);
        partyEntity.getDefendantFlags().addAll(casePartyFlags);
    }

    public void mergePartyFlags(List<ListValue<Party>> incomingParties, Set<PartyEntity> existingParties) {
        Map<UUID, PartyEntity> existingPartiesMap = existingParties.stream()
            .collect(Collectors.toMap(
                PartyEntity::getId,
                Function.identity()
            ));

        for (ListValue<Party> incomingPartyValue : incomingParties) {
            Party incomingParty = incomingPartyValue.getValue();

            PartyEntity partyEntity = existingPartiesMap.get(UUID.fromString(incomingPartyValue.getId()));

            if (incomingParty.getDefendantFlags() != null
                && !CollectionUtils.isEmpty(incomingParty.getDefendantFlags().getDetails())) {
                boolean welshCommsAlreadyActive = hasActiveWelshCommunicationsFlag(partyEntity.getDefendantFlags());
                List<CasePartyFlagEntity> mergedCasePartyFlags = mergeFlagDetails(
                    incomingParty.getDefendantFlags(), null, partyEntity, CasePartyFlagEntity::new,
                    RefDataPolicy.UPDATE_FROM_PAYLOAD);

                partyEntity.getDefendantFlags().clear();
                partyEntity.getDefendantFlags().addAll(mergedCasePartyFlags);

                // Only fire when the flag just became active, to avoid triggering duplicate tasks for the given party
                if (!welshCommsAlreadyActive && hasActiveWelshCommunicationsFlag(mergedCasePartyFlags)) {
                    translationWAService.triggerTranslationTasksForFlaggingParty(partyEntity);
                }
            }
        }
    }

    private void createReviewCaseFlagRequestTask(Long caseReference, List<String> requestedFlags) {
        if (caseReference == null || CollectionUtils.isEmpty(requestedFlags)) {
            return;
        }

        String taskDescription = taskDescriptionService
            .createReviewCaseFlagRequestDescription(caseReference, requestedFlags);
        camundaService.createTask(caseReference, TaskType.REVIEW_CASE_FLAG_REQUEST, taskDescription);
    }

    private static List<String> getRequestedFlagNames(List<ListValue<FlagDetail>> details) {
        if (CollectionUtils.isEmpty(details)) {
            return List.of();
        }

        return details.stream()
            .map(ListValue::getValue)
            .filter(CaseFlagService::isCaseFlagRequested)
            .map(FlagDetail::getName)
            .toList();
    }

    private <T extends BaseCaseFlag> List<T>  mergeFlagDetails(Flags incomingCaseFlags, PcsCaseEntity pcsCaseEntity,
                        PartyEntity partyEntity, Supplier<T> flagEntitySupplier, RefDataPolicy refDataPolicy) {

        List<T> mergedFlagDetails = new ArrayList<>();
        Set<FlagRefDataEntity> flagRefDataEntities = new HashSet<>();

        String flagVisibility = incomingCaseFlags.getVisibility() != null
            ? incomingCaseFlags.getVisibility().getValue()
            : FlagVisibility.INTERNAL.getValue();

        for (ListValue<FlagDetail> incomingFlagDetailListValue : incomingCaseFlags.getDetails()) {
            FlagDetail incomingFlagDetail = incomingFlagDetailListValue.getValue();

            FlagRefDataEntity flagRefDataEntity =
                mergeFlagRefData(incomingFlagDetail, flagVisibility, refDataPolicy);
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

            flagEntity.setOtherDescription(incomingFlagDetail.getOtherDescription());
            flagEntity.setOtherDescriptionWelsh(incomingFlagDetail.getOtherDescriptionCy());

            setFlagPath(incomingFlagDetail, flagEntity);

            mergedFlagDetails.add(flagEntity);
        }
        flagRefDataRepository.saveAll(flagRefDataEntities);

        return mergedFlagDetails;
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

        if (incomingFlagDetail.getPath() != null) {
            // Flags raised outside CCD arrive with path values but no ids
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

    private boolean isWelshCommunicationsPreference(BaseCaseFlag flagEntity) {
        return flagEntity.getFlagRefData() != null
            && WELSH_COMMUNICATIONS_FLAG_CODE.equals(flagEntity.getFlagRefData().getFlagCode())
            && ACTIVE_STATUS.equals(flagEntity.getDefaultStatus());
    }

    private static boolean isCaseFlagActive(FlagDetail flagDetail) {
        return Objects.equals(flagDetail.getStatus(), ACTIVE_STATUS);
    }

    private static boolean isCaseFlagRequested(FlagDetail flagDetail) {
        return Objects.equals(flagDetail.getStatus(), REQUESTED_STATUS);
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
