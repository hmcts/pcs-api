package uk.gov.hmcts.reform.pcs.sdp;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SdpClaimSnapshotReadRepositoryTest {

    private static final UUID CLAIM_ID = UUID.randomUUID();
    private static final UUID PARTY_ID = UUID.randomUUID();
    private static final LocalDateTime SUBMITTED = LocalDateTime.of(2026, 2, 1, 14, 30);

    @Mock
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Mock
    private ResultSet resultSet;

    private SdpClaimSnapshotReadRepository repository;

    @BeforeEach
    void setUp() {
        repository = new SdpClaimSnapshotReadRepository(jdbcTemplate);
    }

    @Test
    void shouldReturnEmptyWhenClaimIdsAreEmpty() {
        assertThat(repository.findHeadersByClaimId(List.of())).isEmpty();
        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    void shouldMapHeaderRow() throws Exception {
        stubQuery();
        when(resultSet.getObject("claim_id", UUID.class)).thenReturn(CLAIM_ID);
        when(resultSet.getString("claimant_type")).thenReturn("TYPE");
        when(resultSet.getString("against_trespassers")).thenReturn("NO");
        when(resultSet.getString("language_used")).thenReturn("ENGLISH");
        when(resultSet.getTimestamp("claim_submitted_date")).thenReturn(Timestamp.valueOf(SUBMITTED));
        when(resultSet.getTimestamp("claim_issued_date")).thenReturn(null);
        when(resultSet.getObject("case_reference", Long.class)).thenReturn(1L);
        when(resultSet.getObject("case_management_location", Integer.class)).thenReturn(45);
        when(resultSet.getString("legislative_country")).thenReturn("ENGLAND");
        when(resultSet.getString("tenancy_type")).thenReturn("SECURE");
        when(resultSet.getString("serving_method")).thenReturn("POST");
        when(resultSet.getObject("notice_date", LocalDate.class)).thenReturn(LocalDate.of(2026, 1, 10));

        assertThat(repository.findHeadersByClaimId(List.of(CLAIM_ID))).containsExactly(
            new SdpClaimSnapshotHeader(
                CLAIM_ID, "TYPE", "NO", "ENGLISH", SUBMITTED, null, 1L, 45, "ENGLAND",
                "SECURE", "POST", LocalDate.of(2026, 1, 10)
            )
        );
    }

    @Test
    void shouldMapPartyRow() throws Exception {
        stubQuery();
        when(resultSet.getObject("claim_id", UUID.class)).thenReturn(CLAIM_ID);
        when(resultSet.getObject("party_id", UUID.class)).thenReturn(PARTY_ID);
        when(resultSet.getString("role")).thenReturn("DEFENDANT");
        when(resultSet.getBoolean("represented")).thenReturn(true);

        assertThat(repository.findPartiesByClaimId(List.of(CLAIM_ID))).containsExactly(
            new SdpClaimSnapshotParty(CLAIM_ID, PARTY_ID, "DEFENDANT", true)
        );
    }

    @Test
    void shouldMapPaymentRow() throws Exception {
        stubQuery();
        when(resultSet.getObject("claim_id", UUID.class)).thenReturn(CLAIM_ID);
        when(resultSet.getString("external_reference")).thenReturn("RC-1");
        when(resultSet.getString("status")).thenReturn("PAID");
        when(resultSet.getTimestamp("request_date")).thenReturn(Timestamp.valueOf(SUBMITTED));

        assertThat(repository.findPaymentsByClaimId(List.of(CLAIM_ID))).containsExactly(
            new SdpClaimSnapshotPayment(CLAIM_ID, "RC-1", "PAID", SUBMITTED)
        );
    }

    @Test
    void shouldMapGroundRow() throws Exception {
        stubQuery();
        when(resultSet.getObject("claim_id", UUID.class)).thenReturn(CLAIM_ID);
        when(resultSet.getString("code")).thenReturn("G8");
        when(resultSet.getString("description")).thenReturn("Arrears");
        when(resultSet.getString("category")).thenReturn("MANDATORY");

        assertThat(repository.findGroundsByClaimId(List.of(CLAIM_ID))).containsExactly(
            new SdpClaimSnapshotGround(CLAIM_ID, "G8", "Arrears", "MANDATORY")
        );
    }

    @Test
    void shouldMapResponseRow() throws Exception {
        stubQuery();
        when(resultSet.getObject("claim_id", UUID.class)).thenReturn(CLAIM_ID);
        when(resultSet.getObject("response_id", Integer.class)).thenReturn(12);
        when(resultSet.getObject("party_id", UUID.class)).thenReturn(PARTY_ID);
        when(resultSet.getString("make_counter_claim")).thenReturn("NO");
        when(resultSet.getTimestamp("response_submitted_date")).thenReturn(Timestamp.valueOf(SUBMITTED));

        assertThat(repository.findResponsesByClaimId(List.of(CLAIM_ID))).containsExactly(
            new SdpClaimSnapshotResponse(CLAIM_ID, 12, PARTY_ID, "NO", SUBMITTED)
        );
    }

    @SuppressWarnings("unchecked")
    private void stubQuery() {
        when(jdbcTemplate.query(anyString(), any(SqlParameterSource.class), any(RowMapper.class)))
            .thenAnswer(invocation -> {
                RowMapper<Object> mapper = invocation.getArgument(2);
                return List.of(mapper.mapRow(resultSet, 1));
            });
    }
}
