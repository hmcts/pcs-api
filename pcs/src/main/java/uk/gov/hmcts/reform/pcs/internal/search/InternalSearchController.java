
package uk.gov.hmcts.reform.pcs.internal.search;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.pcs.hearings.model.CustomSearchRequest;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@RestController
@RequestMapping("")
public class InternalSearchController {

    @PostMapping(value = "/customSearchCases", consumes = MediaType.APPLICATION_JSON_VALUE)
    void createHearing(
        @RequestHeader(AUTHORIZATION) String authorisation,
        @RequestHeader("ServiceAuthorization") String serviceAuthorization,
        @RequestBody CustomSearchRequest jsonData
    ) {
        System.out.println(jsonData.getSupplementaryData());
    }
}
