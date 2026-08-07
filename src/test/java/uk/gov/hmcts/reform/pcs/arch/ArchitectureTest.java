package uk.gov.hmcts.reform.pcs.arch;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaConstructorCall;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTag;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.EvaluationResult;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.reform.pcs.ccd.entity.GenAppEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.feesandpay.FeePaymentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.CounterClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.DefendantResponseEntity;
import uk.gov.hmcts.reform.pcs.exception.RedactedRuntimeException;

import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.belongToAnyOf;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.lang.Priority.HIGH;
import static com.tngtech.archunit.lang.Priority.LOW;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.priority;

@AnalyzeClasses(packages = "uk.gov.hmcts.reform.pcs",
                importOptions = {
                    ImportOption.DoNotIncludeTests.class,
                    ArchIgnoreTestSources.class
                })
@ArchTag("archunit")
@Slf4j
public class ArchitectureTest {

    @ArchTest
    static final ArchRule entities_should_not_use_eager_fetch =
        fields()
            .that().areDeclaredInClassesThat().areAnnotatedWith(Entity.class)
            .and().areDeclaredInClassesThat(not(belongToAnyOf(GenAppEntity.class)))
            .should(notBeAnnotatedWithEagerFetch());

    @ArchTest
    static final ArchRule controllers_should_not_expose_entities =
        noMethods()
            .that().areDeclaredInClassesThat().areAnnotatedWith(
                org.springframework.web.bind.annotation.RestController.class)
            .should().haveRawReturnType(resideInAPackage("..ccd.entity.."));

    @ArchTest
    static final ArchRule entities_should_not_depend_on_services =
        noClasses()
            .that().resideInAPackage("..ccd.entity..")
            .should().dependOnClassesThat().resideInAPackage("..service..");

    @ArchTest
    static final ArchRule no_field_injection =
        noFields()
            .should().beAnnotatedWith(Autowired.class)
            .because("constructor injection should be used instead of field injection");

    @ArchTest
    static final ArchRule exceptions_must_extend_redacted_base =
        priority(HIGH).classes()
            .that().resideInAPackage("uk.gov.hmcts.reform.pcs..")
            .and().areAssignableTo(Throwable.class)
            .and().doNotHaveFullyQualifiedName(RedactedRuntimeException.class.getName())
            .should().beAssignableTo(RedactedRuntimeException.class);

    @ArchTest
    static final ArchRule controllers_should_not_expose_entities_via_parameters =
        noMethods()
            .that().areDeclaredInClassesThat().areAnnotatedWith(
                org.springframework.web.bind.annotation.RestController.class)
            .should(haveAnyParameterTypeResidingIn());

    // Added test to show how many JDK and third party exceptions are used.  This is not an issue 'per say' so long as
    // they are for programatic reasonings - otherwise we could expose things beyond the application boundary.
    // This is deliberately left as a output warning so that it can be used to monitor size.
    @ArchTest
    static void warn_when_custom_exceptions_do_not_extend_redacted_runtime_exception(JavaClasses classes) {
        ArchRule rule = priority(LOW).noClasses()
            .that().areNotAssignableTo(Throwable.class)
            .should().callConstructorWhere(instantiatesForeignThrowable())
            .as("Custom exceptions must extend RedactedRuntimeException")
            .because(
                "Instantiating JDK or third-party exceptions directly bypasses "
                    + "the Redaction formal structure so should only be used for programitic means and not contain "
                    + "application knowledge."
            );
        EvaluationResult result = rule.evaluate(classes);
        if (result.hasViolation()) {
            log.warn("""
                WARNING: Architecture violations found:
                %s
                """.formatted(
                    String.join(
                        System.lineSeparator(),
                        result.getFailureReport().getDetails()
                    )
            ));
        }
    }

    // Those within the not(belongToAnyOf(...) should be removed once the service layer implementation has been created.
    @ArchTest
    static final ArchRule no_entity_listeners =
        noClasses()
            .that(not(belongToAnyOf(FeePaymentEntity.class, CounterClaimEntity.class, DefendantResponseEntity.class)))
            .should().beAnnotatedWith(EntityListeners.class)
            .because("@EntityListeners couples lifecycle behaviour to JPA internals.");

    private static DescribedPredicate<JavaConstructorCall> instantiatesForeignThrowable() {
        return new DescribedPredicate<JavaConstructorCall>("Instantiates a Throwable that "
                                                               + "is not a RedactedRuntimeException") {
            @Override
            public boolean test(JavaConstructorCall call) {
                JavaClass instantiated = call.getTargetOwner();
                return instantiated.isAssignableTo(Throwable.class)
                    && !instantiated.isAssignableTo(RedactedRuntimeException.class);
            }
        };
    }

    private static ArchCondition<JavaMethod> haveAnyParameterTypeResidingIn() {
        String packageIdentifier = "..ccd.entity..";
        return new ArchCondition<JavaMethod>("not have parameters residing in " + packageIdentifier) {
            @Override
            public void check(JavaMethod method, ConditionEvents events) {
                method.getParameterTypes().forEach(paramType -> {
                    if (paramType.toErasure().getPackageName().matches(
                        packageIdentifier.replace("..", ".*"))) {
                        events.add(SimpleConditionEvent.violated(method,
                                                                 method.getFullName()
                                                                     + " exposes entity parameter type "
                                                                     + paramType.getName()));
                    }
                });
            }
        };
    }

    private static ArchCondition<JavaField> notBeAnnotatedWithEagerFetch() {
        return new ArchCondition<JavaField>("not use FetchType.EAGER") {
            @Override
            public void check(JavaField field, ConditionEvents events) {
                field.getAnnotations().forEach(annotation -> {
                    annotation.tryGetExplicitlyDeclaredProperty("fetch")
                        .ifPresent(fetch -> {
                            if (fetch.toString().contains("EAGER")) {
                                log.warn("[ALERT !!!] Architecture violation detected: {}", field.getFullName());
                                events.add(SimpleConditionEvent.violated(field,
                                                                         field.getFullName()
                                                                             + " uses FetchType.EAGER"));
                            }
                        });
                });
            }
        };
    }
}
