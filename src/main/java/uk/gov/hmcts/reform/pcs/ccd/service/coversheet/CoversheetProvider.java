package uk.gov.hmcts.reform.pcs.ccd.service.coversheet;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.service.bulkprint.LetterDocumentFetcher;
import uk.gov.hmcts.reform.pcs.ccd.service.document.DocumentIdExtractor;
import uk.gov.hmcts.reform.pcs.document.model.coversheet.CoversheetPayload;

/**
 * Renders the address coversheet for a recipient and returns its PDF bytes: build the payload, generate the
 * PDF via Docmosis, then fetch its bytes from CDAM. The bytes are merged as the first page of the letter by
 * {@link PdfMerger}.
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

    public byte[] render(String recipientName, AddressUK address, String caseReference) {
        CoversheetPayload payload = coversheetPayloadBuilder.build(recipientName, address, caseReference);
        String coversheetUrl = coversheetDocumentGenerator.generate(payload);
        return letterDocumentFetcher.fetchBytes(documentIdExtractor.extractDocumentId(coversheetUrl));
    }
}
