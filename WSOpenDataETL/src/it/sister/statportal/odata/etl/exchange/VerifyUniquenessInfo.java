package it.sister.statportal.odata.etl.exchange;

/**
 * Classe che contiene le informazioni sull'univocità di un determinato campo.
 * Viene usata dal metodo verifyUniqueness
 *
 */
public class VerifyUniquenessInfo {

	//codice del risultato del controllo
	protected int code;
	
	//messaggio risultante dal controllo
	protected String message;
	
	/**
	 * Crea l'oggetto
	 * @param code il codice del risultato
	 * @param message il messaggio del risultato
	 */
	public VerifyUniquenessInfo(int code, String message){
		this.code = code;
		this.message = message;
	}
	
}
