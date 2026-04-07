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
import java.util.Set;

@Component
public class CaseFlagsView {

    public void setCaseFields(PCSCase pcsCase, PcsCaseEntity pcsCaseEntity) {
        if (pcsCaseEntity.getCaseFlags() != null) {
            FlagsEntity flags = pcsCaseEntity.getCaseFlags();
            Set<PartyEntity> parties = pcsCaseEntity.getParties();
            mapBasicCaseFlagFields(pcsCase, pcsCaseEntity);
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
        List<ListValue<Party>> mappedParties = pcsCaseEntity.getCaseFlags() == null
            ? List.of()
            : pcsCaseEntity.getParties().stream()
            .map(this::mapPartyWithAppellantFlags)
                .reduce((party1, party2) -> party1)
                .orElse(List.of());

        pcsCase.setParties(mappedParties);
    }

    private List<ListValue<Party>> mapPartyWithAppellantFlags(PartyEntity partyEntity) {
        return List.of(ListValue.<Party>builder()
            .id(partyEntity.getId().toString())
            .value(Party.builder()
                        .appellantFlags((Flags) partyEntity.getAppellantFlags().stream()
                           .map(caseFlagEntity -> Flags.builder()
                               .roleOnCase(caseFlagEntity.getRoleOnCase())
                               .partyName(caseFlagEntity.getPartyName())
                               .details(mapFlagDetails(caseFlagEntity))
                               .build())
                           .toList()
                       )
                       .build()
            ).build()
        );
    }

    private List<ListValue<FlagDetail>> mapFlagDetails(FlagsEntity flagsEntity) {
        return flagsEntity.getCaseFlags().stream()
            .map(flagDetailsEntity -> ListValue.<FlagDetail>builder()
                .id(flagDetailsEntity.getId().toString())
                .value(FlagDetail.builder()
                           .flagCode(flagDetailsEntity.getFlagCode())
                           .flagComment(flagDetailsEntity.getFlagComment())
                           .nameCy(flagDetailsEntity.getName())
                           .name(flagDetailsEntity.getNameWelsh())
                           .flagComment(flagDetailsEntity.getFlagComment())
                           .flagCommentCy(flagDetailsEntity.getFlagCommentWelsh())
                           .status(flagDetailsEntity.getDefaultStatus())
                           .subTypeKey(flagDetailsEntity.getSubTypeKey())
                           .build())
                .build())
            .toList();
    }
}
