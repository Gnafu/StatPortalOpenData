package it.sister.statportal.odata.proxy;

import java.util.Stack;

import org.odata4j.expression.AddExpression;
import org.odata4j.expression.AggregateAllFunction;
import org.odata4j.expression.AggregateAnyFunction;
import org.odata4j.expression.AndExpression;
import org.odata4j.expression.BinaryLiteral;
import org.odata4j.expression.BoolParenExpression;
import org.odata4j.expression.BooleanLiteral;
import org.odata4j.expression.ByteLiteral;
import org.odata4j.expression.CastExpression;
import org.odata4j.expression.CeilingMethodCallExpression;
import org.odata4j.expression.CommonExpression;
import org.odata4j.expression.ConcatMethodCallExpression;
import org.odata4j.expression.DateTimeLiteral;
import org.odata4j.expression.DateTimeOffsetLiteral;
import org.odata4j.expression.DayMethodCallExpression;
import org.odata4j.expression.DecimalLiteral;
import org.odata4j.expression.DivExpression;
import org.odata4j.expression.DoubleLiteral;
import org.odata4j.expression.EndsWithMethodCallExpression;
import org.odata4j.expression.EntitySimpleProperty;
import org.odata4j.expression.EqExpression;
import org.odata4j.expression.ExpressionVisitor;
import org.odata4j.expression.FloorMethodCallExpression;
import org.odata4j.expression.GeExpression;
import org.odata4j.expression.GtExpression;
import org.odata4j.expression.GuidLiteral;
import org.odata4j.expression.HourMethodCallExpression;
import org.odata4j.expression.IndexOfMethodCallExpression;
import org.odata4j.expression.Int64Literal;
import org.odata4j.expression.IntegralLiteral;
import org.odata4j.expression.IsofExpression;
import org.odata4j.expression.LeExpression;
import org.odata4j.expression.LengthMethodCallExpression;
import org.odata4j.expression.LtExpression;
import org.odata4j.expression.MinuteMethodCallExpression;
import org.odata4j.expression.ModExpression;
import org.odata4j.expression.MonthMethodCallExpression;
import org.odata4j.expression.MulExpression;
import org.odata4j.expression.NeExpression;
import org.odata4j.expression.NegateExpression;
import org.odata4j.expression.NotExpression;
import org.odata4j.expression.NullLiteral;
import org.odata4j.expression.OrExpression;
import org.odata4j.expression.OrderByExpression;
import org.odata4j.expression.OrderByExpression.Direction;
import org.odata4j.expression.ParenExpression;
import org.odata4j.expression.ReplaceMethodCallExpression;
import org.odata4j.expression.RoundMethodCallExpression;
import org.odata4j.expression.SecondMethodCallExpression;
import org.odata4j.expression.SingleLiteral;
import org.odata4j.expression.StartsWithMethodCallExpression;
import org.odata4j.expression.StringLiteral;
import org.odata4j.expression.SubExpression;
import org.odata4j.expression.SubstringMethodCallExpression;
import org.odata4j.expression.SubstringOfMethodCallExpression;
import org.odata4j.expression.TimeLiteral;
import org.odata4j.expression.ToLowerMethodCallExpression;
import org.odata4j.expression.ToUpperMethodCallExpression;
import org.odata4j.expression.TrimMethodCallExpression;
import org.odata4j.expression.YearMethodCallExpression;

/**
 * Implementazione di un visitor per le espressioni che ricostruisce la query odata a partire da una query sql
 *
 */
public class ReverseExpressionVisitor  implements ExpressionVisitor {

	final transient private StringBuilder builder;
	
	final transient private Stack<CommonExpression> expressionStack;
	
	String substringof = null;
	
	public ReverseExpressionVisitor(){
		this.builder = new StringBuilder();
		this.expressionStack = new Stack<CommonExpression>();
	}
	
	@Override
	public String toString() {
		return builder.toString();
	}
	
	@Override
	public void afterDescend() {
		if(!expressionStack.isEmpty() && expressionStack.peek() instanceof SubstringOfMethodCallExpression){
			builder.append(substringof);
			substringof = null;
			expressionStack.pop();
		}	
	}

	@Override
	public void beforeDescend() {
		if(!expressionStack.isEmpty() && expressionStack.peek() instanceof SubstringOfMethodCallExpression){
			substringof = "substringof('{0}',{1})";
		}
	}

	@Override
	public void betweenDescend() {
		if(!expressionStack.isEmpty()){
			if(!(expressionStack.peek() instanceof SubstringOfMethodCallExpression)){
				CommonExpression expression = expressionStack.pop();
				if(expression != null){
					if(expression instanceof AddExpression){
						builder.append(" add ");
					} else if(expression instanceof AndExpression){
						builder.append(" and ");
					} else if(expression instanceof DivExpression){
						builder.append(" div ");
					} else if(expression instanceof EqExpression){
						builder.append(" eq ");
					} else if(expression instanceof GeExpression){
						builder.append(" ge ");
					} else if(expression instanceof GtExpression){
						builder.append(" gt ");
					} else if(expression instanceof LeExpression){
						builder.append(" le ");
					} else if(expression instanceof LtExpression){
						builder.append(" lt ");
					} else if(expression instanceof MulExpression){
						builder.append(" mul ");
					} else if(expression instanceof NeExpression){
						builder.append(" ne ");
					} else if(expression instanceof OrExpression){
						builder.append(" or ");
					} else if(expression instanceof SubExpression){
						builder.append(" sub ");
					}			
				}
			}
		}
	}

