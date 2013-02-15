package it.sister.statportal.odata.etl.exchange;

import it.sister.statportal.odata.domain.Column.ColumnType;

/**
 * Alias di una colonna di undato
 *
 */
public class Alias {

	protected String alias;
	
	protected int id;
	
	protected ColumnType type;
	
	protected String physicalName;
	
	protected ColumnInfo.ColumnType columnType;
	
	protected int decimals;
	
	/**
	 * Costruisce l'oggetto
	 * @param id id 
	 * @param type tipo del contenuto della colonna
	 * @param alias alias della colonna
	 * @param physicalName nome fisico 
	 * @param columnType tipo della colonna
	 * @param decimals numero di decimali
	 */
	public Alias(int id, ColumnType type, String alias, String physicalName, ColumnInfo.ColumnType columnType, int decimals){
		this.id = id;
		this.type = type;
		this.alias = alias;
		this.physicalName = physicalName;
		this.columnType = columnType;
		this.decimals = decimals;
	}
	
	/**
	 * Fornisce il tipo della colonna
	 * @return il tipo della colonna
	 */
	public ColumnInfo.ColumnType getColumnType(){
		return this.columnType;
	}
	
	/**
	 * Imposta il tipo della colonna
	 * @param value il tipo della colonna
	 */
	public void setColumnType(ColumnInfo.ColumnType value){
		this.columnType = value;
	}
	
	/**
	 * Fornisce il numero di decimali della colonna
	 * @return il numero di decimali della colonna
	 */
	public int getDecimals(){
		return this.decimals;
	}
	
	/**
	 * Imposta il numero di decimali della colonna
	 * @param value il numero di decimali della colonna
	 */
	public void setDecimals(int value){
		this.decimals = value;
	}
	
	/**
	 * Fornisce il nome fisico della colonna
	 * @return il nome fisico della colonna
	 */
	public String getPhysicalName(){
		return this.getPhysicalName();
	}
	
	/**
	 * Imposta il nome fisico della colonna
	 * @param value il nome fisico della colonna
	 */
	public void setPhysicalName(String value){
		this.physicalName = value;
	}
	
	/**
	 * Fornisce l'alias della colonna
	 * @return l'alias della colonna
	 */
	public String getAlias(){
		return this.alias;
	}
	
	/**
	 * Imposta l'alias della colonna
	 * @param value l'alias della colonna
	 */
	public void setAlias(String value){
		this.alias = value;
	}
	
	/**
	 * Fornisce l'id della colonna
	 * @return l'id della colonna
	 */
	public int getId(){
		return this.id;
	}
	
	/**
	 * Imposta l'id della colonna
	 * @param value l'id della colonna
	 */
	public void setId(int value){
		this.id = value;
	}
	
	/**
	 * Fornisce il tipo dei valori contenuti nella colonna
	 * @return il tipo dei valori contenuti nella colonna
	 */
	public ColumnType getType(){
		return this.type;
	}
	
	/**
	 * Imposta il tipo dei valori contenuti nella colonna
	 * @param value il tipo dei valori contenuti nella colonna
	 */
	public void setType(ColumnType value){
		this.type = value;
	}
}
