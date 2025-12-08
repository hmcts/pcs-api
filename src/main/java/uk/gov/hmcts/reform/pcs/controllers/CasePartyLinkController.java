package uk.gov.hmcts.reform.pcs.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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

@RestController
@RequestMapping("/cases")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Citizen Access Code Validation",
        description = "Validate access code and link citizen user ID into defendant JSON")
public class CasePartyLinkController {

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
    @ApiResponse(responseCode = "400", description = "Invalid access code for this case",
            content = @Content())
    @ApiResponse(responseCode = "401", description = "Invalid IDAM token",
            content = @Content())
    @ApiResponse(responseCode = "403", description = "Invalid Service Authorization",
            content = @Content())
    @ApiResponse(responseCode = "404", description = "Case not found or party does not belong to this case",
            content = @Content())
    @ApiResponse(responseCode = "409", description = "Access code already used by another user",
            content = @Content())
    public ResponseEntity<ValidateAccessCodeResponse> validateAccessCode(
            @Parameter(description = "The 12-digit case reference number", required = true)
            @PathVariable long caseReference,
            @Valid @RequestBody ValidateAccessCodeRequest request,
            @RequestHeader("Authorization") String authorization,
            @RequestHeader("ServiceAuthorization") String s2sToken
    ) {
        var user = idamService.validateAuthToken(authorization).getUserDetails();

        ValidateAccessCodeResponse response =
                casePartyLinkService.validateAndLinkParty(caseReference, request.getAccessCode(), user);

        return ResponseEntity.ok(response);
    }

}
