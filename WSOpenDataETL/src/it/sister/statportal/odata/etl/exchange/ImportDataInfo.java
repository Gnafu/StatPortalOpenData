package it.sister.statportal.odata.etl.exchange;

/**
 * Informazioni sull'esito dell'importazione di un dato
 * In particolare questo oggetto fornisce uno stato 
 * dell'importazione ed una descrizione che chiarisce
 * l'esito.
 *
 */
public class ImportDataInfo {

	//Codice che descrive lo stato dell'importazione
	protected int statusCode;
		
	//Descrizione in linguaggio naturale dello stato dell'importazione
	protected String statusDescription;
	
	//array di formati di visualizzazione supportati dal dato caricato
	//valido solo se il caricamento è andato a buon fine
	protected String[] visualizationFormats;
	
	//flag che indica se è un dato geografico
	//valido solo se il caricamento è andato a buon fine
	protected boolean isGeographic;
	
	//Flag che indica se il dato è strutturato
	//valido solo se il caricamente è andato a buon fine.
	//un dato è strutturato se ha almeno una dimensione
	protected boolean isStructured;
	
	//link al dato in odata
	//valido solo se il caricamento è andato a buon fine
	protected String odataLink;
	
	protected String uid;
	
	/**
	 * Costruisce l'oggetto esito dell'importazione
	 * @param status Codice dello stato dell'importazione
	 * @param description Descrizione dello stato dell'importazione
	 * @param visualizationFormats Formati di visualizzazione supportati
	 * @param isGeographic Flag che indica se il dato è geografico
	 * @param isStructured Flag che indica se il dato è strutturato
	 * @param odataLink link al dato in odata
	 */
	public ImportDataInfo(int status, String description, String[] visualizationFormats, boolean isGeographic, boolean isStructured, String odataLink, String uid){
		this.statusCode = status;
		this.statusDescription = description;
		this.visualizationFormats = visualizationFormats;
		this.isGeographic = isGeographic;
		this.isStructured = isStructured;
		this.odataLink = odataLink;
		this.uid = uid;
	}	
	
	/**
	 * Costruisce l'oggetto esito dell'importazione
	 * Da utilizzare quando si è verificato un errore e quindi non sono richiesti formati di visualizzazione
	 * @param status Codice dello stato dell'importazione
	 * @param description Descrizione dello stato dell'importazione
	 */
	public ImportDataInfo(int status, String description){
		this.statusCode = status;
		this.statusDescription = description;
		this.visualizationFormats = new String[0];
	}	
	
	/**
	 * Fornisce il codice dell'esito
	 * @return il codice dell'esito
	 */
	public int getStatusCode(){
		return this.statusCode;
	}
	
	/**
	 * Fornisce la descrizione dell'esito
	 * @return la descrizione dell'esito
	 */
	public String getStatusDescription(){
		return this.statusDescription;
	}
	
	/**
	 * Fornisce l'uid del dato
	 * @return l'uid del dato
	 */
	public String getDataUid(){
		return this.uid;
	}
}
