package uk.gov.hmcts.reform.pcs.ccd.view;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.type.FlagDetail;
import uk.gov.hmcts.ccd.sdk.type.FlagVisibility;
import uk.gov.hmcts.ccd.sdk.type.Flags;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.BaseCaseFlag;
import uk.gov.hmcts.reform.pcs.ccd.entity.CaseFlagEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.CasePartyFlagEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.FlagPathEntity;
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

    public void setCaseFields(PCSCase pcsCase, PcsCaseEntity pcsCaseEntity) {

        mapBasicCaseFlagFields(pcsCase, pcsCaseEntity);
        mapComplexPartyFlagFields(pcsCase, pcsCaseEntity);
    }

    private void mapBasicCaseFlagFields(PCSCase pcsCase, PcsCaseEntity pcsCaseEntity) {
        List<BaseCaseFlag> baseCaseFlags = new ArrayList<>(pcsCaseEntity.getCaseFlags());

        Flags caseFlags = pcsCaseEntity.getCaseFlags().isEmpty()
            ? Flags.builder().build()
            : Flags.builder()
            .visibility(FlagVisibility.INTERNAL)
            .details(mapFlagDetails(baseCaseFlags))
            .build();
        pcsCase.setCaseFlags(caseFlags);
    }

    private List<ListValue<FlagDetail>> mapFlagDetails(List<BaseCaseFlag> flagsEntities) {

        return flagsEntities.stream()
            .map(caseFlagEntity -> ListValue.<FlagDetail>builder()
                .id(caseFlagEntity.getId().toString())
                .value(FlagDetail.builder()
                   .flagCode(caseFlagEntity.getFlagCode())
                   .name(caseFlagEntity.getRefDataFlag().getFlagName())
                   .nameCy(caseFlagEntity.getRefDataFlag().getFlagNameWelsh())
                   .flagComment(caseFlagEntity.getFlagComment())
                   .flagCommentCy(caseFlagEntity.getFlagCommentWelsh())
                   .status(caseFlagEntity.getDefaultStatus())
                   .subTypeKey(caseFlagEntity.getSubTypeKey())
                   .subTypeValue(caseFlagEntity.getSubTypeValue())
                   .subTypeValueCy(caseFlagEntity.getSubTypeValueWelsh())
                   .flagUpdateComment(caseFlagEntity.getFlagUpdateComment() != null
                                      ? caseFlagEntity.getFlagUpdateComment()
                                      : caseFlagEntity.getFlagUpdateCommentWelsh())
                   .dateTimeCreated(caseFlagEntity.getDateTimeCreated())
                   .dateTimeModified(caseFlagEntity.getDateTimeModified())
                   .otherDescription(caseFlagEntity.getOtherDescription())
                   .otherDescriptionCy(caseFlagEntity.getOtherDescriptionWelsh())
                   .hearingRelevant(YesOrNoConverter.toYesOrNo(
                       caseFlagEntity.getRefDataFlag().getHearingRelevant()))
                   .availableExternally(YesOrNoConverter.toYesOrNo(
                       caseFlagEntity.getRefDataFlag().getAvailableExternally()))
                   .path(getPath(caseFlagEntity))
                   .build())
                .build())
            .toList();
    }

    private List<ListValue<String>> getPath(BaseCaseFlag flagEntity) {
        List<FlagPathEntity> flagPathEntities = new ArrayList<>();
        if (flagEntity instanceof CaseFlagEntity) {
            flagPathEntities = flagEntity.getCaseFlagPaths();
        } else if (flagEntity instanceof CasePartyFlagEntity) {
            flagPathEntities = flagEntity.getCasePartyFlagPaths();
        }
        return flagPathEntities.stream()
            .map(pathEntity -> ListValue.<String>builder()
                .id(pathEntity.getId().toString())
                .value(pathEntity.getPath())
                .build())
            .toList();
    }

    private void mapComplexPartyFlagFields(PCSCase pcsCase, PcsCaseEntity pcsCaseEntity) {
        List<ListValue<Party>> mappedParties = pcsCaseEntity.getParties().stream()
            .filter(partyEntity -> partyEntity.getOrgName() == null || partyEntity.getOrgName().isEmpty())
            .map(this::mapPartyWithRespondentFlags)
            .toList();

        pcsCase.setParties(mappedParties);
    }

    private ListValue<Party> mapPartyWithRespondentFlags(PartyEntity partyEntity) {
        return ListValue.<Party>builder()
            .id(partyEntity.getId().toString())
            .value(
                Party.builder()
                    .nameKnown(partyEntity.getNameKnown())
                    .addressKnown(partyEntity.getAddressKnown())
                    .phoneNumberProvided(partyEntity.getPhoneNumberProvided())
                    .emailAddress(partyEntity.getEmailAddress())
                    .firstName(partyEntity.getOrgName() == null || partyEntity.getOrgName().isEmpty()
                                 ? partyEntity.getFirstName() : partyEntity.getOrgName())
                    .lastName(partyEntity.getLastName())
                    .respondentFlags(mapRespondentFlags(partyEntity))
                    .build()
            )
            .build();
    }

    private Flags mapRespondentFlags(PartyEntity partyEntity) {
        if (partyEntity.getRespondentFlags() == null || partyEntity.getRespondentFlags().isEmpty()) {
            return Flags.builder()
                .partyName(partyEntity.getOrgName() == null || partyEntity.getOrgName().isEmpty()
                               ? partyEntity.getFirstName() + " " + partyEntity.getLastName()
                               : partyEntity.getOrgName())
                .roleOnCase(partyEntity.getOrgName() == null || partyEntity.getOrgName().isEmpty()
                                ? RESPONDENT : CLAIMANT)
                .details(new ArrayList<>())
                .build();
        }

        List<BaseCaseFlag> respondentFlags = new ArrayList<>(partyEntity.getRespondentFlags());

        return Flags.builder()
            .partyName(Stream.of(partyEntity.getFirstName(), partyEntity.getLastName(),
                                 partyEntity.getOrgName()).filter(
                Objects::nonNull).collect(Collectors.joining(" ")))
            .roleOnCase(partyEntity.getOrgName() == null || partyEntity.getOrgName().isEmpty()
                            ? RESPONDENT : CLAIMANT)
            .details(mapFlagDetails(respondentFlags))
            .visibility(FlagVisibility.INTERNAL)
            .build();
    }
}
