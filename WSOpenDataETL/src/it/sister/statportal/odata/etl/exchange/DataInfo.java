package it.sister.statportal.odata.etl.exchange;

/**
 * Insieme di informazioni necessarie per la creazione di un dato
 *
 */
public class DataInfo {
	
	//la descrizione del dato fornita dall'utente
	protected String description;

	//nome del dato
	protected String name;
	
	//nome della tabella del dato
	protected String tableName;
	
	/**
	 * Costruzione dell'oggetto
	 * @param name il nome del dato
	 * @param description la sua descrizione
	 */
	public DataInfo(String name, String description){
		this.description = description;
		this.name = name;
	}
	
	public DataInfo(){}
	
	/**
	 * Espone il nome della tabella
	 * @return il nome della tabella
	 */
	public String getTableName(){
		return this.tableName;
	}
	
	/**
	 * Imposta il nome della tabella
	 * @param value il nome della tabella
	 */
	public void setTableName(String value){
		this.tableName = value;
	}
	
	/**
	 * Espone il nome del dato
	 * @return il nome del dato
	 */
	public String getName(){
		return this.name;
	}
	
	/**
	 * Imposta il nome del dato
	 * @param value il nome del dato
	 */
	public void setName(String value){
		this.name = value;
	}
	
	/**
	 * Espone la descrizione del dato
	 * @return la descrizione del dato
	 */
	public String getDescription(){
		return this.description;		
	}
	
	/**
	 * Imposta la descrizione del dato
	 * @param value la descrizione del dato
	 */
	public void setDescription(String value){
		this.description = value;
	}
}
