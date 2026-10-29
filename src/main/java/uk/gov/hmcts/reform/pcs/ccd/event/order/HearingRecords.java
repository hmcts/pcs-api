package uk.gov.hmcts.reform.pcs.ccd.event.order;

import lombok.AllArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.external.ExternalRejection;
import uk.gov.hmcts.reform.pcs.ccd.domain.PossessionGroundEnum;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.ClaimGroundSummary;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.MakeOrderRequest.HearingChange;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.OrderStart;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.OrderStart.CaseFacts;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.OrderStart.JudicialNote;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.OrderStart.Recorded;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.JudicialNoteEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.TenancyLicenceEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.NoticeOfPossessionEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.UserNameEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.hearing.HearingEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.hearing.HearingFactsEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.HearingFactsRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.HearingRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.JudicialNoteRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.UserNameService;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry.WALES;

/**
 * What judges record at a case's hearings: the case's facts as they stood at each hearing, which the
 * next hearing starts from, and each hearing's judicial note. A hearing's record is written when a judge
 * saves its order with them changed, and a judge who started from an older record than the one now
 * stored is refused, rather than overwriting it.
 */
@Component
@AllArgsConstructor
class HearingRecords {

    static final String CHANGED_BY_ANOTHER_JUDGE =
        "The case facts for this hearing have been changed by another judge. Reload the order and try again";
    private static final int NOTE_LIMIT = 30000;
    private static final ZoneId UK = ZoneId.of("Europe/London");

    private final HearingRepository hearingRepository;
    private final HearingFactsRepository hearingFactsRepository;
    private final JudicialNoteRepository judicialNoteRepository;
    private final UserNameService userNameService;
    private final Clock utcClock;

    /** One of the case's hearings that has not been cancelled. */
    HearingEntity hearing(PcsCaseEntity pcsCase, int hearingId) {
        return hearingRepository.findById(hearingId)
            .filter(hearing -> hearing.getPcsCase() != null && hearing.getPcsCase().getId().equals(pcsCase.getId()))
            .filter(hearing -> !Boolean.TRUE.equals(hearing.getCancelled()))
            .orElseThrow(() -> ExternalRejection.because("The hearing is not one of this case's hearings"));
    }

    /** The hearing as a judge starts an order for it, falling back to the claim's facts. */
    OrderStart.Hearing start(PcsCaseEntity pcsCase, HearingEntity hearing) {
        List<HearingFactsEntity> recorded = hearingFactsRepository.findByHearingPcsCaseId(pcsCase.getId());
        Optional<HearingFactsEntity> own = recorded.stream()
            .filter(facts -> facts.getHearingId().equals(hearing.getId()))
            .findFirst();
        Optional<HearingFactsEntity> startFrom = own.or(() -> recorded.stream()
            .filter(facts -> facts.getHearing().getHearingDate().isBefore(hearing.getHearingDate()))
            .max(Comparator.comparing(facts -> facts.getHearing().getHearingDate())));
        return new OrderStart.Hearing(
            hearing.getId(),
            hearing.getHearingDate(),
            pcsCase.getLegislativeCountry() == WALES,
            own.map(HearingFactsEntity::getVersion).orElse(null),
            startFrom.map(HearingRecords::caseFacts).orElseGet(() -> claimFacts(pcsCase)),
            startFrom.map(facts -> new Recorded(facts.getUser().getName(), facts.getUpdatedOn()))
                .orElse(null),
            own.map(HearingFactsEntity::getJudicialNote).map(JudicialNoteEntity::getNote).orElse(null),
            earlierNotes(pcsCase.getJudicialNotes(), recorded, hearing.getHearingDate())
        );
    }

    /** Records the judge's facts and note for the hearing, unless another judge recorded newer ones. */
    void record(PcsCaseEntity pcsCase, HearingEntity hearing, HearingChange change) {
        HearingFactsEntity facts = hearingFactsRepository.findById(hearing.getId()).orElseGet(() -> {
            HearingFactsEntity first = new HearingFactsEntity();
            first.setHearing(hearing);
            return first;
        });
        Long recordedVersion = facts.getHearingId() == null ? null : facts.getVersion();
        if (!Objects.equals(recordedVersion, change.version())) {
            throw ExternalRejection.because(CHANGED_BY_ANOTHER_JUDGE);
        }
        UserNameEntity judge = userNameService.getOrCreateUserNameEntity();
        Instant now = Instant.now(utcClock);
        facts.setUser(judge);
        facts.setUpdatedOn(now);
        copy(change.caseFacts(), facts);
        facts.setJudicialNote(note(pcsCase, facts.getJudicialNote(), judge, now, change.note()));
        try {
            hearingFactsRepository.saveAndFlush(facts);
        } catch (DataIntegrityViolationException | OptimisticLockingFailureException e) {
            // Another judge recorded the hearing's facts at the same moment.
            throw ExternalRejection.because(CHANGED_BY_ANOTHER_JUDGE);
        }
    }

