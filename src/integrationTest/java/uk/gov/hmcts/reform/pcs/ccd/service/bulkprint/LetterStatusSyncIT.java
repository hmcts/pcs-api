package uk.gov.hmcts.reform.pcs.ccd.service.bulkprint;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.support.TransactionTemplate;
import uk.gov.hmcts.reform.pcs.ccd.domain.claimactivitylog.PackDetails;
import uk.gov.hmcts.reform.pcs.ccd.entity.LetterEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.LetterRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.AccessCodeActivityLogService;
import uk.gov.hmcts.reform.pcs.config.AbstractPostgresContainerIT;
import uk.gov.hmcts.reform.pcs.config.PcsCcdEventTest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

@PcsCcdEventTest
class LetterStatusSyncIT extends AbstractPostgresContainerIT {

    @RegisterExtension
    static WireMockExtension sendLetter = WireMockExtension.newInstance()
        .options(wireMockConfig().dynamicPort())
        .build();

    @DynamicPropertySource
    static void sendLetterUrl(DynamicPropertyRegistry registry) {
        registry.add("send-letter.url", sendLetter::baseUrl);
    }

    @Autowired
    private AccessCodeActivityLogService accessCodeActivityLogService;
    @Autowired
    private LetterStatusSyncService letterStatusSyncService;
    @Autowired
    private LetterRepository letterRepository;
    @Autowired
    private TransactionTemplate transactionTemplate;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private EntityManager entityManager;

    @Test
    void recordsTheLetterSendLetterAcceptedAgainstItsPackSentRow() {
        PartyEntity recipient = saveCaseWithParty();
        UUID letterId = UUID.randomUUID();

        accessCodeActivityLogService.recordPackSent(recipient.getPcsCase(), recipient,
            PackDetails.sent(LetterType.DEFENCE_PACK, List.of(), letterId));

        assertThat(letterRepository.findById(letterId).orElseThrow().getStatus()).isEqualTo(LetterStatus.CREATED);
        assertThat(packSentRowsFor(letterId)).isEqualTo(1);
    }

    @Test
    void pointsIdenticalSendsThatSendLetterAnsweredWithOneLetterAtThatLetter() {
        PartyEntity first = saveCaseWithParty();
        PartyEntity second = saveParty(first.getPcsCase());
        UUID letterId = UUID.randomUUID();

        accessCodeActivityLogService.recordPackSent(first.getPcsCase(), first,
            PackDetails.sent(LetterType.DEFENDANT_CLAIM_PACK, List.of(), letterId));
        accessCodeActivityLogService.recordPackSent(second.getPcsCase(), second,
            PackDetails.sent(LetterType.DEFENDANT_CLAIM_PACK, List.of(), letterId));

        assertThat(letterRepository.findById(letterId)).isPresent();
        assertThat(packSentRowsFor(letterId)).isEqualTo(2);
    }

    @Test
    void bringsPendingLettersUpToDateWithSendLetter() {
        UUID printed = sendLetter(LetterStatus.UPLOADED);
        UUID aborted = sendLetter(LetterStatus.CREATED);
        UUID unavailable = sendLetter(LetterStatus.UPLOADED);
        stubStatus(printed, """
            {"id": "%s", "status": "Posted", "sent_to_print_at": "2026-11-20T18:00:00.000Z",
             "printed_at": "2026-11-21T09:30:00.000Z", "copies": 1}""".formatted(printed));
        stubStatus(aborted, """
            {"id": "%s", "status": "Aborted", "sent_to_print_at": null, "printed_at": null}""".formatted(aborted));
        sendLetter.stubFor(get(urlPathEqualTo("/letters/" + unavailable)).willReturn(aResponse().withStatus(503)));

        letterStatusSyncService.syncPendingLetters();

        LetterEntity printedLetter = letterRepository.findById(printed).orElseThrow();
        assertThat(printedLetter.getStatus()).isEqualTo(LetterStatus.POSTED);
        assertThat(printedLetter.getSentToPrintAt()).isEqualTo(LocalDateTime.of(2026, 11, 20, 18, 0));
        assertThat(printedLetter.getPrintedAt()).isEqualTo(LocalDateTime.of(2026, 11, 21, 9, 30));
        assertThat(printedLetter.getLastCheckedAt()).isNotNull();

        assertThat(letterRepository.findById(aborted).orElseThrow().getStatus()).isEqualTo(LetterStatus.ABORTED);

        LetterEntity unavailableLetter = letterRepository.findById(unavailable).orElseThrow();
        assertThat(unavailableLetter.getStatus()).isEqualTo(LetterStatus.UPLOADED);
        assertThat(unavailableLetter.getLastCheckedAt()).isNotNull();
    }

