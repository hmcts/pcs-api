package uk.gov.hmcts.reform.pcs.ccd.domain.tabs.bundle;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.Document;
import uk.gov.hmcts.ccd.sdk.type.ListValue;

import java.io.IOException;
import java.util.List;

/** The Bundle tab: the case bundle, whether it is up to date, and what is missing from it. */
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class BundleTab {

    @CCD(label = "Status")
    private String status;

    @CCD(label = "Bundle")
    @JsonDeserialize(using = UnprefixedDocumentDeserializer.class)
    private Document bundle;

    @CCD(label = "Last updated")
    private String lastUpdated;

    @CCD(label = "Number of pages")
    private Integer pages;

    @CCD(label = "Documents missing from the bundle")
    private List<ListValue<MissingBundleDocument>> missingDocuments;

    /**
     * Reads the Document as written. Without it, Jackson applies the bundleTab_ prefix to the Document's
     * own fields too, so none of them bind and the bundle reads back empty.
     */
    static class UnprefixedDocumentDeserializer extends StdDeserializer<Document> {

        UnprefixedDocumentDeserializer() {
            super(Document.class);
        }

        @Override
        public Document deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            return context.readValue(parser, Document.class);
        }
    }
}
