package uk.gov.hmcts.reform.pcs.bulkclaim.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Reads the CCD state of a case from the decentralised runtime's case table, which shares pcs-api's database.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BulkClaimCaseStateReader {

    private static final String STATE_QUERY = "SELECT state FROM ccd.case_data WHERE reference = ?";
    private static final String STATES_QUERY =
        "SELECT reference, state FROM ccd.case_data WHERE reference IN (:references)";

    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public Optional<String> stateOf(long caseReference) {
        try {
            List<String> states = jdbcTemplate.queryForList(STATE_QUERY, String.class, caseReference);
            return states.stream().findFirst();
        } catch (DataAccessException e) {
            log.warn("Unable to read the CCD state of case {}", caseReference, e);
            return Optional.empty();
        }
    }

    /**
     * Reads the states of several cases in one query. Cases that cannot be found are absent from the result.
     */
    public Map<Long, String> statesOf(Collection<Long> caseReferences) {
        if (caseReferences.isEmpty()) {
            return Map.of();
        }
        Map<Long, String> states = new HashMap<>();
        try {
            namedParameterJdbcTemplate.query(
                STATES_QUERY,
                Map.of("references", caseReferences),
                (RowCallbackHandler) rs -> states.put(rs.getLong("reference"), rs.getString("state")));
        } catch (DataAccessException e) {
            log.warn("Unable to read the CCD state of {} cases", caseReferences.size(), e);
            return Map.of();
        }
        return states;
    }
}
