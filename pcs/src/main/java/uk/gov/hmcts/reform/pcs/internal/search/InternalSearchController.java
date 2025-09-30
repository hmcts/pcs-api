
package uk.gov.hmcts.reform.pcs.internal.search;

import lombok.AllArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.ClaimRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.ClaimService;
import uk.gov.hmcts.reform.pcs.hearings.model.CustomSearchRequest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@AllArgsConstructor
@RestController
@RequestMapping("")
public class InternalSearchController {

    private ClaimService claimService;
    @PostMapping(value = "/customSearchCases", consumes = MediaType.APPLICATION_JSON_VALUE)
    void createHearing(
        @RequestHeader(AUTHORIZATION) String authorisation,
        @RequestHeader("ServiceAuthorization") String serviceAuthorization,
        @RequestBody String jsonData
    ) {
        try {
            String convertedSql = CustomSearchRequest.parse(jsonData);
            System.out.println(convertedSql);

//            ArrayList<ClaimEntity> claimList = claimService.claims();
//            List<Map<String, Object>> claimList = claimService.claims();
            ArrayList<PcsCaseEntity> claimList = claimService.claims();


            System.out.println("Data Returned");

            //exec query on db
            //return CaseSearchResultView which holds a CaseSearchResult which holds
            //a list of CaseDetails

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
