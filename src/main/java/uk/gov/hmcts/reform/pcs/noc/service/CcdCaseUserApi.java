package uk.gov.hmcts.reform.pcs.noc.service;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import uk.gov.hmcts.reform.pcs.noc.model.CaseUserRolesRequest;

@FeignClient(name = "ccd-data-store-case-users", url = "${ccd.data-store.api-url}")
public interface CcdCaseUserApi {

    @PostMapping("/case-users")
    void addCaseUserRoles(
        @RequestHeader("Authorization") String authorisation,
        @RequestHeader("ServiceAuthorization") String serviceAuthorisation,
        @RequestBody CaseUserRolesRequest request
    );

    @DeleteMapping("/case-users")
    void removeCaseUserRoles(
        @RequestHeader("Authorization") String authorisation,
        @RequestHeader("ServiceAuthorization") String serviceAuthorisation,
        @RequestBody CaseUserRolesRequest request
    );
}
