package uk.gov.hmcts.reform.pcs.ccd.view;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.type.FlagDetail;
import uk.gov.hmcts.ccd.sdk.type.FlagVisibility;
import uk.gov.hmcts.ccd.sdk.type.Flags;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.entity.FlagDetailsEntity;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.entity.FlagsEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.util.YesOrNoConverter;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;

import java.util.List;

@Component
@AllArgsConstructor
public class CaseFlagsView {

    public void setCaseFields(PCSCase pcsCase, PcsCaseEntity pcsCaseEntity) {

        mapBasicCaseFlagFields(pcsCase, pcsCaseEntity);
    }

    private void mapBasicCaseFlagFields(PCSCase pcsCase, PcsCaseEntity pcsCaseEntity) {
        Flags caseFlags = pcsCaseEntity.getCaseFlags().isEmpty()
            ? Flags.builder().build()
            : Flags.builder()
            .visibility(FlagVisibility.INTERNAL)
            .details(mapFlagDetails(pcsCaseEntity.getCaseFlags()))
            .build();
        pcsCase.setCaseFlags(caseFlags);
    }

    private List<ListValue<FlagDetail>> mapFlagDetails(List<FlagDetailsEntity> flagsEntities) {

        return flagsEntities.stream()
            .map(flagDetailsEntity -> ListValue.<FlagDetail>builder()
                .id(flagDetailsEntity.getId().toString())
                .value(FlagDetail.builder()
                   .flagCode(flagDetailsEntity.getFlagCode())
                   .name(flagDetailsEntity.getName())
                   .nameCy(flagDetailsEntity.getNameWelsh())
                   .flagComment(flagDetailsEntity.getFlagComment())
                   .flagCommentCy(flagDetailsEntity.getFlagCommentWelsh())
                   .status(flagDetailsEntity.getDefaultStatus())
                   .subTypeKey(flagDetailsEntity.getSubTypeKey())
                   .subTypeValue(flagDetailsEntity.getSubTypeValue())
                   .subTypeValueCy(flagDetailsEntity.getSubTypeValueWelsh())
                   .flagUpdateComment(flagDetailsEntity.getFlagUpdateComment())
                   .dateTimeCreated(flagDetailsEntity.getDateTimeCreated())
                   .dateTimeModified(flagDetailsEntity.getDateTimeModified())
                   .otherDescription(flagDetailsEntity.getOtherDescription())
                   .otherDescriptionCy(flagDetailsEntity.getOtherDescriptionWelsh())
                   .hearingRelevant(YesOrNoConverter.toYesOrNo(flagDetailsEntity.getHearingRelevant()))
                   .availableExternally(YesOrNoConverter.toYesOrNo(flagDetailsEntity.getAvailableExternally()))
                   .path(flagDetailsEntity.getPaths().stream()
                             .map(pathEntity -> ListValue.<String>builder()
                                .id(pathEntity.getId().toString())
                                .value(pathEntity.getPath())
                                .build())
                             .toList())
                   .build())
                .build())
            .toList();
    }

    private void mapComplexPartyFlagFields(PCSCase pcsCase, PcsCaseEntity pcsCaseEntity) {
        List<ListValue<Party>> mappedParties = pcsCaseEntity.getParties().stream()
            .map(this::mapPartyWithAppellantFlags)
            .toList();

        pcsCase.setParties(mappedParties);
    }

    private ListValue<Party> mapPartyWithAppellantFlags(PartyEntity partyEntity) {
        return ListValue.<Party>builder()
            .id(partyEntity.getId().toString())
            .value(
                Party.builder()
                    .firstName(partyEntity.getFirstName())
                    .lastName(partyEntity.getLastName())
                    .appellantFlags(mapAppellantFlags(partyEntity))
                    .build()
            )
            .build();
    }

    private Flags mapAppellantFlags(PartyEntity partyEntity) {
        if (partyEntity.getAppellantFlags() == null || partyEntity.getAppellantFlags().isEmpty()) {
            return Flags.builder().details(List.of()).build();
        }

        FlagsEntity firstAppellantFlag = partyEntity.getAppellantFlags().getFirst();

        return Flags.builder()
            .partyName(firstAppellantFlag.getPartyName())
            .roleOnCase(firstAppellantFlag.getRoleOnCase())
            .details(mapFlagDetails(firstAppellantFlag))
            .build();
    }

}
