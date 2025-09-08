package uk.gov.hmcts.reform.pcs.ccd.service;


import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.pcs.idam.IdamService;
import uk.gov.hmcts.reform.sendletter.api.SendLetterApi;
import uk.gov.hmcts.reform.sendletter.api.SendLetterResponse;

import uk.gov.hmcts.reform.sendletter.api.model.v3.LetterV3;
import uk.gov.hmcts.reform.sendletter.api.model.v3.Document;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class SendLetterService {

    private final SendLetterApi sendLetterApi;
    private final AuthTokenGenerator authTokenGenerator;
    private final IdamService idamService;
    private final String caseDocumentUrl;

    public SendLetterService(SendLetterApi sendLetterApi,
                             AuthTokenGenerator authTokenGenerator, IdamService idamService,
                             @Value("${ccd.ccd-data-store-api.java.environment.CASE_DOCUMENT_AM_URL}")
                             String caseDocumentUrl) {

        this.sendLetterApi = sendLetterApi;
        this.authTokenGenerator = authTokenGenerator;
        this.idamService = idamService;
        this.caseDocumentUrl = caseDocumentUrl;
    }

    public void sendLetterv2(String documentId) {

        String serviceAuthHeader = authTokenGenerator.generate();
        String systemUserAuth = idamService.getSystemUserAuthorisation();

        List<String> personList = new ArrayList<>();
        personList.add("Firstname Lastname");
        Map<String, Object> map = new HashMap<>();
        map.put("recipients", personList);

        byte[] documentBinary = getDocumentBinary(systemUserAuth, serviceAuthHeader, documentId).getBody();

        try {
            SendLetterResponse sendLetterResponse = sendLetterApi.sendLetter(
                serviceAuthHeader,
                buildLetter(personList, documentBinary)
            );
            log.error(sendLetterResponse.letterId.toString());
        } catch (Exception e) {
            log.error(e.getMessage());
        }

    }

    public ResponseEntity<byte[]> getDocumentBinary(String authorisation,
                                                    String serviceAuth,
                                                    String documentId) {

        String url = caseDocumentUrl + "/cases/documents/" + documentId + "/binary";
        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.AUTHORIZATION, authorisation);
            headers.set("ServiceAuthorization", serviceAuth);
            MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
            HttpEntity<?> requestEntity = new HttpEntity<>(requestBody, headers);

            return restTemplate.exchange(
                url,
                HttpMethod.GET, requestEntity,
                byte[].class
            );
        } catch (HttpClientErrorException exception) {
            log.error("Exception: {}", exception.getMessage());
            throw exception;
        }
    }


    private LetterV3 buildLetter(List<String> personList, byte[] pdfBinary) throws IOException, URISyntaxException {
        String xeroxTypeParam = "CMC001";

        String response = Base64.getEncoder().encodeToString(pdfBinary);
        Map<String, Object> additionalData = new HashMap<>();
        additionalData.put("caseReference", "123421323");
        additionalData.put("recipients", personList);
        return new LetterV3(xeroxTypeParam, Arrays.asList(new Document(response, 2)), additionalData);
    }
}
