package uk.gov.hmcts.reform.pcs.bulkclaim.prepare.rental;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.bulkclaim.config.BulkClaimProperties;
import uk.gov.hmcts.reform.pcs.bulkclaim.prepare.common.BulkErrorCollector;
import uk.gov.hmcts.reform.pcs.bulkclaim.xml.generated.NoticeServiceMethodType;
import uk.gov.hmcts.reform.pcs.bulkclaim.xml.generated.WalesNoticeType;
import uk.gov.hmcts.reform.pcs.ccd.domain.NoticeServiceMethod;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.pcs.bulkclaim.prepare.rental.RentalPreparerTestSupport.CLOCK;

class WalesNoticeMapperTest {

    private static final String PATH = "ClaimParticulars/WalesNotice";
    private static final String ABSOLUTE = "/RentalClaimSet/RentalClaim[1]/" + PATH;

    private final WalesNoticeMapper mapper =
        new WalesNoticeMapper(new RentalNoticeMapper(new BulkClaimProperties(), CLOCK));
    private final BulkErrorCollector errors = BulkErrorCollector.forClaimPath("/RentalClaimSet/RentalClaim[1]");
    private final PCSCase target = PCSCase.builder().build();

    private static WalesNoticeType notice(boolean served, String type, String statement) {
        WalesNoticeType notice = new WalesNoticeType();
        notice.setServed(served);
        notice.setTypeOfNoticeServed(type);
        notice.setNoticeStatement(statement);
        return notice;
    }

    private static WalesNoticeType postedNotice(String type) {
        WalesNoticeType notice = notice(true, type, null);
        notice.setServiceMethod(NoticeServiceMethodType.FIRST_CLASS_POST);
        notice.setServedDate(LocalDate.of(2026, 10, 5));
        return notice;
    }

    @Test
    void servedNoticeRecordsItsTypeAndHowItWasServed() {
        String type = mapper.apply(postedNotice("RHW20"), target, PATH, errors);

        assertThat(errors.hasErrors()).isFalse();
        assertThat(type).isEqualTo("RHW20");
        assertThat(target.getWalesNoticeDetails().getNoticeServed()).isEqualTo(YesOrNo.YES);
        assertThat(target.getWalesNoticeDetails().getTypeOfNoticeServed()).isEqualTo("RHW20");
        assertThat(target.getNoticeServedDetails().getServiceMethod()).isEqualTo(NoticeServiceMethod.FIRST_CLASS_POST);
        assertThat(target.getNoticeServedDetails().getPostedDate()).isEqualTo(LocalDate.of(2026, 10, 5));
        assertThat(target.getNoticeServedDetails().getUnableToUploadReason())
            .isEqualTo("Not supplied via bulk interface");
        assertThat(target.getWalesNoticeDetails().getNoticeStatement()).isNull();
    }

    @Test
    void servedNoticeIgnoresAStatement() {
        WalesNoticeType notice = postedNotice("RHW20");
        notice.setNoticeStatement("ignored");

        mapper.apply(notice, target, PATH, errors);

        assertThat(errors.hasErrors()).isFalse();
        assertThat(target.getWalesNoticeDetails().getNoticeStatement()).isNull();
    }

    @Test
    void servedNoticeNeedsATypeOfAtMostTenCharacters() {
        mapper.apply(postedNotice(null), target, PATH, errors);
        mapper.apply(postedNotice("12345678901"), target, PATH, errors);

        assertThat(errors.errors()).extracting(error -> error.path() + ":" + error.code()).containsExactly(
            ABSOLUTE + "/TypeOfNoticeServed:REQUIRED", ABSOLUTE + "/TypeOfNoticeServed:TOO_LONG");
    }

    @Test
    void servedNoticeNeedsAServiceMethodAndItsDetails() {
        mapper.apply(notice(true, "RHW20", null), target, PATH, errors);

        WalesNoticeType handed = notice(true, "RHW20", null);
        handed.setServiceMethod(NoticeServiceMethodType.PERSONALLY_HANDED);
        mapper.apply(handed, target, PATH, errors);

        assertThat(errors.errors()).extracting(error -> error.path() + ":" + error.code()).containsExactly(
            ABSOLUTE + "/ServiceMethod:REQUIRED",
            ABSOLUTE + "/ServedDateTime:REQUIRED",
            ABSOLUTE + "/PersonName:REQUIRED");
    }

    @Test
    void unservedNoticeRecordsTheStatement() {
        String type = mapper.apply(notice(false, "ignored", "I am a landlord under a licence."), target, PATH, errors);

        assertThat(errors.hasErrors()).isFalse();
        assertThat(type).isNull();
        assertThat(target.getWalesNoticeDetails().getNoticeServed()).isEqualTo(YesOrNo.NO);
        assertThat(target.getWalesNoticeDetails().getNoticeStatement()).isEqualTo("I am a landlord under a licence.");
        assertThat(target.getWalesNoticeDetails().getTypeOfNoticeServed()).isNull();
        assertThat(target.getNoticeServedDetails()).isNull();
    }

    @Test
    void unservedNoticeNeedsAStatementOfAtMostFiveHundredCharacters() {
        mapper.apply(notice(false, null, " "), target, PATH, errors);
        mapper.apply(notice(false, null, "x".repeat(501)), target, PATH, errors);

        assertThat(errors.errors()).extracting(error -> error.path() + ":" + error.code()).containsExactly(
            ABSOLUTE + "/NoticeStatement:REQUIRED", ABSOLUTE + "/NoticeStatement:TOO_LONG");
    }

    @Test
    void missingNoticeIsRejected() {
        assertThat(mapper.apply(null, target, PATH, errors)).isNull();

        assertThat(errors.errors()).extracting(error -> error.path() + ":" + error.code())
            .containsExactly(ABSOLUTE + ":REQUIRED");
        assertThat(target.getWalesNoticeDetails()).isNull();
    }
}
