package uk.gov.hmcts.reform.pcs.bulkclaim.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Reads the CCD state of a case from the decentralised runtime's case table, which shares pcs-api's database.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BulkClaimCaseStateReader {

    private static final String STATE_QUERY = "SELECT state FROM ccd.case_data WHERE reference = ?";

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
}
