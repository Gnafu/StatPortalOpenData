package it.sister.statportal.odata;

import it.sister.statportal.odata.domain.Column;
import it.sister.statportal.odata.domain.IData;
import it.sister.statportal.odata.domain.MdData;
import it.sister.statportal.odata.domain.MdDataDim;
import it.sister.statportal.odata.domain.OdataDomainException;
import it.sister.statportal.odata.domain.Column.ColumnType;

import java.util.ArrayList;
import java.util.List;
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
 * Implementazione dell'interfaccia ExpressionVisitor per generare una stringa  SQL valida
 * In particolare utilizziamo questo oggetto per generare le clausole WHERE e ORDER BY
 *
 */
public class SqlExpressionVisitor implements ExpressionVisitor {

	final transient private String entitySetName;
	
	final transient private IData data;

	final transient private List<Column> columns;
	
	final transient private StringBuilder builder;
	
	final transient private Stack<CommonExpression> expressionStack;

	final boolean where;

	boolean literalMode = false;
	
	List<MdDataDim> filteredDims = new ArrayList<MdDataDim>();
	
	String substringof = null;
	
	public List<MdDataDim> getFilteredDims(){
		return this.filteredDims;
	}
	
	/**
	 * Dato un nome logico di colonna restituisce il suo equivalente fisico
	 * @param logicalName il nome logico
	 * @return il nome fisico corrispondente
	 * @throws OpenDataException se il nome non è valido
	 */
	private String getPhysicalName(final String logicalName) throws OpenDataException{
		for(Column column : columns){
			if(column.getLogicalName().compareTo(logicalName) == 0){
				return column.getPhysicalName();
			}
		}
		return logicalName;
	}
	
	public SqlExpressionVisitor(final String entitySetName, final IData data, boolean where, boolean literalMode) throws OdataDomainException{
		this.entitySetName = entitySetName;
		this.data = data;
		this.columns = data.getColumns();
		this.builder = new StringBuilder();
		this.expressionStack = new Stack<CommonExpression>();
		this.where = where;
		this.literalMode = literalMode;
	}
	
	/**
	 * Costruisce l'oggetto.
	 * @param list la lista delle proprietà delle colonne.
	 */
	public SqlExpressionVisitor(final String entitySetName, final IData data, final List<Column> list, boolean where){
		this.entitySetName = entitySetName;
		this.data = data;
		this.columns = list;
		this.builder = new StringBuilder();
		this.expressionStack = new Stack<CommonExpression>();
		this.where = where;
	}
	
	/**
	 * Costruisce l'oggetto.
	 * @param list la lista delle proprietà delle colonne.
	 */
	public SqlExpressionVisitor(final String entitySetName, final IData data, boolean where) throws OdataDomainException{
		this.entitySetName = entitySetName;
		this.data = data;
		this.columns = data.getColumns();
		this.builder = new StringBuilder();
		this.expressionStack = new Stack<CommonExpression>();
		this.where = where;
	}
	
	@Override
	public String toString() {
		return builder.toString();
	}
	