    @Test
    void leavesRecentlyCheckedFinishedLettersAndOldLettersAlone() {
        UUID posted = sendLetter(LetterStatus.POSTED);
        jdbcTemplate.update("UPDATE letter SET last_checked_at = now() - interval '1 day' WHERE letter_id = ?",
            posted);
        UUID old = sendLetter(LetterStatus.UPLOADED);
        jdbcTemplate.update("UPDATE claim_activity_log SET created_at = now() - interval '200 days' "
            + "WHERE letter_id = ?", old);

        letterStatusSyncService.syncPendingLetters();

        sendLetter.verify(0, getRequestedFor(urlPathEqualTo("/letters/" + posted)));
        sendLetter.verify(0, getRequestedFor(urlPathEqualTo("/letters/" + old)));
        assertThat(letterRepository.findById(old).orElseThrow().getLastCheckedAt()).isNull();
    }

    @Test
    void picksUpACorrectionToAFinishedLetterWhenItIsNextRechecked() {
        UUID aborted = sendLetter(LetterStatus.ABORTED);
        jdbcTemplate.update("UPDATE letter SET last_checked_at = now() - interval '8 days' WHERE letter_id = ?",
            aborted);
        stubStatus(aborted, """
            {"id": "%s", "status": "Posted", "sent_to_print_at": "2026-11-20T18:00:00.000Z",
             "printed_at": "2026-11-22T10:00:00.000Z"}""".formatted(aborted));

        letterStatusSyncService.syncPendingLetters();

        LetterEntity corrected = letterRepository.findById(aborted).orElseThrow();
        assertThat(corrected.getStatus()).isEqualTo(LetterStatus.POSTED);
        assertThat(corrected.getPrintedAt()).isEqualTo(LocalDateTime.of(2026, 11, 22, 10, 0));
    }

    @Test
    void tracksAPackSentRowWrittenWithoutItsLetter() {
        PartyEntity recipient = saveCaseWithParty();
        UUID letterId = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO claim_activity_log (case_id, party_id, activity_type, status, details) "
                + "VALUES (?, ?, 'PACK_SENT', 'SUCCESS', CAST(? AS jsonb))",
            recipient.getPcsCase().getId(), recipient.getId(),
            "{\"letterId\": \"%s\", \"packType\": \"DEFENCE_PACK\", \"documents\": []}".formatted(letterId));
        stubStatus(letterId, """
            {"id": "%s", "status": "Uploaded", "sent_to_print_at": "2026-11-20T18:00:00.000Z"}""".formatted(letterId));

        letterStatusSyncService.syncPendingLetters();

        assertThat(packSentRowsFor(letterId)).isEqualTo(1);
        assertThat(letterRepository.findById(letterId).orElseThrow().getStatus()).isEqualTo(LetterStatus.UPLOADED);
    }

    private UUID sendLetter(LetterStatus status) {
        PartyEntity recipient = saveCaseWithParty();
        UUID letterId = UUID.randomUUID();
        accessCodeActivityLogService.recordPackSent(recipient.getPcsCase(), recipient,
            PackDetails.sent(LetterType.DEFENCE_PACK, List.of(), letterId));
        jdbcTemplate.update("UPDATE letter SET status = ? WHERE letter_id = ?", status.name(), letterId);
        return letterId;
    }

    private void stubStatus(UUID letterId, String body) {
        sendLetter.stubFor(get(urlPathEqualTo("/letters/" + letterId)).willReturn(okJson(body)));
    }

    private Integer packSentRowsFor(UUID letterId) {
        return jdbcTemplate.queryForObject(
            "SELECT count(*) FROM claim_activity_log WHERE activity_type = 'PACK_SENT' AND letter_id = ?",
            Integer.class, letterId);
    }

    private PartyEntity saveCaseWithParty() {
        return transactionTemplate.execute(status -> {
            PcsCaseEntity pcsCase = PcsCaseEntity.builder()
                .caseReference(ThreadLocalRandom.current().nextLong(1_000_000_000_000_000L, 9_999_999_999_999_999L))
                .build();
            entityManager.persist(pcsCase);
            PartyEntity party = PartyEntity.builder().pcsCase(pcsCase).orgName("Recipient").build();
            entityManager.persist(party);
            return party;
        });
    }

    private PartyEntity saveParty(PcsCaseEntity pcsCase) {
        return transactionTemplate.execute(status -> {
            PartyEntity party = PartyEntity.builder()
                .pcsCase(entityManager.merge(pcsCase))
                .orgName("Recipient")
                .build();
            entityManager.persist(party);
            return party;
        });
    }
}
