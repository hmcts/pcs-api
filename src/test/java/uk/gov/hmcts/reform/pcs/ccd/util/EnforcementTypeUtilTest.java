package uk.gov.hmcts.reform.pcs.ccd.util;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.SelectEnforcementType;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringList;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringListElement;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EnforcementTypeUtilTest {

    @Test
    void shouldConvertEnumToDynamicStringListElement() {
        SelectEnforcementType type = SelectEnforcementType.WARRANT;

        DynamicStringListElement element = EnforcementTypeUtil.convertToDynamicStringListElement(type);

        assertThat(element).isNotNull();
        assertThat(element.getCode()).isEqualTo(type.name());
        assertThat(element.getLabel()).isEqualTo(type.getLabel());
    }

    @Test
    void shouldCreateDynamicStringListFromEnums() {
        List<SelectEnforcementType> types = Arrays.asList(
            SelectEnforcementType.WARRANT,
            SelectEnforcementType.WRIT,
            SelectEnforcementType.WARRANT_OF_RESTITUTION
        );

        DynamicStringList list = EnforcementTypeUtil.createDynamicStringList(types);

        assertThat(list).isNotNull();
        assertThat(list.getListItems()).hasSize(3);

        assertThat(list.getListItems()).extracting(DynamicStringListElement::getCode)
            .containsExactly("WARRANT", "WRIT", "WARRANT_OF_RESTITUTION");

        assertThat(list.getListItems()).extracting(DynamicStringListElement::getLabel)
            .containsExactly(
                SelectEnforcementType.WARRANT.getLabel(),
                SelectEnforcementType.WRIT.getLabel(),
                SelectEnforcementType.WARRANT_OF_RESTITUTION.getLabel());
    }

    @Test
    void shouldCreateEmptyDynamicStringListWhenGivenEmptyList() {
        DynamicStringList list = EnforcementTypeUtil.createDynamicStringList(Collections.emptyList());

        assertThat(list).isNotNull();
        assertThat(list.getListItems()).isNotNull().isEmpty();
    }

    @Test
    void shouldconvertYesOrNoToVerticalYesNoForYes() {
        YesOrNo val = YesOrNo.YES;

        VerticalYesNo result = EnforcementTypeUtil.convertYesOrNoToVerticalYesNo(val);

        assertThat(result).isEqualTo(VerticalYesNo.YES);
    }

    @Test
    void shouldconvertYesOrNoToVerticalYesNoForNo() {
        YesOrNo val = YesOrNo.NO;

        VerticalYesNo result = EnforcementTypeUtil.convertYesOrNoToVerticalYesNo(val);

        assertThat(result).isEqualTo(VerticalYesNo.NO);
    }
}