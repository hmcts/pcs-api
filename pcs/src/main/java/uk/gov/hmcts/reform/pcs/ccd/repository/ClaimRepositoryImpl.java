package uk.gov.hmcts.reform.pcs.ccd.repository;

import lombok.AllArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@AllArgsConstructor
@Repository
public class ClaimRepositoryImpl implements ClaimRepositoryCustom {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<Map<String, Object>> searchCaseData(String sql) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, Map.of());
        return rows;
    }
}
