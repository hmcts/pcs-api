
package uk.gov.hmcts.reform.pcs.internal.search;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.core.util.Json;
import lombok.AllArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
//import uk.gov.hmcts.ccd.data.casedetails.CaseDetailsEntity;
import uk.gov.hmcts.ccd.data.casedetails.SecurityClassification;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.search.CaseSearchResult;
import uk.gov.hmcts.ccd.domain.model.search.elasticsearch.CaseSearchResultView;
import uk.gov.hmcts.reform.pcs.ccd.service.ClaimService;
import uk.gov.hmcts.reform.pcs.hearings.model.CustomSearchRequest;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@AllArgsConstructor
@RestController
@RequestMapping("")
public class InternalSearchController {

    private ObjectMapper mapper;
    private ClaimService claimService;

    @PostMapping(value = "/customSearchCases", consumes = MediaType.APPLICATION_JSON_VALUE)
    CaseSearchResult customSearchCases(
        @RequestHeader(AUTHORIZATION) String authorisation,
        @RequestHeader("ServiceAuthorization") String serviceAuthorization,
        @RequestBody String jsonData
    ) {
        try {
            String convertedSql = CustomSearchRequest.parse(jsonData);
            System.out.println(convertedSql);

            //Results as map, then manually convert
            List<Map<String, Object>> claimList = claimService.claims();
            ArrayList<CaseDetails> caseDetails = mapToCaseDetails(claimList);
            CaseSearchResult caseSearchResult = new CaseSearchResult((long) caseDetails.size(), caseDetails);

            System.out.println("Data Returned");

            return caseSearchResult;

            //Results as entity
//            ArrayList<CaseDetailsEntity> claimList = claimService.claims();

            //Todo
            //return CaseSearchResultView which holds a CaseSearchResult which holds
            //a list of CaseDetails

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    //XUI is built to receive a DTO CaseDetails. This is usually sent back via Elastic Search
    //As we are replacing elastic search with raw sql query, we instead receive back an entity (or in this case, a
    // map of objects).
    //This method converts the sql result set, into our CaseDetails DTO to slot back into the existing flow.
    //This however does mean we also have to depend on ccd
    private ArrayList<CaseDetails> mapToCaseDetails(List<Map<String, Object>> claimList) throws JsonProcessingException {
        ArrayList<CaseDetails> convertedCaseDetails = new ArrayList<>();

        for (Map<String, Object> individualCase : claimList) {
            CaseDetails convertedCase = new CaseDetails();

            convertedCase.setResolvedTTL((LocalDate) individualCase.getOrDefault("resolved_ttl", null));
            convertedCase.setSecurityClassification(SecurityClassification.valueOf((String) individualCase.get(
                "security_classification")));

            convertedCase.setReference((Long) individualCase.get("reference"));
            convertedCase.setJurisdiction((String) individualCase.get("jurisdiction"));

            // Convert only "data"
            Object raw = individualCase.get("data");
            if (!(raw instanceof String)) {
                throw new IllegalArgumentException("'data' must be a JSON string for readTree");
            }

            JsonNode dataNode = mapper.readTree((String) raw);
            Map<String, JsonNode> target = new HashMap<>();
            target.put("data", dataNode);
            convertedCase.setData(target);

            convertedCase.setState((String) individualCase.get("state"));
            convertedCase.setLastStateModifiedDate(((Timestamp) individualCase.get("last_modified")).toLocalDateTime());
            convertedCase.setVersion((Integer) individualCase.get("version"));

            JsonNode supplementaryDataNode = mapper.readTree((String) individualCase.get("supplementary_data"));
            Map<String, JsonNode> supplementaryTarget = new HashMap<>();
            target.put("supplementary_data", dataNode);
            convertedCase.setSupplementaryData(supplementaryTarget);

            convertedCase.setId(String.valueOf(individualCase.get("id")));
            convertedCase.setLastModified(((Timestamp) individualCase.get("last_modified")).toLocalDateTime());
            convertedCase.setCreatedDate(((Timestamp) individualCase.get("created_date")).toLocalDateTime());
            convertedCase.setCaseTypeId((String) individualCase.get("case_type_id"));

            convertedCaseDetails.add(convertedCase);
        }
        return convertedCaseDetails;
    }
}
