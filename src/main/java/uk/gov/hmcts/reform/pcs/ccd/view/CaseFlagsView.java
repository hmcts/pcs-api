package uk.gov.hmcts.reform.pcs.ccd.view;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.type.FlagDetail;
import uk.gov.hmcts.ccd.sdk.type.Flags;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.entity.FlagsEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;

import java.util.List;


@Component
public class CaseFlagsView {

    public void setCaseFields(PCSCase pcsCase, PcsCaseEntity pcsCaseEntity) {
        if (pcsCaseEntity.getCaseFlags() != null) {
            mapBasicCaseFlagFields(pcsCase, pcsCaseEntity);
        }
        if (pcsCaseEntity.getParties() != null) {
            mapComplexPartyFlagFields(pcsCase, pcsCaseEntity);
        }
    }

    private void mapBasicCaseFlagFields(PCSCase pcsCase, PcsCaseEntity pcsCaseEntity) {
        Flags caseFlags = pcsCaseEntity.getCaseFlags() == null
            ? Flags.builder().build()
            : Flags.builder()
            .details(mapFlagDetails(pcsCaseEntity.getCaseFlags()))
            .build();
        pcsCase.setCaseFlags(caseFlags);
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

    private List<ListValue<FlagDetail>> mapFlagDetails(FlagsEntity flagsEntity) {
        if (flagsEntity.getCaseFlags() == null || flagsEntity.getCaseFlags().isEmpty()) {
            return List.of();
        }

        return flagsEntity.getCaseFlags().stream()
            .map(flagDetailsEntity -> ListValue.<FlagDetail>builder()
                .id(flagDetailsEntity.getId().toString())
                .value(FlagDetail.builder()
                           .flagCode(flagDetailsEntity.getFlagCode())
                           .flagComment(flagDetailsEntity.getFlagComment())
                           .nameCy(flagDetailsEntity.getName())
                           .name(flagDetailsEntity.getNameWelsh())
                           .flagCommentCy(flagDetailsEntity.getFlagCommentWelsh())
                           .status(flagDetailsEntity.getDefaultStatus())
                           .subTypeKey(flagDetailsEntity.getSubTypeKey())
                           .build())
                .build())
            .toList();
    }
}
