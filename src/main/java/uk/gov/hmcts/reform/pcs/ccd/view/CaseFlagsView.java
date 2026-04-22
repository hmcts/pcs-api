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
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.RefDataFlagsRepository;
import uk.gov.hmcts.reform.pcs.ccd.util.YesOrNoConverter;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@AllArgsConstructor
public class CaseFlagsView {

    private static final String RESPONDENT = "respondent";
    private static final String CLAIMANT = "claimant";

    private final RefDataFlagsRepository refDataFlagsRepository;

    public void setCaseFields(PCSCase pcsCase, PcsCaseEntity pcsCaseEntity) {

        mapBasicCaseFlagFields(pcsCase, pcsCaseEntity);
        mapComplexPartyFlagFields(pcsCase, pcsCaseEntity);
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
                   .name(refDataFlagsRepository.findByFlagCode(
                       flagDetailsEntity.getFlagCode()).orElseThrow().getFlagName())
                   .nameCy(refDataFlagsRepository.findByFlagCode(
                       flagDetailsEntity.getFlagCode()).orElseThrow().getFlagNameWelsh())
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
                   .hearingRelevant(YesOrNoConverter.toYesOrNo(refDataFlagsRepository.findByFlagCode(
                       flagDetailsEntity.getFlagCode()).orElseThrow().getHearingRelevant()))
                   .availableExternally(YesOrNoConverter.toYesOrNo(refDataFlagsRepository.findByFlagCode(
                       flagDetailsEntity.getFlagCode()).orElseThrow().getAvailableExternally()))
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
            .map(this::mapPartyWithRespondentFlags)
            .toList();

        pcsCase.setParties(mappedParties);
    }

    private ListValue<Party> mapPartyWithRespondentFlags(PartyEntity partyEntity) {
        return ListValue.<Party>builder()
            .id(partyEntity.getId().toString())
            .value(
                Party.builder()
                    .firstName(partyEntity.getFirstName())
                    .lastName(partyEntity.getLastName())
                    .respondentFlags(mapRespondentFlags(partyEntity))
                    .build()
            )
            .build();
    }

    private Flags mapRespondentFlags(PartyEntity partyEntity) {
        if (partyEntity.getRespondentFlags() == null || partyEntity.getRespondentFlags().isEmpty()) {
            return Flags.builder().details(new ArrayList<>()).build();
        }

        List<FlagDetailsEntity> respondentFlags = partyEntity.getRespondentFlags();

        return Flags.builder()
            .partyName(Stream.of(partyEntity.getFirstName(), partyEntity.getLastName(),
                                 partyEntity.getOrgName()).filter(
                Objects::nonNull).collect(Collectors.joining(" ")))
            .roleOnCase(partyEntity.getOrgName() != null && !partyEntity.getOrgName().isEmpty()
                            ? CLAIMANT
                            : RESPONDENT)
            .details(mapFlagDetails(respondentFlags))
            .visibility(FlagVisibility.INTERNAL)
            .build();
    }
}
