package uk.gov.hmcts.reform.pcs.ccd.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.type.DynamicListElement;
import uk.gov.hmcts.ccd.sdk.type.DynamicMultiSelectList;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.hearing.Hearing;
import uk.gov.hmcts.reform.pcs.ccd.domain.hearing.HearingType;
import uk.gov.hmcts.reform.pcs.ccd.entity.HearingEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;

import java.util.ArrayList;
import java.util.Objects;

@Service
@AllArgsConstructor
public class HearingService {

    private final PcsCaseService pcsCaseService;
    private final PcsCaseRepository pcsCaseRepository;

    public void addHearing(long caseReference, PCSCase pcsCase) {
        PcsCaseEntity pcsCaseEntity = pcsCaseService.loadCase(caseReference);
        HearingEntity hearingEntity = populateHearingEntity(new HearingEntity(), pcsCase);
        pcsCaseEntity.addHearing(hearingEntity);
        pcsCaseRepository.save(pcsCaseEntity);
    }

    public void updateHearing(long caseReference, PCSCase pcsCase) {
        PcsCaseEntity pcsCaseEntity = pcsCaseService.loadCase(caseReference);
        Integer selectedHearingId = Integer.valueOf(Objects.requireNonNull(
            pcsCase.getSelectedHearingId(),
            "Selected hearing id must be populated before editing a hearing"
        ));

        HearingEntity hearingEntity = pcsCaseEntity.getHearings().stream()
            .filter(hearing -> selectedHearingId.equals(hearing.getId()))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException(
                "Selected hearing %s was not found on case %s".formatted(selectedHearingId, caseReference)
            ));

        populateHearingEntity(hearingEntity, pcsCase);
        pcsCaseRepository.save(pcsCaseEntity);
    }

    private HearingEntity populateHearingEntity(HearingEntity hearingEntity, PCSCase pcsCase) {
        Hearing hearing = pcsCase.getHearing();
        HearingType hearingType = hearing.getType();
        VerticalYesNo issueNotice = hearing.getIssueNotice();
        VerticalYesNo isWithoutNotice = hearing.getIsWithoutNotice();

        hearingEntity.setType(hearingType);
        hearingEntity.setNoticeWording(hearing.getNoticeWording());
        hearingEntity.setHearingDate(hearing.getDate());
        hearingEntity.setDurationDays(hearing.getDurationDays());
        hearingEntity.setDurationHours(hearing.getDurationHours());
        hearingEntity.setDurationMinutes(hearing.getDurationMinutes());
        hearingEntity.setIssueNotice(issueNotice);
        hearingEntity.setNotes(hearing.getNotes());
        hearingEntity.setIsWithoutNotice(isWithoutNotice);
        hearingEntity.setAdditionalInformation(hearing.getAdditionalInformation());
        hearingEntity.setNoticeParties(new ArrayList<>());

        if (hearingType == HearingType.OTHER) {
            hearingEntity.setOtherHearingType(hearing.getOtherHearingType());
        } else {
            hearingEntity.setOtherHearingType(null);
        }

        if (issueNotice == VerticalYesNo.YES && isWithoutNotice == VerticalYesNo.YES) {
            DynamicMultiSelectList selectedParties = pcsCase.getPartyMultiSelectionList();

            if (selectedParties != null && selectedParties.getValue() != null) {
                addPartiesToHearingEntity(selectedParties, hearingEntity);
            }
        }

        return hearingEntity;
    }

    private void addPartiesToHearingEntity(DynamicMultiSelectList selectedParties, HearingEntity hearingEntity) {
        selectedParties.getValue()
            .stream()
            .map(DynamicListElement::getCode)
            .forEach(hearingEntity::addParty);
    }
}
