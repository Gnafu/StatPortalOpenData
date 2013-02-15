package it.sister.statportal.odata.etl.exchange;

/**
 * Informazioni su una colonna di un file strutturato.
 * Le informazioni consistono nel nome, tipo ed eventualmente 
 * numero di cifre decimali della colonna.
 *
 */
public class ColumnInfo {

	//Possibili tipi delle colonne
	public enum ColumnType{
		CHAR,
		CURRENCY,
		DATE,
		FLOAT,
		BOOLEAN,
		STRING,
		NUMBER,
		PICTURE,
		UNKNOWN
	}
	
	//Eventuale dimensione rappresentata dalla colonna
	public enum ColumnDimension{
		NOTHING,
		UNKNOWN,
		CITY,
		POSTAL_CODE,
		YEAR,
		PROVINCE,		
		REGION,
		LATITUDE,
		LONGITUDE
	}
	
	//nome della colonna
	protected String name;
	
	//tipo della colonna
	protected ColumnType type;
	
	//dimensione che si ritiene essere legata alla colonna
	protected ColumnDimension inferedDimension;
	
	//numero di decimali
	protected int decimalPositions;
	
	//flag che indica se la colonna è escludibile
	protected boolean isExcludable;
	
	/**
	 * Costruisce l'informazione sulla colonna senza 
	 * considerare il numero di cifre decimali
	 * @param name nome della colonna
	 * @param type tipo della colonna
	 * @param isExcludable flag che indica se è escludibile
	 */
	public ColumnInfo(String name, ColumnType type, boolean isExcludable){
		this.name = name;
		this.type = type;
		this.inferedDimension = ColumnDimension.UNKNOWN;
		this.isExcludable = isExcludable;
	}
	
	/**
	 * Costruisce l'informazione sulla colonna completa
	 * del numero di cifre decimali
	 * @param name nome della colonna
	 * @param type tipo della colonna
	 * @param decimalPositions numero di cifre decimali
	 * @param isExcludable flag che indica se è escludibile
	 */
	public ColumnInfo(String name, ColumnType type, int decimalPositions, boolean isExcludable){
		this.name = name;
		this.type = type;
		this.decimalPositions = decimalPositions;
		this.inferedDimension = ColumnDimension.UNKNOWN;
		this.isExcludable = isExcludable;
	}
	
	/**
	 * Imposta il valore del nome della colonna.
	 * @param name il nome della colonna
	 */
	public void setName(String name){
		this.name = name;
	}
	
	/**
	 * Imposta il valore del tipo della colonna.
	 * @param type il tipo della colonna
	 */
	public void setType(ColumnType type){
		this.type = type;
	}
	
	/**
	 * Imposta il valore del campo "numero di decimali" della colonna
	 * @param decimalPositions il numero di decimali della colonna
	 */
	public void setDecimalPositions(int decimalPositions){
		this.decimalPositions = decimalPositions;
	}
	
	/**
	 * Imposta il valore della dimensione che si ritene legata alla colonna
	 * @param infered la dimensione che si ritiene legata alla colonna
	 */
	public void setInferedDimension(ColumnDimension infered){
		this.inferedDimension = infered;
	}
	
	/**
	 * Espone il nome della colonna
	 * @return il nome della colonna
	 */
	public String getName(){
		return this.name;
	}
	
	/**
	 * Restituisce il tipo della colonna
	 * @return il tipo della colonna
	 */
	public ColumnType getColumnType(){
		return this.type;
	}
	
	/**
	 * Restituisce il numero di cifre decimale
	 * @return il numero di cifre decimali
	 */
	public int getDecimalPositions(){
		return this.decimalPositions;
	}
	
	/**
	 * Espone il valore della dimensione che si ritiene legata alla colonna
	 * @return il valore della dimensione che si ritiene legata alla colonna
	 */
	public ColumnDimension getInferedDimension(){
		return this.inferedDimension;
	}
	
	/**
	 * Restituisce il flag che indica se la colonna è escludibile
	 * @return il flag che indica se la colonna è escludibile
	 */
	public boolean getIsExcludable(){
		return this.isExcludable;
	}
	
	/**
	 * Imposta il flag che indica se la colonna è escludibile
	 * @param value il flag che indica se la colonna è escludibile
	 */
	public void setIsExcludable(boolean value){
		this.isExcludable = value;
	}
}
