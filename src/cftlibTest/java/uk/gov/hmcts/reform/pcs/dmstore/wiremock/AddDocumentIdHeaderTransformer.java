package uk.gov.hmcts.reform.pcs.dmstore.wiremock;

import com.github.tomakehurst.wiremock.extension.requestfilter.RequestFilterAction;
import com.github.tomakehurst.wiremock.extension.requestfilter.RequestWrapper;
import com.github.tomakehurst.wiremock.extension.requestfilter.StubRequestFilterV2;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;

import java.util.UUID;

public class AddDocumentIdHeaderTransformer implements StubRequestFilterV2 {

    public static final String NAME = "add-document-id-header";
    public static final String DOCUMENT_ID_HEADER = "X-document-id";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public RequestFilterAction filter(Request request, ServeEvent serveEvent) {
        UUID documentId = UUID.randomUUID();
        Request wrappedRequest = RequestWrapper.create()
            .addHeader(DOCUMENT_ID_HEADER, documentId.toString())
            .wrap(request);

        return RequestFilterAction.continueWith(wrappedRequest);
    }

}
