package uk.gov.hmcts.reform.pcs.ccd.service.document;

import uk.gov.hmcts.ccd.sdk.type.Document;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.AdditionalDocument;
import uk.gov.hmcts.reform.pcs.ccd.domain.NoticeServedDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentArrearsSection;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrantofrestitution.EvidenceOfDefendants;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrantofrestitution.WarrantOfRestitutionDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.OccupationLicenceDetailsWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.WalesDocuments;
import uk.gov.hmcts.reform.pcs.ccd.event.EventId;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * The files a user uploaded into a draft, read from the upload fields only. A draft also holds documents copied
 * from the case view by mid-event saves, and those are not uploads.
 */
public final class DraftUploads {

    private DraftUploads() {
    }

    public static List<Document> forEvent(EventId eventId, PCSCase draft) {
        if (draft == null) {
            return List.of();
        }
        return switch (eventId) {
            case resumePossessionClaim -> claimUploads(draft);
            case enforceTheOrder -> enforcementUploads(draft);
            default -> List.of();
        };
    }

    /** The claim upload fields; the same set DocumentService copies into the case on submit. */
    public static List<Document> claimUploads(PCSCase draft) {
        List<Document> documents = new ArrayList<>();
        Optional.ofNullable(draft.getAdditionalDocuments()).stream()
            .flatMap(Collection::stream)
            .map(ListValue::getValue)
            .filter(Objects::nonNull)
            .map(AdditionalDocument::getDocument)
            .forEach(documents::add);
        documents.addAll(values(draft.getRentArrears(), RentArrearsSection::getStatementDocuments));
        documents.addAll(values(draft.getTenancyLicenceDetails(), TenancyLicenceDetails::getTenancyLicenceDocuments));
        documents.addAll(values(draft.getOccupationLicenceDetailsWales(),
                                OccupationLicenceDetailsWales::getLicenceDocuments));
        documents.addAll(values(draft.getNoticeServedDetails(), NoticeServedDetails::getDocuments));
        documents.addAll(values(draft.getRequiredDocumentsWales(), WalesDocuments::getEnergyPerformance));
        documents.addAll(values(draft.getRequiredDocumentsWales(), WalesDocuments::getGasSafetyReport));
        documents.addAll(values(draft.getRequiredDocumentsWales(), WalesDocuments::getElectricalInstallation));
        return withUrl(documents);
    }

    public static List<Document> enforcementUploads(PCSCase draft) {
        return withUrl(Optional.ofNullable(draft.getEnforcementOrder())
            .map(EnforcementOrder::getWarrantOfRestitutionDetails)
            .map(WarrantOfRestitutionDetails::getAdditionalDocuments)
            .stream()
            .flatMap(Collection::stream)
            .map(ListValue::getValue)
            .filter(Objects::nonNull)
            .map(EvidenceOfDefendants::getDocument)
            .toList());
    }

    private static <T> List<Document> values(T section, Function<T, List<ListValue<Document>>> getter) {
        return Optional.ofNullable(section)
            .map(getter)
            .stream()
            .flatMap(Collection::stream)
            .map(ListValue::getValue)
            .toList();
    }

    private static List<Document> withUrl(List<Document> documents) {
        return documents.stream()
            .filter(Objects::nonNull)
            .filter(document -> document.getUrl() != null)
            .toList();
    }
}
