package uk.gov.hmcts.reform.pcs.dmstore.wiremock;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.extension.ResponseDefinitionTransformerV2;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;

import java.util.Objects;

public class ServeDocumentFileTransformer implements ResponseDefinitionTransformerV2 {

    public static final String NAME = "serve-document-file-transformer";

    @Override
    public ResponseDefinition transform(ServeEvent serveEvent) {
        String documentId = serveEvent.getRequest().getPathParameters().get("documentId");
        Objects.requireNonNull(documentId, "Request urlPathTemplate should have a documentId path parameter");

        return new ResponseDefinitionBuilder()
            .withStatus(200)
            .withBodyFile("dm-store/%s.pdf".formatted(documentId))
            .build();
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public boolean applyGlobally() {
        return false;
    }
}
