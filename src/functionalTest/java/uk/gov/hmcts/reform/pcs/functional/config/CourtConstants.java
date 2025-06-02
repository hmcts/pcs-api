package uk.gov.hmcts.reform.pcs.functional.config;

import java.util.List;
import java.util.Map;

public class CourtConstants {
    public static final String POSTCODE_VALID = "M13 9PL";
    public static final String POSTCODE_INVALID = "W3 7RY";
    public static final int COURT_TYPE_ID = 10;
    public static final Map<String, Object> EXPECTED_COURT_MAP = Map.of(
        "id", 40827,
        "name", "Central London County Court",
        "epimId", 20262
    );
    public static final List<Map<String, Object>> EXPECTED_COURT_LIST = List.of(EXPECTED_COURT_MAP);
}
