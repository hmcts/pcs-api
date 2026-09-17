package uk.gov.hmcts.reform.pcs.ccd.service.document;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.domain.AdditionalDocumentType;
import uk.gov.hmcts.reform.pcs.ccd.domain.DocumentType;
import uk.gov.hmcts.reform.pcs.ccd.domain.legalrepdocumentupload.ClaimantDocumentType;
import uk.gov.hmcts.reform.pcs.ccd.domain.legalrepdocumentupload.DefendantDocumentType;
import uk.gov.hmcts.reform.pcs.ccd.domain.legalrepdocumentupload.wales.ClaimantDocumentTypeWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.legalrepdocumentupload.wales.DefendantDocumentTypeWales;

@Component
@SuppressWarnings("DuplicatedCode")
public class DocumentTypeMapper {

    public DocumentType mapToDocumentType(AdditionalDocumentType additionalType) {
        if (additionalType == null) {
            return null;
        }

        return switch (additionalType) {
            case WITNESS_STATEMENT -> DocumentType.WITNESS_STATEMENT;
            case RENT_STATEMENT -> DocumentType.RENT_STATEMENT;
            case OCCUPATION_LICENCE -> DocumentType.OCCUPATION_LICENCE;
            case ENERGY_PERFORMANCE_CERTIFICATE -> DocumentType.ENERGY_PERFORMANCE_CERTIFICATE;
            case GAS_SAFETY_CERTIFICATE -> DocumentType.GAS_SAFETY_CERTIFICATE;
            case EICR_REPORT -> DocumentType.EICR_REPORT;
            case TENANCY_AGREEMENT -> DocumentType.TENANCY_AGREEMENT;
            case CERTIFICATE_OF_SERVICE -> DocumentType.CERTIFICATE_OF_SERVICE;
            case CORRESPONDENCE_FROM_DEFENDANT -> DocumentType.CORRESPONDENCE_FROM_DEFENDANT;
            case CORRESPONDENCE_FROM_CLAIMANT -> DocumentType.CORRESPONDENCE_FROM_CLAIMANT;
            case POSSESSION_NOTICE -> DocumentType.POSSESSION_NOTICE;
            case NOTICE_FOR_SERVICE_OUT_OF_JURISDICTION -> DocumentType.NOTICE_FOR_SERVICE_OUT_OF_JURISDICTION;
            case PHOTOGRAPHIC_EVIDENCE -> DocumentType.PHOTOGRAPHIC_EVIDENCE;
            case INSPECTION_OR_REPORT -> DocumentType.INSPECTION_OR_REPORT;
            case CERTIFICATE_OF_SUITABILITY_AS_LF -> DocumentType.CERTIFICATE_OF_SUITABILITY_AS_LF;
            case LEGAL_AID_CERTIFICATE -> DocumentType.LEGAL_AID_CERTIFICATE;
            case OTHER -> DocumentType.OTHER;
        };
    }

    public DocumentType mapToDocumentType(ClaimantDocumentType claimantDocumentType) {
        if (claimantDocumentType == null) {
            return null;
        }

        return switch (claimantDocumentType) {
            case WITNESS_STATEMENT -> DocumentType.WITNESS_STATEMENT;
            case RENT_STATEMENT -> DocumentType.RENT_STATEMENT;
            case TENANCY_AGREEMENT -> DocumentType.TENANCY_AGREEMENT;
            case CERTIFICATE_OF_SERVICE -> DocumentType.CERTIFICATE_OF_SERVICE;
            case CORRESPONDENCE_FROM_DEFENDANT -> DocumentType.CORRESPONDENCE_FROM_DEFENDANT;
            case CORRESPONDENCE_FROM_CLAIMANT -> DocumentType.CORRESPONDENCE_FROM_CLAIMANT;
            case POSSESSION_NOTICE -> DocumentType.POSSESSION_NOTICE;
            case NOTICE_FOR_SERVICE_OUT_OF_JURISDICTION -> DocumentType.NOTICE_FOR_SERVICE_OUT_OF_JURISDICTION;
            case PHOTOGRAPHIC_EVIDENCE -> DocumentType.PHOTOGRAPHIC_EVIDENCE;
            case INSPECTION_OR_REPORT -> DocumentType.INSPECTION_OR_REPORT;
            case CERTIFICATE_OF_SUITABILITY_AS_LF -> DocumentType.CERTIFICATE_OF_SUITABILITY_AS_LF;
            case LEGAL_AID_CERTIFICATE -> DocumentType.LEGAL_AID_CERTIFICATE;
            case OTHER -> DocumentType.OTHER;
        };
    }

