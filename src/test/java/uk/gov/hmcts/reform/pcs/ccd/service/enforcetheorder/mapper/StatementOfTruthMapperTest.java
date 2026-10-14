package uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.mapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.common.StatementOfTruthDetailsEnforcement;
import uk.gov.hmcts.reform.pcs.ccd.domain.statementoftruth.StatementOfTruthCompletedBy;
import uk.gov.hmcts.reform.pcs.ccd.entity.StatementOfTruthEntity;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatementOfTruthMapperTest {
    private static final LocalDateTime TEST_UTC_DATE_TIME = LocalDate.of(2025, Month.AUGUST, 27)
            .atTime(12, 51, 19);

    @Mock
    private Clock utcClock;

    private StatementOfTruthMapper underTest;

    @BeforeEach
    void setUp() {
        underTest = new StatementOfTruthMapper(utcClock);

        when(utcClock.instant()).thenReturn(TEST_UTC_DATE_TIME.toInstant(ZoneOffset.UTC));
        when(utcClock.getZone()).thenReturn(ZoneOffset.UTC);
    }

    @Test
    void shouldMapStatementOfTruthClaimant() {
        // Given
        StatementOfTruthDetailsEnforcement statementOfTruth = new StatementOfTruthDetailsEnforcement();
        statementOfTruth.setCompletedBy(StatementOfTruthCompletedBy.CLAIMANT);
        statementOfTruth.setFullNameParty("John Doe");
        statementOfTruth.setPositionParty("Owner");
        statementOfTruth.setFullNameLegalRep("Jane Smith");
        statementOfTruth.setFirmNameLegalRep("Smith & Co");
        statementOfTruth.setPositionLegalRep("Solicitor");

        // When
        StatementOfTruthEntity result = underTest.toEntity(statementOfTruth);

        // Then
        assertThat(result.getCompletedBy()).isEqualTo(StatementOfTruthCompletedBy.CLAIMANT);
        assertThat(result.getFullName()).isEqualTo("John Doe");
        assertThat(result.getPositionHeld()).isEqualTo("Owner");
        assertThat(result.getFirmName()).isNull();
    }

    @Test
    void shouldMapStatementOfTruthLegalRepresentative() {
        // Given
        StatementOfTruthDetailsEnforcement statementOfTruth = new StatementOfTruthDetailsEnforcement();
        statementOfTruth.setCompletedBy(StatementOfTruthCompletedBy.LEGAL_REPRESENTATIVE);
        statementOfTruth.setFullNameLegalRep("Jane Smith");
        statementOfTruth.setFirmNameLegalRep("Smith & Co");
        statementOfTruth.setPositionLegalRep("Solicitor");

        // When
        StatementOfTruthEntity result = underTest.toEntity(statementOfTruth);

        // Then
        assertThat(result.getCompletedBy()).isEqualTo(StatementOfTruthCompletedBy.LEGAL_REPRESENTATIVE);

        assertThat(result.getFullName()).isEqualTo("Jane Smith");
        assertThat(result.getFirmName()).isEqualTo("Smith & Co");
        assertThat(result.getPositionHeld()).isEqualTo("Solicitor");
    }
}