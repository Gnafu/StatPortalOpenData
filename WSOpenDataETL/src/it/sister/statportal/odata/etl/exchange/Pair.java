package it.sister.statportal.odata.etl.exchange;

/**
 * Generica coppia di oggetti
 *
 */
public class Pair<F,S> {

	protected F first;
	
	protected S second;
	
	/**
	 * Costruisce la coppia
	 * @param first primo elemento
	 * @param second secondo elemento
	 */
	public Pair(F first, S second){
		this.first = first;
		this.second = second;
	}
	
	/**
	 * Fornisce il primo elemento della coppia
	 * @return il primo elemento della coppia
	 */
	public F getFirst(){
		return this.first;
	}
	
	/**
	 * Fornisce il secondo elemento della coppia
	 * @return il secondo elemento della coppia
	 */
	public S getSecond(){
		return this.second;
	}
	
}
