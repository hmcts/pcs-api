package uk.gov.hmcts.reform.pcs.bulkclaim.xml;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.pcs.bulkclaim.model.BulkFailReason;
import uk.gov.hmcts.reform.pcs.bulkclaim.model.BulkValidationError;
import uk.gov.hmcts.reform.pcs.bulkclaim.xml.generated.BulkClaimResponse;
import uk.gov.hmcts.reform.pcs.bulkclaim.xml.generated.BulkClaimStatusResponse;
import uk.gov.hmcts.reform.pcs.bulkclaim.xml.generated.ClaimResponseType;
import uk.gov.hmcts.reform.pcs.bulkclaim.xml.generated.MortgageClaimSet;
import uk.gov.hmcts.reform.pcs.bulkclaim.xml.generated.PaymentStatusType;
import uk.gov.hmcts.reform.pcs.bulkclaim.xml.generated.RentalClaimSet;
import uk.gov.hmcts.reform.pcs.bulkclaim.xml.generated.StatusType;
import uk.gov.hmcts.reform.pcs.bulkclaim.xml.generated.TenancyTypeCode;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

class BulkClaimXmlCodecTest {

    private final BulkClaimXmlCodec codec = new BulkClaimXmlCodec();
    private final BulkClaimResponseBuilder responseBuilder = new BulkClaimResponseBuilder();

    @Test
    void shouldParseRentalEnglandAssuredSample() throws IOException {
        RentalClaimSet claimSet = codec.parseRental(sample("rental-england-assured-arrears.xml"));

        assertThat(claimSet.getPaymentAccountNumber()).isEqualTo("PBA1234567");
        assertThat(claimSet.getRentalClaim()).hasSize(1);
        RentalClaimSet.RentalClaim claim = claimSet.getRentalClaim().getFirst();
        assertThat(claim.getCustomerUniqueIdentifier()).isEqualTo("CLAIM-0001");
        assertThat(claim.getClaimParticulars().getTenancyType()).isEqualTo(TenancyTypeCode.ASSURED_TENANCY);
        assertThat(claim.getClaimParticulars().getTenancyCommenceDate()).isEqualTo(LocalDate.of(2022, 3, 1));
        assertThat(claim.getClaimParticulars().getDailyRent()).isEqualByComparingTo(new BigDecimal("26.30"));
        assertThat(claim.getClaimParticulars().getPaymentHistory().getPaymentLine()).hasSize(2);
    }

    @Test
    void shouldParseRentalSecureSample() throws IOException {
        RentalClaimSet claimSet = codec.parseRental(sample("rental-secure-tenancy.xml"));

        RentalClaimSet.RentalClaim claim = claimSet.getRentalClaim().getFirst();
        assertThat(claim.getPaymentAccountNumber()).isEqualTo("PBA1111111");
        assertThat(claim.getClaimParticulars().getTenancyType()).isEqualTo(TenancyTypeCode.SECURE_CONTRACT);
        assertThat(claim.getClaimParticulars().getGrounds().getGround().getFirst().getEstateManagementGround())
            .extracting(g -> g.getCode().value(), g -> g.getReason())
            .containsExactly(tuple("BUILDING_WORKS", "The block is being redeveloped."),
                             tuple("REDEVELOPMENT_SCHEMES", "The estate is part of a regeneration scheme."));
    }

    @Test
    void shouldParseMortgageSample() throws IOException {
        MortgageClaimSet claimSet = codec.parseMortgage(sample("mortgage-claim.xml"));

        MortgageClaimSet.MortgageClaim claim = claimSet.getMortgageClaim().getFirst();
        assertThat(claim.getDefendant().getFirst().isIsOccupant()).isFalse();
        assertThat(claim.getParticularsOfClaim().getLoanAmount()).isEqualByComparingTo("200000.00");
        assertThat(claim.getParticularsOfClaim().getNoticeServedDate()).isEqualTo(LocalDate.of(2026, 10, 20));
        assertThat(claim.getOccupant()).hasSize(1);
    }

    @Test
    void shouldReportAllSchemaErrorsForInvalidSample() throws IOException {
        byte[] invalid = sample("rental-invalid.xml");

        assertThatThrownBy(() -> codec.parseRental(invalid))
            .isInstanceOfSatisfying(BulkClaimSchemaValidationException.class, e ->
                assertThat(e.getErrors()).hasSizeGreaterThan(1).allSatisfy(error ->
                    assertThat(error).matches("\\d+:\\d+ .+"))
                    .anySatisfy(error -> assertThat(error).startsWith("3:"))
                    .anySatisfy(error -> assertThat(error).startsWith("5:"))
                    .anySatisfy(error -> assertThat(error).startsWith("8:"))
                    .anySatisfy(error -> assertThat(error).startsWith("14:")));
    }

    @Test
    void shouldRejectWrongRootElement() throws IOException {
        byte[] mortgage = sample("mortgage-claim.xml");

        assertThatThrownBy(() -> codec.parseRental(mortgage))
            .isInstanceOf(BulkClaimSchemaValidationException.class);
    }

