package uk.gov.hmcts.reform.pcs.ccd.service.document;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.domain.CaseFileCategory;
import uk.gov.hmcts.reform.pcs.ccd.domain.legalrepdocumentupload.ClaimantDocumentType;
import uk.gov.hmcts.reform.pcs.ccd.domain.legalrepdocumentupload.DefendantDocumentType;
import uk.gov.hmcts.reform.pcs.ccd.domain.legalrepdocumentupload.wales.ClaimantDocumentTypeWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.legalrepdocumentupload.wales.DefendantDocumentTypeWales;

import java.util.Optional;

@Component
@SuppressWarnings("DuplicatedCode")
public class DocumentCategoryMapper {

    public Optional<CaseFileCategory> mapToCategory(ClaimantDocumentType claimantDocumentType) {
        if (claimantDocumentType == null) {
            return Optional.empty();
        }

        return switch (claimantDocumentType) {
            case WITNESS_STATEMENT,
                 CERTIFICATE_OF_SERVICE,
                 CORRESPONDENCE_FROM_DEFENDANT,
                 CORRESPONDENCE_FROM_CLAIMANT,
                 PHOTOGRAPHIC_EVIDENCE,
                 INSPECTION_OR_REPORT ->
                Optional.of(CaseFileCategory.EVIDENCE);
            case RENT_STATEMENT,
                 TENANCY_AGREEMENT,
                 POSSESSION_NOTICE ->
                Optional.of(CaseFileCategory.PROPERTY_DOCUMENTS);
            case NOTICE_FOR_SERVICE_OUT_OF_JURISDICTION ->
                Optional.of(CaseFileCategory.STATEMENTS_OF_CASE);
            case CERTIFICATE_OF_SUITABILITY_AS_LF,
                 LEGAL_AID_CERTIFICATE ->
                Optional.of(CaseFileCategory.CORRESPONDENCE);
            case OTHER ->
                Optional.of(CaseFileCategory.UNCATEGORISED_DOCUMENTS);
        };
    }

    public Optional<CaseFileCategory> mapToCategory(ClaimantDocumentTypeWales claimantDocumentTypeWales) {
        if (claimantDocumentTypeWales == null) {
            return Optional.empty();
        }

        return switch (claimantDocumentTypeWales) {
            case WITNESS_STATEMENT,
                 CERTIFICATE_OF_SERVICE,
                 CORRESPONDENCE_FROM_DEFENDANT,
                 CORRESPONDENCE_FROM_CLAIMANT,
                 PHOTOGRAPHIC_EVIDENCE,
                 INSPECTION_OR_REPORT ->
                Optional.of(CaseFileCategory.EVIDENCE);
            case RENT_STATEMENT,
                 OCCUPATION_LICENCE,
                 ENERGY_PERFORMANCE_CERTIFICATE,
                 GAS_SAFETY_CERTIFICATE,
                 EICR_REPORT,
                 POSSESSION_NOTICE ->
                Optional.of(CaseFileCategory.PROPERTY_DOCUMENTS);
            case NOTICE_FOR_SERVICE_OUT_OF_JURISDICTION ->
                Optional.of(CaseFileCategory.STATEMENTS_OF_CASE);
            case CERTIFICATE_OF_SUITABILITY_AS_LF,
                 LEGAL_AID_CERTIFICATE ->
                Optional.of(CaseFileCategory.CORRESPONDENCE);
            case OTHER ->
                Optional.of(CaseFileCategory.UNCATEGORISED_DOCUMENTS);
        };
    }

    public Optional<CaseFileCategory> mapToCategory(DefendantDocumentType defendantDocumentType) {
        if (defendantDocumentType == null) {
            return Optional.empty();
        }

        return switch (defendantDocumentType) {
            case RENT_STATEMENT,
                 TENANCY_AGREEMENT ->
                Optional.of(CaseFileCategory.PROPERTY_DOCUMENTS);
            case CORRESPONDENCE_FROM_DEFENDANT,
                 CORRESPONDENCE_FROM_CLAIMANT,
                 PHOTOGRAPHIC_EVIDENCE,
                 WITNESS_STATEMENT ->
                Optional.of(CaseFileCategory.EVIDENCE);
            case CERTIFICATE_OF_SUITABILITY_AS_LF,
                 LEGAL_AID_CERTIFICATE ->
                Optional.of(CaseFileCategory.CORRESPONDENCE);
            case OTHER ->
                Optional.of(CaseFileCategory.UNCATEGORISED_DOCUMENTS);
        };
    }

    public Optional<CaseFileCategory> mapToCategory(DefendantDocumentTypeWales defendantDocumentTypeWales) {
        if (defendantDocumentTypeWales == null) {
            return Optional.empty();
        }

        return switch (defendantDocumentTypeWales) {
            case RENT_STATEMENT,
                OCCUPATION_LICENCE ->
                Optional.of(CaseFileCategory.PROPERTY_DOCUMENTS);
            case CORRESPONDENCE_FROM_DEFENDANT,
                CORRESPONDENCE_FROM_CLAIMANT,
                PHOTOGRAPHIC_EVIDENCE,
                WITNESS_STATEMENT ->
                Optional.of(CaseFileCategory.EVIDENCE);
            case CERTIFICATE_OF_SUITABILITY_AS_LF,
                LEGAL_AID_CERTIFICATE ->
                Optional.of(CaseFileCategory.CORRESPONDENCE);
            case OTHER ->
                Optional.of(CaseFileCategory.UNCATEGORISED_DOCUMENTS);
        };

    }

}
