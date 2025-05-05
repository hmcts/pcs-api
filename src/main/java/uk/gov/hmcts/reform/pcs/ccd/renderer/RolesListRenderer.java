package uk.gov.hmcts.reform.pcs.ccd.renderer;

import io.pebbletemplates.pebble.PebbleEngine;
import io.pebbletemplates.pebble.template.PebbleTemplate;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;
import uk.gov.hmcts.reform.pcs.exception.TemplateRenderingException;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;
import java.util.Map;

@Component
public class RolesListRenderer {

    private final PebbleEngine pebbleEngine;

    public RolesListRenderer(PebbleEngine pebbleEngine) {
        this.pebbleEngine = pebbleEngine;
    }

    public String render(UserInfo userInfo, List<String> roles) {
        PebbleTemplate compiledTemplate = pebbleEngine.getTemplate("rolesList");
        Writer writer = new StringWriter();

        Map<String, Object> context = Map.of(
            "currentLogin", userInfo.getSub(),
            "roles", roles
        );

        try {
            compiledTemplate.evaluate(writer, context);
        } catch (IOException e) {
            throw new TemplateRenderingException("Failed to render template", e);
        }

        return writer.toString();
    }

}
