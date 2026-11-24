package uk.gov.hmcts.reform.pcs.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.pcs.ccd.entity.AddressEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.config.AbstractPostgresContainerIT;
import uk.gov.hmcts.reform.pcs.idam.IdamUserInfoApi;
import uk.gov.hmcts.reform.pcs.idam.UserInfo;
import uk.gov.hmcts.reform.pcs.util.IdamHelper;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("integration")
@DisplayName("Claims a citizen has made as claimant")
class CitizenClaimListControllerIT extends AbstractPostgresContainerIT {

    private static final String PATH = "/cases/citizen-claims/as-claimant";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private IdamHelper idamHelper;
    @Autowired
    private PcsCaseRepository pcsCaseRepository;

    @MockitoBean
    private AuthTokenGenerator authTokenGenerator;
    @MockitoBean
    private IdamUserInfoApi idamUserInfoApi;
    @MockitoBean
    private OAuth2AuthorizedClientManager authorizedClientManager;
    @MockitoBean
    private CoreCaseDataApi coreCaseDataApi;

    @BeforeEach
    void setUp() {
        idamHelper.stubIdamSystemUser(authorizedClientManager, "system-user-id-token");
        when(authTokenGenerator.generate()).thenReturn("ServiceAuthToken");
        when(idamUserInfoApi.getUserInfo(anyString()))
            .thenReturn(UserInfo.builder().uid("Citizen-1").roles(List.of("citizen")).build());
    }

    @Test
    @Transactional
    @DisplayName("Lists only the cases the signed-in citizen created, with CCD state and property address")
    void shouldListOnlyTheCasesTheCitizenCreated() throws Exception {
        saveCase(1111222233334444L, "citizen-1", true);
        saveCase(5555666677778888L, "citizen-2", true);
        saveCase(9999000011112222L, "citizen-1", false);
        when(coreCaseDataApi.getCase(anyString(), anyString(), anyString()))
            .thenReturn(CaseDetails.builder().state("AWAITING_SUBMISSION_TO_HMCTS").build());

        mockMvc.perform(get(PATH)
                            .header(AUTHORIZATION, "Bearer citizen-token")
                            .header("ServiceAuthorization", "ServiceAuthToken"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].caseReference").value("1111222233334444"))
            .andExpect(jsonPath("$[0].state").value("AWAITING_SUBMISSION_TO_HMCTS"))
            .andExpect(jsonPath("$[0].propertyAddress.addressLine1").value("15 Garden Drive"))
            .andExpect(jsonPath("$[0].propertyAddress.postcode").value("LU1 1AB"));
    }

    private void saveCase(long caseReference, String idamId, boolean claimCreator) {
        PcsCaseEntity pcsCase = new PcsCaseEntity();
        pcsCase.setCaseReference(caseReference);
        pcsCase.setPropertyAddress(AddressEntity.builder()
                                       .addressLine1("15 Garden Drive")
                                       .postTown("Luton")
                                       .postcode("LU1 1AB")
                                       .build());
        PartyEntity party = new PartyEntity();
        party.setIdamId(idamId);
        party.setClaimCreator(claimCreator);
        party.setRemoved(false);
        pcsCase.addParty(party);
        pcsCaseRepository.save(pcsCase);
    }
}
