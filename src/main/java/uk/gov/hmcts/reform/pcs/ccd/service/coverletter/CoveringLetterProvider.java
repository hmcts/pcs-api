package uk.gov.hmcts.reform.pcs.ccd.service.coverletter;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseNameFormatter;
import uk.gov.hmcts.reform.pcs.ccd.service.bulkprint.LetterDocumentFetcher;
import uk.gov.hmcts.reform.pcs.ccd.service.document.DocumentIdExtractor;
import uk.gov.hmcts.reform.pcs.ccd.service.form.PartyDisplayMapper;
import uk.gov.hmcts.reform.pcs.document.model.coverletter.CoveringLetterPayload;
import uk.gov.hmcts.reform.pcs.location.model.CourtVenue;
import uk.gov.hmcts.reform.pcs.location.service.LocationReferenceService;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import static uk.gov.hmcts.reform.pcs.ccd.service.form.PartyDisplayMapper.partiesByRole;

@Service
public class CoveringLetterProvider {

    private final CoveringLetterPayloadBuilder coveringLetterPayloadBuilder;
    private final CoveringLetterDocumentGenerator coveringLetterDocumentGenerator;
    private final DocumentIdExtractor documentIdExtractor;
    private final LetterDocumentFetcher letterDocumentFetcher;
    private final LocationReferenceService locationReferenceService;
    private final CaseNameFormatter caseNameFormatter;
    private final Clock ukClock;

    public CoveringLetterProvider(CoveringLetterPayloadBuilder coveringLetterPayloadBuilder,
                                  CoveringLetterDocumentGenerator coveringLetterDocumentGenerator,
                                  DocumentIdExtractor documentIdExtractor,
                                  LetterDocumentFetcher letterDocumentFetcher,
                                  LocationReferenceService locationReferenceService,
                                  CaseNameFormatter caseNameFormatter,
                                  @Qualifier("ukClock") Clock ukClock) {
        this.coveringLetterPayloadBuilder = coveringLetterPayloadBuilder;
        this.coveringLetterDocumentGenerator = coveringLetterDocumentGenerator;
        this.documentIdExtractor = documentIdExtractor;
        this.letterDocumentFetcher = letterDocumentFetcher;
        this.locationReferenceService = locationReferenceService;
        this.caseNameFormatter = caseNameFormatter;
        this.ukClock = ukClock;
    }

    public byte[] render(PcsCaseEntity pcsCase, PartyEntity recipient, String recipientName,
                         AddressUK address, String caseReference) {
        CourtVenue court = resolveOwningCourt(pcsCase);
        CoveringLetterPayload payload = coveringLetterPayloadBuilder.build(
            recipient, recipientName, address, caseReference, caseName(pcsCase), court, LocalDate.now(ukClock));
        String letterUrl = coveringLetterDocumentGenerator.generate(payload);
        return letterDocumentFetcher.fetchBytes(documentIdExtractor.extractDocumentId(letterUrl));
    }

    private CourtVenue resolveOwningCourt(PcsCaseEntity pcsCase) {
        Integer epimsId = pcsCase.getCaseManagementLocation();
        if (epimsId == null) {
            throw new IllegalStateException(
                "No case management location set for case " + pcsCase.getCaseReference()
                    + "; cannot resolve respond-by-post court");
        }
        List<CourtVenue> venues = locationReferenceService.getCourtVenues(List.of(epimsId));
        if (venues == null || venues.isEmpty()) {
            throw new IllegalStateException(
                "No court venue found for epimsId " + epimsId + " on case " + pcsCase.getCaseReference()
                    + "; cannot resolve respond-by-post court");
        }
        return venues.getFirst();
    }

    private String caseName(PcsCaseEntity pcsCase) {
        if (pcsCase.getClaims().isEmpty()) {
            return null;
        }
        ClaimEntity claim = pcsCase.getClaims().getFirst();
        return caseNameFormatter.formatCaseName(domainParties(claim, PartyRole.CLAIMANT),
            domainParties(claim, PartyRole.DEFENDANT));
    }

    private List<Party> domainParties(ClaimEntity claim, PartyRole role) {
        return partiesByRole(claim, role).stream()
            .map(PartyDisplayMapper::toDomainParty)
            .toList();
    }
}
