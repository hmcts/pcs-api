package uk.gov.hmcts.reform.pcs.ccd.service.counterclaimform;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.CounterClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseReferenceFormatter;
import uk.gov.hmcts.reform.pcs.document.model.counterclaimform.CounterClaimFormPayload;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class CounterClaimFormPayloadBuilderTest {

    private static final long CASE_REFERENCE = 1234567812345678L;

    private CounterClaimFormPayloadBuilder builder;

    @BeforeEach
    void setUp() {
        Clock ukClock = Clock.fixed(
            LocalDateTime.of(2026, Month.JANUARY, 1, 0, 0).toInstant(ZoneOffset.UTC),
            ZoneId.of("Europe/London"));
        builder = new CounterClaimFormPayloadBuilder(new CaseReferenceFormatter(), ukClock);
    }

    @Test
    void buildsPayloadFromCounterClaim() {
        CounterClaimEntity counterClaim = CounterClaimEntity.builder()
            .pcsCase(PcsCaseEntity.builder().caseReference(CASE_REFERENCE).build())
            .claimIssuedDate(LocalDateTime.of(2026, Month.JUNE, 15, 10, 0))
            .claimSubmittedDate(LocalDateTime.of(2026, Month.JUNE, 14, 9, 30))
            .build();

        CounterClaimFormPayload payload = builder.build(counterClaim);

        assertThat(payload.getReferenceNumber()).isEqualTo("1234-5678-1234-5678");
        assertThat(payload.getIssueDateSealed()).isEqualTo(LocalDate.of(2026, Month.JUNE, 15));
        assertThat(payload.getSubmittedOn()).isEqualTo(LocalDate.of(2026, Month.JUNE, 14));
    }

    @Test
    void leavesDatesNullWhenSourceTimestampsAreNull() {
        CounterClaimEntity counterClaim = CounterClaimEntity.builder()
            .pcsCase(PcsCaseEntity.builder().caseReference(CASE_REFERENCE).build())
            .build();

        CounterClaimFormPayload payload = builder.build(counterClaim);

        assertThat(payload.getReferenceNumber()).isEqualTo("1234-5678-1234-5678");
        assertThat(payload.getIssueDateSealed()).isNull();
        assertThat(payload.getSubmittedOn()).isNull();
    }

    @Test
    void utcTimestampLateInDayConvertsToNextUkDateDuringBst() {
        CounterClaimEntity counterClaim = CounterClaimEntity.builder()
            .pcsCase(PcsCaseEntity.builder().caseReference(CASE_REFERENCE).build())
            .claimIssuedDate(LocalDateTime.of(2026, Month.JUNE, 15, 23, 30))
            .build();

        CounterClaimFormPayload payload = builder.build(counterClaim);

        assertThat(payload.getIssueDateSealed()).isEqualTo(LocalDate.of(2026, Month.JUNE, 16));
    }
}
