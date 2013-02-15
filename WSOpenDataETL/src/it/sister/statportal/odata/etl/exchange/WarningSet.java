package it.sister.statportal.odata.etl.exchange;

/**
 * Insieme di warning
 *
 */
public class WarningSet {

	protected boolean canForceImport;
	
	protected Warning[] warnings;
	
	/**
	 * Costruisce l'insieme di warning
	 * @param canForceImport flag che indica se in presenza di questi warning è possibile forzare il caricamento
	 * @param warnings insieme di warning
	 */
	public WarningSet(boolean canForceImport, Warning[] warnings){
		this.canForceImport = canForceImport;
		this.warnings = warnings;
	}
	
	/**
	 * Fornisce il flag che indica se è possibile forzare il caricamento
	 * @return flag che indica se è possibile forzare il caricamento
	 */
	public boolean getCanForceImport(){
		return this.canForceImport;
	}
	
	/**
	 * Imposta il flag che indica se è possibile forzare il caricamento
	 * @param value il valore del flag
	 */
	public void setCanForceImport(boolean value){
		this.canForceImport = value;
	}
	
	/**
	 * Fornisce l'insieme dei warning
	 * @return l'insieme dei warning
	 */
	public Warning[] getWarnings(){
		return this.warnings;
	}
	
	/**
	 * Imposta l'insieme dei warning
	 * @param value l'insieme dei warning
	 */
	public void setWarnings(Warning[] value){
		this.warnings = value;
	}
	
	
}
