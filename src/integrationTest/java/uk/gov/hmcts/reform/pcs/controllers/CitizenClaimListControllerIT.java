package uk.gov.hmcts.reform.pcs.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
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
import static org.mockito.Mockito.verifyNoInteractions;
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
    @Autowired
    private JdbcTemplate jdbcTemplate;

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
    @DisplayName("Lists only the cases the citizen created, newest first, with state and address, without CCD")
    void shouldListOnlyTheCasesTheCitizenCreated() throws Exception {
        saveCase(1111222233334444L, "citizen-1", true, "AWAITING_SUBMISSION_TO_HMCTS");
        saveCase(5555666677778888L, "citizen-2", true, "CASE_ISSUED");
        saveCase(9999000011112222L, "citizen-1", false, "CASE_ISSUED");
        saveCase(2222333344445555L, "citizen-1", true, "CASE_ISSUED");
        saveFeePayment(2222333344445555L, "NOT_PAID", "CLAIM");
        saveFeePayment(2222333344445555L, "PAID", "CLAIM");
        saveFeePayment(2222333344445555L, "NOT_PAID", "COUNTER_CLAIM_ISSUE");

        mockMvc.perform(get(PATH)
                            .header(AUTHORIZATION, "Bearer citizen-token")
                            .header("ServiceAuthorization", "ServiceAuthToken"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].caseReference").value("2222333344445555"))
            .andExpect(jsonPath("$[0].state").value("CASE_ISSUED"))
            .andExpect(jsonPath("$[0].paymentStatus").value("PAID"))
            .andExpect(jsonPath("$[1].caseReference").value("1111222233334444"))
            .andExpect(jsonPath("$[1].state").value("AWAITING_SUBMISSION_TO_HMCTS"))
            .andExpect(jsonPath("$[1].paymentStatus").doesNotExist())
            .andExpect(jsonPath("$[1].propertyAddress.addressLine1").value("15 Garden Drive"))
            .andExpect(jsonPath("$[1].propertyAddress.postcode").value("LU1 1AB"));

        verifyNoInteractions(coreCaseDataApi);
    }

    private void saveFeePayment(long caseReference, String status, String handlerType) {
        jdbcTemplate.update(
            "INSERT INTO claim (id, case_id) SELECT gen_random_uuid(), id FROM pcs_case "
                + "WHERE case_reference = ? AND NOT EXISTS (SELECT 1 FROM claim WHERE case_id = pcs_case.id)",
            caseReference);
        jdbcTemplate.update(
            "INSERT INTO fee_payment (request_date, service_request_reference, status, "
                + "payment_callback_handler_type, possession_claim_id) "
                + "SELECT now(), 'SR-' || gen_random_uuid(), ?, ?, c.id FROM claim c "
                + "JOIN pcs_case pc ON pc.id = c.case_id "
                + "WHERE pc.case_reference = ?", status, handlerType, caseReference);
    }

    private void saveCase(long caseReference, String idamId, boolean claimCreator, String state) {
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
        pcsCaseRepository.saveAndFlush(pcsCase);
        jdbcTemplate.update(
            "INSERT INTO ccd.case_data (id, reference, security_classification, jurisdiction, case_type_id, "
                + "state, data)"
                + " VALUES (?, ?, 'PUBLIC', 'PCS', 'PCS', ?, '{}'::jsonb)", caseReference, caseReference, state);
    }
}
