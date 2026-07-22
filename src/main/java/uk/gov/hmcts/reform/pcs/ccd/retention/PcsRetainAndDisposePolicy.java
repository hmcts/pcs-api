package uk.gov.hmcts.reform.pcs.ccd.retention;

import static uk.gov.hmcts.reform.pcs.ccd.CaseType.getCaseType;
import static uk.gov.hmcts.reform.pcs.ccd.domain.State.AWAITING_SUBMISSION_TO_HMCTS;

import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.RetainAndDisposePolicy;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;

@Component
@RequiredArgsConstructor
public class PcsRetainAndDisposePolicy implements RetainAndDisposePolicy {

    static final int DRAFT_INACTIVITY_DAYS = 30;

    private final NamedParameterJdbcTemplate jdbc;
    private final PcsCaseRepository pcsCaseRepository;

    @Override
    public Set<String> caseTypes() {
        return Set.of(getCaseType());
    }

    @Override
    public List<Long> findCandidatesForDisposal() {
        return jdbc.queryForList(
            """
                select reference
                from ccd.case_data
                where case_type_id in (:caseTypeIds)
                  and state = :draftState
                  and last_modified::date + :inactivityDays < current_date
                order by reference asc
                """,
            Map.of(
                "caseTypeIds", caseTypes(),
                "draftState", AWAITING_SUBMISSION_TO_HMCTS.name(),
                "inactivityDays", DRAFT_INACTIVITY_DAYS
            ),
            Long.class
        );
    }

    @Override
    public void dispose(long caseReference) {
        pcsCaseRepository.findByCaseReference(caseReference)
            .ifPresent(pcsCaseRepository::delete);
    }
}
