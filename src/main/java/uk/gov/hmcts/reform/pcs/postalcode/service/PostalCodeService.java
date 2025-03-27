package uk.gov.hmcts.reform.pcs.postalcode.service;

import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.postalcode.dto.PostCodeResponse;

@Service
public class PostalCodeService {

    // Test values from Jira    - to be replaced with actual repository call once HDPI-350 is merged
    private static final Map<String, String> TEST_EPIMS_IDS = new HashMap<>() {{
        put("W3 7RX", "20262");
        put("W3 6RS", "36791");
        put("M13 9PL", "144641");
        put("LE2 0QB", "425094");
        put("UB7 0DG", "28837");
        put("SW1H 9EA", "990000");
    }};

    public PostCodeResponse getEPIMSIdByPostcode(String postcode) {
        // Simulate fetching EPIMS ID from external service
        return new PostCodeResponse(TEST_EPIMS_IDS.getOrDefault(postcode, ""));
    }

}
