//package uk.gov.hmcts.reform.pcs.document.service;
//
//import org.springframework.cloud.openfeign.FeignClient;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.RequestBody;
//import org.springframework.web.bind.annotation.RequestHeader;
//import uk.gov.hmcts.reform.pcs.testingsupport.model.DocAssemblyRequest;
//
//@FeignClient(name = "doc-assembly", url = "${doc-assembly.url}")
//public interface DocAssemblyApi {
//    @PostMapping(value = "/api/template-renditions", consumes = "application/json")
//    String generateDocument(
//        @RequestHeader("Authorization") String authorization,
//        @RequestHeader("ServiceAuthorization") String serviceAuthorization,
//        @RequestBody DocAssemblyRequest request
//    );
//}
