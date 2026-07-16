package uk.gov.hmcts.reform.pcs.arch;

import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.MethodCallExpr;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

class LoggingExceptionMessageTest {

    private static final Set<String> LOG_METHODS = Set.of("trace", "debug", "info", "warn", "error");

    @BeforeAll
    static void configureJavaParser() {
        StaticJavaParser.getParserConfiguration().setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21);
    }

    @Test
    void loggingStatementsShouldNotContainExceptionGetMessage() throws IOException {
        Path sourceRoot = Path.of("src/main/java");
        List<String> violations;
        try (Stream<Path> files = Files.walk(sourceRoot)) {
            violations = files
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(".java"))
                .flatMap(LoggingExceptionMessageTest::findViolations)
                .toList();
        }

        Path reportPath = Path.of("build/reports/logging-exception-message-violations.txt");
        Files.createDirectories(reportPath.getParent());
        String report = violations.isEmpty() ? "None found" : String.join(System.lineSeparator(), violations);

        Files.writeString(reportPath, report);

        assertTrue(violations.isEmpty(),
            () -> """
                Logging statements containing getMessage() were found:
                %s
                """.formatted(String.join(System.lineSeparator(), violations))
        );
    }

    private static Stream<String> findViolations(Path file) {
        try {
            CompilationUnit compilationUnit = StaticJavaParser.parse(file);
            return compilationUnit.findAll(MethodCallExpr.class).stream()
                .filter(LoggingExceptionMessageTest::isLoggingCall)
                .filter(LoggingExceptionMessageTest::containsGetMessageCall)
                .map(call -> formatViolation(file, call));
        } catch (Exception exception) {
            throw new IllegalStateException("Could not parse " + file, exception);
        }
    }

    private static boolean isLoggingCall(MethodCallExpr call) {
        return LOG_METHODS.contains(call.getNameAsString())
            && call.getScope()
            .map(Object::toString)
            .filter(scope -> scope.equals("log") || scope.equals("logger"))
            .isPresent();
    }

    private static boolean containsGetMessageCall(MethodCallExpr loggingCall) {
        return loggingCall.getArguments().stream()
            .anyMatch(argument ->
                          argument.findAll(MethodCallExpr.class).stream()
                              .anyMatch(call ->
                                            call.getNameAsString().equals("getMessage")
                                                && call.getArguments().isEmpty()
                              )
            );
    }

    private static String formatViolation(Path file, MethodCallExpr call) {
        int line = call.getBegin()
            .map(position -> position.line)
            .orElse(-1);

        return "%s:%d%n    %s".formatted(file, line, call.toString());
    }
}
