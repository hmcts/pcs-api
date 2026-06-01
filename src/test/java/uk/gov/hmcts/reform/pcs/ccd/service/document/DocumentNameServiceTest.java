package uk.gov.hmcts.reform.pcs.ccd.service.document;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.GenAppEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;

class DocumentNameServiceTest {

    private DocumentNameService underTest;

    @BeforeEach
    void setUp() {
        underTest = new DocumentNameService();
    }

    @ParameterizedTest
    @MethodSource("genAppNamingScenarios")
    void shouldAddGenAppNumberAndPartyLabelToGenAppDocument(PartyRole partyRole,
                                                            String originalFilename,
                                                            String expectedFilename) {
        // Given
        UUID applicantPartyId = UUID.randomUUID();
        GenAppEntity genAppEntity = GenAppEntity.builder()
            .rank(5)
            .build();

        PartyEntity party1 = PartyEntity.builder()
            .id(applicantPartyId)
            .build();

        ClaimPartyEntity claimParty1 = ClaimPartyEntity.builder()
            .party(party1)
            .rank(2)
            .role(partyRole)
            .build();

        ClaimEntity mainClaim = ClaimEntity.builder()
            .claimParties(List.of(claimParty1))
            .build();

        // When
        String updatedFilename
            = underTest.appendGenAppPostfix(originalFilename, genAppEntity, mainClaim, applicantPartyId);

        // Then
        assertThat(updatedFilename).isEqualTo(expectedFilename);
    }

    private static Stream<Arguments> genAppNamingScenarios() {
        return Stream.of(
            // Party role, original filename, expected updated filename
            argumentSet("null filename",
                        PartyRole.DEFENDANT, null, null),
            argumentSet("no extension, defendant",
                        PartyRole.DEFENDANT, "sample", "sample GA5 - Defendant 2"),
            argumentSet("with extension, defendant",
                        PartyRole.DEFENDANT, "sample.pdf", "sample GA5 - Defendant 2.pdf"),
            argumentSet("no extension, claimant",
                        PartyRole.CLAIMANT, "sample", "sample GA5 - Claimant 2"),
            argumentSet("with extension, claimant",
                        PartyRole.CLAIMANT, "sample.pdf", "sample GA5 - Claimant 2.pdf"),
            argumentSet("no extension, other party type",
                        PartyRole.UNDERLESSEE_OR_MORTGAGEE, "sample", "sample GA5"),
            argumentSet("with extension, other party type",
                        PartyRole.UNDERLESSEE_OR_MORTGAGEE, "sample.pdf", "sample GA5.pdf")
        );
    }

}
