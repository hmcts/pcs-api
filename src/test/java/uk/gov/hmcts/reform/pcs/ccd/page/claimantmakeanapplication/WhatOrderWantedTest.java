package uk.gov.hmcts.reform.pcs.ccd.page.claimantmakeanapplication;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.api.DisplayContext;
import uk.gov.hmcts.ccd.sdk.api.Field;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;

import static org.assertj.core.api.Assertions.assertThat;

class WhatOrderWantedTest extends BasePageTest {

    @Test
    void shouldOfferDraftWordingWithoutRequiringIt() {
        setPageUnderTest(new WhatOrderWanted());

        assertThat(event.getFields().getFields().stream().map(Field.FieldBuilder::build).toList())
            .filteredOn(field -> "xui_genapp_DraftOrderWording".equals(field.getId()))
            .singleElement()
            .satisfies(field -> {
                assertThat(field.getContext()).isEqualTo(DisplayContext.Optional);
                assertThat(field.getShowCondition()).isEqualTo("xui_genapp_DraftOrderWordingAllowed=\"YES\"");
            });
    }

    @Test
    void shouldOfferOptionalConditionalDraftWordingOnTheDefendantSolicitorApplicationPage() {
        setPageUnderTest(new uk.gov.hmcts.reform.pcs.ccd.page.makeanapplication.WhatOrderWanted());

        assertThat(event.getFields().getFields().stream().map(Field.FieldBuilder::build).toList())
            .filteredOn(field -> "xui_genapp_DraftOrderWording".equals(field.getId()))
            .singleElement()
            .satisfies(field -> {
                assertThat(field.getContext()).isEqualTo(DisplayContext.Optional);
                assertThat(field.getShowCondition()).isEqualTo("xui_genapp_DraftOrderWordingAllowed=\"YES\"");
            });
    }

    @Test
    void shouldNotOfferDraftWordingOnTheStaffEnterApplicationPage() {
        setPageUnderTest(new uk.gov.hmcts.reform.pcs.ccd.page.caseworker.entergenapp.ApplicationDetails(null, null));

        assertThat(event.getFields().getFields().stream().map(Field.FieldBuilder::build).toList())
            .noneMatch(field -> "xui_genapp_DraftOrderWording".equals(field.getId()));
    }

}
