package org.concordion.internal.command;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.concordion.api.AbstractCommand;
import org.concordion.api.CommandCall;
import org.concordion.api.Element;
import org.concordion.api.Evaluator;
import org.concordion.api.Result;
import org.concordion.api.ResultRecorder;
import org.concordion.api.listener.ExpressionEvaluatedEvent;
import org.concordion.api.listener.MissingRowEvent;
import org.concordion.api.listener.SurplusRowEvent;
import org.concordion.api.listener.VerifyRowsListener;
import org.concordion.internal.Row;
import org.concordion.internal.TableSupport;
import org.concordion.internal.util.Announcer;
import org.concordion.internal.util.Check;

public class VerifyRowsUnorderedCommand extends AbstractCommand {

    private Announcer<VerifyRowsListener> listeners = Announcer.to(VerifyRowsListener.class);

    public void addVerifyRowsListener(VerifyRowsListener listener) {
        listeners.addListener(listener);
    }

    public void removeVerifyRowsListener(VerifyRowsListener listener) {
        listeners.removeListener(listener);
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public void verify(CommandCall commandCall, Evaluator evaluator, ResultRecorder resultRecorder) {
        Pattern pattern = Pattern.compile("(#.+?) *: *(.+)");
        Matcher matcher = pattern.matcher(commandCall.getExpression());
        if (!matcher.matches()) {
            throw new RuntimeException("The expression for a \"verifyRows\" should be of the form: #var : collectionExpr");
        }
        String loopVariableName = matcher.group(1);
        String iterableExpression = matcher.group(2);

        Object obj = evaluator.evaluate(iterableExpression);
        Check.notNull(obj, "Expression returned null (should be an Iterable).");
        Check.isTrue(obj instanceof Iterable, obj.getClass().getCanonicalName() + " is not Iterable");
        Iterable<Object> iterable = (Iterable<Object>) obj;
        
        
        Check.isTrue(obj instanceof List, obj.getClass().getCanonicalName() + " is not List");
        List<Object> list = (List<Object>) obj;
        
        
        TableSupport tableSupport = new TableSupport(commandCall);
        Row[] detailRows = tableSupport.getDetailRows();
        List<Row> detailList = Arrays.asList(detailRows);

        announceExpressionEvaluated(commandCall.getElement());
        
//		TODO
//		pseudo code showing how i want to implement this.
//       
//        for (Object loopVar : iterable) {
//        	for (cell : detailRow) {
//        		if (loopVar matches cell){
//        			report match
//        		}
//        	}
//        }
//        
//        report any missing or surplus results.

        Object tVar = list.get(0);
        evaluator.setVariable(loopVariableName, tVar);
        tableSupport.copyCommandCallsTo(detailRows[0]);
        System.out.println(commandCall.getChildren().resultPeak(evaluator));
        
        int index = 0;
        for (Object loopVar : iterable) {
            evaluator.setVariable(loopVariableName, loopVar);
            Row detailRow;
            if (detailRows.length > index) {
                detailRow = detailRows[index];
            } else {
                detailRow = tableSupport.addDetailRow();
                announceSurplusRow(detailRow.getElement());
            }
            tableSupport.copyCommandCallsTo(detailRow);
            commandCall.getChildren().verify(evaluator, resultRecorder);
            index++;
        }
    }
    
    private void announceExpressionEvaluated(Element element) {
        listeners.announce().expressionEvaluated(new ExpressionEvaluatedEvent(element));
    }

    private void announceMissingRow(Element element) {
        listeners.announce().missingRow(new MissingRowEvent(element));
    }

    private void announceSurplusRow(Element element) {
        listeners.announce().surplusRow(new SurplusRowEvent(element));
    }
}
