package uk.gov.hmcts.reform.pcs.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenPropertyEligibility;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenPropertyEligibilityResponse;
import uk.gov.hmcts.reform.pcs.ccd.service.citizenclaim.CitizenPossessionClaimCaseService;
import uk.gov.hmcts.reform.pcs.idam.IdamAuthenticator;
import uk.gov.hmcts.reform.pcs.idam.UserInfo;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;
import uk.gov.hmcts.reform.pcs.service.FeatureFlag;
import uk.gov.hmcts.reform.pcs.service.FeatureToggleService;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static uk.gov.hmcts.reform.pcs.ccd.service.citizenclaim.CitizenPossessionClaimCaseService.LEGISLATIVE_COUNTRY_REQUIRED;
import static uk.gov.hmcts.reform.pcs.ccd.service.citizenclaim.CitizenPossessionClaimCaseService.PROPERTY_NOT_IN_ENGLAND;

@RestController
@RequestMapping("/cases/citizen-claims")
@RequiredArgsConstructor
@Tag(name = "Citizen Claim List", description = "Retrieve claims associated with the authenticated citizen")
public class CitizenPropertyEligibilityController {

    private static final int POSTCODE_MAX = 14;

    private final IdamAuthenticator idamAuthenticator;
    private final FeatureToggleService featureToggleService;
    private final CitizenPossessionClaimCaseService citizenPossessionClaimCaseService;

    @GetMapping(value = "/property-eligibility", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Check whether a property can be claimed for by a citizen private landlord",
        description = "Applies the same postcode check as creating the citizen claim",
        security = {
            @SecurityRequirement(name = "AuthorizationToken"),
            @SecurityRequirement(name = "ServiceAuthorization")
        }
    )
    @ApiResponse(responseCode = "200", description = "Eligibility checked")
    @ApiResponse(responseCode = "400", description = "Missing or invalid postcode or legislative country")
    @ApiResponse(responseCode = "401", description = "Invalid access token")
    @ApiResponse(responseCode = "403", description = "Not a citizen, or invalid Service Authorization")
    @ApiResponse(responseCode = "404", description = "The citizen claim journey is not enabled")
    public ResponseEntity<?> getPropertyEligibility(
        @Parameter(description = "Bearer token for user authentication", required = true)
        @RequestHeader("Authorization") String authorization,
        @Parameter(description = "Service-to-Service (S2S) authorization token", required = true)
        @RequestHeader("ServiceAuthorization") String s2sToken,
        @Parameter(description = "Property postcode", required = true)
        @RequestParam(value = "postcode", required = false) String postcode,
        @Parameter(description = "Country chosen for a cross-border postcode, as its label, e.g. England")
        @RequestParam(value = "legislativeCountry", required = false) String legislativeCountry
    ) {
        if (!featureToggleService.isEnabled(FeatureFlag.PRIVATE_LANDLORD_MAKE_A_CLAIM)) {
            return ResponseEntity.notFound().build();
        }

        UserInfo user = idamAuthenticator.validateAuthToken(authorization).getUserDetails();
        List<String> roles = Optional.ofNullable(user.getRoles()).orElse(List.of());
        if (!roles.contains(UserRole.CITIZEN.getRole())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new RestExceptionHandler.Error("Access denied"));
        }

        if (StringUtils.isBlank(postcode) || postcode.length() > POSTCODE_MAX) {
            return badRequest("Enter a postcode of up to " + POSTCODE_MAX + " characters");
        }
        Optional<LegislativeCountry> chosenCountry = toLegislativeCountry(legislativeCountry);
        if (StringUtils.isNotBlank(legislativeCountry) && chosenCountry.isEmpty()) {
            return badRequest("Unknown legislative country");
        }

        CitizenPropertyEligibility status = citizenPossessionClaimCaseService
            .checkPostcodeInEngland(postcode, chosenCountry.orElse(null))
            .map(CitizenPropertyEligibilityController::toEligibility)
            .orElse(CitizenPropertyEligibility.ELIGIBLE);

        return ResponseEntity.ok(new CitizenPropertyEligibilityResponse(status));
    }

    private static CitizenPropertyEligibility toEligibility(String refusal) {
        return switch (refusal) {
            case PROPERTY_NOT_IN_ENGLAND -> CitizenPropertyEligibility.NOT_ENGLAND;
            case LEGISLATIVE_COUNTRY_REQUIRED -> CitizenPropertyEligibility.LEGISLATIVE_COUNTRY_REQUIRED;
            default -> CitizenPropertyEligibility.NOT_COVERED;
        };
    }

    private static Optional<LegislativeCountry> toLegislativeCountry(String value) {
        if (StringUtils.isBlank(value)) {
            return Optional.empty();
        }
        return Arrays.stream(LegislativeCountry.values())
            .filter(country -> country.getLabel().equalsIgnoreCase(value) || country.name().equalsIgnoreCase(value))
            .findFirst();
    }

    private static ResponseEntity<RestExceptionHandler.Error> badRequest(String message) {
        return ResponseEntity.badRequest().body(new RestExceptionHandler.Error(message));
    }
}
