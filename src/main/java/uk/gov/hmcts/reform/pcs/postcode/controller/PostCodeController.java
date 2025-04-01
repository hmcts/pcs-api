package uk.gov.hmcts.reform.pcs.postcode.controller;

import com.azure.core.annotation.QueryParam;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.hmcts.reform.pcs.postcode.dto.PostCodeResponse;
import uk.gov.hmcts.reform.pcs.postcode.service.PostCodeService;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static uk.gov.hmcts.reform.pcs.hearings.constants.HearingConstants.SERVICE_AUTHORIZATION;

@AllArgsConstructor
@RestController
public class PostCodeController {

    public static final String INVALID_POSTCODE_MESSAGE = "Invalid postcode";
    private final PostCodeService postCodeService;

    @GetMapping("/court")
    public ResponseEntity<PostCodeResponse> getEpimIdByPostcode(@RequestHeader(AUTHORIZATION) String authorisation,
                                                                @RequestHeader(SERVICE_AUTHORIZATION) String serviceAuthorization,
                                                                @QueryParam("postcode") String postcode) {
        if (StringUtils.isEmpty(postcode)) {
            throw new ResponseStatusException(BAD_REQUEST, INVALID_POSTCODE_MESSAGE);
        }
        return ResponseEntity.ok().body(postCodeService.getEpimIdByPostCode(postcode));
    }

}
