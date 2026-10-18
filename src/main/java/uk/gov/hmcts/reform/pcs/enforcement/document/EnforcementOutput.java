package uk.gov.hmcts.reform.pcs.enforcement.document;

import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.reform.pcs.ccd.domain.DocumentType;
import uk.gov.hmcts.reform.pcs.enforcement.domain.SelectEnforcementType;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Every document the enforcement journeys generate, all of them filed in the Enforcement folder of Case File
 * View.
 *
 * <p>{@link #getTemplateKey()} is the key under {@code enforcement.documents.templates} that carries the
 * Docmosis template id. None of the real templates exists yet (OPEN-QUESTIONS 42), so every key defaults to a
 * deployed stand-in template. Every template receives the same {@link EnforcementDocumentPayload}: the case
 * reference, case name and property address; the enforcement type and number; the seal and generation time; the
 * case management court's name and address; the allocated bailiff's name; the language the claimant used; the
 * eviction date and whether service was dispensed with; and all the claimant's answers as submitted. A real
 * template that needs anything else, such as Welsh wording (HDPI-9566), needs a payload change as well as its
 * template id.</p>
 */
@AllArgsConstructor
@Getter
public enum EnforcementOutput {

    /**
     * 01-claimant-applies-for-enforcement.feature, "Submitting a warrant application generates the application
     * and risk assessment outputs" (HDPI-7555).
     */
    WARRANT_OF_POSSESSION_APPLICATION("Warrant of possession application", "warrant-of-possession-application",
                                      DocumentType.ENFORCEMENT_APPLICATION, Seal.NONE, true, false),
    WRIT_OF_POSSESSION_APPLICATION("Writ of possession application", "writ-of-possession-application",
                                   DocumentType.ENFORCEMENT_APPLICATION, Seal.NONE, true, false),
    WARRANT_OF_RESTITUTION_APPLICATION("Warrant of restitution application", "warrant-of-restitution-application",
                                       DocumentType.ENFORCEMENT_APPLICATION, Seal.NONE, true, false),
    WRIT_OF_RESTITUTION_APPLICATION("Writ of restitution application", "writ-of-restitution-application",
                                    DocumentType.ENFORCEMENT_APPLICATION, Seal.NONE, true, false),
    RISK_ASSESSMENT("Risk assessment", "risk-assessment", DocumentType.RISK_ASSESSMENT, Seal.NONE, true, false),

    /**
     * 02-caseworker-decides-enforcement.feature, "Generated document is sealed and visible according to type"
     * (HDPI-9566).
     */
    WARRANT_OF_POSSESSION("Warrant of possession", "warrant-of-possession", DocumentType.WARRANT_OF_POSSESSION,
                          Seal.COUNTY_COURT, false, false),
    WRIT_OF_POSSESSION("Writ of possession", "writ-of-possession", DocumentType.WRIT_OF_POSSESSION,
                       Seal.HIGH_COURT, true, false),
    WARRANT_OF_RESTITUTION("Warrant of restitution", "warrant-of-restitution", DocumentType.WARRANT_OF_RESTITUTION,
                           Seal.COUNTY_COURT, false, false),
    WRIT_OF_RESTITUTION("Writ of restitution", "writ-of-restitution", DocumentType.WRIT_OF_RESTITUTION,
                        Seal.HIGH_COURT, true, false),

    /**
     * 05-schedule-eviction.feature, "Scheduling generates a sealed notice of eviction" (HDPI-7354) and
     * "Scheduling generates and issues an unsealed notice of appointment" (HDPI-7355). A reschedule generates
     * both again and keeps the originals, so these two are not one per enforcement.
     */
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
    /**
     * Whether the claimant sees the document in Case File View. Internal users always do; defendants never do.
     */
    private final boolean visibleToClaimant;
    /**
     * Whether the enforcement can have more than one of this document.
     */
    private final boolean repeatable;

    /**
     * The documents generated once the application is submitted and paid for: the application, and for a
     * warrant the claimant's risk assessment answers.
     */
    public static List<EnforcementOutput> forApplication(SelectEnforcementType enforcementType) {
        return switch (enforcementType) {
            case WARRANT -> List.of(WARRANT_OF_POSSESSION_APPLICATION, RISK_ASSESSMENT);
            case WRIT -> List.of(WRIT_OF_POSSESSION_APPLICATION);
            case WARRANT_OF_RESTITUTION -> List.of(WARRANT_OF_RESTITUTION_APPLICATION, RISK_ASSESSMENT);
            case WRIT_OF_RESTITUTION -> List.of(WRIT_OF_RESTITUTION_APPLICATION);
        };
    }

    /**
     * The sealed warrant or writ generated when the caseworker approves the application.
     */
    public static EnforcementOutput forApproval(SelectEnforcementType enforcementType) {
        return switch (enforcementType) {
            case WARRANT -> WARRANT_OF_POSSESSION;
            case WRIT -> WRIT_OF_POSSESSION;
            case WARRANT_OF_RESTITUTION -> WARRANT_OF_RESTITUTION;
            case WRIT_OF_RESTITUTION -> WRIT_OF_RESTITUTION;
        };
    }

    /**
     * The notices generated each time the bailiff schedules or reschedules the eviction.
     */
    public static List<EnforcementOutput> forEvictionSchedule() {
        return List.of(NOTICE_OF_EVICTION, NOTICE_OF_APPOINTMENT);
    }

    /**
     * Whether the claimant must not see a document of this type: the warrants and the notice of eviction are for
     * internal users only.
     */
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
