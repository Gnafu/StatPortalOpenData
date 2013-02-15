package it.sister.statportal.odata.etl.exchange;

import java.util.HashSet;

/**
 * Insieme di alias
 *
 */
public class AliasSet {

	protected Alias[] aliases;
	
	/**
	 * Crea un insieme di alias
	 * @param aliases array di alias
	 */
	public AliasSet(Alias[] aliases){
		this.aliases = aliases;
	}
	
	/**
	 * Fornisce l'insieme di alias
	 * @return l'insieme di alias
	 */
	public Alias[] getAliases(){
		return this.aliases;
	}
	
	/**
	 * Imposta l'insieme di alias
	 * @param value l'insieme di alias
	 */
	public void setAliases(Alias[] value){
		this.aliases = value;
	}

	/**
	 * Controlla se l'array ha degli alias duplicati
	 * @return true se ci sono alias duplicati, false altrimenti
	 */
	public boolean hasDuplicatedAliases() {
		HashSet<String> newAliases = new HashSet<String>();
		for(Alias alias : this.aliases){
			if(newAliases.contains(alias.alias)){
				return true;
			}
			newAliases.add(alias.alias);
		}
		return false;
	}
}
