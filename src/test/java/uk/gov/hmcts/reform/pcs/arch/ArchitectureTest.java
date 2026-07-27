package uk.gov.hmcts.reform.pcs.arch;

import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTag;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import jakarta.persistence.Entity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.reform.pcs.ccd.entity.GenAppEntity;

import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.belongToAnyOf;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.lang.Priority.HIGH;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;
import uk.gov.hmcts.reform.pcs.exception.RedactedRuntimeException;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.priority;

@AnalyzeClasses(packages = "uk.gov.hmcts.reform.pcs")
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
    static final ArchRule controllers_should_not_expose_entities_via_return_type =
        noMethods()
            .that().areDeclaredInClassesThat().areAnnotatedWith(
                org.springframework.web.bind.annotation.RestController.class)
            .should().haveRawReturnType(resideInAPackage("..ccd.entity.."));

    @ArchTest
    static final ArchRule exceptions_must_extend_redacted_base =
        priority(HIGH).classes()
            .that().resideInAPackage("..exception..")
            .and().areAssignableTo(Throwable.class)
            .and().doNotHaveFullyQualifiedName(RedactedRuntimeException.class.getName())
            .should().beAssignableTo(RedactedRuntimeException.class);

    @ArchTest
    static final ArchRule controllers_should_not_expose_entities_via_parameters =
        noMethods()
            .that().areDeclaredInClassesThat().areAnnotatedWith(
                org.springframework.web.bind.annotation.RestController.class)
            .should(haveAnyParameterTypeResidingIn());

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
                // System.out.println("[SCOPE] checking field: " + field.getFullName());
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