    /** The hearing's note as the judge left it: a blank note removes the one they recorded before. */
    private JudicialNoteEntity note(PcsCaseEntity pcsCase, JudicialNoteEntity recorded, UserNameEntity judge,
                                    Instant now, String text) {
        // A line break counts once, as the judge's browser counts it.
        String note = text == null || text.isBlank() ? null : text.strip().replace("\r\n", "\n");
        if (note != null && note.length() > NOTE_LIMIT) {
            throw ExternalRejection.because("Hearing notes must be 30,000 characters or fewer");
        }
        if (note == null) {
            if (recorded != null) {
                pcsCase.getJudicialNotes().remove(recorded);
                judicialNoteRepository.delete(recorded);
            }
            return null;
        }
        if (recorded != null) {
            recorded.setNote(note);
            recorded.setUser(judge);
            return recorded;
        }
        JudicialNoteEntity created = JudicialNoteEntity.builder().note(note).createdOn(now).build();
        pcsCase.addJudicialNote(created);
        judge.addJudicialNote(created);
        return judicialNoteRepository.save(created);
    }

    /**
     * The case's judicial notes made before this hearing, newest first: at an earlier hearing, or between
     * hearings, with the hearing each was made at.
     */
    private static List<JudicialNote> earlierNotes(List<JudicialNoteEntity> notes, List<HearingFactsEntity> recorded,
                                                   LocalDateTime before) {
        Map<Integer, LocalDateTime> hearingDates = recorded.stream()
            .filter(facts -> facts.getJudicialNote() != null)
            .collect(Collectors.toMap(facts -> facts.getJudicialNote().getId(),
                facts -> facts.getHearing().getHearingDate()));
        return notes.stream()
            .map(note -> new JudicialNote(note.getUser() == null ? null : note.getUser().getName(),
                note.getCreatedOn(), hearingDates.get(note.getId()), note.getNote()))
            .filter(note -> madeAt(note).isBefore(before))
            .sorted(Comparator.comparing(HearingRecords::madeAt).reversed())
            .toList();
    }

    private static LocalDateTime madeAt(JudicialNote note) {
        return note.hearingDate() != null ? note.hearingDate() : LocalDateTime.ofInstant(note.createdOn(), UK);
    }

    /** The facts the case's main claim gives, which a case's first hearing starts from. */
    private static CaseFacts claimFacts(PcsCaseEntity pcsCase) {
        TenancyLicenceEntity tenancy = pcsCase.getTenancyLicence();
        ClaimEntity claim = pcsCase.getClaims().stream().findFirst().orElse(null);
        NoticeOfPossessionEntity notice = claim == null ? null : claim.getNoticeOfPossession();
        return new CaseFacts(
            tenancy == null ? null : tenancy.getStartDate(),
            tenancy == null || tenancy.getType() == null ? null : tenancy.getType().name(),
            noticeDate(notice),
            tenancy == null ? null : tenancy.getRentAmount(),
            tenancy == null || tenancy.getRentFrequency() == null ? null : tenancy.getRentFrequency().name(),
            claim == null ? null : groundsPleaded(claim),
            null, null, null, null, null
        );
    }

    /** The day the notice was served, whether the claimant recorded a date or a moment. */
    private static LocalDate noticeDate(NoticeOfPossessionEntity notice) {
        if (notice == null) {
            return null;
        }
        if (notice.getNoticeDate() != null) {
            return notice.getNoticeDate();
        }
        return notice.getNoticeDateTime() == null ? null : notice.getNoticeDateTime().toLocalDate();
    }

    private static String groundsPleaded(ClaimEntity claim) {
        String grounds = claim.getClaimGrounds().stream()
            .map(ground -> ClaimGroundSummary.resolveGround(ground.getCategory(), ground.getCode()))
            .filter(Objects::nonNull)
            .map(PossessionGroundEnum::getLabel)
            .distinct()
            .sorted()
            .collect(Collectors.joining(", "));
        return grounds.isEmpty() ? null : grounds;
    }

    private static CaseFacts caseFacts(HearingFactsEntity facts) {
        return new CaseFacts(facts.getTenancyStartDate(), facts.getTenancyType(), facts.getNoticeDate(),
            facts.getCurrentRent(), facts.getRentFrequency(), facts.getGroundsPleaded(), facts.getArrearsAtNotice(),
            facts.getArrearsAtIssue(), facts.getLastPaymentDate(), facts.getLastPayment(), facts.getArrearsToday());
    }

    private static void copy(CaseFacts facts, HearingFactsEntity to) {
        to.setTenancyStartDate(facts.tenancyStartDate());
        to.setTenancyType(facts.tenancyType());
        to.setNoticeDate(facts.noticeDate());
        to.setCurrentRent(facts.currentRent());
        to.setRentFrequency(facts.rentFrequency());
        to.setGroundsPleaded(facts.groundsPleaded());
        to.setArrearsAtNotice(facts.arrearsAtNotice());
        to.setArrearsAtIssue(facts.arrearsAtIssue());
        to.setLastPaymentDate(facts.lastPaymentDate());
        to.setLastPayment(facts.lastPayment());
        to.setArrearsToday(facts.arrearsToday());
    }
}
