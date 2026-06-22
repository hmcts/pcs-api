package uk.gov.hmcts.reform.pcs.ccd.service;

import lombok.AllArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;

@Service
@AllArgsConstructor
public class CcdCaseDataService {

    private final JdbcTemplate jdbcTemplate;

    public boolean isCaseDeletedOrMissing(long caseReference) {
        try {
            String state = jdbcTemplate.queryForObject(
                "SELECT state FROM ccd.case_data WHERE reference = ?",
                String.class,
                caseReference
            );
            return State.DELETED.name().equals(state);
        } catch (EmptyResultDataAccessException e) {
            return true;
        }
    }
}
