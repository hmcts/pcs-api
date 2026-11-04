package uk.gov.hmcts.reform.pcs.dmstore.wiremock;

import com.github.tomakehurst.wiremock.extension.ResponseTransformerV2;
import com.github.tomakehurst.wiremock.http.Response;
import com.github.tomakehurst.wiremock.http.multipart.FileItemPartAdapter;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.exception.CustomTransformerException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import static uk.gov.hmcts.reform.pcs.dmstore.wiremock.AddDocumentIdHeaderTransformer.DOCUMENT_ID_HEADER;

@Component
public class SaveDocumentFileTransformer implements ResponseTransformerV2 {

    public static final String NAME = "save-document-file-transformer";

    private final Path documentDirectory;

    public SaveDocumentFileTransformer(@Qualifier("wiremockWorkingDirectory") Path wiremockWorkingDirectory) {
        documentDirectory = wiremockWorkingDirectory.resolve("__files/dm-store");

        try {
            Files.createDirectories(documentDirectory);
        } catch (IOException e) {
            throw new CustomTransformerException("Unable to create wiremock documents directory", e);
        }
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Response transform(Response response, ServeEvent serveEvent) {
        LoggedRequest loggedRequest = serveEvent.getRequest();
        String documentId = loggedRequest.getHeader(DOCUMENT_ID_HEADER);
        Objects.requireNonNull(documentId, "Request should have a %s header"
            .formatted(DOCUMENT_ID_HEADER));

        FileItemPartAdapter files = (FileItemPartAdapter) serveEvent.getRequest().getPart("files");

        try {
            Files.write(documentDirectory.resolve("%s.pdf".formatted(documentId)), files.getBody().asBytes());
        } catch (IOException e) {
            throw new CustomTransformerException("Unable to write document file", e);
        }

        return response;
    }

    @Override
    public boolean applyGlobally() {
        return false;
    }
}
