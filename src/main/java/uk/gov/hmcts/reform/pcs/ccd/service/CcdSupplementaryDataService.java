package uk.gov.hmcts.reform.pcs.ccd.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
import uk.gov.hmcts.reform.pcs.idam.IdamService;

import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.singletonMap;

@RequiredArgsConstructor
@Service
public class CcdSupplementaryDataService {

    private final IdamService idamService;

    private final AuthTokenGenerator authTokenGenerator;

    private final CoreCaseDataApi coreCaseDataApi;

    @Value("${hmcts.hmctsOrgId:AAA3}")
    private String hmctsServiceId;

    private static final String SUPPLEMENTARY_FIELD = "supplementary_data_updates";
    private static final String SERVICE_ID_FIELD = "HMCTSServiceId";
    private static final String SET_OPERATION = "$set";

    public void submitSupplementaryDataToCcd(String caseId) {
        Map<String, Map<String, Map<String, Object>>> supplementaryDataUpdates = new HashMap<>();
        supplementaryDataUpdates.put(SUPPLEMENTARY_FIELD,
            singletonMap(SET_OPERATION, singletonMap(SERVICE_ID_FIELD,
                hmctsServiceId)));

        coreCaseDataApi.submitSupplementaryData(idamService.getSystemUserAuthorisation(),
            authTokenGenerator.generate(),
            caseId,
            supplementaryDataUpdates);
    }
}