package uk.gov.hmcts.reform.pcs.postalcode.controller;

import com.azure.core.annotation.QueryParam;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.hmcts.reform.pcs.postalcode.dto.PostCodeResponse;
import uk.gov.hmcts.reform.pcs.postalcode.service.PostalCodeService;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static uk.gov.hmcts.reform.pcs.hearings.constants.HearingConstants.SERVICE_AUTHORIZATION;

@AllArgsConstructor
@RestController
public class PostalCodeController {

    public static final String INVALID_POSTCODE_FORMAT = "Invalid postcode format";
    private final PostalCodeService postalCodeService;

    @GetMapping("/court")
    public ResponseEntity<PostCodeResponse> getEPIMSIdByPostcode(@RequestHeader(AUTHORIZATION) String authorisation,
                                                          @RequestHeader(SERVICE_AUTHORIZATION) String serviceAuthorization,
                                                          @QueryParam("postcode") String postcode) {
        if (StringUtils.isEmpty(postcode)) {
            throw new ResponseStatusException(BAD_REQUEST, INVALID_POSTCODE_FORMAT);
        }
        return ResponseEntity.ok().body(postalCodeService.getEPIMSIdByPostcode(postcode));
    }

}
