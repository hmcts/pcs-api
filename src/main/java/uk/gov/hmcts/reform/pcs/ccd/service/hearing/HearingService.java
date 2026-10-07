package uk.gov.hmcts.reform.pcs.ccd.service.hearing;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.hearing.Hearing;
import uk.gov.hmcts.reform.pcs.ccd.domain.hearing.HearingType;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.HearingEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.repository.HearingRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicMultiSelectStringList;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringListElement;
import uk.gov.hmcts.reform.pcs.exception.HearingNotFoundException;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class HearingService {

    private final PcsCaseService pcsCaseService;
    private final PcsCaseRepository pcsCaseRepository;
    private final HearingRepository hearingRepository;
    private final PartyService partyService;
    private final Clock ukClock;

    public HearingService(PcsCaseService pcsCaseService,
                          PcsCaseRepository pcsCaseRepository,
                          HearingRepository hearingRepository,
                          PartyService partyService,
                          @Qualifier("ukClock") Clock ukClock) {
        this.pcsCaseService = pcsCaseService;
        this.pcsCaseRepository = pcsCaseRepository;
        this.hearingRepository = hearingRepository;
        this.partyService = partyService;
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

    public void cancelHearing(Hearing hearing) {
        int hearingId = Objects.requireNonNull(hearing.getHearingId(), "Hearing ID must be set");

        HearingEntity hearingEntity = hearingRepository.findById(hearingId)
            .orElseThrow(() -> new HearingNotFoundException("Hearing not found with ID " + hearingId));

        hearingEntity.setCancelled(true);
        hearingEntity.setCancellationReason(hearing.getCancellationReason());
    }

    public void clearHearingForm(PCSCase pcsCase) {
        pcsCase.setHearing(Hearing.builder().build());
        pcsCase.setManageHearingDraft(null);
        pcsCase.setPartyMultiSelectionList(clearSelectedParties(pcsCase.getPartyMultiSelectionList()));
        pcsCase.setMhDraftPartyList(null);
    }

    public void setSelectedEditableHearingId(PCSCase pcsCase, PcsCaseEntity pcsCaseEntity) {
        findEditableHearing(pcsCaseEntity).ifPresent(selectedHearing ->
            pcsCase.setSelectedHearingId(selectedHearing.getId().toString())
        );
    }

    public Optional<HearingEntity> findEditableHearing(PcsCaseEntity pcsCaseEntity) {
        return editableHearing(pcsCaseEntity, null);
    }

    public Optional<HearingEntity> findEditableHearing(long caseReference) {
        return findEditableHearing(pcsCaseService.loadCase(caseReference));
    }

    public void initialiseEditableHearing(long caseReference, PCSCase pcsCase, String previouslySelectedHearingId) {
        PcsCaseEntity pcsCaseEntity = pcsCaseService.loadCase(caseReference);

        setSelectedEditableHearingId(pcsCase, pcsCaseEntity);
        if (Objects.equals(previouslySelectedHearingId, pcsCase.getSelectedHearingId())
            && hasEditableHearingDetails(pcsCase.getManageHearingDraft())) {
            restoreDraftHearingForm(pcsCase);
        } else {
            prepopulateEditableHearing(pcsCase, pcsCaseEntity);
        }
    }

    public void storeDraftHearingForm(PCSCase pcsCase) {
        pcsCase.setManageHearingDraft(copyHearing(pcsCase.getHearing()));
        pcsCase.setMhDraftPartyList(pcsCase.getPartyMultiSelectionList());
    }

    public void prepopulateEditableHearing(long caseReference, PCSCase pcsCase) {
        PcsCaseEntity pcsCaseEntity = pcsCaseService.loadCase(caseReference);
        prepopulateEditableHearing(pcsCase, pcsCaseEntity);
    }

    private void prepopulateEditableHearing(PCSCase pcsCase, PcsCaseEntity pcsCaseEntity) {
        editableHearing(pcsCaseEntity, pcsCase.getSelectedHearingId()).ifPresent(selectedHearing -> {
            Hearing currentHearing = pcsCase.getHearing();
            String hearingSummaryMarkdown = currentHearing == null ? null : currentHearing.getHearingSummaryMarkdown();
            pcsCase.setSelectedHearingId(selectedHearing.getId().toString());
            Hearing hearing = mapToHearing(selectedHearing);
            hearing.setHearingSummaryMarkdown(hearingSummaryMarkdown);
            pcsCase.setHearing(hearing);
            pcsCase.setPartyMultiSelectionList(preselectNoticeRecipients(
                partyListForPrepopulation(pcsCase, pcsCaseEntity),
                selectedHearing.getNoticeParties()
            ));
        });
    }

    public DynamicMultiSelectStringList buildPartyList(PcsCaseEntity pcsCaseEntity) {
        ClaimEntity mainClaim = pcsCaseEntity.getMainClaim();
        Map<PartyRole, List<ClaimPartyEntity>> partyRoleListMap = mainClaim.getClaimParties().stream()
            .collect(Collectors.groupingBy(ClaimPartyEntity::getRole));

        List<DynamicStringListElement> partyElementList = new ArrayList<>();

        partyRoleListMap.getOrDefault(PartyRole.CLAIMANT, List.of()).stream()
            .map(claimPartyEntity -> mapToPartyListElement(mainClaim, claimPartyEntity.getParty()))
            .forEach(partyElementList::add);

        partyRoleListMap.getOrDefault(PartyRole.DEFENDANT, List.of()).stream()
            .map(claimPartyEntity -> mapToPartyListElement(mainClaim, claimPartyEntity.getParty()))
            .forEach(partyElementList::add);

        return DynamicMultiSelectStringList.builder()
            .value(new ArrayList<>())
            .listItems(partyElementList)
            .build();
    }

    private void restoreDraftHearingForm(PCSCase pcsCase) {
        if (pcsCase.getManageHearingDraft() != null) {
            pcsCase.setHearing(copyHearing(pcsCase.getManageHearingDraft()));
        }
        if (pcsCase.getMhDraftPartyList() != null) {
            pcsCase.setPartyMultiSelectionList(pcsCase.getMhDraftPartyList());
        }
    }

    private boolean hasEditableHearingDetails(Hearing hearing) {
        return hearing != null
            && (hearing.getType() != null
                || hearing.getNoticeWording() != null
                || hearing.getDate() != null
                || hearing.getDurationDays() != null
                || hearing.getDurationHours() != null
                || hearing.getDurationMinutes() != null
                || hearing.getIssueNotice() != null
                || hearing.getNotes() != null
                || hearing.getAdditionalInformation() != null);
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
        hearingEntity.setDurationHours(hearing.getDurationHours().intValue());
        hearingEntity.setDurationMinutes(hearing.getDurationMinutes().intValue());
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
            selectedNoticePartyIds(pcsCase).forEach(hearingEntity::addParty);
        }

        return hearingEntity;
    }

    private DynamicMultiSelectStringList partyListForPrepopulation(PCSCase pcsCase, PcsCaseEntity pcsCaseEntity) {
        DynamicMultiSelectStringList currentPartyList = pcsCase.getPartyMultiSelectionList();
        if (currentPartyList != null && !CollectionUtils.isEmpty(currentPartyList.getListItems())) {
            return currentPartyList;
        }

        return buildPartyList(pcsCaseEntity);
    }

    private DynamicStringListElement mapToPartyListElement(ClaimEntity mainClaim, PartyEntity partyEntity) {
        String partyName = partyService.getPartyName(partyEntity);
        String partyLabel = partyService.getPartyLabel(mainClaim, partyEntity.getId());
        String label = ("%s - %s").formatted(partyName, partyLabel);
        return DynamicStringListElement.builder()
            .code(partyEntity.getId().toString())
            .label(label)
            .build();
    }

    private List<UUID> selectedNoticePartyIds(PCSCase pcsCase) {
        DynamicMultiSelectStringList selectedParties = pcsCase.getPartyMultiSelectionList();
        if (selectedParties != null && !CollectionUtils.isEmpty(selectedParties.getValue())) {
            return selectedParties.getValue().stream()
                .map(DynamicStringListElement::getCode)
                .filter(Objects::nonNull)
                .map(UUID::fromString)
                .toList();
        }

        return List.of();
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
            .filter(hearing -> !Boolean.TRUE.equals(hearing.getCancelled()))
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
            .hearingId(hearing.getId())
            .type(hearing.getType())
            .otherHearingType(hearing.getOtherHearingType())
            .noticeWording(hearing.getNoticeWording())
            .date(hearing.getHearingDate())
            .durationDays(hearing.getDurationDays())
            .durationHours(hearing.getDurationHours().floatValue())
            .durationMinutes(hearing.getDurationMinutes().floatValue())
            .notes(hearing.getNotes())
            .issueNotice(hearing.getIssueNotice())
            .isWithoutNotice(hearing.getIsWithoutNotice())
            .additionalInformation(hearing.getAdditionalInformation())
            .cancellationReason(hearing.getCancellationReason())
            .build();
    }

    private Hearing copyHearing(Hearing hearing) {
        if (hearing == null) {
            return null;
        }

        return Hearing.builder()
            .hearingId(hearing.getHearingId())
            .hearingSummaryMarkdown(hearing.getHearingSummaryMarkdown())
            .type(hearing.getType())
            .otherHearingType(hearing.getOtherHearingType())
            .noticeWording(hearing.getNoticeWording())
            .date(hearing.getDate())
            .durationDays(hearing.getDurationDays())
            .durationHours(hearing.getDurationHours())
            .durationMinutes(hearing.getDurationMinutes())
            .notes(hearing.getNotes())
            .issueNotice(hearing.getIssueNotice())
            .isWithoutNotice(hearing.getIsWithoutNotice())
            .additionalInformation(hearing.getAdditionalInformation())
            .cancellationReason(hearing.getCancellationReason())
            .build();
    }

    private DynamicMultiSelectStringList clearSelectedParties(DynamicMultiSelectStringList partyList) {
        if (partyList == null) {
            return null;
        }

        return DynamicMultiSelectStringList.builder()
            .value(new ArrayList<>())
            .listItems(partyList.getListItems())
            .build();
    }

    private DynamicMultiSelectStringList preselectNoticeRecipients(DynamicMultiSelectStringList partyList,
                                                                  List<UUID> selectedPartyIds) {
        if (partyList == null || CollectionUtils.isEmpty(partyList.getListItems())
            || CollectionUtils.isEmpty(selectedPartyIds)) {
            return partyList;
        }

        List<String> selectedPartyIdStrings = selectedPartyIds.stream()
            .map(UUID::toString)
            .toList();

        List<DynamicStringListElement> selectedParties = partyList.getListItems().stream()
            .filter(party -> selectedPartyIdStrings.contains(party.getCode()))
            .toList();

        return DynamicMultiSelectStringList.builder()
            .value(selectedParties)
            .listItems(partyList.getListItems())
            .build();
    }
}
