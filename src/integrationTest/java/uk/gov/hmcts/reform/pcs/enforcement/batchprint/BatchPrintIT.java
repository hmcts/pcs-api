package uk.gov.hmcts.reform.pcs.enforcement.batchprint;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import feign.Request;
import jakarta.persistence.EntityManager;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.ccd.document.am.feign.CaseDocumentClientApi;
import uk.gov.hmcts.reform.pcs.am.RoleAssignment;
import uk.gov.hmcts.reform.pcs.am.RoleAssignmentApi;
import uk.gov.hmcts.reform.pcs.am.RoleAssignmentAttributes;
import uk.gov.hmcts.reform.pcs.am.RoleAssignmentResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.DocumentType;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.StatementOfTruthEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.StatementOfTruthRepository;
import uk.gov.hmcts.reform.pcs.config.AbstractPostgresContainerIT;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus;
import uk.gov.hmcts.reform.pcs.enforcement.entity.BailiffAllocationEntity;
import uk.gov.hmcts.reform.pcs.enforcement.entity.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.enforcement.entity.WarrantEntity;
import uk.gov.hmcts.reform.pcs.enforcement.repository.BailiffAllocationRepository;
import uk.gov.hmcts.reform.pcs.enforcement.repository.EnforcementOrderRepository;
import uk.gov.hmcts.reform.pcs.enforcement.repository.WarrantRepository;
import uk.gov.hmcts.reform.pcs.idam.IdamUserInfoApi;
import uk.gov.hmcts.reform.pcs.idam.UserInfo;
import uk.gov.hmcts.reform.pcs.service.FeatureFlag;
import uk.gov.hmcts.reform.pcs.service.FeatureToggleService;
import uk.gov.hmcts.reform.pcs.util.IdamHelper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("integration")
@Transactional
@DisplayName("Batch print of enforcement documents (HDPI-8012)")
class BatchPrintIT extends AbstractPostgresContainerIT {

    private static final String BAILIFF_TOKEN = "Bearer bailiff-token";
    private static final String SERVICE_AUTHORIZATION = "ServiceAuthorization";
    private static final String BAILIFF = "bob-id";
    private static final long FIRST = 8012000000000001L;
    private static final long SECOND = 8012000000000002L;
    private static final long NOT_MINE = 8012000000000003L;
    private static final Instant MONDAY = Instant.parse("2026-10-12T08:00:00Z");

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private PcsCaseRepository pcsCaseRepository;
    @Autowired
    private EnforcementOrderRepository enforcementOrderRepository;
    @Autowired
    private WarrantRepository warrantRepository;
    @Autowired
    private StatementOfTruthRepository statementOfTruthRepository;
    @Autowired
    private BailiffAllocationRepository bailiffAllocationRepository;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private IdamHelper idamHelper;

    @MockitoBean
    private IdamUserInfoApi idamUserInfoApi;
    @MockitoBean
    private AuthTokenGenerator authTokenGenerator;
    @MockitoBean
    private OAuth2AuthorizedClientManager authorizedClientManager;
    @MockitoBean
    private CaseDocumentClientApi caseDocumentClientApi;
    @MockitoBean
    private RoleAssignmentApi roleAssignmentApi;
    @MockitoBean
    private FeatureToggleService featureToggleService;

