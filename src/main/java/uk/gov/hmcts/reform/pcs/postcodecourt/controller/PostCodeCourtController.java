package uk.gov.hmcts.reform.pcs.postcodecourt.controller;

import com.azure.core.annotation.QueryParam;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.hmcts.reform.pcs.postcodecourt.service.PostCodeCourtService;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static uk.gov.hmcts.reform.pcs.hearings.constants.HearingConstants.SERVICE_AUTHORIZATION;

@AllArgsConstructor
@RestController
public class PostCodeCourtController {

    public static final String POSTCODE = "postcode";
    public static final String INVALID_POSTCODE_MESSAGE = "Invalid postcode";
    private final PostCodeCourtService postCodeCourtService;

    @GetMapping("/court")
    public ResponseEntity<Void> getByPostcode(@RequestHeader(AUTHORIZATION) String authorisation,
        @RequestHeader(SERVICE_AUTHORIZATION) String serviceAuthorization, @QueryParam(POSTCODE) String postCode) {
        if (StringUtils.isEmpty(postCode)) {
            throw new ResponseStatusException(BAD_REQUEST, INVALID_POSTCODE_MESSAGE);
        }
        postCodeCourtService.getEpimIdByPostCode(postCode);
        return ResponseEntity.ok().build();
    }

}
