package uk.gov.hmcts.reform.pcs.arch;

import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.stmt.CatchClause;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

class LoggingExceptionExposureTest {

    private static final Set<String> REDACTED_EXCEPTION_TYPES = Set.of("RedactedException", "RedactedRuntimeException");

    private static final Set<String> LOG_METHODS =
        Set.of("trace", "debug", "info", "warn", "error");

    private static final Set<String> LOGGER_NAMES =
        Set.of("log", "logger", "logging");

    private static final Set<String> EXCEPTION_MESSAGE_METHODS =
        Set.of(
            "getMessage",
            "getLocalizedMessage",
            "getRootCauseMessage"
        );

    private static final Set<String> EXCEPTION_CAUSE_METHODS =
        Set.of(
            "getCause",
            "getRootCause"
        );

    private static final Set<String> STACK_TRACE_METHODS =
        Set.of(
            "getStackTrace",
            "getStackFrames",
            "getRootCauseStackTrace",
            "getFullStackTrace",
            "getStackTraceAsString"
        );

    private static final Set<String> CONVENTIONAL_EXCEPTION_NAMES =
        Set.of(
            "e",
            "ex",
            "exception",
            "throwable",
            "error",
            "cause"
        );

    @BeforeAll
    static void configureJavaParser() {
        StaticJavaParser.getParserConfiguration()
            .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21);
    }

    @Test
    void loggingStatementsShouldNotExposeExceptionDetails() throws IOException {
        Path sourceRoot = Path.of("src/main/java");

        List<Violation> violations;

        try (Stream<Path> files = Files.walk(sourceRoot)) {
            violations = files
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(".java"))
                .flatMap(LoggingExceptionExposureTest::findViolations)
                .sorted()
                .toList();
        }

        Path reportPath =
            Path.of("build/reports/logging-exception-exposure-violations.txt");

        Files.createDirectories(reportPath.getParent());

        String report = violations.isEmpty()
            ? "None found"
            : violations.stream()
            .map(Violation::toString)
            .collect(java.util.stream.Collectors.joining(
                System.lineSeparator() + System.lineSeparator()
            ));

        Files.writeString(reportPath, report);

        assertTrue(
            violations.isEmpty(),
            () -> """
                Potential exception information exposure was found.

                Report:
                %s

                Violations:
                %s
                """.formatted(
                reportPath,
                report
            )
        );
    }

    private static Stream<Violation> findViolations(Path file) {
        try {
            CompilationUnit compilationUnit = StaticJavaParser.parse(file);

            if (isRedactedExceptionClass(compilationUnit)) {
                return Stream.empty();
            }

            Stream<Violation> loggingViolations =
                compilationUnit.findAll(MethodCallExpr.class).stream()
                    .filter(LoggingExceptionExposureTest::isLoggingCall)
                    .flatMap(call -> inspectLoggingCall(file, call));

            Stream<Violation> printStackTraceViolations =
                compilationUnit.findAll(MethodCallExpr.class).stream()
                    .filter(call -> call.getNameAsString().equals("printStackTrace"))
                    .map(call -> violation(
                        file,
                        call,
                        Rule.PRINT_STACK_TRACE,
                        "printStackTrace() writes exception details directly"
                    ));

            return Stream.concat(
                loggingViolations,
                printStackTraceViolations
            );
        } catch (Exception exception) {
            return Stream.of(new Violation(
                file,
                -1,
                Rule.PARSER_FAILURE,
                "Could not parse source file: "
                    + exception.getClass().getSimpleName(),
                ""
            ));
        }
    }

    private static boolean isRedactedExceptionClass(
        CompilationUnit compilationUnit
    ) {
        return compilationUnit.getTypes().stream()
            .map(type -> type.getNameAsString())
            .anyMatch(REDACTED_EXCEPTION_TYPES::contains);
    }

    private static Stream<Violation> inspectLoggingCall(
        Path file,
        MethodCallExpr loggingCall
    ) {
        Set<String> throwableVariables =
            findThrowableVariableNames(loggingCall);

        return loggingCall.getArguments().stream()
            .flatMap(argument ->
                         inspectLoggingArgument(
                             file,
                             loggingCall,
                             argument,
                             throwableVariables
                         )
            );
    }

    private static Stream<Violation> inspectLoggingArgument(
        Path file,
        MethodCallExpr loggingCall,
        Expression argument,
        Set<String> throwableVariables
    ) {
        Set<Violation> violations = new HashSet<>();

        argument.findAll(MethodCallExpr.class).forEach(methodCall -> {
            String methodName = methodCall.getNameAsString();

            if (EXCEPTION_MESSAGE_METHODS.contains(methodName)
                && referencesThrowable(methodCall, throwableVariables)) {

                violations.add(violation(
                    file,
                    loggingCall,
                    Rule.EXCEPTION_MESSAGE,
                    "Logging an exception message using " + methodName + "()"
                ));
            }

            if (EXCEPTION_CAUSE_METHODS.contains(methodName)
                && referencesThrowable(methodCall, throwableVariables)) {

                violations.add(violation(
                    file,
                    loggingCall,
                    Rule.EXCEPTION_CAUSE,
                    "Logging an exception cause using " + methodName + "()"
                ));
            }

            if (methodName.equals("toString")
                && referencesThrowable(methodCall, throwableVariables)) {

                violations.add(violation(
                    file,
                    loggingCall,
                    Rule.EXCEPTION_TO_STRING,
                    "Calling toString() on an exception may include its message"
                ));
            }

            if (STACK_TRACE_METHODS.contains(methodName)
                && isStackTraceExtraction(methodCall, throwableVariables)) {

                violations.add(violation(
                    file,
                    loggingCall,
                    Rule.STACK_TRACE_AS_TEXT,
                    "Converting an exception stack trace to loggable content "
                        + "using " + methodName + "()"
                ));
            }
        });

        return violations.stream();
    }

    private static boolean isLoggingCall(MethodCallExpr call) {
        if (!LOG_METHODS.contains(call.getNameAsString())) {
            return false;
        }

        return call.getScope()
            .map(Expression::toString)
            .map(LoggingExceptionExposureTest::lastScopeSegment)
            .filter(LOGGER_NAMES::contains)
            .isPresent();
    }

    private static String lastScopeSegment(String scope) {
        int finalDot = scope.lastIndexOf('.');
        return finalDot < 0
            ? scope
            : scope.substring(finalDot + 1);
    }

    @SuppressWarnings("unchecked")
    private static Set<String> findThrowableVariableNames(
        MethodCallExpr loggingCall
    ) {
        Set<String> names = new HashSet<>(CONVENTIONAL_EXCEPTION_NAMES);
        Set<String> redactedNames = new HashSet<>();

        loggingCall.findAncestor(MethodDeclaration.class)
            .ifPresent(method ->
                           collectThrowableNames(method, names, redactedNames)
            );

        loggingCall.findAncestor(ConstructorDeclaration.class)
            .ifPresent(constructor ->
                           collectThrowableNames(constructor, names, redactedNames)
            );

        loggingCall.findAncestor(CatchClause.class)
            .map(CatchClause::getParameter)
            .ifPresent(parameter -> {
                if (isRedactedExceptionType(parameter.getType().asString())) {
                    redactedNames.add(parameter.getNameAsString());
                } else if (isThrowableParameter(parameter)) {
                    names.add(parameter.getNameAsString());
                }
            });

        names.removeAll(redactedNames);

        return names;
    }

    private static void collectThrowableNames(
        CallableDeclaration<?> callable,
        Set<String> throwableNames,
        Set<String> redactedNames
    ) {
        callable.getParameters().forEach(parameter -> {
            if (isRedactedExceptionType(parameter.getType().asString())) {
                redactedNames.add(parameter.getNameAsString());
            } else if (isThrowableParameter(parameter)) {
                throwableNames.add(parameter.getNameAsString());
            }
        });

        callable.findAll(VariableDeclarator.class).forEach(variable -> {
            if (isRedactedExceptionType(variable.getType().asString())) {
                redactedNames.add(variable.getNameAsString());
            } else if (isThrowableVariable(variable)) {
                throwableNames.add(variable.getNameAsString());
            }
        });

        callable.findAll(CatchClause.class).stream()
            .map(CatchClause::getParameter)
            .forEach(parameter -> {
                if (isRedactedExceptionType(parameter.getType().asString())) {
                    redactedNames.add(parameter.getNameAsString());
                } else if (isThrowableParameter(parameter)) {
                    throwableNames.add(parameter.getNameAsString());
                }
            });
    }

    private static boolean isRedactedExceptionType(String typeName) {
        String simpleType = typeName
            .replace("[]", "")
            .replaceAll("^.*\\.", "");
        return REDACTED_EXCEPTION_TYPES.contains(simpleType);
    }

    private static boolean isThrowableParameter(Parameter parameter) {
        return isThrowableType(parameter.getType().asString());
    }

    private static boolean isThrowableVariable(
        VariableDeclarator variable
    ) {
        return isThrowableType(variable.getType().asString());
    }

    private static boolean isThrowableType(String typeName) {
        String simpleType = typeName
            .replace("[]", "")
            .replaceAll("^.*\\.", "");

        return simpleType.equals("Throwable")
            || simpleType.endsWith("Exception")
            || simpleType.endsWith("Error");
    }

    private static boolean referencesThrowable(
        MethodCallExpr methodCall,
        Set<String> throwableVariables
    ) {
        return methodCall.getScope()
            .map(scope -> referencesThrowable(scope, throwableVariables))
            .orElse(false)
            || methodCall.getArguments().stream()
            .anyMatch(argument ->
                          referencesThrowable(argument, throwableVariables)
            );
    }

    private static boolean referencesThrowable(
        Expression expression,
        Set<String> throwableVariables
    ) {
        return expression.findAll(NameExpr.class).stream()
            .map(NameExpr::getNameAsString)
            .anyMatch(throwableVariables::contains);
    }

    private static boolean isStackTraceExtraction(
        MethodCallExpr methodCall,
        Set<String> throwableVariables
    ) {
        if (referencesThrowable(methodCall, throwableVariables)) {
            return true;
        }

        return methodCall.getScope()
            .map(Expression::toString)
            .map(scope ->
                     scope.endsWith("ExceptionUtils")
                         || scope.endsWith("ThrowableUtils")
                         || scope.endsWith("Throwables")
            )
            .orElse(false);
    }

    private static boolean isSafeThrowableMetadata(Expression argument) {
        if (!argument.isMethodCallExpr()) {
            return false;
        }

        MethodCallExpr outerCall = argument.asMethodCallExpr();

        if (!Set.of(
            "getSimpleName",
            "getName",
            "getCanonicalName"
        ).contains(outerCall.getNameAsString())) {
            return false;
        }

        return outerCall.getScope()
            .filter(Expression::isMethodCallExpr)
            .map(Expression::asMethodCallExpr)
            .filter(call -> call.getNameAsString().equals("getClass"))
            .isPresent();
    }

    private static Violation violation(
        Path file,
        MethodCallExpr call,
        Rule rule,
        String description
    ) {
        int line = call.getBegin()
            .map(position -> position.line)
            .orElse(-1);

        return new Violation(
            file,
            line,
            rule,
            description,
            call.toString()
        );
    }

    private enum Rule {
        EXCEPTION_MESSAGE,
        EXCEPTION_CAUSE,
        EXCEPTION_TO_STRING,
        THROWABLE_ARGUMENT,
        STACK_TRACE_AS_TEXT,
        PRINT_STACK_TRACE,
        PARSER_FAILURE
    }

    private record Violation(
        Path file,
        int line,
        Rule rule,
        String description,
        String source
    ) implements Comparable<Violation> {

        @Override
        public int compareTo(Violation other) {
            int fileComparison =
                file.toString().compareTo(other.file.toString());

            if (fileComparison != 0) {
                return fileComparison;
            }

            int lineComparison = Integer.compare(line, other.line);

            if (lineComparison != 0) {
                return lineComparison;
            }

            return rule.compareTo(other.rule);
        }

        @Override
        public String toString() {
            return """
                %s:%d
                    Rule: %s
                    Reason: %s
                    Code: %s
                """.formatted(
                file,
                line,
                rule,
                description,
                source.isBlank() ? "<unavailable>" : source
            ).stripTrailing();
        }
    }
}
