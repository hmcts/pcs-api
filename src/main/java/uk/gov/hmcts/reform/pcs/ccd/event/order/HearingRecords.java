package uk.gov.hmcts.reform.pcs.ccd.event.order;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.external.ExternalRejection;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.MakeOrderRequest.HearingChange;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.OrderStart;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.OrderStart.CaseFacts;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.OrderStart.JudicialNote;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.OrderStart.Recorded;
import uk.gov.hmcts.reform.pcs.ccd.entity.JudicialNoteEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.UserNameEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.hearing.HearingEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.hearing.HearingFactsEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.HearingFactsRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.HearingRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.JudicialNoteRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.UserNameService;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * What judges record at a case's hearings: the case's facts as they stood at each hearing, which the
 * next hearing starts from, and each hearing's judicial note. A hearing's record is written when its
 * order is sent for review, and a judge who started from an older record than the one now stored is
 * refused, rather than overwriting it.
 */
@Component
@AllArgsConstructor
class HearingRecords {

    static final String CHANGED_BY_ANOTHER_JUDGE =
        "The case facts for this hearing have been changed by another judge. Reload the order and try again";
    private static final int NOTE_LIMIT = 30000;

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
    OrderStart.Hearing start(PcsCaseEntity pcsCase, HearingEntity hearing, CaseFacts claimFacts) {
        Optional<HearingFactsEntity> own = hearingFactsRepository.findById(hearing.getId());
        Optional<HearingFactsEntity> startFrom = own.or(() -> hearingFactsRepository
            .findFirstByHearingPcsCaseIdAndHearingIdNotOrderByHearingHearingDateDescHearingIdDesc(
                pcsCase.getId(), hearing.getId()));
        JudicialNoteEntity ownNote = own.map(HearingFactsEntity::getJudicialNote).orElse(null);
        return new OrderStart.Hearing(
            hearing.getId(),
            hearing.getHearingDate(),
            own.map(HearingFactsEntity::getVersion).orElse(null),
            startFrom.map(HearingRecords::caseFacts).orElse(claimFacts),
            startFrom.map(facts -> new Recorded(
                facts.getUser().getName(), facts.getHearing().getHearingDate(), facts.getUpdatedOn())).orElse(null),
            ownNote == null ? null : ownNote.getNote(),
            earlierNotes(pcsCase.getId(), pcsCase.getJudicialNotes(), ownNote)
        );
    }

    /** Records the judge's facts and note for the hearing, unless another judge recorded newer ones. */
    void record(PcsCaseEntity pcsCase, HearingEntity hearing, HearingChange change) {
        HearingFactsEntity facts = hearingFactsRepository.findById(hearing.getId())
            .orElseGet(() -> HearingFactsEntity.builder().hearing(hearing).build());
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
        hearingFactsRepository.save(facts);
    }

    /** The hearing's note as the judge left it: a blank note removes the one they recorded before. */
    private JudicialNoteEntity note(PcsCaseEntity pcsCase, JudicialNoteEntity recorded, UserNameEntity judge,
                                    Instant now, String text) {
        String note = text == null || text.isBlank() ? null : text.strip();
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

    /** The case's judicial notes other than this hearing's, newest first, with the hearing each was made at. */
    private List<JudicialNote> earlierNotes(UUID caseId, List<JudicialNoteEntity> notes, JudicialNoteEntity own) {
        Map<Integer, LocalDateTime> hearingDates = hearingFactsRepository.findByHearingPcsCaseId(caseId).stream()
            .filter(facts -> facts.getJudicialNote() != null)
            .collect(Collectors.toMap(facts -> facts.getJudicialNote().getId(),
                facts -> facts.getHearing().getHearingDate()));
        return notes.stream()
            .filter(note -> own == null || !note.getId().equals(own.getId()))
            .sorted(Comparator.comparing(JudicialNoteEntity::getCreatedOn).reversed())
            .map(note -> new JudicialNote(note.getUser() == null ? null : note.getUser().getName(),
                note.getCreatedOn(), hearingDates.get(note.getId()), note.getNote()))
            .toList();
    }

    private static CaseFacts caseFacts(HearingFactsEntity facts) {
        return new CaseFacts(facts.getTenancyStartDate(), facts.getTenancyType(), facts.getNoticeDate(),
            facts.getCurrentRent(), facts.getRentFrequency(), facts.getGroundsPleaded(), facts.getArrearsAtNotice(),
            facts.getArrearsAtIssue(), facts.getLastPaymentDate(), facts.getLastPayment(), facts.getArrearsToday());
    }

    private static void copy(CaseFacts from, HearingFactsEntity to) {
        CaseFacts facts = from == null ? CaseFacts.fromClaim(null, null, null, null, null, null) : from;
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
