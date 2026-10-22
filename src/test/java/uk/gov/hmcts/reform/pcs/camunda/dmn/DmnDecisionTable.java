package uk.gov.hmcts.reform.pcs.camunda.dmn;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * One decision table out of {@code src/main/resources/dmn}, parsed so that tests can assert on its shape and on
 * the rows a given set of inputs collects.
 *
 * <p>{@link #evaluate} is not a FEEL engine: it understands empty cells, {@code -}, lists of string literals
 * and {@code not(...)} around such a list, and counts anything else in {@link #cellsThatAreNotLiterals}. The
 * real Camunda engine is no use here because some PCS cells hold free text that is not a valid unary test.</p>
 */
final class DmnDecisionTable {

    private static final Pattern STRING_LITERAL = Pattern.compile("\"([^\"]*)\"");
    private static final Pattern NEGATION = Pattern.compile("not\\((.*)\\)", Pattern.DOTALL);
    private static final String ANY = "-";

    private final String fileName;
    private final String decisionId;
    private final String hitPolicy;
    private final List<String> inputNames;
    private final List<String> outputNames;
    private final List<Rule> rules;

    private DmnDecisionTable(String fileName, String decisionId, String hitPolicy, List<String> inputNames,
                             List<String> outputNames, List<Rule> rules) {
        this.fileName = fileName;
        this.decisionId = decisionId;
        this.hitPolicy = hitPolicy;
        this.inputNames = List.copyOf(inputNames);
        this.outputNames = List.copyOf(outputNames);
        this.rules = List.copyOf(rules);
    }

    record Rule(String id, List<String> inputEntries, List<String> outputEntries) {
    }

    static DmnDecisionTable load(String fileName) {
        Document document = parse(fileName);
        Element decision = onlyChild(document.getDocumentElement(), "decision");
        Element decisionTable = onlyChild(decision, "decisionTable");

        List<String> inputNames = new ArrayList<>();
        List<Element> inputs = children(decisionTable, "input");
        for (int i = 0; i < inputs.size(); i++) {
            inputNames.add(inputName(inputs.get(i), i));
        }

        List<String> outputNames = children(decisionTable, "output").stream()
            .map(output -> output.getAttribute("name"))
            .toList();

        List<Rule> rules = children(decisionTable, "rule").stream()
            .map(rule -> new Rule(
                rule.getAttribute("id"),
                entryTexts(rule, "inputEntry"),
                entryTexts(rule, "outputEntry")))
            .toList();

        return new DmnDecisionTable(fileName, decision.getAttribute("id"), decisionTable.getAttribute("hitPolicy"),
                                    inputNames, outputNames, rules);
    }

    String fileName() {
        return fileName;
    }

    String decisionId() {
        return decisionId;
    }

    String hitPolicy() {
        return hitPolicy;
    }

    List<String> inputNames() {
        return inputNames;
    }

    List<String> outputNames() {
        return outputNames;
    }

    List<Rule> rules() {
        return rules;
    }

    List<Map<String, String>> evaluate(Map<String, String> inputs) {
        List<Map<String, String>> matched = new ArrayList<>();
        for (Rule rule : rules) {
            if (matches(rule, inputs)) {
                Map<String, String> row = new LinkedHashMap<>();
                for (int i = 0; i < outputNames.size(); i++) {
                    row.put(outputNames.get(i), unquote(rule.outputEntries().get(i)));
                }
                matched.add(row);
            }
        }
        return matched;
    }

    List<String> collect(Map<String, String> inputs, String outputName) {
        return evaluate(inputs).stream()
            .map(row -> row.get(outputName))
            .filter(value -> !value.isEmpty())
            .toList();
    }

    Map<String, String> cellsThatAreNotLiterals() {
        Map<String, String> notLiterals = new LinkedHashMap<>();
        for (Rule rule : rules) {
            for (String entry : rule.inputEntries()) {
                if (!entry.isEmpty() && !ANY.equals(entry) && condition(entry) == null) {
                    notLiterals.put(rule.id(), entry);
                }
            }
        }
        return notLiterals;
    }

    private boolean matches(Rule rule, Map<String, String> inputs) {
        for (int i = 0; i < inputNames.size(); i++) {
            String entry = rule.inputEntries().get(i);
            if (entry.isEmpty() || ANY.equals(entry)) {
                continue;
            }
            Condition condition = condition(entry);
            if (condition == null || !condition.matches(inputs.get(inputNames.get(i)))) {
                return false;
            }
        }
        return true;
    }

    private record Condition(boolean negated, List<String> literals) {

        boolean matches(String value) {
            return literals.contains(value) != negated;
        }
    }

    private static Condition condition(String entry) {
        Matcher negation = NEGATION.matcher(entry);
        boolean negated = negation.matches();
        List<String> literals = literals(negated ? negation.group(1) : entry);
        return literals == null ? null : new Condition(negated, literals);
    }

    private static List<String> literals(String entry) {
        List<String> literals = new ArrayList<>();
        Matcher matcher = STRING_LITERAL.matcher(entry);
        StringBuilder remainder = new StringBuilder();
        int end = 0;
        while (matcher.find()) {
            literals.add(matcher.group(1));
            remainder.append(entry, end, matcher.start());
            end = matcher.end();
        }
        remainder.append(entry.substring(end));
        boolean onlyLiteralsAndCommas = remainder.toString().replace(",", "").isBlank();
        return literals.isEmpty() || !onlyLiteralsAndCommas ? null : literals;
    }

    private static String unquote(String text) {
        return text.length() > 1 && text.startsWith("\"") && text.endsWith("\"")
            ? text.substring(1, text.length() - 1)
            : text;
    }

    private static String inputName(Element input, int index) {
        String inputVariable = input.getAttributeNS("http://camunda.org/schema/1.0/dmn", "inputVariable");
        if (!inputVariable.isEmpty()) {
            return inputVariable;
        }
        String expression = text(onlyChild(input, "inputExpression"));
        return expression.matches("[A-Za-z][A-Za-z0-9]*") ? expression : "column" + (index + 1);
    }

    private static List<String> entryTexts(Element rule, String entryName) {
        return children(rule, entryName).stream()
            .map(DmnDecisionTable::text)
            .toList();
    }

    /**
     * A second {@code <text>} in one cell is invalid DMN that Camunda refuses to deploy, so reject it here
     * rather than reading the first and leaving the file to fail in the pipeline.
     */
    private static String text(Element element) {
        List<Element> texts = children(element, "text");
        if (texts.size() > 1) {
            throw new IllegalStateException("Expected one <text> in <%s id=\"%s\"> but found %d"
                                                .formatted(element.getLocalName(), element.getAttribute("id"),
                                                           texts.size()));
        }
        return texts.isEmpty() ? "" : texts.getFirst().getTextContent().trim();
    }

    private static List<Element> children(Element parent, String localName) {
        List<Element> found = new ArrayList<>();
        NodeList childNodes = parent.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node child = childNodes.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE && localName.equals(child.getLocalName())) {
                found.add((Element) child);
            }
        }
        return found;
    }

    private static Element onlyChild(Element parent, String localName) {
        List<Element> found = children(parent, localName);
        if (found.size() != 1) {
            throw new IllegalStateException("Expected one <%s> but found %d".formatted(localName, found.size()));
        }
        return found.getFirst();
    }

    private static Document parse(String fileName) {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        try {
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            try (InputStream dmn = DmnDecisionTable.class.getResourceAsStream("/dmn/" + fileName)) {
                if (dmn == null) {
                    throw new IllegalArgumentException("No DMN file on the classpath at /dmn/" + fileName);
                }
                return factory.newDocumentBuilder().parse(dmn);
            }
        } catch (ParserConfigurationException | SAXException e) {
            throw new IllegalStateException(fileName + " is not well-formed DMN XML", e);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
