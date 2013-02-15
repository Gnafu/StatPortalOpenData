package it.sister.statportal.odata.proxy;

/**
 * Astrazione di una riga ottenuta con la navigation property GenericColumnAggregationRow
 *
 */
public class GenericColumnAggregationRow {
	protected int id;
	
	protected String variable;
	
	protected String column;
	
	/**
	 * costruisce l'astrazione della riga
	 * @param id l'id della riga
	 * @param variable il valore della variabile
	 * @param column il valore del campo colonna
	 */
	public GenericColumnAggregationRow(int id, String variable, String column){
		this.id = id;
		this.variable = variable;
		this.column = column;
	}
	
	public int getId(){
		return this.id;		
	}
	
	public String getVariable(){
		return this.variable;
	}
	
	public String getColumn(){
		return this.column;
	}
}
