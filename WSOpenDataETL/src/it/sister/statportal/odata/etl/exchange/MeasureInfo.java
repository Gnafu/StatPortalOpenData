package it.sister.statportal.odata.etl.exchange;


/**
 * Insieme di informazioni necessarie per la creazione di una misura
 * e non calcolabili dal server
 *
 */
public class MeasureInfo extends ImportColumnInfo {
	
	//alias della misura
	protected String alias;
	
	//descrizione della misura
	protected String description;
	
	//unità di misura
	protected String measureUnit;

	//nome della colonna
	protected String columnName;
	
	//decimali per la misura
	protected Short decimals;
	
	/**
	 * Crea l'oggetto
	 * @param alias l'alias della misura
	 * @param description la descrizione della misura
	 * @param measureUnit l'unità di misura
	 * @param columnName il nome della colonna del file associata alla misura
	 * @param decimals il numero di decimali della misura
	 */
	public MeasureInfo(String alias, String description, String measureUnit, String columnName, Short decimals){
		this.alias = alias;
		this.description = description;
		this.measureUnit = measureUnit;
		this.columnName = columnName;
		this.decimals = decimals;
	}
	
	public MeasureInfo(){}
	
	/**
	 * Espone i decimali della misura
	 * @return i decimali della misura
	 */
	public Short getDecimals(){
		return this.decimals;
	}
	
	/**
	 * Imposta i decimali della misura
	 * @param value i decimali della misura
	 */
	public void setDecimals(Short value){
		this.decimals = value;
	}
	
	/**
	 * Espone l'alias della misura
	 * @return l'alias della misura
	 */
	public String getAlias(){
		return this.alias;
	}
	
	/**
	 * Imposta l'alias della misura
	 * @param value l'alias della misura
	 */
	public void setAlias(String value){
		this.alias = value;
	}
	
	/**
	 * Espone la descrizione della misura
	 * @return la descrizione della misura
	 */
	public String getDescription(){
		return this.description;
	}
	
	/**
	 * Imposta la descrizione della misura
	 * @param value la descrizione della misura
	 */
	public void setDescription(String value){
		this.description = value;
	}
	
	/**
	 * Espone l'unità di misura
	 * @return  l'unità di misura
	 */
	public String getMeasureUnit(){
		return this.measureUnit;
	}
	
	/**
	 * Imposta l'unità di misura
	 * @param value l'unità di misura
	 */
	public void setMeasureUnit(String value){
		this.measureUnit = value;
	}
	
	@Override
	public String getColumnName(){
		return this.columnName;
	}
	
	@Override
	public void setColumnName(String value){
		this.columnName = value;
	}
	
	@Override
	public ColumnType getColumnType() {
		return ColumnType.MEASURE;
	}
}
