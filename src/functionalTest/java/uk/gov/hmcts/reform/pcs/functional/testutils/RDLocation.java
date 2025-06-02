package uk.gov.hmcts.reform.pcs.functional.testutils;
import io.restassured.response.Response;
import io.vavr.collection.HashMap;
import net.serenitybdd.rest.SerenityRest;
import uk.gov.hmcts.reform.pcs.functional.config.CourtConstants;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


public class RDLocation {


    public static List<Map<String, Object>> getCourtName(int epimId) {

        String baseUrl1 = System.getenv("RD_LOCATION");
        baseUrl1 = "http://rd-location-ref-api-aat.service.core-compute-aat.internal";

        final String endpoint = "/refdata/location/court-venues";
        ServiceAuthenticationGenerator serviceAuthenticationGenerator = new ServiceAuthenticationGenerator();
        String pcsApiS2sToken = serviceAuthenticationGenerator.generate();
        String idamToken;
        idamToken = IdamAuthenticationGenerator.generateToken();

        HashMap<String, String> headers = HashMap.empty();
        headers = headers.put("accept", "application/json");
        headers = headers.put("ServiceAuthorization", pcsApiS2sToken);
        headers = headers.put("Authorization", "Bearer " + idamToken);
        String url = baseUrl1 + endpoint + "?epimms_id=" + epimId + "&court_type_id=" + CourtConstants.COURT_TYPE_ID;
        CurlLogger.logCurlCommand("GET", url, headers);

        Response response =  SerenityRest.given()
            .baseUri(baseUrl1)
            .basePath(endpoint)
            .queryParam("epimms_id", epimId)
            .queryParam("court_type_id", CourtConstants.COURT_TYPE_ID)
            .header("accept", "application/json")
            .header("ServiceAuthorization", pcsApiS2sToken)
            .header("Authorization", "Bearer " + idamToken)
            .when()
            .get();
        response
            .then()
            .statusCode(200);



        int courtVenueId = response.jsonPath().getInt("[0].court_venue_id");
        String courtName = response.jsonPath().getString("[0].court_name");
        int epimmsId = response.jsonPath().getInt("[0].epimms_id");


        System.out.println("A: " + courtVenueId);
        System.out.println("C: " + epimmsId);
        System.out.println("D: " + courtName);
        Map<String, Object> record1 = new LinkedHashMap<>();
        record1.put("id", courtVenueId);
        record1.put("name", courtName);
        record1.put("epimId", epimmsId);

        List<Map<String, Object>> result = new ArrayList<>();
        result.add(record1);
        // Add more records if needed
        return result;
    }
}
