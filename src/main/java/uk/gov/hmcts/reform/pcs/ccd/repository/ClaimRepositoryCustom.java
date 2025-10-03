package uk.gov.hmcts.reform.pcs.ccd.repository;

import java.util.List;
import java.util.Map;

public interface ClaimRepositoryCustom {

    List<Map<String, Object>> searchCaseData(String sql);
}