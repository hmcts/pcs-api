package uk.gov.hmcts.reform.pcs.postcodecourt.controller;

import com.azure.core.annotation.QueryParam;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.pcs.postcodecourt.record.CourtVenue;
import uk.gov.hmcts.reform.pcs.postcodecourt.service.PostCodeCourtService;

import java.util.List;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static uk.gov.hmcts.reform.pcs.hearings.constants.HearingConstants.SERVICE_AUTHORIZATION;

@AllArgsConstructor
@RestController
public class PostCodeCourtController {

    public static final String COURTS_ENDPOINT = "/courts";
    public static final String POSTCODE = "postcode";

    private final PostCodeCourtService postCodeCourtService;

    @GetMapping(COURTS_ENDPOINT)
    public ResponseEntity<List<CourtVenue>> getByPostcode(@RequestHeader(AUTHORIZATION) String authorisation,
        @RequestHeader(SERVICE_AUTHORIZATION) String serviceAuthorization, @QueryParam(POSTCODE) String postcode) {
        return ResponseEntity.ok(postCodeCourtService.getEpimIdByPostCode(postcode, authorisation));
    }

}
