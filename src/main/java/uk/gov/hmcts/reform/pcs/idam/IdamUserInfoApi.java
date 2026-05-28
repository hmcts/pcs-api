package uk.gov.hmcts.reform.pcs.idam;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@FeignClient(name = "idam-userinfo", url = "${idam.api.url}")
public interface IdamUserInfoApi {

    @GetMapping("/o/userinfo")
    UserInfo getUserInfo(@RequestHeader(AUTHORIZATION) String bearerToken);
}
