package uk.gov.hmcts.reform.pcs.enforcement.testcasesupport;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.enforcement.entity.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.enforcement.service.CurrentEnforcementService;

import java.time.Duration;
import java.time.Period;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EnforcementBackdateServiceTest {

    private static final long CASE_REFERENCE = 1234L;
    private static final UUID CURRENT_ID = UUID.randomUUID();
    private static final UUID OLDER_ID = UUID.randomUUID();

    @Mock
    private PcsCaseService pcsCaseService;
    @Mock
    private CurrentEnforcementService currentEnforcementService;
    @Mock
    private EntityManager entityManager;
    @Mock
    private Query query;
    @InjectMocks
    private EnforcementBackdateService underTest;

    @BeforeEach
    void setUp() {
        PcsCaseEntity pcsCase = new PcsCaseEntity();
        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(pcsCase);
        when(currentEnforcementService.findEnforcementsCurrentFirst(pcsCase)).thenReturn(List.of(
            EnforcementOrderEntity.builder().id(CURRENT_ID).enforcementNumber("WP-00000002").build(),
            EnforcementOrderEntity.builder().id(OLDER_ID).enforcementNumber("WP-00000001").build()));
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
    }

    @Test
    void shouldBackdateTheCurrentEnforcementWhenNoNumberIsGiven() {
        assertThat(underTest.backdate(CASE_REFERENCE, null, Period.ofMonths(12), null)).contains(CURRENT_ID);

        verify(query, times(EnforcementBackdateService.TIMESTAMP_COLUMNS.size())).setParameter("id", CURRENT_ID);
    }

    @Test
    void shouldBackdateTheNumberedEnforcement() {
        assertThat(underTest.backdate(CASE_REFERENCE, "WP-00000001", null, Duration.ofHours(73)))
            .contains(OLDER_ID);

        verify(query, times(EnforcementBackdateService.TIMESTAMP_COLUMNS.size())).setParameter("id", OLDER_ID);
    }

    @Test
    void shouldChangeNothingWhenTheNumberedEnforcementIsNotOnTheCase() {
        assertThat(underTest.backdate(CASE_REFERENCE, "WP-99999999", Period.ofMonths(1), null)).isEmpty();

        verify(entityManager, never()).createNativeQuery(anyString());
    }

    @Test
    void shouldPassThePeriodAndDurationAsIntervalParts() {
        underTest.backdate(CASE_REFERENCE, null, Period.of(1, 2, 3), Duration.ofHours(73).plusMillis(500));

        verify(query, times(EnforcementBackdateService.TIMESTAMP_COLUMNS.size())).setParameter("months", 14);
        verify(query, times(EnforcementBackdateService.TIMESTAMP_COLUMNS.size())).setParameter("days", 3);
        verify(query, times(EnforcementBackdateService.TIMESTAMP_COLUMNS.size()))
            .setParameter("secs", 73 * 3600 + 0.5);
    }

    /**
     * The loaded entities must not be flushed over the shifted values by the native updates' auto-flush.
     */
    @Test
    void shouldFlushAndClearBeforeTheUpdates() {
        underTest.backdate(CASE_REFERENCE, null, Period.ofDays(1), null);

        InOrder order = inOrder(entityManager, query);
        order.verify(entityManager).flush();
        order.verify(entityManager).clear();
        order.verify(query, times(EnforcementBackdateService.TIMESTAMP_COLUMNS.size())).executeUpdate();
    }

    @Test
    void shouldShiftEveryTimestampColumnOfTheTable() {
        assertThat(new EnforcementBackdateService.TimestampColumns("enf_warrant_return", "enf_case_id",
                                                                   List.of("eviction_at", "recorded_at"))
                       .updateSql())
            .isEqualTo("UPDATE enf_warrant_return SET "
                           + "eviction_at = eviction_at - make_interval(months => :months, days => :days, "
                           + "secs => :secs), "
                           + "recorded_at = recorded_at - make_interval(months => :months, days => :days, "
                           + "secs => :secs) "
                           + "WHERE enf_case_id = :id");
    }
}
