package uk.gov.hmcts.reform.pcs.ccd.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.domain.Hearing;
import uk.gov.hmcts.reform.pcs.ccd.domain.HearingType;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.hearing.HearingEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.PartyRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicMultiSelectStringList;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringListElement;

import java.util.List;
import java.util.UUID;

@Service
@AllArgsConstructor
public class HearingService {

    private final PcsCaseService pcsCaseService;
    private final PartyRepository partyRepository;
    private final PcsCaseRepository pcsCaseRepository;

    public void addHearing(long caseReference, PCSCase pcsCase) {
        PcsCaseEntity pcsCaseEntity = pcsCaseService.loadCase(caseReference);
        HearingEntity hearingEntity = createHearingEntity(pcsCase);
        pcsCaseEntity.addHearing(hearingEntity);
        pcsCaseRepository.save(pcsCaseEntity);
    }

    private HearingEntity createHearingEntity(PCSCase pcsCase) {
        Hearing hearing = pcsCase.getHearing();
        HearingType hearingType = hearing.getType();
        VerticalYesNo noticeIssued = hearing.getNoticeIssued();
        VerticalYesNo isWithoutNotice = hearing.getIsWithoutNotice();
        HearingEntity hearingEntity = HearingEntity.builder()
            .type(hearingType)
            .noticeWording(hearing.getNoticeWording())
            .date(hearing.getDate())
            .durationHours(hearing.getDurationHours())
            .durationMinutes(hearing.getDurationMinutes())
            .noticeIssued(noticeIssued)
            .notes(hearing.getNotes())
            .isWithoutNotice(isWithoutNotice)
            .additionalInformation(hearing.getAdditionalInformation())
            .build();

        if (hearingType == HearingType.OTHER) {
            hearingEntity.setOtherHearingType(hearing.getOtherHearingType());
        }

        if (noticeIssued == VerticalYesNo.YES && isWithoutNotice == VerticalYesNo.YES) {
            DynamicMultiSelectStringList selectedParties = pcsCase.getPartyMultiSelectionList();

            if (selectedParties != null) {
                addPartiesToHearingEntity(selectedParties, hearingEntity);
            }
        }

        return hearingEntity;
    }

    private void addPartiesToHearingEntity(DynamicMultiSelectStringList selectedParties, HearingEntity hearingEntity) {
        List<UUID> selectedIds = selectedParties.getValue()
            .stream()
            .map(DynamicStringListElement::getCode)
            .map(UUID::fromString)
            .toList();

        List<PartyEntity> parties = partyRepository.findAllById(selectedIds);

        for (PartyEntity partyEntity : parties) {
            hearingEntity.addParty(partyEntity);
        }
    }
}
