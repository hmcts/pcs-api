package uk.gov.hmcts.reform.pcs.ccd.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import uk.gov.hmcts.ccd.sdk.type.DynamicListElement;
import uk.gov.hmcts.ccd.sdk.type.DynamicMultiSelectList;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.hearing.Hearing;
import uk.gov.hmcts.reform.pcs.ccd.domain.hearing.HearingType;
import uk.gov.hmcts.reform.pcs.ccd.entity.HearingEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
public class HearingService {

    private final PcsCaseService pcsCaseService;
    private final PcsCaseRepository pcsCaseRepository;
    private final Clock ukClock;

    public HearingService(PcsCaseService pcsCaseService,
                          PcsCaseRepository pcsCaseRepository,
                          @Qualifier("ukClock") Clock ukClock) {
        this.pcsCaseService = pcsCaseService;
        this.pcsCaseRepository = pcsCaseRepository;
        this.ukClock = ukClock;
    }

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

    public void clearHearingForm(PCSCase pcsCase) {
        pcsCase.setHearing(Hearing.builder().build());
        pcsCase.setPartyMultiSelectionList(clearSelectedParties(pcsCase.getPartyMultiSelectionList()));
    }

    public void setSelectedEditableHearingId(PCSCase pcsCase, PcsCaseEntity pcsCaseEntity) {
        editableHearing(pcsCaseEntity, null).ifPresent(selectedHearing ->
            pcsCase.setSelectedHearingId(selectedHearing.getId().toString())
        );
    }

    public void prepopulateEditableHearing(long caseReference, PCSCase pcsCase) {
        PcsCaseEntity pcsCaseEntity = pcsCaseService.loadCase(caseReference);

        editableHearing(pcsCaseEntity, pcsCase.getSelectedHearingId()).ifPresent(selectedHearing -> {
            pcsCase.setSelectedHearingId(selectedHearing.getId().toString());
            pcsCase.setHearing(mapToHearing(selectedHearing));
            pcsCase.setPartyMultiSelectionList(preselectNoticeRecipients(
                pcsCase.getPartyMultiSelectionList(),
                selectedHearing.getNoticeParties()
            ));
        });
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

    private Optional<HearingEntity> editableHearing(PcsCaseEntity pcsCaseEntity, String selectedHearingId) {
        if (CollectionUtils.isEmpty(pcsCaseEntity.getHearings())) {
            return Optional.empty();
        }

        if (selectedHearingId != null) {
            return pcsCaseEntity.getHearings().stream()
                .filter(hearing -> hearing != null && hearing.getId() != null)
                .filter(hearing -> selectedHearingId.equals(hearing.getId().toString()))
                .findFirst();
        }

        List<HearingEntity> hearingsByDate = pcsCaseEntity.getHearings().stream()
            .filter(hearing -> hearing != null && hearing.getHearingDate() != null)
            .toList();

        LocalDateTime now = LocalDateTime.now(ukClock);

        Optional<HearingEntity> nextUpcomingHearing = hearingsByDate.stream()
            .filter(hearing -> !hearing.getHearingDate().isBefore(now))
            .min(Comparator.comparing(HearingEntity::getHearingDate));

        return nextUpcomingHearing.or(() -> hearingsByDate.stream()
            .max(Comparator.comparing(HearingEntity::getHearingDate)));
    }

    private Hearing mapToHearing(HearingEntity hearing) {
        return Hearing.builder()
            .type(hearing.getType())
            .otherHearingType(hearing.getOtherHearingType())
            .noticeWording(hearing.getNoticeWording())
            .date(hearing.getHearingDate())
            .durationDays(hearing.getDurationDays())
            .durationHours(hearing.getDurationHours())
            .durationMinutes(hearing.getDurationMinutes())
            .notes(hearing.getNotes())
            .issueNotice(hearing.getIssueNotice())
            .isWithoutNotice(hearing.getIsWithoutNotice())
            .additionalInformation(hearing.getAdditionalInformation())
            .build();
    }

    private DynamicMultiSelectList clearSelectedParties(DynamicMultiSelectList partyList) {
        if (partyList == null) {
            return null;
        }

        return DynamicMultiSelectList.builder()
            .listItems(partyList.getListItems())
            .build();
    }

    private DynamicMultiSelectList preselectNoticeRecipients(DynamicMultiSelectList partyList,
                                                            List<UUID> selectedPartyIds) {
        if (partyList == null || CollectionUtils.isEmpty(partyList.getListItems())
            || CollectionUtils.isEmpty(selectedPartyIds)) {
            return partyList;
        }

        List<DynamicListElement> selectedParties = partyList.getListItems().stream()
            .filter(party -> selectedPartyIds.contains(party.getCode()))
            .toList();

        return DynamicMultiSelectList.builder()
            .value(selectedParties)
            .listItems(partyList.getListItems())
            .build();
    }
}
