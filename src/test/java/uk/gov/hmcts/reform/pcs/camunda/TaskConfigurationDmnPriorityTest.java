package uk.gov.hmcts.reform.pcs.camunda;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.DocumentBuilderFactory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ExUI labels a task from its majorPriority (rpx-xui-webapp priority-field.component.ts):
 * 2000 or less is Urgent, exactly 5000 is Low/Medium/High by priority date, over 5000 is Low,
 * and anything in between is always High. A task in that last band shows High the moment
 * it is created, whatever its due date.
 */
class TaskConfigurationDmnPriorityTest {

    private static final String DMN = "/dmn/wa-task-configuration-possession_service.dmn";
    private static final int URGENT_LIMIT = 2000;
    private static final int DATE_DRIVEN = 5000;

    private static Map<String, Integer> majorPriorityByTaskType;

    @BeforeAll
    static void readMajorPriorities() throws Exception {
        majorPriorityByTaskType = new LinkedHashMap<>();
        try (InputStream dmn = TaskConfigurationDmnPriorityTest.class.getResourceAsStream(DMN)) {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            Document document = factory.newDocumentBuilder().parse(dmn);

            NodeList rules = document.getElementsByTagNameNS("*", "rule");
            for (int i = 0; i < rules.getLength(); i++) {
                Element rule = (Element) rules.item(i);
                List<String> outputs = texts(rule, "outputEntry");
                if (!"\"majorPriority\"".equals(outputs.get(0))) {
                    continue;
                }
                String taskTypes = texts(rule, "inputEntry").get(2);
                int priority = Integer.parseInt(outputs.get(1));
                for (String taskType : taskTypes.split(",")) {
                    majorPriorityByTaskType.put(taskType.trim().replace("\"", ""), priority);
                }
            }
        }
    }

    @Test
    void shouldConfigureMajorPriorityForTaskTypes() {
        assertThat(majorPriorityByTaskType).isNotEmpty();
    }

    @Test
    void shouldNotStartAnyTaskAsHighPriority() {
        Map<String, Integer> alwaysHigh = new LinkedHashMap<>();
        majorPriorityByTaskType.forEach((taskType, priority) -> {
            if (priority > URGENT_LIMIT && priority < DATE_DRIVEN) {
                alwaysHigh.put(taskType, priority);
            }
        });

        assertThat(alwaysHigh)
            .as("task types ExUI shows as High as soon as they are created")
            .isEmpty();
    }

    @Test
    void shouldDrivePriorityByDueDateForNewHearingAndCaseFlagTasks() {
        assertThat(majorPriorityByTaskType)
            .containsEntry("NewClaimCreateNewHearing", DATE_DRIVEN)
            .containsEntry("ReviewCaseFlagRequest", DATE_DRIVEN)
            .containsEntry("ReviewCaseFlag", DATE_DRIVEN);
    }

    private static List<String> texts(Element rule, String entryName) {
        NodeList entries = rule.getElementsByTagNameNS("*", entryName);
        List<String> texts = new ArrayList<>();
        for (int i = 0; i < entries.getLength(); i++) {
            texts.add(entries.item(i).getTextContent().trim());
        }
        return texts;
    }
}
