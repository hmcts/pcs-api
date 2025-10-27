package uk.gov.hmcts.reform.pcs.reference.api;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import uk.gov.hmcts.reform.pcs.reference.dto.OrganisationDetailsResponse;

@FeignClient(name = "rd-professional", url = "${rd-professional.api-url}")
public interface RdProfessionalApi {

    @GetMapping("/refdata/internal/v1/organisations/orgDetails/{userId}")
    OrganisationDetailsResponse getOrganisationDetails(
        @PathVariable("userId") String userId,
        @RequestHeader("ServiceAuthorization") String s2sToken,
        @RequestHeader("Authorization") String prdAdminToken
    );
}
