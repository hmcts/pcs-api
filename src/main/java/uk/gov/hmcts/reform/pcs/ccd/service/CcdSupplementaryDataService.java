package uk.gov.hmcts.reform.pcs.ccd.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
import uk.gov.hmcts.reform.pcs.idam.IdamService;

import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.singletonMap;

@Service
public class CcdSupplementaryDataService {

    private final IdamService idamService;
    private final AuthTokenGenerator authTokenGenerator;
    private final CoreCaseDataApi coreCaseDataApi;
    private final String hmctsServiceId;

    public CcdSupplementaryDataService(IdamService idamService,
                                       AuthTokenGenerator authTokenGenerator,
                                       CoreCaseDataApi coreCaseDataApi,
                                       @Value("${hmcts.hmctsOrgId}") String hmctsServiceId) {
        this.idamService = idamService;
        this.authTokenGenerator = authTokenGenerator;
        this.coreCaseDataApi = coreCaseDataApi;
        this.hmctsServiceId = hmctsServiceId;
    }

    private static final String SUPPLEMENTARY_FIELD = "supplementary_data_updates";
    private static final String SERVICE_ID_FIELD = "HMCTSServiceId";
    private static final String SET_OPERATION = "$set";

    public void submitSupplementaryDataRequestToCcd(String caseId) {
        Map<String, Map<String, Map<String, Object>>> supplementaryDataRequest = new HashMap<>();
        supplementaryDataRequest.put(SUPPLEMENTARY_FIELD,
            singletonMap(SET_OPERATION, singletonMap(SERVICE_ID_FIELD,
                hmctsServiceId)));

        coreCaseDataApi.submitSupplementaryData(idamService.getSystemUserAuthorisation(),
            authTokenGenerator.generate(),
            caseId,
            supplementaryDataRequest);
    }
}