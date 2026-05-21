package uk.gov.hmcts.reform.pcs.xui;

import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class XuiRedirectLandingController {

    @GetMapping(value = "/cases/case-create/{jurisdiction}/{caseType}/{eventId}", produces = MediaType.TEXT_HTML_VALUE)
    public String caseCreate(@PathVariable String jurisdiction,
                             @PathVariable String caseType,
                             @PathVariable String eventId,
                             @RequestParam Map<String, String> queryParams) {
        return page("create", null, jurisdiction, caseType, eventId, queryParams);
    }

    @GetMapping(value = "/cases/{caseReference}/event/{eventId}", produces = MediaType.TEXT_HTML_VALUE)
    public String caseEvent(@PathVariable String caseReference,
                            @PathVariable String eventId,
                            @RequestParam Map<String, String> queryParams) {
        return page("event", caseReference, null, null, eventId, queryParams);
    }

    private String page(String journey,
                        String caseReference,
                        String jurisdiction,
                        String caseType,
                        String eventId,
                        Map<String, String> queryParams) {
        return """
            <!doctype html>
            <html lang="en">
              <head><title>PCS XUI redirect target</title></head>
              <body>
                <h1>PCS XUI redirect target</h1>
                <dl>
                  <dt>journey</dt><dd id="journey">%s</dd>
                  <dt>caseReference</dt><dd id="caseReference">%s</dd>
                  <dt>jurisdiction</dt><dd id="jurisdiction">%s</dd>
                  <dt>caseType</dt><dd id="caseType">%s</dd>
                  <dt>eventId</dt><dd id="eventId">%s</dd>
                  <dt>expectedSub</dt><dd id="expectedSub">%s</dd>
                  <dt>taskId</dt><dd id="taskId">%s</dd>
                </dl>
              </body>
            </html>
            """.formatted(
                escape(journey),
                escape(caseReference),
                escape(jurisdiction),
                escape(caseType),
                escape(eventId),
                escape(queryParams.get("expected_sub")),
                escape(queryParams.get("tid"))
            );
    }

    private String escape(String value) {
        return value == null ? "" : value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;");
    }
}
