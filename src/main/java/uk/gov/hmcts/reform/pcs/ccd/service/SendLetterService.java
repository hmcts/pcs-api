package uk.gov.hmcts.reform.pcs.ccd.service;


import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.sendletter.api.SendLetterApi;
import uk.gov.hmcts.reform.sendletter.api.SendLetterResponse;

import uk.gov.hmcts.reform.sendletter.api.model.v3.LetterV3;
import uk.gov.hmcts.reform.sendletter.api.model.v3.Document;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@AllArgsConstructor
@Slf4j
public class SendLetterService {

    private SendLetterApi sendLetterApi;
    private AuthTokenGenerator authTokenGenerator;

    public void sendLetterv2() {
        String serviceAuthHeader = authTokenGenerator.generate();

        List<String> personList = new ArrayList<>();
        personList.add("Firstname Lastname");
        Map<String, Object> map = new HashMap<>();
        map.put("recipients", personList);

        try {
            SendLetterResponse sendLetterResponse = sendLetterApi.sendLetter(
                serviceAuthHeader,
                buildLetter(personList)
            );
            log.error(sendLetterResponse.letterId.toString());
        } catch (Exception e) {
            log.error(e.getMessage());
        }

    }

    private LetterV3 buildLetter(List<String> personList) throws IOException, URISyntaxException {
        String XEROX_TYPE_PARAMETER = "CMC001";
        byte[] pdf = Files.readAllBytes(Path.of("/Users/toby.plunkett/Documents/Projects/pcs-api/src/main/resources/files/myPdf.pdf"));
        String response = Base64.getEncoder().encodeToString(pdf);
        Map<String, Object> additionalData = new HashMap<>();
        additionalData.put("caseReference", "123421323");
        additionalData.put("recipients", personList);
        return new LetterV3(XEROX_TYPE_PARAMETER, Arrays.asList(new Document(response, 2)), additionalData);
    }
}
