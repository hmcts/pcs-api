package uk.gov.hmcts.reform.pcs.ccd.domain.tabs.bundle;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.type.Document;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.config.JacksonConfiguration;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BundleTabTest {

    private final ObjectMapper objectMapper = new JacksonConfiguration().draftCaseDataObjectMapper();

    @Test
    void shouldReadBackTheBundleTabItWrites() throws Exception {
        BundleTab bundleTab = BundleTab.builder()
            .status("The bundle is up to date.")
            .bundle(Document.builder().url("url").binaryUrl("url/binary").filename("bundle.pdf")
                        .categoryId("bundles").build())
            .lastUpdated("29 October 2026 at 9:00am")
            .pages(12)
            .missingDocuments(List.of(ListValue.<MissingBundleDocument>builder().id("1")
                                          .value(new MissingBundleDocument("Witness.pdf", "Evidence", 3, "Gone"))
                                          .build()))
            .build();

        String json = objectMapper.writeValueAsString(PCSCase.builder().bundleTab(bundleTab).build());

        assertThat(objectMapper.readTree(json).get("bundleTab_Bundle").get("document_url").asText())
            .isEqualTo("url");
        assertThat(objectMapper.readValue(json, PCSCase.class).getBundleTab()).isEqualTo(bundleTab);
    }
}
