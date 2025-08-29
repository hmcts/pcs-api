package uk.gov.hmcts.reform.pcs.ccd;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.ConfigBuilder;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentCategory;

import static java.lang.System.getenv;
import static java.util.Optional.ofNullable;
import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.NEVER_SHOW;

/**
 * Setup some common possessions case type configuration.
 */
@Component
public class CaseType implements CCDConfig<PCSCase, State, UserRole> {

    private static final String CASE_TYPE_ID = "PCS";
    private static final String CASE_TYPE_NAME = "Civil Possessions";
    private static final String CASE_TYPE_DESCRIPTION = "Civil Possessions Case Type";
    private static final String JURISDICTION_ID = "PCS";
    private static final String JURISDICTION_NAME = "Possessions";
    private static final String JURISDICTION_DESCRIPTION = "Possessions Jurisdiction";

    public static String getCaseType() {
        return withChangeId(CASE_TYPE_ID, "-");
    }

    public static String getCaseTypeName() {
        return withChangeId(CASE_TYPE_NAME, " ");
    }

    private static String withChangeId(String base, String separator) {
        return ofNullable(getenv().get("CHANGE_ID"))
            .map(changeId -> base + separator + changeId)
            .orElse(base);
    }

    @Override
    public void configure(final ConfigBuilder<PCSCase, State, UserRole> builder) {
        builder.setCallbackHost(getenv().getOrDefault("CASE_API_URL", "http://localhost:3206"));

        builder.decentralisedCaseType(getCaseType(), getCaseTypeName(), CASE_TYPE_DESCRIPTION);
        builder.jurisdiction(JURISDICTION_ID, JURISDICTION_NAME, JURISDICTION_DESCRIPTION);
        buildSupportingDocumentsCaseFileViewTab(builder);

        String paymentLabel = "Payment Status";

        builder.searchInputFields()
            .caseReferenceField()
            .field(PCSCase::getPaymentStatus, paymentLabel);

        builder.searchCasesFields()
            .caseReferenceField()
            .field(PCSCase::getPaymentStatus, paymentLabel);

        builder.searchResultFields()
            .caseReferenceField()
            .field(PCSCase::getPaymentStatus, paymentLabel);

        builder.workBasketInputFields()
            .caseReferenceField()
            .field(PCSCase::getClaimantName, "Claimant Name");

        builder.workBasketResultFields()
            .caseReferenceField()
            .field(PCSCase::getPropertyAddress, "Property Address");

        builder.tab("summary", "Property Details")
            .field(PCSCase::getPropertyAddress);

        builder.tab("CaseHistory", "History")
            .field("caseHistory");

        builder.tab("ClaimPayment", "Payment")
            .showCondition("claimPaymentTabMarkdown!=\"\"")
            .label("claimPaymentTabMarkdownLabel", null, "${claimPaymentTabMarkdown}")
            .field("claimPaymentTabMarkdown", NEVER_SHOW);

        builder.tab("hidden", "HiddenFields")
            .showCondition(NEVER_SHOW)
            .field(PCSCase::getPageHeadingMarkdown);
    }

    private void buildSupportingDocumentsCaseFileViewTab(ConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        configBuilder.categories(UserRole.PCS_CASE_WORKER)
            .categoryID(DocumentCategory.CATEGORY_A.getLabel())
            .categoryLabel(DocumentCategory.CATEGORY_A.getLabel())
            .displayOrder(1)
            .build();
        configBuilder.categories(UserRole.PCS_CASE_WORKER)
            .categoryID(DocumentCategory.CATEGORY_B.getLabel())
            .categoryLabel(DocumentCategory.CATEGORY_B.getLabel())
            .displayOrder(2).parentCategoryID(DocumentCategory.CATEGORY_A.getLabel())
            .build();
        configBuilder.categories(UserRole.PCS_CASE_WORKER)
            .categoryID("Category_C")
            .categoryLabel("Category_C")
            .displayOrder(3).parentCategoryID(DocumentCategory.CATEGORY_B.getLabel())
            .build();
        configBuilder.categories(UserRole.PCS_CASE_WORKER)
            .categoryID("Category_D")
            .categoryLabel("Category_D")
            .displayOrder(4)
            .build();
        configBuilder.tab("caseFileView", "Supporting Documents")
            .forRoles(UserRole.PCS_CASE_WORKER)
            .field(PCSCase::getCaseFileView, null, "#ARGUMENT(CaseFileView)");
    }
}
