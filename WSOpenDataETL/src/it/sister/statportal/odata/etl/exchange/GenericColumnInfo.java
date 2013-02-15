package it.sister.statportal.odata.etl.exchange;


/**
 * Insieme di informazioni necessarie per il caricamento
 * di una colonna generica
 *
 */
public class GenericColumnInfo extends ImportColumnInfo{

	//Nome della colonna nella tabella
	protected String columnName;
	
	//Alias della colonna
	protected String alias;
	
	//Flag che indica se è un campo descrittivo
	protected boolean descriptiveField;
	
	//cardinalità dei valori in colonna
	protected int cardinality;
	
	//indica se la colonna contiene i valori della latitudine
	protected boolean isLatitude;
	
	//indica se la colonna contiene i valori della longitudine
	protected boolean isLongitude;
	
	/**
	 * Crea l'oggetto 
	 * @param columnName il nome della colonna
	 * @param alias l'alias della colonna
	 * @param descriptiveField flag che indica se è una colonna descrittiva
	 * @param isCoordinate flag che indica se è una coordinata
	 */
	public GenericColumnInfo(String columnName, String alias, boolean descriptiveField, boolean isLatitude, boolean isLongitude){
		this.columnName = columnName;
		this.alias = alias;
		this.descriptiveField = descriptiveField;
		this.cardinality = 0;
		this.isLatitude = isLatitude;
		this.isLongitude = isLongitude;
	}
	
	/**
	 * Fornisce il valore del flag che indica se la colonna rappresenta la latitudine
	 * @return il valore del flag
	 */
	public boolean getIsLatitude(){
		return this.isLatitude;
	}
	
	/**
	 * Imposta il flag che indica se la colonna rappresenta la latitudine
	 * @param value il valore del flag
	 */
	public void setIsLatitude(boolean value){
		this.isLatitude = value;
	}

	/**
	 * Fornisce il valore del flag che indica se la colonna rappresenta la longitudine
	 * @return il valore del flag
	 */
	public boolean getIsLongitude(){
		return this.isLongitude;
	}
	
	/**
	 * Imposta il flag che indica se la colonna rappresenta la longitudine
	 * @param value il valore del flag
	 */
	public void setIsLongitude(boolean value){
		this.isLongitude = value;
	}
	
	@Override
	public void setColumnName(String value){
		this.columnName = value;
	}
	
	@Override
	public String getColumnName(){
		return this.columnName;
	}
	
	/**
	 * Imposta la cardinalità della colonna
	 * @param value la cardinalità della colonna
	 */
	public void setCardinality(int value){
		this.cardinality = value;
	}
	
	/**
	 * Espone la cardinalità della colonna
	 * @return la cardinalità della colonna
	 */
	public int getCardinality(){
		return this.cardinality;
	}
	
	/**
	 * Imposta l'alias della colonna
	 * @param value l'alias della colonna
	 */
	public void setAlias(String value){
		this.alias = value;
	}
	
	/**
	 * Espone l'alias della colonna
	 * @return l'alias della colonna
	 */
	public String getAlias(){
		return this.alias;
	}
	
	/**
	 * Imposta il flag che indica se la colonna è descrittiva
	 * @param value il flag che indica se la colonna è descrittiva
	 */
	public void setDescriptiveField(boolean value){
		this.descriptiveField = value;
	}
	
	/**
	 * Espone il flag che indica se la colonna è descrittiva
	 * @return il flag che indica se la colonna è descrittiva
	 */
	public boolean getDescriptiveField(){
		return this.descriptiveField;
	}
	
	@Override
	public ColumnType getColumnType() {
		return ColumnType.GENERIC;
	}
}