    @BeforeEach
    void setUp() {
        idamHelper.stubIdamSystemUser(authorizedClientManager, "system-token");
        when(authTokenGenerator.generate()).thenReturn("s2s-token");
        when(idamUserInfoApi.getUserInfo(anyString())).thenReturn(UserInfo.builder().uid("BOB-ID").build());
        when(featureToggleService.isEnabled(FeatureFlag.ENFORCEMENT)).thenReturn(true);
        givenAmRoles("BOB-ID", "bailiff");

        // enf_case.enforcement_number is unique across the database these tests share, so these are
        // namespaced on the ticket like the case references above rather than taken from the low end of the
        // sequence EnforcementNumberGenerator issues.
        EnforcementOrderEntity first = warrant(FIRST, "WP-80120001", LocalDateTime.of(2026, 10, 15, 10, 0), BAILIFF);
        document(first, DocumentType.WARRANT_OF_POSSESSION, "Warrant 1", 1);
        document(first, DocumentType.RISK_ASSESSMENT, "Risk assessment 1", 2);
        document(first, DocumentType.NOTICE_OF_EVICTION, "Original notice 1", 3);
        document(first, DocumentType.NOTICE_OF_EVICTION, "Rescheduled notice 1", 4);

        EnforcementOrderEntity second = warrant(SECOND, "WP-80120002", LocalDateTime.of(2026, 10, 13, 9, 0), BAILIFF);
        document(second, DocumentType.WARRANT_OF_POSSESSION, "Warrant 2", 1);
        document(second, DocumentType.NOTICE_OF_EVICTION, "Notice 2", 2);

        EnforcementOrderEntity notMine = warrant(NOT_MINE, "WP-80120003", LocalDateTime.of(2026, 10, 14, 9, 0),
                                                 "alex-id");
        document(notMine, DocumentType.WARRANT_OF_POSSESSION, "Warrant 3", 1);

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("prints each chosen case in turn, latest documents only, leaving out a case that is not mine")
    void printsTheChosenCases() throws Exception {
        Set<Path> tempFilesBefore = batchPrintTempFiles();

        MvcResult result = mockMvc.perform(batchPrint("", FIRST, NOT_MINE, SECOND))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_PDF))
            .andExpect(header().string(BatchPrintController.INCLUDED_HEADER, FIRST + "," + SECOND))
            .andExpect(header().string(BatchPrintController.EXCLUDED_HEADER, NOT_MINE + "=NOT_ALLOCATED"))
            .andReturn();

