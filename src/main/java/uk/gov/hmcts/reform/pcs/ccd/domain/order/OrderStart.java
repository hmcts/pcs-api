package uk.gov.hmcts.reform.pcs.ccd.domain.order;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.entity.DraftOrderEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * What the frontend is sent when an order event starts: the order, and the case it is made on. The make order
 * event sends the judge their working order, or the order a caseworker returned to them with its query; the
 * confirm order review event sends the caseworker the order awaiting review.
 */
public record OrderStart(Order order, CaseContext caseContext) {

    /**
     * The order; for a judge's working order, no id means they have no draft on the case yet. An order
     * with an application decides that application.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Order(UUID id,
                        DraftOrderState state,
                        long version,
                        String orderType,
                        Map<String, Object> formData,
                        JsonNode docweaveSnapshot,
                        String queryFromCaseworker,
                        UUID genAppId) {

        public static Order of(DraftOrderEntity order) {
            return of(order, null);
        }

        /** An order a caseworker returned to the judge, with the query they returned it with. */
        public static Order of(DraftOrderEntity order, String queryFromCaseworker) {
            return new Order(order.getId(), order.getState(), order.getVersion(), order.getOrderType(),
                order.getFormData(), order.getDocweaveSnapshot(), queryFromCaseworker, order.getGenAppId());
        }

        /** A judge's first order on the case, or on one of its applications. */
        public static Order none(UUID genAppId) {
            return new Order(null, DraftOrderState.DRAFT, 0, null, Map.of(), null, null, genAppId);
        }
    }

    /**
     * Open means issued and not concluded by an order: nothing yet closes a counterclaim. An order
     * deciding an application has that application.
     */
    public record CaseContext(long caseReference,
                              AddressUK propertyAddress,
                              List<Party> claimants,
                              List<Party> defendants,
                              CaseFacts caseFacts,
                              boolean openCounterclaim,
                              boolean openApplication,
                              Application application) {
    }

    /**
     * The general application an order decides, as the judge deciding it reads it: what was applied
     * for, by whom, the applicant's answers, the order they asked for, their documents, and what court
     * staff said when they referred it.
     */
    public record Application(UUID id,
                              String reference,
                              String type,
                              String somethingElseDetails,
                              String applicant,
                              LocalDate submittedOn,
                              String within14Days,
                              String otherPartiesAgreed,
                              String withoutNotice,
                              String withoutNoticeReason,
                              String whatOrderWanted,
                              List<ApplicationDocument> documents,
                              LocalDate referredOn,
                              String referralNote) {
    }

    /** A document filed with an application; the id is the document's id in the document store. */
    public record ApplicationDocument(String id, String fileName) {
    }

    public record CaseFacts(LocalDate tenancyStartDate,
                            String tenancyType,
                            LocalDate noticeDate,
                            BigDecimal currentRent,
                            String rentFrequency,
                            String groundsPleaded) {
    }

    public record Party(String id, String name) {
    }
}
