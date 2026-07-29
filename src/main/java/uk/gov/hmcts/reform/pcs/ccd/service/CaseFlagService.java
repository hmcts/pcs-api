package uk.gov.hmcts.reform.pcs.ccd.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
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

import static java.util.Objects.requireNonNullElse;


@Service
@AllArgsConstructor
public class CaseFlagService {

    private static final String RA_FLAG_CODE_PREFIX = "RA";

    private FlagRefDataRepository flagRefDataRepository;

    public List<CaseFlagEntity> mergeCaseFlags(Flags incomingCaseFlags, PcsCaseEntity pcsCaseEntity) {

        return mergeFlagDetails(incomingCaseFlags, pcsCaseEntity,null,
                        CaseFlagEntity::new);
    }

    /**
     * Applies the reasonable adjustment flags a defendant supplied via the cui-ra microsite to their
     * party. Only the RA flags are replaced, so any flag a caseworker raised against the same party
     * survives - the defendant is never shown those and so could not have supplied them back.
     */
    public void savePartyFlags(PartyEntity partyEntity, Flags incomingFlags) {
        if (incomingFlags == null || CollectionUtils.isEmpty(incomingFlags.getDetails())) {
            return;
        }

        List<CasePartyFlagEntity> mergedCasePartyFlags = mergeFlagDetails(
            incomingFlags, null, partyEntity, CasePartyFlagEntity::new);

        partyEntity.getDefendantFlags().removeIf(CaseFlagService::isReasonableAdjustmentFlag);
        partyEntity.getDefendantFlags().addAll(mergedCasePartyFlags);
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
                && !incomingParty.getDefendantFlags().getDetails().isEmpty()) {
                List<CasePartyFlagEntity> mergedCasePartyFlags = mergeFlagDetails(
                    incomingParty.getDefendantFlags(), null, partyEntity, CasePartyFlagEntity::new);

                partyEntity.getDefendantFlags().clear();
                partyEntity.getDefendantFlags().addAll(mergedCasePartyFlags);
            }
        }
    }

    private <T extends BaseCaseFlag> List<T>  mergeFlagDetails(Flags incomingCaseFlags, PcsCaseEntity pcsCaseEntity,
                        PartyEntity partyEntity, Supplier<T> flagEntitySupplier) {

        List<T> mergedFlagDetails = new ArrayList<>();
        Set<FlagRefDataEntity> flagRefDataEntities = new HashSet<>();

        String flagVisibility = incomingCaseFlags.getVisibility() != null
            ? incomingCaseFlags.getVisibility().getValue()
            : FlagVisibility.INTERNAL.getValue();

        for (ListValue<FlagDetail> incomingFlagDetailListValue : incomingCaseFlags.getDetails()) {
            FlagDetail incomingFlagDetail = incomingFlagDetailListValue.getValue();

            FlagRefDataEntity flagRefDataEntity = mergeFlagRefData(incomingFlagDetail, flagVisibility);
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

    private static boolean isReasonableAdjustmentFlag(BaseCaseFlag flag) {
        return flag.getFlagRefData() != null
            && flag.getFlagRefData().getFlagCode() != null
            && flag.getFlagRefData().getFlagCode().startsWith(RA_FLAG_CODE_PREFIX);
    }

    private void setFlagPath(FlagDetail incomingFlagDetail, BaseCaseFlag flagEntity) {

        if (incomingFlagDetail.getPath() == null) {
            // The paths column is not nullable
            flagEntity.setPaths("");
            return;
        }

        // Flags raised outside CCD arrive with path values but no ids
        String paths = incomingFlagDetail.getPath().stream()
            .map(pathListValue -> requireNonNullElse(pathListValue.getId(), "")
                + CaseFlagsView.PATH_DELIMITER + pathListValue.getValue())
            .collect(Collectors.joining(CaseFlagsView.PATHS_DELIMITER));

        flagEntity.setPaths(paths);
    }
}

