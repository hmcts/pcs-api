package uk.gov.hmcts.reform.pcs.search;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.postgresql.util.PGobject;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.ccd.data.casedetails.SecurityClassification;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.search.CaseSearchResult;
import uk.gov.hmcts.ccd.domain.model.search.CaseTypeResults;
import uk.gov.hmcts.reform.pcs.ccd.service.ClaimService;
import uk.gov.hmcts.reform.pcs.ccd.utils.CustomSearchRequestMapper;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@Slf4j
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
            String convertedSql = CustomSearchRequestMapper.parse(jsonData);

            List<Map<String, Object>> claimList = claimService.claims(convertedSql);
            ArrayList<CaseDetails> caseDetails = mapToCaseDetails(claimList);

            ArrayList<CaseTypeResults> caseTypeResults = new ArrayList<>();
            caseTypeResults.add(new CaseTypeResults("PCS", 1));

            return new CaseSearchResult((long) caseDetails.size(), caseDetails, caseTypeResults);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    //XUI is built to receive a DTO CaseDetails. This is usually sent back via Elastic Search
    //As we are replacing elastic search with raw sql query, we instead receive back an entity (or in this case, a
    // map of objects).
    //This method converts the sql result set, into our CaseDetails DTO to slot back into the existing flow.
    //This however does mean we also have to depend on ccd
    private ArrayList<CaseDetails> mapToCaseDetails(List<Map<String, Object>> claimList)
        throws JsonProcessingException {
        ArrayList<CaseDetails> convertedCaseDetails = new ArrayList<>();

        for (Map<String, Object> individualCase : claimList) {
            CaseDetails convertedCase = new CaseDetails();

            convertedCase.setResolvedTTL((LocalDate) individualCase.getOrDefault("resolved_ttl", null));
            convertedCase.setSecurityClassification(SecurityClassification.valueOf((String) individualCase.get(
                "security_classification")));

            convertedCase.setReference((Long) individualCase.get("reference"));
            convertedCase.setJurisdiction((String) individualCase.get("jurisdiction"));

            Map<String, JsonNode> data = new HashMap<>();

            //address
            PGobject dataObject = (PGobject) individualCase.get("data");
            String dataString = dataObject.getValue();
            JsonNode dataNode = mapper.readTree(dataString);

            try {
                String propertyString = dataNode.get("formattedClaimantContactAddress").toString();
                propertyString = propertyString.substring(1, propertyString.length() - 1);

                String[] propertyStringArray = propertyString.split("<br>");

                ObjectNode value = mapper.createObjectNode();
                value.put("AddressLine3", "");
                value.put("AddressLine2", "");
                value.put("AddressLine1", propertyStringArray[0]);
                value.put("Country", "United Kingdom");
                value.put("PostTown", propertyStringArray[1]);
                value.put("PostCode", propertyStringArray[2]);
                value.put("County", "");
                data.put("propertyAddress", value);

                convertedCase.setData(data);
            } catch (NullPointerException exception) {
                log.error(exception.getMessage());
                //It looks like the current data returned by SQL query only contains address
                // when the case has been issued. To handle this, in this POC, I set defaults

                ObjectNode value = mapper.createObjectNode();

                value.put("AddressLine3", "");
                value.put("AddressLine2", "");
                value.put("AddressLine1", "1 Rse Way");
                value.put("Country", "United Kingdom");
                value.put("PostTown", "London");
                value.put("PostCode", "W37RX");
                value.put("County", "");
                data.put("propertyAddress", value);
                convertedCase.setData(data);
            }
            convertedCase.setState((String) individualCase.get("state"));
            convertedCase.setLastStateModifiedDate(((Timestamp) individualCase.get("last_modified")).toLocalDateTime());
            convertedCase.setVersion((Integer) individualCase.get("version"));

            convertedCase.setId(String.valueOf(individualCase.get("id")));
            convertedCase.setLastModified(((Timestamp) individualCase.get("last_modified")).toLocalDateTime());
            convertedCase.setCreatedDate(((Timestamp) individualCase.get("created_date")).toLocalDateTime());
            convertedCase.setCaseTypeId((String) individualCase.get("case_type_id"));

            convertedCaseDetails.add(convertedCase);
        }
        return convertedCaseDetails;
    }
}