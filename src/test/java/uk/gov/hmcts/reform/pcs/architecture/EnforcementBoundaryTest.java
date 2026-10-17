package uk.gov.hmcts.reform.pcs.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementOrder;

import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.belongToAnyOf;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

/**
 * Keeps the enforcement module encapsulated. Core code reaches enforcement through
 * extension points it owns (CaseViewContributor, TestCaseGenerator, DocumentService)
 * and through a small allowlist of entry-point types. Shared registries such as
 * EventId, State, FeatureFlag and FeeType may name enforcement, but must not import it.
 */
@AnalyzeClasses(packages = "uk.gov.hmcts.reform.pcs", importOptions = ImportOption.DoNotIncludeTests.class)
class EnforcementBoundaryTest {

    private static final String ENFORCEMENT = "uk.gov.hmcts.reform.pcs.enforcement..";

    /**
     * The only enforcement types core code may refer to. PCSCase holds the
     * {@code @JsonUnwrapped EnforcementOrder enforcementOrder} field.
     */
    private static final Class<?>[] ENTRY_POINTS = {
        EnforcementOrder.class
    };

    @ArchTest
    static final ArchRule coreUsesOnlyEnforcementEntryPoints =
        noClasses().that().resideOutsideOfPackage(ENFORCEMENT)
            .should().dependOnClassesThat(resideInAPackage(ENFORCEMENT).and(not(belongToAnyOf(ENTRY_POINTS))))
            .because("core code should reach enforcement only through its extension points "
                         + "and the allowlisted entry-point types");

    @ArchTest
    static final ArchRule enforcementInternalsArePrivate =
        noClasses().that().resideOutsideOfPackage(ENFORCEMENT)
            .should().dependOnClassesThat(resideInAnyPackage(
                "uk.gov.hmcts.reform.pcs.enforcement.repository..",
                "uk.gov.hmcts.reform.pcs.enforcement.service..",
                "uk.gov.hmcts.reform.pcs.enforcement.entity..",
                "uk.gov.hmcts.reform.pcs.enforcement.ccd..",
                "uk.gov.hmcts.reform.pcs.enforcement.view.."
            ))
            .because("persistence, services, CCD config and views are enforcement implementation details");

    @ArchTest
    static final ArchRule enforcementSlicesAreFreeOfCycles =
        slices().matching("uk.gov.hmcts.reform.pcs.enforcement.(*)..")
            .should().beFreeOfCycles()
            .because("cyclic sub-packages cannot be understood, tested or extracted on their own");
}
