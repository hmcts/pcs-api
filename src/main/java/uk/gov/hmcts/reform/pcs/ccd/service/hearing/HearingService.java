package uk.gov.hmcts.reform.pcs.ccd.service.hearing;

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
import uk.gov.hmcts.reform.pcs.ccd.repository.HearingRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.exception.HearingNotFoundException;

import java.util.Objects;

@Service
@AllArgsConstructor
public class HearingService {

    private final PcsCaseService pcsCaseService;
    private final PcsCaseRepository pcsCaseRepository;
    private final HearingRepository hearingRepository;

    public void addHearing(long caseReference, PCSCase pcsCase) {
        PcsCaseEntity pcsCaseEntity = pcsCaseService.loadCase(caseReference);
        HearingEntity hearingEntity = createHearingEntity(pcsCase);
        pcsCaseEntity.addHearing(hearingEntity);
        pcsCaseRepository.save(pcsCaseEntity);
    }

    public void cancelHearing(Hearing hearing) {
        Long hearingId = Objects.requireNonNull(hearing.getHearingId(), "Hearing ID must be set");

        HearingEntity hearingEntity = loadHearing(hearingId);

        hearingEntity.setCancelled(true);
        hearingEntity.setCancellationReason(hearing.getCancellationReason());
    }

    private HearingEntity loadHearing(Long hearingId) {
        return hearingRepository.findById(hearingId)
            .orElseThrow(() -> new HearingNotFoundException("Hearing not found with ID " + hearingId));
    }

    private HearingEntity createHearingEntity(PCSCase pcsCase) {
        Hearing hearing = pcsCase.getHearing();
        HearingType hearingType = hearing.getType();
        VerticalYesNo issueNotice = hearing.getIssueNotice();
        VerticalYesNo isWithoutNotice = hearing.getIsWithoutNotice();
        HearingEntity hearingEntity = HearingEntity.builder()
            .type(hearingType)
            .noticeWording(hearing.getNoticeWording())
            .hearingDate(hearing.getDate())
            .durationHours(hearing.getDurationHours())
            .durationMinutes(hearing.getDurationMinutes())
            .issueNotice(issueNotice)
            .notes(hearing.getNotes())
            .isWithoutNotice(isWithoutNotice)
            .additionalInformation(hearing.getAdditionalInformation())
            .build();

        if (hearingType == HearingType.OTHER) {
            hearingEntity.setOtherHearingType(hearing.getOtherHearingType());
        }

        if (issueNotice == VerticalYesNo.YES && isWithoutNotice == VerticalYesNo.YES) {
            DynamicMultiSelectList selectedParties = pcsCase.getPartyMultiSelectionList();

            if (selectedParties != null) {
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
