package uk.gov.hmcts.reform.pcs.ccd.service.bulkprint;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.pcs.ccd.entity.LetterEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.LetterRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.bulkprint.SendLetterStatusApi.LetterStatusResponse;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

/**
 * Asks Send Letter where each pending letter has got to and records the answer on the letter. Letters in a
 * final status are asked again now and then, as Send Letter's operators can correct one. Letters older than
 * the max age are left alone: Send Letter gives up on a letter well before then, and deletes PCS letters
 * after six months.
 */
@Service
@Slf4j
public class LetterStatusSyncService {

    private static final String MDC_LETTER_ID = "letterId";

    private final LetterRepository letterRepository;
    private final SendLetterStatusApi sendLetterStatusApi;
    private final AuthTokenGenerator authTokenGenerator;
    private final int batchSize;
    private final int maxAgeDays;
    private final int recheckFinalAfterDays;

    public LetterStatusSyncService(LetterRepository letterRepository,
                                   SendLetterStatusApi sendLetterStatusApi,
                                   AuthTokenGenerator authTokenGenerator,
                                   @Value("${bulk-print.status-sync.batch-size}") int batchSize,
                                   @Value("${bulk-print.status-sync.max-age-days}") int maxAgeDays,
                                   @Value("${bulk-print.status-sync.recheck-final-after-days}")
                                   int recheckFinalAfterDays) {
        this.letterRepository = letterRepository;
        this.sendLetterStatusApi = sendLetterStatusApi;
        this.authTokenGenerator = authTokenGenerator;
        this.batchSize = batchSize;
        this.maxAgeDays = maxAgeDays;
        this.recheckFinalAfterDays = recheckFinalAfterDays;
    }

    public void syncPendingLetters() {
        LocalDateTime sentAfter = now().minusDays(maxAgeDays);
        linkUnlinkedPackSentRows(sentAfter);
        List<LetterEntity> letters = letterRepository.findToSync(LetterStatus.pending(),
            now().minusDays(recheckFinalAfterDays), sentAfter, PageRequest.of(0, batchSize));
        log.info("Letter status sync starting for {} pending letters", letters.size());
        letters.forEach(this::syncLetter);
    }

    // Catching up is best-effort: whatever it misses, the next run picks up, so it never stops the polling.
    private void linkUnlinkedPackSentRows(LocalDateTime sentAfter) {
        try {
            int created = letterRepository.createLettersForUnlinkedPackSentRows(sentAfter);
            int linked = letterRepository.linkUnlinkedPackSentRows(sentAfter);
            if (linked > 0) {
                log.warn("Linked {} PACK_SENT rows written without their letter ({} new letters)",
                    linked, created);
            }
        } catch (RuntimeException e) {
            log.warn("Could not link PACK_SENT rows written without their letter; will retry next run", e);
        }
    }

    private void syncLetter(LetterEntity letter) {
        MDC.put(MDC_LETTER_ID, String.valueOf(letter.getLetterId()));
        try {
            letter.setLastAttemptedAt(now());
            askSendLetter(letter);
            letterRepository.save(letter);
        } finally {
            MDC.remove(MDC_LETTER_ID);
        }
    }

    // Only an answer Send Letter gives counts as a check; a failed attempt leaves the letter due.
    private void askSendLetter(LetterEntity letter) {
        try {
            LetterStatusResponse response =
                sendLetterStatusApi.getLetterStatus(authTokenGenerator.generate(), letter.getLetterId());
            Optional<LetterStatus> status = LetterStatus.fromSendLetter(response.status());
            if (status.isPresent()) {
                record(letter, status.get(), response);
                letter.setLastCheckedAt(now());
            } else {
                log.warn("Send Letter returned unknown status {} for letter {}", response.status(),
                    letter.getLetterId());
            }
        } catch (FeignException e) {
            log.warn("Could not get the status of letter {} from Send Letter: {} {}", letter.getLetterId(),
                e.status(), e.getMessage());
        }
    }

    private void record(LetterEntity letter, LetterStatus status, LetterStatusResponse response) {
        final LetterStatus previous = letter.getStatus();
        letter.setStatus(status);
        letter.setSentToPrintAt(utc(response.sentToPrintAt()));
        letter.setPrintedAt(utc(response.printedAt()));
        if (status == previous) {
            return;
        }
        if (status.isFailed()) {
            log.error("Letter {} for case(s) {} was not printed: Send Letter status {}", letter.getLetterId(),
                letterRepository.findCaseReferences(letter.getLetterId()), status);
        } else {
            log.info("Letter {} moved from {} to {}", letter.getLetterId(), previous, status);
        }
    }

    private static LocalDateTime utc(OffsetDateTime dateTime) {
        return dateTime == null ? null : dateTime.withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime();
    }

    // UTC to match the DB's timestamp storage.
    private static LocalDateTime now() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }
}