    @Test
    void shouldRejectExternalEntityDoctype() {
        byte[] xxe = ("""
            <?xml version="1.0"?>
            <!DOCTYPE RentalClaimSet [<!ENTITY xxe SYSTEM "file:///etc/passwd">]>
            <RentalClaimSet xmlns="http://hmcts.gov.uk/pcs/bulkclaim/v2">
              <Version>&xxe;</Version>
            </RentalClaimSet>
            """).getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> codec.parseRental(xxe))
            .isInstanceOfSatisfying(BulkClaimSchemaValidationException.class, e ->
                assertThat(String.join(" ", e.getErrors())).doesNotContain("root:"));
    }

    @Test
    void shouldRejectBillionLaughs() {
        byte[] bomb = ("""
            <?xml version="1.0"?>
            <!DOCTYPE RentalClaimSet [
              <!ENTITY a "aaaaaaaaaa">
              <!ENTITY b "&a;&a;&a;&a;&a;&a;&a;&a;&a;&a;">
              <!ENTITY c "&b;&b;&b;&b;&b;&b;&b;&b;&b;&b;">
              <!ENTITY d "&c;&c;&c;&c;&c;&c;&c;&c;&c;&c;">
            ]>
            <RentalClaimSet xmlns="http://hmcts.gov.uk/pcs/bulkclaim/v2">
              <Version>&d;</Version>
            </RentalClaimSet>
            """).getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> codec.parseRental(bomb))
            .isInstanceOf(BulkClaimSchemaValidationException.class);
    }

    @Test
    void shouldRejectMalformedXml() {
        assertThatThrownBy(() -> codec.parseRental("<RentalClaimSet".getBytes(StandardCharsets.UTF_8)))
            .isInstanceOf(BulkClaimSchemaValidationException.class);
    }

    @Test
    void shouldRoundTripBulkClaimResponse() throws Exception {
        BulkClaimResponse response = responseBuilder.overallFailure(
            BulkFailReason.CLAIMS_FAILED,
            List.of(
                responseBuilder.claimSuccess("A-1", "1234567890123456"),
                responseBuilder.claimFailure("A-2", BulkFailReason.VALIDATION_FAILED,
                                             List.of(new BulkValidationError("/RentalClaim[2]/Grounds", "BAD", "Bad"))),
                responseBuilder.claimPending("A-3")));

        String xml = new String(codec.write(response), StandardCharsets.UTF_8);

        assertThat(xml)
            .contains("<ClaimNumber>1234-5678-9012-3456</ClaimNumber>")
            .contains("<CaseReference>1234567890123456</CaseReference>")
            .contains("<FailReasonCode>4</FailReasonCode>")
            .contains("<Path>/RentalClaim[2]/Grounds</Path>");

        var unmarshalled = (BulkClaimResponse) jakarta.xml.bind.JAXBContext
            .newInstance(BulkClaimResponse.class)
            .createUnmarshaller()
            .unmarshal(new java.io.StringReader(xml));
        assertThat(unmarshalled.getBulkStatus().getStatusCode()).isEqualTo(2);
        assertThat(unmarshalled.getClaimResponse()).extracting(c -> c.getClaimStatus().getStatusCode())
            .containsExactly(1, 2, 3);
    }

    @Test
    void shouldWriteStatusResponse() {
        BulkClaimStatusResponse response = new BulkClaimStatusResponse();
        response.setCustomerUniqueIdentifier("A-1");
        StatusType status = responseBuilder.status(BulkFailReason.CLAIM_SUCCESS);
        response.setClaimStatus(status);
        response.setPaymentStatus(PaymentStatusType.PENDING);

        String xml = new String(codec.write(response), StandardCharsets.UTF_8);

        assertThat(xml).contains("<PaymentStatus>PENDING</PaymentStatus>")
            .contains("<CustomerUniqueIdentifier>A-1</CustomerUniqueIdentifier>");
    }

    @Test
    void shouldCreateSuccessResponse() {
        ClaimResponseType claim = responseBuilder.claimSuccess("A-1", "1234567890123456");

        BulkClaimResponse response = responseBuilder.overallSuccess(List.of(claim));

        assertThat(response.getBulkStatus().getStatusCode()).isEqualTo(1);
        assertThat(response.getBulkStatus().getFailReasonCode()).isNull();
    }

    @Test
    void shouldComputeClaimHashIndependentOfWhitespace() throws IOException {
        String original = new String(sample("rental-england-assured-arrears.xml"), StandardCharsets.UTF_8);
        String compact = original.replaceAll(">\\s+<", "><");
        String spaced = original.replaceAll(">\\s*<", ">\n\t\t\n<");

        String hash = codec.claimHash(codec.parseRental(original.getBytes(StandardCharsets.UTF_8))
                                          .getRentalClaim().getFirst());

        assertThat(hash).hasSize(64)
            .isEqualTo(codec.claimHash(codec.parseRental(compact.getBytes(StandardCharsets.UTF_8))
                                           .getRentalClaim().getFirst()))
            .isEqualTo(codec.claimHash(codec.parseRental(spaced
                                                             .getBytes(StandardCharsets.UTF_8))
                                           .getRentalClaim().getFirst()));
    }

    @Test
    void shouldChangeClaimHashWhenContentChanges() throws IOException {
        String original = new String(sample("rental-england-assured-arrears.xml"), StandardCharsets.UTF_8);
        String changed = original.replace("<DailyRent>26.30</DailyRent>", "<DailyRent>26.31</DailyRent>");

        assertThat(codec.claimHash(codec.parseRental(changed.getBytes(StandardCharsets.UTF_8))
                                       .getRentalClaim().getFirst()))
            .isNotEqualTo(codec.claimHash(codec.parseRental(original.getBytes(StandardCharsets.UTF_8))
                                              .getRentalClaim().getFirst()));
    }

    @Test
    void shouldComputeSha256Hex() {
        assertThat(codec.sha256Hex("abc".getBytes(StandardCharsets.UTF_8)))
            .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
    }

    private byte[] sample(String name) throws IOException {
        try (InputStream stream = getClass().getResourceAsStream("/bulkclaim/v2/" + name)) {
            assertThat(stream).isNotNull();
            return stream.readAllBytes();
        }
    }
}
