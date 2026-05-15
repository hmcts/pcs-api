package uk.gov.hmcts.reform.pcs.ccd.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.ccd.sdk.type.Flags;
import uk.gov.hmcts.ccd.sdk.type.FlagDetail;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.ccd.sdk.type.FlagVisibility;
import uk.gov.hmcts.reform.pcs.ccd.entity.CaseFlagEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.CasePartyFlagEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.FlagRefDataEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.BaseCaseFlag;
import uk.gov.hmcts.reform.pcs.ccd.repository.FlagRefDataRepository;
import uk.gov.hmcts.reform.pcs.ccd.util.YesOrNoConverter;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.view.CaseFlagsView;

import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;
import java.util.ArrayList;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;


@Service
@AllArgsConstructor
public class CaseFlagService {

    private FlagRefDataRepository flagRefDataRepository;

    public List<CaseFlagEntity> mergeCaseFlags(Flags incomingCaseFlags, PcsCaseEntity pcsCaseEntity, String flow) {

        Map<UUID, CaseFlagEntity> existingCaseFlagEntitiesMap =
                pcsCaseEntity.getCaseFlags().stream()
                    .collect(Collectors.toMap(BaseCaseFlag::getId, Function.identity()));

        return mergeFlagDetails(existingCaseFlagEntitiesMap, incomingCaseFlags, pcsCaseEntity,null, flow,
                        CaseFlagEntity::new);
    }

    public void mergePartyFlags(List<ListValue<Party>> incomingParties, Set<PartyEntity> existingParties,
                                             String flow) {
        Map<UUID, PartyEntity> existingPartiesMap = existingParties.stream()
            .collect(Collectors.toMap(
                PartyEntity::getId,
                Function.identity()
            ));

        for (ListValue<Party> incomingPartyValue : incomingParties) {
            Party incomingParty = incomingPartyValue.getValue();

            PartyEntity partyEntity = existingPartiesMap.get(UUID.fromString(incomingPartyValue.getId()));

            if (incomingParty.getDefendantFlags() != null
                && !incomingParty.getDefendantFlags().getDetails().isEmpty()) {
                mergePartyFlagGroup(incomingParty.getDefendantFlags(), partyEntity.getDefendantFlags(),
                                    partyEntity, flow
                );
            }
        }
    }

    private void mergePartyFlagGroup(Flags incomingPartyFlags, List<CasePartyFlagEntity> existingFlags,
                                     PartyEntity partyEntity, String flow) {

        Map<UUID, CasePartyFlagEntity> existingPartyFlagEntityMap =
            existingFlags.stream()
                .collect(Collectors.toMap(BaseCaseFlag::getId, Function.identity()));

        List<CasePartyFlagEntity> mergedCasePartyFlags = mergeFlagDetails(
            existingPartyFlagEntityMap, incomingPartyFlags, null,
            partyEntity, flow, CasePartyFlagEntity::new);

        partyEntity.getDefendantFlags().clear();
        partyEntity.getDefendantFlags().addAll(mergedCasePartyFlags);
    }

    private <T extends BaseCaseFlag> List<T>  mergeFlagDetails(
                        Map<UUID, T> existingFlagEntitiesMap, Flags incomingCaseFlags, PcsCaseEntity pcsCaseEntity,
                        PartyEntity partyEntity, String flow, Supplier<T> flagEntitySupplier) {

        List<T> mergedFlagDetails = new ArrayList<>();
        Set<FlagRefDataEntity> flagRefDataEntities = new HashSet<>();

        String flagVisibility = incomingCaseFlags.getVisibility() != null
            ? incomingCaseFlags.getVisibility().getValue()
            : FlagVisibility.INTERNAL.getValue();

        for (ListValue<FlagDetail> incomingFlagDetailListValue : incomingCaseFlags.getDetails()) {
            FlagDetail incomingFlagDetail = incomingFlagDetailListValue.getValue();

            FlagRefDataEntity flagRefDataEntity = mergeFlagRefData(incomingFlagDetail, flagVisibility);
            flagRefDataEntities.add(flagRefDataEntity);

            T flagEntity = existingFlagEntitiesMap.remove(UUID.fromString(incomingFlagDetailListValue.getId()));


            if (flagEntity == null) {
                flagEntity = flagEntitySupplier.get();
            }

            flagEntity.setParentEntity(pcsCaseEntity, partyEntity);

            if ("CREATE".equals(flow)) {
                flagEntity.setFlagComment(incomingFlagDetail.getFlagComment());
                flagEntity.setFlagCommentWelsh(incomingFlagDetail.getFlagCommentCy());
            } else {
                if ("Inactive".equals(flagEntity.getDefaultStatus())) {
                    flagEntity.setFlagUpdateComment(incomingFlagDetail.getFlagUpdateComment());
                } else {
                    flagEntity.setFlagUpdateComment(incomingFlagDetail.getFlagComment());
                }
                flagEntity.setFlagUpdateCommentWelsh(incomingFlagDetail.getFlagCommentCy());
            }

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
                                               String visibility) {

        FlagRefDataEntity flagRefDataEntity = flagRefDataRepository.findByFlagCode(
            incomingFlagDetail.getFlagCode()).orElse(new FlagRefDataEntity());

        flagRefDataEntity.setFlagCode(incomingFlagDetail.getFlagCode());
        flagRefDataEntity.setFlagName(incomingFlagDetail.getName());
        flagRefDataEntity.setFlagNameWelsh(incomingFlagDetail.getNameCy());
        flagRefDataEntity.setVisibility(visibility);
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