	@Override
	public void visit(String arg0) {
		builder.append(arg0);
	}

	@Override
	public void visit(OrderByExpression expr) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(Direction direction) {
		if(direction == Direction.ASCENDING){
			builder.append(" asc");
		}else{
			builder.append(" desc");
		}
	}

	@Override
	public void visit(AddExpression expr) {
		expressionStack.push(expr);
	}

	@Override
	public void visit(AndExpression expr) {
		expressionStack.push(expr);
	}

	@Override
	public void visit(BooleanLiteral expr) {
		builder.append(expr.getValue());
	}

	@Override
	public void visit(CastExpression arg0) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void visit(ConcatMethodCallExpression arg0) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void visit(DateTimeLiteral arg0) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void visit(DateTimeOffsetLiteral arg0) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void visit(DecimalLiteral expr) {
		builder.append(expr.getValue().toString());
	}

	@Override
	public void visit(DivExpression expr) {
		expressionStack.push(expr);
	}

	@Override
	public void visit(EndsWithMethodCallExpression arg0) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void visit(EntitySimpleProperty expr) {
		if(substringof != null){
			if(substringof.contains("{1}")){
				substringof = substringof.replace("{1}", expr.getPropertyName());
			}
		}else{			
			builder.append(expr.getPropertyName());
		}
	}

	@Override
	public void visit(EqExpression expr) {
		expressionStack.push(expr);
	}

	@Override
	public void visit(GeExpression expr) {
		expressionStack.push(expr);
	}

	@Override
	public void visit(GtExpression expr) {
		expressionStack.push(expr);
	}

	@Override
	public void visit(GuidLiteral expr) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void visit(BinaryLiteral expr) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void visit(ByteLiteral expr) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void visit(IndexOfMethodCallExpression expr) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void visit(SingleLiteral expr) {
		builder.append(expr.getValue());
	}

	@Override
	public void visit(DoubleLiteral expr) {
		builder.append(expr.getValue());
	}

	@Override
	public void visit(IntegralLiteral expr) {
		builder.append(expr.getValue());
	}

	@Override
	public void visit(Int64Literal expr) {
		builder.append(expr.getValue());
	}

	@Override
	public void visit(IsofExpression arg0) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void visit(LeExpression expr) {
		expressionStack.push(expr);
	}

	@Override
	public void visit(LengthMethodCallExpression arg0) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void visit(LtExpression expr) {
		expressionStack.push(expr);
	}

	@Override
	public void visit(ModExpression arg0) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void visit(MulExpression expr) {
		expressionStack.push(expr);
	}

	@Override
	public void visit(NeExpression expr) {
		expressionStack.push(expr);
	}

	@Override
	public void visit(NegateExpression arg0) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void visit(NotExpression arg0) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void visit(NullLiteral arg0) {
		builder.append("null");
	}

	@Override
	public void visit(OrExpression expr) {
		expressionStack.push(expr);
	}

	@Override
	public void visit(ParenExpression arg0) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void visit(BoolParenExpression arg0) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void visit(ReplaceMethodCallExpression arg0) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void visit(StartsWithMethodCallExpression arg0) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void visit(StringLiteral expr) {
		if(substringof != null){
			substringof = substringof.replace("{0}", expr.getValue().replace("\'", "\'\'"));
					}else{
			builder.append(expr.getValue());
		}
	}

	@Override
	public void visit(SubExpression expr) {
		expressionStack.push(expr);
	}

	@Override
	public void visit(SubstringMethodCallExpression arg0) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void visit(SubstringOfMethodCallExpression expr) {
		expressionStack.push(expr);
	}

	@Override
	public void visit(TimeLiteral arg0) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void visit(ToLowerMethodCallExpression arg0) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void visit(ToUpperMethodCallExpression arg0) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void visit(TrimMethodCallExpression arg0) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void visit(YearMethodCallExpression arg0) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void visit(MonthMethodCallExpression arg0) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void visit(DayMethodCallExpression arg0) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void visit(HourMethodCallExpression arg0) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void visit(MinuteMethodCallExpression arg0) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void visit(SecondMethodCallExpression arg0) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void visit(RoundMethodCallExpression arg0) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void visit(FloorMethodCallExpression arg0) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void visit(CeilingMethodCallExpression arg0) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void visit(AggregateAnyFunction arg0) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void visit(AggregateAllFunction arg0) {
		throw new UnsupportedOperationException();
	}
}
