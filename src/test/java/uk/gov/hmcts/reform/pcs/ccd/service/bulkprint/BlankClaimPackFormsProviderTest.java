package uk.gov.hmcts.reform.pcs.ccd.service.bulkprint;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BlankClaimPackFormsProviderTest {

    @Test
    @DisplayName("Loads real classpath form resources and validates that PDFBox can parse both documents")
    void shouldLoadClasspathFormResourcesSuccessfully() throws IOException {
        Resource defenceForm = new ClassPathResource("forms/ONC-002R-defence-form.pdf");
        Resource counterclaimForm = new ClassPathResource("forms/ONC-003-counterclaim.pdf");

        BlankClaimPackFormsProvider provider = new BlankClaimPackFormsProvider(defenceForm, counterclaimForm);

        byte[] defenceFormBytes = provider.getBlankResponseForm();
        byte[] counterclaimFormBytes = provider.getBlankCounterclaimForm();

        assertThat(defenceFormBytes).isNotEmpty();
        assertThat(counterclaimFormBytes).isNotEmpty();

        try (PDDocument defenceDoc = Loader.loadPDF(defenceFormBytes)) {
            assertThat(defenceDoc.getNumberOfPages()).isEqualTo(16);
        }

        try (PDDocument counterclaimDoc = Loader.loadPDF(counterclaimFormBytes)) {
            assertThat(counterclaimDoc.getNumberOfPages()).isEqualTo(4);
        }
    }

    @Test
    @DisplayName("Returns cached byte arrays on subsequent calls")
    void shouldReturnCachedByteArrays() {
        byte[] defenceBytes = "defence-form-content".getBytes();
        byte[] counterclaimBytes = "counterclaim-form-content".getBytes();

        BlankClaimPackFormsProvider provider = new BlankClaimPackFormsProvider(
            new ByteArrayResource(defenceBytes),
            new ByteArrayResource(counterclaimBytes)
        );

        assertThat(provider.getBlankResponseForm()).isSameAs(provider.getBlankResponseForm());
        assertThat(provider.getBlankCounterclaimForm()).isSameAs(provider.getBlankCounterclaimForm());
        assertThat(provider.getBlankResponseForm()).containsExactly(defenceBytes);
        assertThat(provider.getBlankCounterclaimForm()).containsExactly(counterclaimBytes);
    }

    @Test
    @DisplayName("Throws IllegalStateException when loading resource fails")
    void shouldThrowIllegalStateExceptionWhenResourceFailsToLoad() throws IOException {
        Resource failingResource = mock(Resource.class);
        when(failingResource.getInputStream()).thenThrow(new IOException("Disk read error"));

        Resource validResource = new ByteArrayResource("valid".getBytes());

        assertThatThrownBy(() -> new BlankClaimPackFormsProvider(failingResource, validResource))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Failed to load blank defence form (ONC 002R)");
    }
}
