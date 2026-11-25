package uk.gov.hmcts.reform.pcs.ccd.domain.order;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import uk.gov.hmcts.ccd.sdk.bankholidays.BankHoliday;
import uk.gov.hmcts.ccd.sdk.bankholidays.BankHolidays;
import uk.gov.hmcts.ccd.sdk.bankholidays.Division;
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

import static uk.gov.hmcts.reform.pcs.config.ClockConfiguration.UK_ZONE_ID;

/**
 * What the frontend is sent when an order event starts: the order, the case it is made on, the hearing it is
 * made for, if any, the case's other hearings a judge can choose to make it at instead, each as they would start an
 * order for it, the application the judge's task asks them to decide, if it asks them to decide one, and the
 * judge the order is made by, as it names them as made before them. The bank holidays are England and Wales's as
 * GOV.UK publishes them, which a date the order gives to apply by moves off. Court staff reviewing an order choose
 * no hearing, so are sent no others.
 */
public record OrderStart(Order order, CaseContext caseContext, Hearing hearing, List<Hearing> otherHearings,
                         UUID taskGenAppId, String judge, List<LocalDate> bankHolidays) {

    /** The judge as an order names them: their title and name, or a placeholder for them to fill in. */
    public static String judge(String name) {
        // TODO: the judge's title from Judicial Reference Data. Until then every judge is a District Judge.
        return name == null || name.isBlank() ? "[judge’s name and title]" : "District Judge " + name;
    }

    /** England and Wales's bank holidays, as the frontend moves a date off them. */
    public static List<LocalDate> bankHolidays(BankHolidays bankHolidays) {
        return bankHolidays.holidays(Division.ENGLAND_AND_WALES).stream().map(BankHoliday::date).toList();
    }

    /**
     * No id, and no state, means the judge has no draft on the case yet. The version is the latest step's id, sent
     * back with a change to the order. Made on is the day an order sent for review is made, which it names: absent
     * while the judge is still making it.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Order(UUID id,
                        long version,
                        OrderState state,
                        OrderType orderType,
                        Map<String, Object> formData,
                        JsonNode docweaveSnapshot,
                        String queryFromCaseworker,
                        LocalDate madeOn) {

        public static final Order NONE = new Order(null, 0, null, null, Map.of(), null, null, null);

        /** An order as its latest step left it, with the query it was returned with if it was. */
        public static Order of(OrderEntity order, OrderStepEntity latest, String queryFromCaseworker) {
            return new Order(order.getId(), latest.getId(), order.getState(), latest.getOrderType(),
                latest.getFormData(), latest.getDocweaveSnapshot(), queryFromCaseworker, null);
        }

        /** An order sent for review as its judge sent it, which is made on the day it names. */
        public static Order submitted(OrderEntity order, OrderStepEntity submitted) {
            return new Order(order.getId(), submitted.getId(), order.getState(), submitted.getOrderType(),
                submitted.getFormData(), submitted.getDocweaveSnapshot(), null, madeOn(order, submitted));
        }

        /** Made at its hearing, or else when its judge sent it for review, however long court staff take to issue. */
        public static LocalDate madeOn(OrderEntity order, OrderStepEntity submitted) {
            return order.getHearing() != null && order.getHearing().getHearingDate() != null
                ? order.getHearing().getHearingDate().toLocalDate()
                : submitted.getCreatedAt().atZone(UK_ZONE_ID).toLocalDate();
        }
    }

    /**
     * Open means issued: nothing yet closes a counterclaim. The applications are every one the order can decide,
     * in the order they were made. The hearings are the case's that have not been cancelled, earliest first, any of
     * which a judge can choose to make the order at.
     */
    public record CaseContext(long caseReference,
                              AddressUK propertyAddress,
                              List<Party> claimants,
                              List<Party> defendants,
                              boolean openCounterclaim,
                              List<Application> applications,
                              CaseBundle caseBundle,
                              List<HearingOption> hearings,
                              String court) {
    }

    /** One of the case's hearings, as a judge chooses which one an order is made at. */
    public record HearingOption(int id, LocalDateTime date) {
    }

    /** The case's bundle, every document on it in one PDF, for a judge once one has been made. */
    public record CaseBundle(UUID id, String fileName, Instant updatedAt) {
    }

    /**
     * The general application an order decides, as the judge deciding it reads it: what was applied for,
     * by whom, the applicant's answers, the order they asked for, and their documents. The id names its answers on
     * the make order form. An order can suspend enforcement on conditions on an application to suspend it. Listed
     * says an issued order has listed it for a hearing, where it is still to be decided. The draft
     * order wording is the applicant's draft order as rich text (HTML); whatOrderWanted is their explanation, which
     * held the order they asked for before they could give it as a draft.
     */
    public record Application(UUID id,
                              boolean enforcementSuspension,
                              boolean listed,
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

    /** Who last updated the facts, by name or "A judge", and when. */
    public record Recorded(String by, Instant on) {
    }

    /** A judicial note, by its judge's name or "A judge", with the hearing it was made at if it was made at one. */
    public record JudicialNote(String by, Instant createdOn, LocalDateTime hearingDate, String note) {
    }

    public record Party(String id, String name) {
    }
}
