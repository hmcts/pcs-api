package uk.gov.hmcts.reform.pcs.enforcement.document;

import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.reform.pcs.ccd.domain.DocumentType;
import uk.gov.hmcts.reform.pcs.enforcement.domain.SelectEnforcementType;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@AllArgsConstructor
@Getter
public enum EnforcementOutput {

    WARRANT_OF_POSSESSION_APPLICATION("Warrant of possession application", "warrant-of-possession-application",
                                      DocumentType.ENFORCEMENT_APPLICATION, Seal.NONE, true, false),
    WRIT_OF_POSSESSION_APPLICATION("Writ of possession application", "writ-of-possession-application",
                                   DocumentType.ENFORCEMENT_APPLICATION, Seal.NONE, true, false),
    WARRANT_OF_RESTITUTION_APPLICATION("Warrant of restitution application", "warrant-of-restitution-application",
                                       DocumentType.ENFORCEMENT_APPLICATION, Seal.NONE, true, false),
    WRIT_OF_RESTITUTION_APPLICATION("Writ of restitution application", "writ-of-restitution-application",
                                    DocumentType.ENFORCEMENT_APPLICATION, Seal.NONE, true, false),
    RISK_ASSESSMENT("Risk assessment", "risk-assessment", DocumentType.RISK_ASSESSMENT, Seal.NONE, true, false),

    WARRANT_OF_POSSESSION("Warrant of possession", "warrant-of-possession", DocumentType.WARRANT_OF_POSSESSION,
                          Seal.COUNTY_COURT, false, false),
    WRIT_OF_POSSESSION("Writ of possession", "writ-of-possession", DocumentType.WRIT_OF_POSSESSION,
                       Seal.HIGH_COURT, true, false),
    WARRANT_OF_RESTITUTION("Warrant of restitution", "warrant-of-restitution", DocumentType.WARRANT_OF_RESTITUTION,
                           Seal.COUNTY_COURT, false, false),
    WRIT_OF_RESTITUTION("Writ of restitution", "writ-of-restitution", DocumentType.WRIT_OF_RESTITUTION,
                        Seal.HIGH_COURT, true, false),

    NOTICE_OF_EVICTION("Notice of eviction", "notice-of-eviction", DocumentType.NOTICE_OF_EVICTION,
                       Seal.COUNTY_COURT, false, true),
    NOTICE_OF_APPOINTMENT("Notice of appointment", "notice-of-appointment", DocumentType.NOTICE_OF_APPOINTMENT,
                          Seal.NONE, true, true);

    private static final Set<DocumentType> INTERNAL_ONLY_DOCUMENT_TYPES = Arrays.stream(values())
        .filter(output -> !output.visibleToClaimant)
        .map(EnforcementOutput::getDocumentType)
        .collect(Collectors.toUnmodifiableSet());

    private final String title;
    private final String templateKey;
    private final DocumentType documentType;
    private final Seal seal;
    private final boolean visibleToClaimant;
    private final boolean repeatable;

    public static List<EnforcementOutput> forApplication(SelectEnforcementType enforcementType) {
        return switch (enforcementType) {
            case WARRANT -> List.of(WARRANT_OF_POSSESSION_APPLICATION, RISK_ASSESSMENT);
            case WRIT -> List.of(WRIT_OF_POSSESSION_APPLICATION);
            case WARRANT_OF_RESTITUTION -> List.of(WARRANT_OF_RESTITUTION_APPLICATION, RISK_ASSESSMENT);
            case WRIT_OF_RESTITUTION -> List.of(WRIT_OF_RESTITUTION_APPLICATION);
        };
    }

    public static EnforcementOutput forApproval(SelectEnforcementType enforcementType) {
        return switch (enforcementType) {
            case WARRANT -> WARRANT_OF_POSSESSION;
            case WRIT -> WRIT_OF_POSSESSION;
            case WARRANT_OF_RESTITUTION -> WARRANT_OF_RESTITUTION;
            case WRIT_OF_RESTITUTION -> WRIT_OF_RESTITUTION;
        };
    }

    public static List<EnforcementOutput> forEvictionSchedule() {
        return List.of(NOTICE_OF_EVICTION, NOTICE_OF_APPOINTMENT);
    }

    public static boolean isInternalOnly(DocumentType documentType) {
        return documentType != null && INTERNAL_ONLY_DOCUMENT_TYPES.contains(documentType);
    }

    @AllArgsConstructor
    @Getter
    public enum Seal {
        COUNTY_COURT("County Court"),
        HIGH_COURT("High Court"),
        NONE(null);

        private final String label;
    }
}