	@Override
	public void beforeDescend() {
		if(!expressionStack.isEmpty() && expressionStack.peek() instanceof SubstringOfMethodCallExpression){
			if(literalMode){
				substringof = "(CASE WHEN UPPER({0}) LIKE UPPER('{1}') THEN TRUE ELSE FALSE END)";
			}else{
				substringof = "(CASE WHEN UPPER({0}) LIKE UPPER('%{1}%') THEN TRUE ELSE FALSE END)";
			}
		}
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
	public void betweenDescend() {
		//questo metodo viene chiamato a metà visita di un'espressione, dopo aver visitato il ramo destro e quello sinistro.
		//es.  espressione A = 10. Si visita il ramo sinistro che restituisce A e, prima di visitare il destro che restituirebbe 10, 
		//si chiama questo metodo che quindi deve restituire =.
		//quello che restituiamo dipende dal tipo di espressione che stiamo considerando e da come si mappa in SQL.
		if(!expressionStack.isEmpty()){
			if(!(expressionStack.peek() instanceof SubstringOfMethodCallExpression)){
				CommonExpression expression = expressionStack.pop();
				if(expression != null){
					if(expression instanceof AddExpression){
						builder.append(" + ");
					} else if(expression instanceof AndExpression){
						builder.append(" AND ");
					} else if(expression instanceof DivExpression){
						builder.append(" / ");
					} else if(expression instanceof EqExpression){
						builder.append(" = ");
					} else if(expression instanceof GeExpression){
						builder.append(" >= ");
					} else if(expression instanceof GtExpression){
						builder.append(" > ");
					} else if(expression instanceof LeExpression){
						builder.append(" <= ");
					} else if(expression instanceof LtExpression){
						builder.append(" < ");
					} else if(expression instanceof MulExpression){
						builder.append(" * ");
					} else if(expression instanceof NeExpression){
						builder.append(" <> ");
					} else if(expression instanceof OrExpression){
						builder.append(" OR ");
					} else if(expression instanceof SubExpression){
						builder.append(" - ");
					}			
				}
			}
		}
	}

	@Override
	public void visit(String type) {
		builder.append(" '"+type+"' ");
	}

	@Override
	public void visit(OrderByExpression expr) {	
	}

	@Override
	public void visit(Direction direction) {
		if(direction == Direction.ASCENDING){
			builder.append(" asc ");
		}else{
			builder.append(" desc ");
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
		if(expr.getValue()){
			builder.append(" true ");
		}else{
			builder.append(" false ");
		}
	}

	@Override
	public void visit(CastExpression expr) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void visit(ConcatMethodCallExpression expr) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void visit(DateTimeLiteral expr) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void visit(DateTimeOffsetLiteral expr) {
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
	public void visit(EndsWithMethodCallExpression expr) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void visit(EntitySimpleProperty expr) {
		try {
			String logicalName = JITProducer.uniqueNameToRawName(entitySetName, expr.getPropertyName());
			String physicalName = getPhysicalName(logicalName);
			MdDataDim relatedDim = findMdDataDim(data, physicalName);
			if(relatedDim != null && !filteredDims.contains(relatedDim)){
				filteredDims.add(relatedDim);
			}
			if(substringof != null){
				if(substringof.contains("{1}")){
					substringof = substringof.replace("{1}", "CAST ("+data.getDescriptionField(physicalName)+" AS text)");
				}else{
					substringof = substringof.replace("{0}", "CAST ("+data.getDescriptionField(physicalName)+" AS text)");
				}
			}else{			
				if(where){
					if(isMeasureOrDimension(logicalName)){
						builder.append("\""+physicalName+"\"");				
					}else{
						builder.append("CAST (\""+((MdData)data).getTableName()+"\".\""+physicalName+"\" AS text)");
					}
				}else{
					builder.append("\""+physicalName+"\"");
				}
			}			
		} catch (OpenDataException e) {
			//se la colonna non è valida generiamo una query inconsistente.
			e.printStackTrace();
		}
	}

	private MdDataDim findMdDataDim(IData data, String physicalName){
		for(MdDataDim dim : ((MdData)data).getMdDataDims()){
			if(dim.getDimcodeField().equalsIgnoreCase(physicalName)){
				return dim;
			}
		}
		return null;
	}
	
	protected boolean isMeasureOrDimension(String name){
		try {
			for(Column column : data.getColumns()){
				if(column.getLogicalName().equals(name)){
					return column.getType() == ColumnType.MEASURE || column.getType() == ColumnType.DIMENSION;
				}
			}
			return false;
		} catch (OdataDomainException e) {
			return false;
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
	}

	@Override
	public void visit(IndexOfMethodCallExpression expr) {
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
	public void visit(IsofExpression expr) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void visit(LeExpression expr) {
		expressionStack.push(expr);
	}

	@Override
	public void visit(LengthMethodCallExpression expr) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void visit(LtExpression expr) {
		expressionStack.push(expr);
	}

	@Override
	public void visit(ModExpression expr) {
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
	public void visit(NegateExpression expr) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void visit(NotExpression expr) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void visit(NullLiteral expr) {
		if(builder.lastIndexOf(" <> ") == (builder.length()-4)){
			builder.replace(builder.length()-4, builder.length()-1, " is not ");
		}
		builder.append("null");
	}

	@Override
	public void visit(OrExpression expr) {
		expressionStack.push(expr);
	}

	@Override
	public void visit(ParenExpression expr) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void visit(BoolParenExpression expr) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void visit(ReplaceMethodCallExpression expr) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void visit(StartsWithMethodCallExpression expr) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void visit(StringLiteral expr) {
		if(substringof != null){
			substringof = substringof.replace("{1}", expr.getValue().replace("\'", "\'\'"));
		}else{
			builder.append(expr.getValue());
		}
	}

	@Override
	public void visit(SubExpression expr) {
		expressionStack.push(expr);
	}

	@Override
	public void visit(SubstringMethodCallExpression expr) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void visit(SubstringOfMethodCallExpression expr) {
		expressionStack.push(expr);
	}

	@Override
	public void visit(TimeLiteral expr) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void visit(ToLowerMethodCallExpression expr) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void visit(ToUpperMethodCallExpression expr) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void visit(TrimMethodCallExpression expr) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void visit(YearMethodCallExpression expr) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void visit(MonthMethodCallExpression expr) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void visit(DayMethodCallExpression expr) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void visit(HourMethodCallExpression expr) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void visit(MinuteMethodCallExpression expr) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void visit(SecondMethodCallExpression expr) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void visit(RoundMethodCallExpression expr) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void visit(FloorMethodCallExpression expr) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void visit(CeilingMethodCallExpression expr) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void visit(AggregateAnyFunction expr) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void visit(AggregateAllFunction expr) {
		throw new UnsupportedOperationException();
	}
}
