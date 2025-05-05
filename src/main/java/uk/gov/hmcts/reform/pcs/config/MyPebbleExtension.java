package uk.gov.hmcts.reform.pcs.config;

import io.pebbletemplates.pebble.extension.AbstractExtension;
import io.pebbletemplates.pebble.operator.Associativity;
import io.pebbletemplates.pebble.operator.BinaryOperator;
import io.pebbletemplates.pebble.operator.BinaryOperatorImpl;
import io.pebbletemplates.pebble.operator.BinaryOperatorType;
import uk.gov.hmcts.reform.pcs.ccd.renderer.ContainsAnyExpression;

import java.util.ArrayList;
import java.util.List;

public class MyPebbleExtension extends AbstractExtension {

    @Override
    public List<BinaryOperator> getBinaryOperators() {
        List<BinaryOperator> operators = new ArrayList<>();

        operators.add(new BinaryOperatorImpl("containsAny", 20, ContainsAnyExpression::new,
                                             BinaryOperatorType.NORMAL, Associativity.LEFT));

        return operators;
    }
}