        try (PDDocument pdf = Loader.loadPDF(result.getResponse().getContentAsByteArray())) {
            assertThat(pdf.getNumberOfPages()).isEqualTo(5);
            assertThat(pageTexts(pdf)).containsExactly(
                "Warrant 1", "Risk assessment 1", "Rescheduled notice 1", "Warrant 2", "Notice 2");
        }
        assertThat(batchPrintTempFiles()).isEqualTo(tempFilesBefore);
    }

    /**
     * The batch print is streamed from a temp file in a directory of its own, so neither may be left in the
     * filesystem once the request is over: {@link BatchPrintFileCleanupFilter} runs in the real filter chain
     * to see to that.
     */
    private static Set<Path> batchPrintTempFiles() throws IOException {
        try (Stream<Path> tempFiles = Files.list(Path.of(System.getProperty("java.io.tmpdir")))) {
            return tempFiles.filter(file -> file.getFileName().toString().startsWith("batch-print-"))
                .collect(Collectors.toSet());
        }
    }

    @Test
    @DisplayName("checks what would be printed and says why a case is left out")
    void checksTheChosenCases() throws Exception {
        mockMvc.perform(batchPrint("/check", SECOND, NOT_MINE))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.included", hasSize(1)))
            .andExpect(jsonPath("$.included[0].caseReference").value(String.valueOf(SECOND)))
            .andExpect(jsonPath("$.included[0].warrantNumber").value("WP-80120002"))
            .andExpect(jsonPath("$.included[0].documents[*].kind", contains("WARRANT", "NOTICE_OF_EVICTION")))
            .andExpect(jsonPath("$.included[0].documents[0].documentId").doesNotExist())
            .andExpect(jsonPath("$.included[0].missingDocuments", contains("RISK_ASSESSMENT")))
            .andExpect(jsonPath("$.excluded[0].caseReference").value(String.valueOf(NOT_MINE)))
            .andExpect(jsonPath("$.excluded[0].code").value("NOT_ALLOCATED"))
            .andExpect(jsonPath("$.excluded[0].reason").value("You are not the allocated bailiff on this case"));
    }

    @Test
    @DisplayName("answers 422 with the reasons when no chosen case can be printed")
    void refusesWhenNothingCanBePrinted() throws Exception {
        mockMvc.perform(batchPrint("", NOT_MINE))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.included", hasSize(0)))
            .andExpect(jsonPath("$.excluded[0].code").value("NOT_ALLOCATED"));
    }

    @Test
    @DisplayName("refuses a request naming no cases")
    void refusesAnEmptyRequest() throws Exception {
        mockMvc.perform(post("/enforcement/batch-print")
                            .header(AUTHORIZATION, BAILIFF_TOKEN)
                            .header(SERVICE_AUTHORIZATION, "s2s")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("caseReferences", List.of()))))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("lists my warrants with an eviction this week, soonest first")
    void listsMyWarrants() throws Exception {
        mockMvc.perform(get("/enforcement/batch-print/cases")
                            .param("from", "2026-10-12")
                            .param("to", "2026-10-18")
                            .header(AUTHORIZATION, BAILIFF_TOKEN)
                            .header(SERVICE_AUTHORIZATION, "s2s"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[*].caseReference", contains(String.valueOf(SECOND), String.valueOf(FIRST))))
            .andExpect(jsonPath("$[0].warrantNumber").value("WP-80120002"))
            .andExpect(jsonPath("$[0].status").value("CLAIMANT_CONFIRMED"));
    }

    @Test
    @DisplayName("refuses a user whose IDAM token is not valid with 401")
    void refusesAnInvalidToken() throws Exception {
        Request request = Request.create(Request.HttpMethod.GET, "/o/userinfo", Map.of(), null,
                                         StandardCharsets.UTF_8, null);
        when(idamUserInfoApi.getUserInfo("Bearer expired-token"))
            .thenThrow(new FeignException.Unauthorized("expired", request, null, Map.of()));

        mockMvc.perform(batchPrintAs("Bearer expired-token", "", FIRST))
            .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/enforcement/batch-print/cases")
                            .header(AUTHORIZATION, "Bearer expired-token")
                            .header(SERVICE_AUTHORIZATION, "s2s"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("refuses with 403 a user still allocated a warrant who no longer has the bailiff role in AM")
    void refusesAUserWithoutTheBailiffRole() throws Exception {
        givenAmRoles("BOB-ID", "hearing-centre-admin");

        mockMvc.perform(batchPrint("", FIRST))
            .andExpect(status().isForbidden());
        mockMvc.perform(batchPrint("/check", FIRST))
            .andExpect(status().isForbidden());
        mockMvc.perform(get("/enforcement/batch-print/cases")
                            .header(AUTHORIZATION, BAILIFF_TOKEN)
                            .header(SERVICE_AUTHORIZATION, "s2s"))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("refuses with 403 a user whose bailiff role is in another jurisdiction")
    void refusesABailiffInAnotherJurisdiction() throws Exception {
        givenAmRoles("BOB-ID", "bailiff", "CIVIL");

        mockMvc.perform(batchPrint("", FIRST))
            .andExpect(status().isForbidden());
        mockMvc.perform(get("/enforcement/batch-print/cases")
                            .header(AUTHORIZATION, BAILIFF_TOKEN)
                            .header(SERVICE_AUTHORIZATION, "s2s"))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("answers 404 on every endpoint while the enforcement-enabled flag is off")
    void answersNotFoundWhileEnforcementIsOff() throws Exception {
        when(featureToggleService.isEnabled(FeatureFlag.ENFORCEMENT)).thenReturn(false);

        mockMvc.perform(batchPrint("", FIRST))
            .andExpect(status().isNotFound());
        mockMvc.perform(batchPrint("/check", FIRST))
            .andExpect(status().isNotFound());
        mockMvc.perform(get("/enforcement/batch-print/cases")
                            .header(AUTHORIZATION, BAILIFF_TOKEN)
                            .header(SERVICE_AUTHORIZATION, "s2s"))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("indexes active allocations by lower-cased bailiff id (V050)")
    void indexesActiveAllocationsByBailiff() {
        assertThat(jdbcTemplate.queryForObject(
            "SELECT indexdef FROM pg_indexes WHERE indexname = 'idx_enf_bailiff_allocation_active_bailiff'",
            String.class))
            .contains("lower(bailiff_user_id)")
            .contains("WHERE (ended_at IS NULL)");
    }

    private void givenAmRoles(String userId, String roleName) {
        givenAmRoles(userId, roleName, "PCS");
    }

    private void givenAmRoles(String userId, String roleName, String jurisdiction) {
        when(roleAssignmentApi.getRoles(anyString(), anyString(), eq(userId)))
            .thenReturn(RoleAssignmentResponse.builder()
                            .roleAssignment(List.of(RoleAssignment.builder()
                                                        .actorId(userId)
                                                        .roleType("ORGANISATION")
                                                        .roleName(roleName)
                                                        .attributes(RoleAssignmentAttributes.builder()
                                                                        .jurisdiction(jurisdiction)
                                                                        .build())
                                                        .build()))
                            .build());
    }

    private RequestBuilder batchPrint(String path, long... caseReferences) throws IOException {
        return batchPrintAs(BAILIFF_TOKEN, path, caseReferences);
    }

    private RequestBuilder batchPrintAs(String authorization, String path, long... caseReferences)
        throws IOException {
        List<String> references = Arrays.stream(caseReferences).mapToObj(String::valueOf).toList();
        return post("/enforcement/batch-print" + path)
            .header(AUTHORIZATION, authorization)
            .header(SERVICE_AUTHORIZATION, "s2s")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Map.of("caseReferences", references)));
    }

    private EnforcementOrderEntity warrant(long caseReference, String number, LocalDateTime evictionDate,
                                           String bailiffUserId) {
        PcsCaseEntity pcsCase = PcsCaseEntity.builder().caseReference(caseReference).build();
        pcsCase.addClaim(new ClaimEntity());
        ClaimEntity claim = pcsCaseRepository.save(pcsCase).getClaims().getFirst();

        StatementOfTruthEntity statementOfTruth = new StatementOfTruthEntity();
        statementOfTruth.setAccepted(YesOrNo.YES);
        statementOfTruth.setFullName("Test Name");

        EnforcementOrderEntity enforcement = new EnforcementOrderEntity();
        enforcement.setClaim(claim);
        enforcement.setEnforcementOrder(EnforcementOrder.builder().build());
        enforcement.setStatementOfTruth(statementOfTruthRepository.save(statementOfTruth));
        enforcement.setStatus(EnforcementStatus.CLAIMANT_CONFIRMED);
        enforcement.setStatusUpdatedAt(MONDAY);
        enforcement.setEnforcementNumber(number);
        enforcement.setBailiffDate(evictionDate);
        EnforcementOrderEntity saved = enforcementOrderRepository.save(enforcement);
        saved.setWarrantDetails(warrantRepository.save(WarrantEntity.builder().enforcementOrder(saved).build()));
        bailiffAllocationRepository.save(BailiffAllocationEntity.builder()
                                             .enforcementOrder(saved)
                                             .bailiffUserId(bailiffUserId)
                                             .startedAt(MONDAY)
                                             .build());
        return saved;
    }

    /**
     * Files a one-page PDF reading {@code title} in the Enforcement folder, served by the mocked CDAM, with a
     * submitted date {@code minutes} after Monday so the order between documents is fixed.
     */
    private void document(EnforcementOrderEntity enforcement, DocumentType type, String title, int minutes) {
        UUID documentId = UUID.randomUUID();
        DocumentEntity document = DocumentEntity.builder()
            .documentId(documentId)
            .url("http://dm-store/documents/" + documentId)
            .fileName(title + ".pdf")
            .type(type)
            .categoryId("enforcement")
            .build();
        PcsCaseEntity pcsCase = enforcement.getClaim().getPcsCase();
        pcsCase.addDocument(document);
        enforcement.addDocuments(List.of(document));
        entityManager.flush();
        jdbcTemplate.update("UPDATE document SET submitted_date = ? WHERE id = ?",
                            Timestamp.from(MONDAY.plusSeconds(60L * minutes)), document.getId());

        byte[] pdf = onePagePdf(title);
        when(caseDocumentClientApi.getDocumentBinary(any(), any(), eq(documentId)))
            .thenReturn(ResponseEntity.ok(new ByteArrayResource(pdf)));
    }

    private static byte[] onePagePdf(String title) {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                content.beginText();
                content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                content.newLineAtOffset(72, 720);
                content.showText(title);
                content.endText();
            }
            document.save(output);
            return output.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static List<String> pageTexts(PDDocument pdf) throws IOException {
        PDFTextStripper stripper = new PDFTextStripper();
        List<String> texts = new ArrayList<>();
        for (int page = 1; page <= pdf.getNumberOfPages(); page++) {
            stripper.setStartPage(page);
            stripper.setEndPage(page);
            texts.add(stripper.getText(pdf).trim());
        }
        return texts;
    }
}
