package uk.gov.hmcts.reform.pcs.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.pcs.idam.IdamService;
import uk.gov.hmcts.reform.pcs.model.ValidateAccessCodeRequest;
import uk.gov.hmcts.reform.pcs.model.ValidateAccessCodeResponse;
import uk.gov.hmcts.reform.pcs.service.CasePartyLinkService;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@RestController
@RequestMapping("/cases")
@RequiredArgsConstructor
@Slf4j
@Tag(
        name = "Citizen Access Code Validation",
        description = "Validate access code and link citizen user ID into defendant JSON"
)
public class CasePartyLinkController {

    private static final String SERVICE_AUTHORIZATION = "ServiceAuthorization";

    private final IdamService idamService;
    private final CasePartyLinkService casePartyLinkService;

    @PostMapping(
            value = "/{caseReference}/validate-access-code",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
            summary = "Validate access code and link citizen to a case",
            description = "Stores the citizen's IDAM user ID inside the matching defendant JSON record"
    )
    @ApiResponse(responseCode = "200", description = "Successful validation and linking",
            content = @Content(schema = @Schema(implementation = ValidateAccessCodeResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid request or schema")
    @ApiResponse(responseCode = "401", description = "Invalid IDAM token")
    @ApiResponse(responseCode = "403", description = "Invalid Service Authorization")
    @ApiResponse(responseCode = "404", description = "Case or Access Code not found")
    @ApiResponse(responseCode = "409", description = "Access code already used by another user")
    public ResponseEntity<ValidateAccessCodeResponse> validateAccessCode(
            @PathVariable long caseReference,
            @Valid @RequestBody ValidateAccessCodeRequest request,
            @RequestHeader(AUTHORIZATION) String authorization,
            @RequestHeader(SERVICE_AUTHORIZATION) String s2sToken
    ) {
        log.info("Validating access code for case reference: {}", caseReference);
        log.debug("Request details - caseReference: {}, accessCode: {}", caseReference, request.getAccessCode());

        var user = idamService.validateAuthToken(authorization).getUserDetails();
        log.debug("IDAM token validated successfully for user: {}", user.getUid());

        ValidateAccessCodeResponse response =
                casePartyLinkService.validateAndLinkParty(caseReference, request.getAccessCode(), user);

        log.info("Successfully linked user to case reference: {}", caseReference);
        log.debug("Response - caseReference: {}, status: {}", response.getCaseReference(), response.getStatus());

        return ResponseEntity.ok(response);
    }

}
