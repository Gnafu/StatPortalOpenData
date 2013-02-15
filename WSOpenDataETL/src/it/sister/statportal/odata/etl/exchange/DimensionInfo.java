package it.sister.statportal.odata.etl.exchange;

import it.sister.statportal.odata.domain.MdHierNode;
import it.sister.statportal.odata.utility.DBUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * Insieme di informazioni necessarie per la creazione di una nuova dimensione
 * e non calcolabili dal server
 *
 */
public class DimensionInfo extends ImportColumnInfo{

	//Alias della dimensione (es. Anno di nascita)
	protected String alias;
	
	//Descrizione della dimensione
	protected String description;
	
	//Nome della colonna nel file
	protected String columnName;
	
	//Id del nodo a cui la dimensione va collegata
	protected int idHierNode;
	
	//flag che indica se la dimensione è agganciata per chiave o descrizione
	protected Boolean isByKey;
	
	protected HashMap<String, String> table = null;
	
	protected Pair<HashSet<String>,HashSet<String>> keysAndDescriptions = null;
	
	/**
	 * Crea l'oggetto
	 * @param idHierNode id del nodo a cui collegare la dimensione
	 * @param alias alias della dimensione
	 * @param description descrizione della dimensione
	 * @param columnName nome della colonna nel file
	 */
	public DimensionInfo(int idHierNode, String alias, String description, String columnName){
		this.idHierNode = idHierNode;
		this.alias = alias;
		this.description = description;
		this.columnName = columnName;
	}
	
	public DimensionInfo(){}
	
	/**
	 * Imposta il valore della tabella di decodifica
	 * @param value il valore della tabella di decodifica
	 */
	public void setTable(List<Map<String, Object>> value){
		this.table = new HashMap<String, String>();
		for(Map<String, Object> record : value){
			String description = record.get("NODE_DESCR").toString();
			String key = record.get("NODE_ID").toString();
			Double d = new Double(key);
			if(!this.table.containsKey(description)){
				this.table.put(description.toUpperCase(), ""+d.intValue());
			}
		}
	}
	
	/**
	 * Fornisce una coppia di insiemi contenenti le chiavi e le descrizioni della dimensione
	 * @return una coppia di insiemi contenenti le chiavi e le descrizioni della dimensione
	 */
	public Pair<HashSet<String>,HashSet<String>> getKeysAndDescriptions(){
		if(this.keysAndDescriptions == null){
			initializeTables();
		}
		return this.keysAndDescriptions;
	}
	
	/**
	 * Inizializza la coppia di insiemi contenenti le chiavi e le descrizioni della dimensione
	 */
	private void initializeTables(){
		this.table = new HashMap<String, String>();
		MdHierNode node = MdHierNode.findMdHierNode(this.getIdHierNode());
		List<Map<String, Object>> result = DBUtils.executeQueryForList("SELECT " + node.getPkField()+ " AS NODE_ID, " + node.getDescField()+ " AS NODE_DESCR FROM " + node.getTableName());
		HashSet<String> nodeIdSet = new HashSet<String>();
		HashSet<String> nodeDescrSet = new HashSet<String>();
		for(Map<String, Object> entry : result){
			Double key = Double.parseDouble(entry.get("NODE_ID").toString());				
			String nodeId =  Integer.valueOf(key.intValue()).toString();
			if(!nodeIdSet.contains(nodeId)){
				nodeIdSet.add(nodeId);
			}
			String nodeDescr = entry.get("NODE_DESCR").toString();
			if(!nodeDescrSet.contains(nodeDescr)){
				nodeDescrSet.add(nodeDescr.toUpperCase());
			}
			this.table.put(nodeDescr.toUpperCase(), nodeId);
		}
		this.keysAndDescriptions = new Pair<HashSet<String>,HashSet<String>>(nodeIdSet, nodeDescrSet);
	}
	
	/**
	 * Espone la tabella di decodifica
	 * @return la tabella di decodifica
	 */
	public HashMap<String, String> getTable(){
		if(this.table == null){
			initializeTables();
		}
		return this.table;
	}
	
	/**
	 * Espone il flag che indica se la dimensione è agganciata per chiave o descrizione
	 * @return il flag che indica se la dimensione è agganciata per chiave o descrizione
	 */
	public Boolean getIsByKey(){
		return this.isByKey;
	}
	
	/**
	 * Imposta il flag che indica se la dimensione è agganciata per chiave o descrizione
	 * @param value il flag che indica se la dimensione è agganciata per chiave o descrizione
	 */
	public void setIsByKey(Boolean value){
		this.isByKey = value;
	}
	
	/**
	 * Espone l'id del nodo a cui la dimensione va collegata
	 * @return l'id del nodo a cui la dimensione va collegata
	 */
	public int getIdHierNode(){
		return this.idHierNode;
	}
	
	/**
	 * Imposta il valore dell'id del nodo a cui la dimensione va collegata
	 * @param value l'id del nodo a cui la dimensione va collegata
	 */
	public void setIdHierNode(int value){
		this.idHierNode = value;
	}
	
	/**
	 * Espose l'alias della dimensione
	 * @return l'alias della dimensione
	 */
	public String getAlias(){
		return this.alias;
	}
	
	/**
	 * Imposta il valore dell'alias della dimensione
	 * @param value l'alias della dimensione
	 */
	public void setAlias(String value){
		this.alias = value;
	}
	
	/**
	 * Espone la descrizione della dimensione
	 * @return la descrizione della dimensione
	 */
	public String getDescription(){
		return this.description;
	}
	
	/**
	 * Imposta il valore della descrizione della dimensione
	 * @param value la descrizione della dimensione
	 */
	public void setDescription(String value){
		this.description = value;
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
		return ColumnType.DIMENSION;
	}
}
