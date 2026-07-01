package uk.gov.hmcts.reform.pcs.ccd.service.bulkprint;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.ccd.document.am.feign.CaseDocumentClientApi;
import uk.gov.hmcts.reform.pcs.ccd.domain.claimactivitylog.ClaimActivityType;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.AccessCodeActivityLogService;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseReferenceFormatter;
import uk.gov.hmcts.reform.pcs.ccd.service.document.DocumentIdExtractor;
import uk.gov.hmcts.reform.pcs.document.model.coversheet.CoversheetPayload;
import uk.gov.hmcts.reform.pcs.security.IdamTokenProvider;
import uk.gov.hmcts.reform.sendletter.api.SendLetterApi;
import uk.gov.hmcts.reform.sendletter.api.SendLetterResponse;
import uk.gov.hmcts.reform.sendletter.api.model.v3.Document;
import uk.gov.hmcts.reform.sendletter.api.model.v3.LetterV3;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Posts one pack to a recipient via the Send Letter Service: renders the address coversheet, prepends it,
 * fetches each document's bytes from CDAM, sends, and records the outcome. Generic across packs — the caller
 * resolves the recipient's name and address.
 */
@Service
@Slf4j
public class BulkPrintService {

    private static final String RECIPIENTS = "recipients";
    private static final String CASE_REFERENCE = "caseReference";
    private static final int SINGLE_COPY = 1;

    private final CoversheetPayloadBuilder coversheetPayloadBuilder;
    private final CoversheetDocumentGenerator coversheetDocumentGenerator;
    private final CaseDocumentClientApi caseDocumentClientApi;
    private final SendLetterApi sendLetterApi;
    private final AuthTokenGenerator authTokenGenerator;
    private final IdamTokenProvider systemUpdateUserTokenProvider;
    private final AccessCodeActivityLogService accessCodeActivityLogService;
    private final DocumentIdExtractor documentIdExtractor;
    private final CaseReferenceFormatter caseReferenceFormatter;

    public BulkPrintService(
        CoversheetPayloadBuilder coversheetPayloadBuilder,
        CoversheetDocumentGenerator coversheetDocumentGenerator,
        CaseDocumentClientApi caseDocumentClientApi,
        SendLetterApi sendLetterApi,
        AuthTokenGenerator authTokenGenerator,
        @Qualifier("systemUpdateUserTokenProvider") IdamTokenProvider systemUpdateUserTokenProvider,
        AccessCodeActivityLogService accessCodeActivityLogService,
        DocumentIdExtractor documentIdExtractor,
        CaseReferenceFormatter caseReferenceFormatter
    ) {
        this.coversheetPayloadBuilder = coversheetPayloadBuilder;
        this.coversheetDocumentGenerator = coversheetDocumentGenerator;
        this.caseDocumentClientApi = caseDocumentClientApi;
        this.sendLetterApi = sendLetterApi;
        this.authTokenGenerator = authTokenGenerator;
        this.systemUpdateUserTokenProvider = systemUpdateUserTokenProvider;
        this.accessCodeActivityLogService = accessCodeActivityLogService;
        this.documentIdExtractor = documentIdExtractor;
        this.caseReferenceFormatter = caseReferenceFormatter;
    }

    public UUID sendPack(PcsCaseEntity pcsCase, PartyEntity recipient, ClaimActivityType packSentType,
                         LetterType letterType, String recipientName, AddressUK address,
                         List<DocumentEntity> documents) {
        requirePostalAddress(address, recipient);
        String caseReference = caseReferenceFormatter.formatCaseReferenceWithDashes(pcsCase.getCaseReference());

        List<Document> letterDocuments = new ArrayList<>();
        letterDocuments.add(renderCoversheet(recipientName, address, caseReference));
        documents.forEach(document -> letterDocuments.add(fetchLetterDocument(document.getDocumentId())));

        LetterV3 letter = new LetterV3(letterType.getCode(), letterDocuments,
            additionalData(caseReference, recipientName, address));
        SendLetterResponse response = sendLetterApi.sendLetter(authTokenGenerator.generate(), letter);

        accessCodeActivityLogService.logSuccess(pcsCase, recipient, packSentType);
        recordSent(caseReference, recipient, letterType, response.letterId);
        return response.letterId;
    }

    private void requirePostalAddress(AddressUK address, PartyEntity recipient) {
        if (address == null || StringUtils.isBlank(address.getAddressLine1())) {
            throw new MissingPostalAddressException("No postal address for party " + recipient.getId());
        }
    }

    private Document renderCoversheet(String recipientName, AddressUK address, String caseReference) {
        CoversheetPayload payload = coversheetPayloadBuilder.build(recipientName, address, caseReference);
        String coversheetUrl = coversheetDocumentGenerator.generate(payload);
        return fetchLetterDocument(documentIdExtractor.extractDocumentId(coversheetUrl));
    }

    private Document fetchLetterDocument(UUID documentId) {
        Resource resource = caseDocumentClientApi.getDocumentBinary(
            systemUpdateUserTokenProvider.getAuthToken(),
            authTokenGenerator.generate(),
            documentId
        ).getBody();
        return new Document(Base64.getEncoder().encodeToString(readAllBytes(resource)), SINGLE_COPY);
    }

    private byte[] readAllBytes(Resource resource) {
        try {
            return resource.getInputStream().readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read document content for bulk print", e);
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

    private void recordSent(String caseReference, PartyEntity recipient, LetterType letterType, UUID letterId) {
        MDC.put("letterId", String.valueOf(letterId));
        log.info("Bulk print letter sent - case: {}, party: {}, letterType: {}, letterId: {}",
            caseReference, recipient.getId(), letterType, letterId);
    }
}
