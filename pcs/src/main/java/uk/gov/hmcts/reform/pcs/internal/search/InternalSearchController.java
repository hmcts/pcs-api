
package uk.gov.hmcts.reform.pcs.internal.search;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.pcs.hearings.model.HearingRequest;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static uk.gov.hmcts.reform.pcs.hearings.constants.HearingConstants.SERVICE_AUTHORIZATION;

@RestController
@RequestMapping("")
public class InternalSearchController {

    @PostMapping(value = "/customSearchCases", consumes = MediaType.APPLICATION_JSON_VALUE)
    void createHearing() {
        System.out.println("hittttt");
    }
}
