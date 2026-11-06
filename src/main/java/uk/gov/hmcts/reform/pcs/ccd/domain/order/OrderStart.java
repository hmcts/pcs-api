package uk.gov.hmcts.reform.pcs.ccd.domain.order;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.OrderEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.OrderStepEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.hearing.HearingEntity;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * What the frontend is sent when an order event starts: the order, the case it is made on, the hearing it is
 * made for, if any, and the application the judge's task asks them to decide, if it asks them to decide one.
 */
public record OrderStart(Order order, CaseContext caseContext, Hearing hearing, UUID taskGenAppId) {

    /**
     * No id means the judge has no draft for the hearing, or made without one, yet. The version is the latest
     * step's id, sent back with a change to the order.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Order(UUID id,
                        long version,
                        OrderType orderType,
                        Map<String, Object> formData,
                        JsonNode docweaveSnapshot,
                        String queryFromCaseworker) {

        public static final Order NONE = new Order(null, 0, null, Map.of(), null, null);

        /** An order as its latest step left it, with the query it was returned with if it was. */
        public static Order of(OrderEntity order, OrderStepEntity latest, String queryFromCaseworker) {
            return new Order(order.getId(), latest.getId(), latest.getOrderType(),
                latest.getFormData(), latest.getDocweaveSnapshot(), queryFromCaseworker);
        }
    }

    /**
     * Open means issued: nothing yet closes a counterclaim. The applications are every one the order can decide,
     * in the order they were made.
     */
    public record CaseContext(long caseReference,
                              AddressUK propertyAddress,
                              List<Party> claimants,
                              List<Party> defendants,
                              boolean openCounterclaim,
                              List<Application> applications) {
    }

    /**
     * The general application an order decides, as the judge deciding it reads it: what was applied for,
     * by whom, the applicant's answers, the order they asked for, and their documents. The id names its answers on
     * the make order form. An application to suspend the warrant of possession is decided by suspending the
     * warrant on conditions or saying when the eviction may go ahead.
     */
    public record Application(UUID id,
                              boolean warrantSuspension,
                              String reference,
                              String type,
                              String somethingElseDetails,
                              String applicant,
                              LocalDate submittedOn,
                              VerticalYesNo within14Days,
                              VerticalYesNo otherPartiesAgreed,
                              VerticalYesNo withoutNotice,
                              String withoutNoticeReason,
                              String whatOrderWanted,
                              List<ApplicationDocument> documents) {
    }

    /** A document filed with an application; the id is the document's id in the document store. */
    public record ApplicationDocument(UUID id, String fileName) {
    }

    /** The claim gives the first six; a judge records the rest at a hearing. */
    public record CaseFacts(LocalDate tenancyStartDate,
                            String tenancyType,
                            LocalDate noticeDate,
                            BigDecimal currentRent,
                            String rentFrequency,
                            String groundsPleaded,
                            BigDecimal arrearsAtNotice,
                            BigDecimal arrearsAtIssue,
                            LocalDate lastPaymentDate,
                            BigDecimal lastPayment,
                            BigDecimal arrearsToday) {
    }

    /**
     * The hearing an order is made for. The facts are this hearing's if they have been recorded,
     * otherwise the latest earlier hearing's, otherwise the claim's; recorded says who recorded them,
     * and is null for the claim's. The version is null until this hearing's facts are recorded, and
     * goes back with the judge's change to them. The earlier notes are the case's judicial notes made
     * before this hearing, newest first. A case in Wales has contracts where one in England has tenancies.
     */
    public record Hearing(int id,
                          LocalDateTime date,
                          boolean inWales,
                          Long version,
                          CaseFacts caseFacts,
                          Recorded recorded,
                          String note,
                          List<JudicialNote> earlierNotes) {

        /** The order's hearing as court staff reviewing it see it: when it was, without what the judge records. */
        public static Hearing ofOrder(OrderEntity order) {
            HearingEntity hearing = order.getHearing();
            return hearing == null ? null : new Hearing(hearing.getId(), hearing.getHearingDate(),
                order.getPcsCase().getLegislativeCountry() == LegislativeCountry.WALES, null, null, null, null,
                List.of());
        }
    }

    /** Who last updated the facts, and when. */
    public record Recorded(String by, Instant on) {
    }

    /** A judicial note, with the hearing it was made at if it was made at one. */
    public record JudicialNote(String by, Instant createdOn, LocalDateTime hearingDate, String note) {
    }

    public record Party(String id, String name) {
    }
}
