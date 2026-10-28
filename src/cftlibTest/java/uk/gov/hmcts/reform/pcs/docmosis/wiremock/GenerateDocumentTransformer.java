package uk.gov.hmcts.reform.pcs.docmosis.wiremock;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.extension.ResponseDefinitionTransformerV2;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.docmosis.RenderRequest;
import uk.gov.hmcts.reform.pcs.docmosis.RenderService;

import java.io.ByteArrayOutputStream;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class GenerateDocumentTransformer implements ResponseDefinitionTransformerV2 {

    public static final String NAME = "generate-placeholder-document";

    private static final TypeReference<Map<String, Object>> MAP_TYPE_REFERENCE = new TypeReference<>() {};

    private final RenderService renderService;
    private final ObjectMapper objectMapper;

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public ResponseDefinition transform(ServeEvent serveEvent) {
        LoggedRequest loggedRequest = serveEvent.getRequest();

        Map<String, Object> data = getTemplateData(serveEvent);

        RenderRequest renderRequest = RenderRequest.builder()
            .templateName(loggedRequest.getPart("templateName").getBody().asString())
            .outputName(loggedRequest.getPart("outputName").getBody().asString())
            .data(data)
            .build();

        ByteArrayOutputStream byteArrayOutputStream = renderService.renderTemplate(renderRequest);

        return new ResponseDefinitionBuilder()
            .withStatus(200)
            .withBody(byteArrayOutputStream.toByteArray())
            .build();
    }

    @Override
    public boolean applyGlobally() {
        return false;
    }

    private Map<String, Object> getTemplateData(ServeEvent serveEvent) {
        String dataAsString = serveEvent.getRequest().getPart("data").getBody().asString();
        try {
            return objectMapper.readValue(dataAsString, MAP_TYPE_REFERENCE);
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse render data - ignoring for placeholder generated document", e);
            return Map.of();
        }
    }
}
