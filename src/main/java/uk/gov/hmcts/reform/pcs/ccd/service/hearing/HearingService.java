package uk.gov.hmcts.reform.pcs.ccd.service.hearing;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import uk.gov.hmcts.reform.pcs.ccd.domain.DocumentType;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.hearing.Hearing;
import uk.gov.hmcts.reform.pcs.ccd.domain.hearing.HearingType;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.hearing.HearingEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.hearing.HearingNoticePartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.repository.DocumentRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.HearingRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.PartyRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicMultiSelectStringList;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringListElement;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringList;
import uk.gov.hmcts.reform.pcs.exception.HearingNotFoundException;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class HearingService {

    private final PcsCaseService pcsCaseService;
    private final HearingRepository hearingRepository;
    private final PartyRepository partyRepository;
    private final PartyService partyService;
    private final DocumentRepository documentRepository;
    private final Clock ukClock;

    public HearingService(PcsCaseService pcsCaseService,
                          HearingRepository hearingRepository,
                          PartyRepository partyRepository,
                          PartyService partyService,
                          DocumentRepository documentRepository,
                          @Qualifier("ukClock") Clock ukClock) {
        this.pcsCaseService = pcsCaseService;
        this.hearingRepository = hearingRepository;
        this.partyRepository = partyRepository;
        this.partyService = partyService;
        this.documentRepository = documentRepository;
        this.ukClock = ukClock;
    }

    public HearingEntity addHearing(long caseReference, PCSCase pcsCase) {
        PcsCaseEntity pcsCaseEntity = pcsCaseService.loadCase(caseReference);
        HearingEntity hearingEntity = populateHearingEntity(new HearingEntity(), pcsCase);
        pcsCaseEntity.addHearing(hearingEntity);
        assignNoticeNumber(pcsCaseEntity, hearingEntity);
        return hearingRepository.save(hearingEntity);
    }

    public HearingEntity updateHearing(long caseReference, PCSCase pcsCase) {
        PcsCaseEntity pcsCaseEntity = pcsCaseService.loadCase(caseReference);
        HearingEntity hearingEntity = requireAvailableHearing(pcsCaseEntity, pcsCase.getSelectedHearingId(),
            pcsCase.makeOrderEnabled());

        populateHearingEntity(hearingEntity, pcsCase);
        // Edits before the notice is generated keep the same notice number and later edits get the next number
        if (hearingEntity.getNoticeNumber() == null
            || documentRepository.existsByHearing_IdAndType(hearingEntity.getId(), DocumentType.NOTICE_OF_HEARING)) {
            assignNoticeNumber(pcsCaseEntity, hearingEntity);
        }
        return hearingRepository.save(hearingEntity);
    }

    private static void assignNoticeNumber(PcsCaseEntity pcsCaseEntity, HearingEntity hearingEntity) {
        if (hearingEntity.getIssueNotice() != VerticalYesNo.YES) {
            return;
        }
        int latestNoticeNumber = pcsCaseEntity.getHearings().stream()
            .map(HearingEntity::getNoticeNumber)
            .filter(Objects::nonNull)
            .max(Integer::compare)
            .orElse(0);
        hearingEntity.setNoticeNumber(latestNoticeNumber + 1);
    }

    public void cancelHearing(long caseReference, PCSCase pcsCase) {
        PcsCaseEntity entity = pcsCaseService.loadCase(caseReference);
        String hearingId = pcsCase.getSelectedHearingId();
        if (hearingId == null && pcsCase.getHearing() != null && pcsCase.getHearing().getHearingId() != null) {
            // A cancellation opened with the previous definition carries only the hearing's ID.
            hearingId = pcsCase.getHearing().getHearingId().toString();
        }
        HearingEntity hearingEntity = requireAvailableHearing(entity, hearingId, pcsCase.makeOrderEnabled());
        hearingEntity.setCancelled(true);
        hearingEntity.setCancellationReason(pcsCase.getHearing().getCancellationReason());
    }

    public DynamicStringList buildHearingSelection(PcsCaseEntity pcsCaseEntity) {
        var formatter = DateTimeFormatter.ofPattern("d MMMM yyyy 'at' h:mma", Locale.UK);
        LocalDateTime now = LocalDateTime.now(ukClock);
        var options = pcsCaseEntity.getHearings().stream()
            .filter(hearing -> hearing != null && hearing.getId() != null)
            .filter(hearing -> isLiveFutureHearing(hearing, now))
            .sorted(Comparator.comparing(HearingEntity::getHearingDate).thenComparing(HearingEntity::getId))
            .map(hearing -> DynamicStringListElement.builder()
                .code(hearing.getId().toString())
                .label("%s — %s".formatted(
                    hearing.getType() == HearingType.OTHER
                        ? hearing.getOtherHearingType() : hearing.getType().getLabel(),
                    formatter.format(hearing.getHearingDate())))
                .build())
            .toList();
        return DynamicStringList.builder().listItems(options)
            .value(options.size() == 1 ? options.getFirst() : null).build();
    }

    public HearingEntity resolveHearingSelection(long caseReference, PCSCase form) {
        PcsCaseEntity pcsCase = pcsCaseService.loadCase(caseReference);
        if (!form.makeOrderEnabled()) {
            return findEditableHearing(pcsCase).orElseThrow(HearingService::unavailableHearing);
        }
        String selectedId = form.getHearingSelection() == null ? null : form.getHearingSelection().getValueCode();
        return requireAvailableHearing(pcsCase, selectedId, true);
    }

    // Staff who pick a hearing may only change one that has not happened yet. With the flag off staff are shown the
    // next hearing, which can be the latest past hearing, so only a cancelled or another case's hearing is refused.
    private HearingEntity requireAvailableHearing(PcsCaseEntity pcsCaseEntity, String hearingId, boolean futureOnly) {
        LocalDateTime now = LocalDateTime.now(ukClock);
        return pcsCaseEntity.getHearings().stream()
            .filter(hearing -> hearing != null && hearing.getId() != null)
            .filter(hearing -> hearing.getId().toString().equals(hearingId))
            .filter(hearing -> futureOnly ? isLiveFutureHearing(hearing, now)
                : !Boolean.TRUE.equals(hearing.getCancelled()))
            .findFirst()
            .orElseThrow(HearingService::unavailableHearing);
    }

    private static boolean isLiveFutureHearing(HearingEntity hearing, LocalDateTime now) {
        return !Boolean.TRUE.equals(hearing.getCancelled())
            && hearing.getHearingDate() != null
            && !hearing.getHearingDate().isBefore(now);
    }

    private static HearingNotFoundException unavailableHearing() {
        return new HearingNotFoundException("Select an available hearing on this case");
    }

    public void clearHearingForm(PCSCase pcsCase) {
        Hearing currentHearing = pcsCase.getHearing();
        String hearingSummaryMarkdown = currentHearing == null ? null : currentHearing.getHearingSummaryMarkdown();
        pcsCase.setHearing(Hearing.builder().hearingSummaryMarkdown(hearingSummaryMarkdown).build());
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

        if (pcsCase.getSelectedHearingId() == null) {
            setSelectedEditableHearingId(pcsCase, pcsCaseEntity);
        }
        if (Objects.equals(previouslySelectedHearingId, pcsCase.getSelectedHearingId())
            && hasEditableHearingDetails(pcsCase.getManageHearingDraft())) {
            restoreDraftHearingForm(pcsCase);
        } else {
            pcsCase.setManageHearingDraft(null);
            pcsCase.setMhDraftPartyList(null);
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
            List<UUID> selectedPartyIds = selectedHearing.getHearingNoticeParties()
                .stream()
                .map(HearingNoticePartyEntity::getParty)
                .map(PartyEntity::getId)
                .toList();
            pcsCase.setPartyMultiSelectionList(preselectNoticeRecipients(
                partyListForPrepopulation(pcsCase, pcsCaseEntity),
                selectedPartyIds
            ));
        });
    }

    public DynamicMultiSelectStringList buildPartyList(PcsCaseEntity pcsCaseEntity) {
        ClaimEntity mainClaim = pcsCaseEntity.getMainClaim();
        Map<PartyRole, List<ClaimPartyEntity>> partyRoleListMap = mainClaim.getClaimParties().stream()
            .filter(this::isActiveClaimParty)
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

        if (hearingType == HearingType.OTHER) {
            hearingEntity.setOtherHearingType(hearing.getOtherHearingType());
        } else {
            hearingEntity.setOtherHearingType(null);
        }

        List<HearingNoticePartyEntity> hearingNoticeParties = new ArrayList<>(hearingEntity.getHearingNoticeParties());

        if (issueNotice == VerticalYesNo.YES && isWithoutNotice == VerticalYesNo.YES) {
            Set<UUID> partyIds = selectedNoticePartyIds(pcsCase);

            for (HearingNoticePartyEntity hearingNoticeParty : hearingNoticeParties) {
                UUID id = hearingNoticeParty.getParty().getId();
                if (partyIds.contains(id)) {
                    partyIds.remove(id);
                } else {
                    hearingNoticeParty.removeHearingNoticeParty();
                }
            }

            List<PartyEntity> partyEntities = partyRepository.findAllById(partyIds).stream()
                .filter(party -> party != null && !party.isRemoved())
                .toList();
            partyEntities.forEach(hearingEntity::addParty);
        } else {
            hearingNoticeParties.forEach(HearingNoticePartyEntity::removeHearingNoticeParty);
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

    private boolean isActiveClaimParty(ClaimPartyEntity claimPartyEntity) {
        return claimPartyEntity != null
            && !claimPartyEntity.getParty().isRemoved();
    }

    private Set<UUID> selectedNoticePartyIds(PCSCase pcsCase) {
        DynamicMultiSelectStringList selectedParties = pcsCase.getPartyMultiSelectionList();
        if (selectedParties != null && !CollectionUtils.isEmpty(selectedParties.getValue())) {
            return selectedParties.getValue().stream()
                .map(DynamicStringListElement::getCode)
                .filter(Objects::nonNull)
                .map(UUID::fromString)
                .collect(Collectors.toSet());
        }

        return new HashSet<>();
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
