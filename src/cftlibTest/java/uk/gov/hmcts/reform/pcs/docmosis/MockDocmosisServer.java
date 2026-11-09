package uk.gov.hmcts.reform.pcs.docmosis;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.docmosis.wiremock.GenerateDocumentTransformer;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;

@Component
@RequiredArgsConstructor
public class MockDocmosisServer {

    private static final String RENDER_URL = "/api/render";

    private final WireMockServer wireMockServer;
    private final RenderService renderService;

    @PostConstruct
    public void stubEndpoints() {
        stubRenderEndpoint();
    }

    private void stubRenderEndpoint() {
        wireMockServer
            .stubFor(WireMock.post(RENDER_URL)
                         .persistent(false)
                         .willReturn(aResponse()
                                         .withTransformers(GenerateDocumentTransformer.NAME)
                         ));
    }

}
