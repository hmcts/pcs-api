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

    private static final String DEFENCE_FORM_WELSH = "forms/ONC-002RW-defence-form-welsh.pdf";
    private static final String COUNTERCLAIM_FORM_WELSH = "forms/ONC-003-counterclaim-welsh.pdf";

    @Test
    @DisplayName("Loads real classpath form resources and validates that PDFBox can parse both documents")
    void shouldLoadClasspathFormResourcesSuccessfully() throws IOException {
        Resource defenceForm = new ClassPathResource("forms/ONC-002R-defence-form.pdf");
        Resource counterclaimForm = new ClassPathResource("forms/ONC-003-counterclaim.pdf");

        BlankClaimPackFormsProvider provider = new BlankClaimPackFormsProvider(defenceForm, counterclaimForm,
            new ClassPathResource(DEFENCE_FORM_WELSH), new ClassPathResource(COUNTERCLAIM_FORM_WELSH));

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
    @DisplayName("Loads the real Welsh classpath forms and validates that PDFBox can parse both documents")
    void shouldLoadTheWelshClasspathForms() throws IOException {
        BlankClaimPackFormsProvider provider = new BlankClaimPackFormsProvider(
            new ClassPathResource("forms/ONC-002R-defence-form.pdf"),
            new ClassPathResource("forms/ONC-003-counterclaim.pdf"),
            new ClassPathResource(DEFENCE_FORM_WELSH),
            new ClassPathResource(COUNTERCLAIM_FORM_WELSH));

        try (PDDocument defenceDoc = Loader.loadPDF(provider.getBlankResponseFormWelsh())) {
            assertThat(defenceDoc.getNumberOfPages()).isEqualTo(15);
        }

        try (PDDocument counterclaimDoc = Loader.loadPDF(provider.getBlankCounterclaimFormWelsh())) {
            assertThat(counterclaimDoc.getNumberOfPages()).isEqualTo(4);
        }
    }

    @Test
    @DisplayName("Returns cached byte arrays on subsequent calls")
    void shouldReturnCachedByteArrays() {
        byte[] defenceBytes = "defence-form-content".getBytes();
        byte[] counterclaimBytes = "counterclaim-form-content".getBytes();
        byte[] defenceWelshBytes = "defence-form-welsh-content".getBytes();
        byte[] counterclaimWelshBytes = "counterclaim-form-welsh-content".getBytes();

        BlankClaimPackFormsProvider provider = new BlankClaimPackFormsProvider(
            new ByteArrayResource(defenceBytes),
            new ByteArrayResource(counterclaimBytes),
            new ByteArrayResource(defenceWelshBytes),
            new ByteArrayResource(counterclaimWelshBytes)
        );

        assertThat(provider.getBlankResponseForm()).isSameAs(provider.getBlankResponseForm());
        assertThat(provider.getBlankCounterclaimForm()).isSameAs(provider.getBlankCounterclaimForm());
        assertThat(provider.getBlankResponseFormWelsh()).isSameAs(provider.getBlankResponseFormWelsh());
        assertThat(provider.getBlankCounterclaimFormWelsh()).isSameAs(provider.getBlankCounterclaimFormWelsh());
        assertThat(provider.getBlankResponseForm()).containsExactly(defenceBytes);
        assertThat(provider.getBlankCounterclaimForm()).containsExactly(counterclaimBytes);
        assertThat(provider.getBlankResponseFormWelsh()).containsExactly(defenceWelshBytes);
        assertThat(provider.getBlankCounterclaimFormWelsh()).containsExactly(counterclaimWelshBytes);
    }

    @Test
    @DisplayName("Throws IllegalStateException when loading resource fails")
    void shouldThrowIllegalStateExceptionWhenResourceFailsToLoad() throws IOException {
        Resource failingResource = mock(Resource.class);
        when(failingResource.getInputStream()).thenThrow(new IOException("Disk read error"));

        Resource validResource = new ByteArrayResource("valid".getBytes());

        assertThatThrownBy(
            () -> new BlankClaimPackFormsProvider(failingResource, validResource, validResource, validResource))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Failed to load blank defence form (ONC 002R)");
    }

    @Test
    @DisplayName("Names the Welsh defence form when it fails to load")
    void shouldNameTheWelshDefenceFormWhenItFailsToLoad() throws IOException {
        Resource failingResource = mock(Resource.class);
        when(failingResource.getInputStream()).thenThrow(new IOException("Disk read error"));
        Resource validResource = new ByteArrayResource("valid".getBytes());

        assertThatThrownBy(
            () -> new BlankClaimPackFormsProvider(validResource, validResource, failingResource, validResource))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Failed to load blank Welsh defence form (ONC 002RW)");
    }

    @Test
    @DisplayName("Names the Welsh counterclaim form when it fails to load")
    void shouldNameTheWelshCounterclaimFormWhenItFailsToLoad() throws IOException {
        Resource failingResource = mock(Resource.class);
        when(failingResource.getInputStream()).thenThrow(new IOException("Disk read error"));
        Resource validResource = new ByteArrayResource("valid".getBytes());

        assertThatThrownBy(
            () -> new BlankClaimPackFormsProvider(validResource, validResource, validResource, failingResource))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Failed to load blank Welsh counterclaim form (ONC 003)");
    }
}
