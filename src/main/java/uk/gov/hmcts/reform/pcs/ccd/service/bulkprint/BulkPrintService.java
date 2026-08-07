package uk.gov.hmcts.reform.pcs.ccd.service.bulkprint;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.service.coversheet.CoversheetProvider;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseReferenceFormatter;
import uk.gov.hmcts.reform.pcs.exception.ErrorCode;
import uk.gov.hmcts.reform.pcs.exception.RedactionContext;
import uk.gov.hmcts.reform.sendletter.api.SendLetterApi;
import uk.gov.hmcts.reform.sendletter.api.model.v3.Document;
import uk.gov.hmcts.reform.sendletter.api.model.v3.LetterV3;

import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Assembles one pack — coversheet then documents, merged into a single PDF by {@link PdfMerger} — into a
 * Send Letter letter and posts it. Recording the outcome is the caller's job.
 */
@Service
public class BulkPrintService {

    private static final int SINGLE_COPY = 1;
    private static final String RECIPIENTS = "recipients";
    private static final String CASE_REFERENCE = "caseReference";

    private final CoversheetProvider coversheetProvider;
    private final LetterDocumentFetcher letterDocumentFetcher;
    private final PdfMerger pdfMerger;
    private final SendLetterApi sendLetterApi;
    private final AuthTokenGenerator authTokenGenerator;
    private final CaseReferenceFormatter caseReferenceFormatter;

    public BulkPrintService(CoversheetProvider coversheetProvider,
                            LetterDocumentFetcher letterDocumentFetcher,
                            PdfMerger pdfMerger,
                            SendLetterApi sendLetterApi,
                            AuthTokenGenerator authTokenGenerator,
                            CaseReferenceFormatter caseReferenceFormatter) {
        this.coversheetProvider = coversheetProvider;
        this.letterDocumentFetcher = letterDocumentFetcher;
        this.pdfMerger = pdfMerger;
        this.sendLetterApi = sendLetterApi;
        this.authTokenGenerator = authTokenGenerator;
        this.caseReferenceFormatter = caseReferenceFormatter;
    }

    public UUID sendPack(PcsCaseEntity pcsCase, PartyEntity recipient, LetterType letterType,
                         String recipientName, AddressUK address, List<DocumentEntity> documents) {
        requirePostalAddress(address, recipient);
        String caseReference = caseReferenceFormatter.formatCaseReferenceWithDashes(pcsCase.getCaseReference());

        List<byte[]> pdfs = new ArrayList<>();
        pdfs.add(coversheetProvider.render(recipientName, address, caseReference));
        documents.forEach(document -> pdfs.add(letterDocumentFetcher.fetchBytes(document.getDocumentId())));
        Document mergedLetter = new Document(Base64.getEncoder().encodeToString(pdfMerger.merge(pdfs)), SINGLE_COPY);

        LetterV3 letter = new LetterV3(letterType.getCode(), List.of(mergedLetter),
            additionalData(caseReference, recipientName, address));
        return sendLetterApi.sendLetter(authTokenGenerator.generate(), letter).letterId;
    }

    private void requirePostalAddress(AddressUK address, PartyEntity recipient) {
        if (address == null || StringUtils.isBlank(address.getAddressLine1())) {
            throw new MissingPostalAddressException(ErrorCode.MISSING_POSTAL_ADDRESS, RedactionContext.builder()
                .value("No postal address for party ", recipient.getId()).build());
        }
    }

    private Map<String, Object> additionalData(String caseReference, String recipientName, AddressUK address) {
        Map<String, Object> additionalData = new HashMap<>();
        additionalData.put(CASE_REFERENCE, caseReference);
        additionalData.put(RECIPIENTS, recipients(recipientName, address));
        return additionalData;
    }

    private List<String> recipients(String recipientName, AddressUK address) {
        List<String> recipients = new ArrayList<>();
        recipients.add(recipientName);
        addIfPresent(recipients, address.getAddressLine1());
        addIfPresent(recipients, address.getAddressLine2());
        addIfPresent(recipients, address.getAddressLine3());
        addIfPresent(recipients, address.getPostTown());
        addIfPresent(recipients, address.getPostCode());
        return recipients;
    }

    private void addIfPresent(List<String> recipients, String value) {
        if (StringUtils.isNotBlank(value)) {
            recipients.add(value);
        }
    }
}
