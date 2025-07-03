package uk.gov.hmcts.reform.pcs.postcodecourt.controller;

import com.azure.core.annotation.QueryParam;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.Court;
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

    @Operation(summary = "Get courts by postcode",
        description = "Returns a list of courts matching the given postcode",
        security = {
            @SecurityRequirement(name = "AuthorizationToken"),
            @SecurityRequirement(name = "ServiceAuthorization")
        }
    )
    @ApiResponse(responseCode = "200",
        description = "Successful response with list of courts or empty list if no match found",
        content =
            {
                @Content(mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = Court.class)))
        })
    @ApiResponse(responseCode = "400",
        description = "Missing or empty postcode query parameter",
        content = @Content()
    )
    @ApiResponse(responseCode = "401",
        description = "Invalid or missing Authorization/Service Authorization header",
        content = @Content()
    )
    @ApiResponse(responseCode = "403",
        description = "Unauthorised Service Authorization header",
        content = @Content()
    )
    @GetMapping(COURTS_ENDPOINT)
    public ResponseEntity<List<Court>> getCourts(@RequestHeader(AUTHORIZATION) String authorisation,
                                                 @RequestHeader(SERVICE_AUTHORIZATION) String serviceAuthorization,
                                                 @QueryParam(POSTCODE) String postcode) {
        return ResponseEntity.ok(postCodeCourtService.getCountyCourtsByPostCode(postcode));
    }

}
