package uk.gov.hmcts.reform.pcs.ccd.renderer;

import io.pebbletemplates.pebble.PebbleEngine;
import io.pebbletemplates.pebble.template.PebbleTemplate;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.config.PebbleConfiguration;
import uk.gov.hmcts.reform.pcs.exception.TemplateRenderingException;

import java.io.IOException;
import java.io.Writer;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


class OrdersTableRendererTest {

    private static final String DOCUMENT_URL =
        "http://dm-store/documents/11111111-1111-1111-1111-111111111111";
    private static final String XUI_DOCUMENT_URL =
        "/documents/11111111-1111-1111-1111-111111111111/binary";
    private static final String BINARY_URL = DOCUMENT_URL + "/binary";
    private static final LocalDateTime UPLOADED_DATE =
        LocalDateTime.of(2026, 10, 6, 12, 30);

    private final OrdersTableRenderer underTest =
        new OrdersTableRenderer(new PebbleConfiguration().pebbleEngine());

    @Test
    void shouldRenderPossessionOrderWithFormattedDateAndDocumentLink() {
        DocumentEntity document = document("Possession order.pdf", BINARY_URL);

        String html = underTest.render(document, UPLOADED_DATE);

        assertThat(html)
            .contains("<td class=\"govuk-table__cell\">Possession order</td>")
            .contains("<td class=\"govuk-table__cell\">06 October 2026</td>")
            .contains("href=\"" + XUI_DOCUMENT_URL + "\">Possession order.pdf</a>")
            .doesNotContain("Not available", "Document unavailable");
    }

    @Test
    void shouldRenderNotAvailableWhenUploadedDateIsMissing() {
        DocumentEntity document = document("Possession order.pdf", BINARY_URL);

        String html = underTest.render(document, null);

        assertThat(html)
            .contains("<td class=\"govuk-table__cell\">Not available</td>")
            .contains("href=\"" + XUI_DOCUMENT_URL + "\">Possession order.pdf</a>");
    }

    @Test
    void shouldWrapIOExceptionWhenTemplateEvaluationFails() throws IOException {
        PebbleEngine pebbleEngine = mock(PebbleEngine.class);
        PebbleTemplate template = mock(PebbleTemplate.class);
        IOException cause = new IOException("Unable to write rendered template");
        when(pebbleEngine.getTemplate("ordersTable")).thenReturn(template);
        doThrow(cause).when(template).evaluate(any(Writer.class), anyMap());
        OrdersTableRenderer failingRenderer = new OrdersTableRenderer(pebbleEngine);
        DocumentEntity document = document("Possession order.pdf", BINARY_URL);

        assertThatThrownBy(() -> failingRenderer.render(document, UPLOADED_DATE))
            .isInstanceOf(TemplateRenderingException.class)
            .hasMessage("Failed to render orders table")
            .hasCause(cause);
    }


    private static DocumentEntity document(String fileName, String binaryUrl) {
        return DocumentEntity.builder()
            .fileName(fileName)
            .url(DOCUMENT_URL)
            .binaryUrl(binaryUrl)
            .build();
    }

}
