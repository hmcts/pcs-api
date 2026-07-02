package uk.gov.hmcts.reform.pcs.ccd.service.bulkprint;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.service.document.DocumentIdExtractor;
import uk.gov.hmcts.reform.pcs.document.model.coversheet.CoversheetPayload;
import uk.gov.hmcts.reform.sendletter.api.model.v3.Document;

/**
 * Renders the address coversheet for a recipient and returns it as a Send Letter {@link Document}:
 * build the payload, generate the PDF via Docmosis, then fetch its bytes from CDAM.
 */
@Service
public class CoversheetProvider {

    private final CoversheetPayloadBuilder coversheetPayloadBuilder;
    private final CoversheetDocumentGenerator coversheetDocumentGenerator;
    private final DocumentIdExtractor documentIdExtractor;
    private final LetterDocumentFetcher letterDocumentFetcher;

    public CoversheetProvider(CoversheetPayloadBuilder coversheetPayloadBuilder,
                              CoversheetDocumentGenerator coversheetDocumentGenerator,
                              DocumentIdExtractor documentIdExtractor,
                              LetterDocumentFetcher letterDocumentFetcher) {
        this.coversheetPayloadBuilder = coversheetPayloadBuilder;
        this.coversheetDocumentGenerator = coversheetDocumentGenerator;
        this.documentIdExtractor = documentIdExtractor;
        this.letterDocumentFetcher = letterDocumentFetcher;
    }

    public Document render(String recipientName, AddressUK address, String caseReference) {
        CoversheetPayload payload = coversheetPayloadBuilder.build(recipientName, address, caseReference);
        String coversheetUrl = coversheetDocumentGenerator.generate(payload);
        return letterDocumentFetcher.fetch(documentIdExtractor.extractDocumentId(coversheetUrl));
    }
}
