package uk.gov.hmcts.reform.pcs.bulkclaim.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
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
    private static final String STATES_QUERY = "SELECT reference, state FROM ccd.case_data WHERE reference IN (%s)";

    private final JdbcTemplate jdbcTemplate;

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
        String placeholders = String.join(",", Collections.nCopies(caseReferences.size(), "?"));
        Map<Long, String> states = new HashMap<>();
        try {
            jdbcTemplate.query(
                STATES_QUERY.formatted(placeholders),
                rs -> {
                    states.put(rs.getLong("reference"), rs.getString("state"));
                },
                caseReferences.toArray());
        } catch (DataAccessException e) {
            log.warn("Unable to read the CCD state of {} cases", caseReferences.size(), e);
            return Map.of();
        }
        return states;
    }
}
