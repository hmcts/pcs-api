package uk.gov.hmcts.reform.pcs;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantContactPreferences;
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantInformation;
import uk.gov.hmcts.reform.pcs.ccd.domain.CompletionNextStep;
import uk.gov.hmcts.reform.pcs.ccd.domain.LanguageUsed;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceType;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.bundle.CaseBundleVersionEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.CaseBundleVersionRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.client.CcdClient;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;
import uk.gov.hmcts.rse.ccd.lib.test.CftlibTest;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.resumePossessionClaim;
import static uk.gov.hmcts.reform.pcs.utils.TestDataGenerator.createDefendant;
import static uk.gov.hmcts.reform.pcs.utils.TestDataGenerator.createTestAddress;

/**
 * The judges' case bundle through the whole stack: a claim made through CCD, the doc toolkit's hook in each
 * event, the worker rendering in the background, CDAM storing the PDF in dm-store, and PCS keeping the version.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CaseBundleTest extends CftlibTest {

    @Autowired
    private CcdClient ccdClient;
    @Autowired
    private IdamClient idamClient;
    @Autowired
    private PcsCaseRepository cases;
    @Autowired
    private CaseBundleVersionRepository versions;

    private String solicitorToken;

    @BeforeAll
    void setup() {
        solicitorToken = idamClient.getAccessToken("pcs-solicitor1@test.com", "password");
    }

    @Test
    void claimMadeThroughCcdGetsAStitchedBundleStoredThroughCdam() throws Exception {
        CaseDetails created = ccdClient.createCase(PCSCase.builder()
            .propertyAddress(AddressUK.builder().addressLine1("1 Bundle Street").postTown("London")
                                 .postCode("NW1 6XE").build())
            .legislativeCountry(LegislativeCountry.ENGLAND)
            .build(), solicitorToken);
        long reference = created.getId();

        ccdClient.updateCase(resumePossessionClaim, reference, PCSCase.builder()
            .caseManagementLocationNumber(20262)
            .regionId(1)
            .claimantInformation(ClaimantInformation.builder()
                                     .claimantName("Bundle Claimant")
                                     .isClaimantNameCorrect(VerticalYesNo.YES)
                                     .build())
            .claimantContactPreferences(ClaimantContactPreferences.builder()
                                            .overriddenClaimantContactAddress(createTestAddress())
                                            .build())
            .tenancyLicenceDetails(TenancyLicenceDetails.builder()
                                       .typeOfTenancyLicence(TenancyLicenceType.ASSURED_TENANCY)
                                       .build())
            .defendant1(createDefendant("Danny", "Defendant"))
            .addAnotherDefendant(VerticalYesNo.NO)
            .noticeServed(YesOrNo.NO)
            .languageUsed(LanguageUsed.ENGLISH)
            .completionNextStep(CompletionNextStep.SUBMIT_AND_PAY_NOW)
            .build(), solicitorToken);

        // Each event submitted the bundle; the worker renders it and PCS keeps each version.
        await().atMost(Duration.ofMinutes(2)).pollInterval(Duration.ofSeconds(1))
            .until(() -> !versions(reference).isEmpty());
        CaseBundleVersionEntity bundle = versions(reference).getFirst();
        assertThat(bundle.getFileName()).isEqualTo(reference + "-case-bundle.pdf");

        // The stitched PDF is in dm-store, as CDAM stored it.
        HttpResponse<byte[]> download = HttpClient.newHttpClient().send(
            HttpRequest.newBuilder(URI.create(bundle.getBinaryUrl())).GET().build(),
            HttpResponse.BodyHandlers.ofByteArray());
        assertThat(download.statusCode()).isEqualTo(200);
        try (PDDocument pdf = Loader.loadPDF(download.body())) {
            assertThat(pdf.getNumberOfPages()).isEqualTo(bundle.getPageCount());
            String text = new PDFTextStripper().getText(pdf);
            assertThat(text).contains("Case bundle", "Index", "Statements of case", "Evidence",
                                      "Uncategorised documents", "of " + bundle.getPageCount());
        }
    }

    private List<CaseBundleVersionEntity> versions(long reference) {
        return cases.findByCaseReference(reference)
            .map(pcsCase -> versions.findByCaseIdOrderByCreatedAtDesc(pcsCase.getId()))
            .orElse(List.of());
    }
}