    public DocumentType mapToDocumentType(ClaimantDocumentTypeWales claimantDocumentTypeWales) {
        if (claimantDocumentTypeWales == null) {
            return null;
        }

        return switch (claimantDocumentTypeWales) {
            case WITNESS_STATEMENT -> DocumentType.WITNESS_STATEMENT;
            case RENT_STATEMENT -> DocumentType.RENT_STATEMENT;
            case OCCUPATION_LICENCE -> DocumentType.OCCUPATION_LICENCE;
            case CERTIFICATE_OF_SERVICE -> DocumentType.CERTIFICATE_OF_SERVICE;
            case ENERGY_PERFORMANCE_CERTIFICATE -> DocumentType.ENERGY_PERFORMANCE_CERTIFICATE;
            case GAS_SAFETY_CERTIFICATE -> DocumentType.GAS_SAFETY_CERTIFICATE;
            case EICR_REPORT -> DocumentType.EICR_REPORT;
            case CORRESPONDENCE_FROM_DEFENDANT -> DocumentType.CORRESPONDENCE_FROM_DEFENDANT;
            case CORRESPONDENCE_FROM_CLAIMANT -> DocumentType.CORRESPONDENCE_FROM_CLAIMANT;
            case POSSESSION_NOTICE -> DocumentType.POSSESSION_NOTICE;
            case NOTICE_FOR_SERVICE_OUT_OF_JURISDICTION -> DocumentType.NOTICE_FOR_SERVICE_OUT_OF_JURISDICTION;
            case PHOTOGRAPHIC_EVIDENCE -> DocumentType.PHOTOGRAPHIC_EVIDENCE;
            case INSPECTION_OR_REPORT -> DocumentType.INSPECTION_OR_REPORT;
            case CERTIFICATE_OF_SUITABILITY_AS_LF -> DocumentType.CERTIFICATE_OF_SUITABILITY_AS_LF;
            case LEGAL_AID_CERTIFICATE -> DocumentType.LEGAL_AID_CERTIFICATE;
            case OTHER -> DocumentType.OTHER;
        };
    }

    public DocumentType mapToDocumentType(DefendantDocumentType defendantDocumentType) {
        if (defendantDocumentType == null) {
            return null;
        }

        return switch (defendantDocumentType) {
            case RENT_STATEMENT -> DocumentType.RENT_STATEMENT;
            case TENANCY_AGREEMENT -> DocumentType.TENANCY_AGREEMENT;
            case CORRESPONDENCE_FROM_CLAIMANT -> DocumentType.CORRESPONDENCE_FROM_CLAIMANT;
            case CORRESPONDENCE_FROM_DEFENDANT -> DocumentType.CORRESPONDENCE_FROM_DEFENDANT;
            case PHOTOGRAPHIC_EVIDENCE -> DocumentType.PHOTOGRAPHIC_EVIDENCE;
            case CERTIFICATE_OF_SUITABILITY_AS_LF -> DocumentType.CERTIFICATE_OF_SUITABILITY_AS_LF;
            case LEGAL_AID_CERTIFICATE -> DocumentType.LEGAL_AID_CERTIFICATE;
            case OTHER -> DocumentType.OTHER;
            case WITNESS_STATEMENT -> DocumentType.WITNESS_STATEMENT;
        };
    }

    public DocumentType mapToDocumentType(DefendantDocumentTypeWales defendantDocumentTypeWales) {
        if (defendantDocumentTypeWales == null) {
            return null;
        }

        return switch (defendantDocumentTypeWales) {
            case RENT_STATEMENT -> DocumentType.RENT_STATEMENT;
            case OCCUPATION_LICENCE -> DocumentType.OCCUPATION_LICENCE;
            case CORRESPONDENCE_FROM_CLAIMANT -> DocumentType.CORRESPONDENCE_FROM_CLAIMANT;
            case CORRESPONDENCE_FROM_DEFENDANT -> DocumentType.CORRESPONDENCE_FROM_DEFENDANT;
            case PHOTOGRAPHIC_EVIDENCE -> DocumentType.PHOTOGRAPHIC_EVIDENCE;
            case CERTIFICATE_OF_SUITABILITY_AS_LF -> DocumentType.CERTIFICATE_OF_SUITABILITY_AS_LF;
            case LEGAL_AID_CERTIFICATE -> DocumentType.LEGAL_AID_CERTIFICATE;
            case OTHER -> DocumentType.OTHER;
            case WITNESS_STATEMENT -> DocumentType.WITNESS_STATEMENT;
        };
    }

}
