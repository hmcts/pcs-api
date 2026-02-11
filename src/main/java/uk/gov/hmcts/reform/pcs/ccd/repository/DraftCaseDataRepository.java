package uk.gov.hmcts.reform.pcs.ccd.repository;

import lombok.AllArgsConstructor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.pcs.ccd.entity.DraftCaseDataEntity;
import uk.gov.hmcts.reform.pcs.ccd.event.EventId;

import java.util.Optional;
import java.util.UUID;

@Repository
@AllArgsConstructor
public class DraftCaseDataRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    private static final RowMapper<DraftCaseDataEntity> ROW_MAPPER = (rs, rowNum) -> {
        DraftCaseDataEntity entity = new DraftCaseDataEntity();
        entity.setId(UUID.fromString(rs.getString("id")));

        Long caseReference = rs.getLong("case_reference");
        if (!rs.wasNull()) {
            entity.setCaseReference(caseReference);
        }

        entity.setCaseData(rs.getString("case_data"));
        entity.setEventId(EventId.valueOf(rs.getString("event_id")));
        entity.setIdamUserId(UUID.fromString(rs.getString("idam_user_id")));
        return entity;
    };

    public Optional<DraftCaseDataEntity> findByCaseReferenceAndEventIdAndIdamUserId(
        long caseReference, EventId eventId, UUID idamUserId) {

        String sql = "SELECT id, case_reference, case_data, event_id, idam_user_id "
            + "FROM draft.draft_case_data "
            + "WHERE case_reference = :caseReference "
            + "AND event_id = :eventId "
            + "AND idam_user_id = :idamUserId";

        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("caseReference", caseReference)
            .addValue("eventId", eventId.name())
            .addValue("idamUserId", idamUserId);

        return jdbcTemplate.query(sql, params, ROW_MAPPER)
            .stream()
            .findFirst();
    }

    public boolean existsByCaseReferenceAndEventIdAndIdamUserId(
        long caseReference, EventId eventId, UUID idamUserId) {

        String sql = "SELECT COUNT(*) FROM draft.draft_case_data "
            + "WHERE case_reference = :caseReference "
            + "AND event_id = :eventId "
            + "AND idam_user_id = :idamUserId";

        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("caseReference", caseReference)
            .addValue("eventId", eventId.name())
            .addValue("idamUserId", idamUserId);

        Integer count = jdbcTemplate.queryForObject(sql, params, Integer.class);
        return count != null && count > 0;
    }

    public void deleteByCaseReferenceAndEventIdAndIdamUserId(
        long caseReference, EventId eventId, UUID idamUserId) {

        String sql = "DELETE FROM draft.draft_case_data "
            + "WHERE case_reference = :caseReference "
            + "AND event_id = :eventId "
            + "AND idam_user_id = :idamUserId";

        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("caseReference", caseReference)
            .addValue("eventId", eventId.name())
            .addValue("idamUserId", idamUserId);

        jdbcTemplate.update(sql, params);
    }

    public DraftCaseDataEntity save(DraftCaseDataEntity entity) {
        if (entity.getId() == null) {
            return insert(entity);
        } else {
            return update(entity);
        }
    }

    private DraftCaseDataEntity insert(DraftCaseDataEntity entity) {
        UUID id = UUID.randomUUID();

        String sql = "INSERT INTO draft.draft_case_data "
            + "(id, case_reference, case_data, event_id, idam_user_id) "
            + "VALUES (:id, :caseReference, CAST(:caseData AS jsonb), CAST(:eventId AS text), :idamUserId)";

        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("id", id)
            .addValue("caseReference", entity.getCaseReference())
            .addValue("caseData", entity.getCaseData())
            .addValue("eventId", entity.getEventId().name())
            .addValue("idamUserId", entity.getIdamUserId());

        jdbcTemplate.update(sql, params);
        entity.setId(id);
        return entity;
    }

    private DraftCaseDataEntity update(DraftCaseDataEntity entity) {
        String sql = "UPDATE draft.draft_case_data "
            + "SET case_reference = :caseReference, "
            + "case_data = CAST(:caseData AS jsonb), "
            + "event_id = CAST(:eventId AS text), "
            + "idam_user_id = :idamUserId "
            + "WHERE id = :id";

        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("id", entity.getId())
            .addValue("caseReference", entity.getCaseReference())
            .addValue("caseData", entity.getCaseData())
            .addValue("eventId", entity.getEventId().name())
            .addValue("idamUserId", entity.getIdamUserId());

        jdbcTemplate.update(sql, params);
        return entity;
    }

}
