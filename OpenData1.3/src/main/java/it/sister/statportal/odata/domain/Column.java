package it.sister.statportal.odata.domain;

/***
 * 
 * Classe che definisce una colonna attraverso il nome logico e quello fisico
 * 
 */
public class Column {

    /**
     * Id
     */
    private int id;

    /**
    * Nome fisico
    */
    private String physicalName;
    
    /**
    * Nome logico
    */
    private String logicalName;

    /**
     * Tipo della colonna
     */
    private ColumnType type;
    
    private Integer differentDistinctCount;

    /**
     * Costruttore
     */
    public Column() {

    }

    /**
     * Costruttore completo
     * 
     * @param physicalName
     * @param logicalName
     * @param id
     * @param type
     */
    public Column(String physicalName, String logicalName, int id,
	    ColumnType type) {
	this.physicalName = physicalName;
	this.logicalName = logicalName;
	this.id = id;
	this.type = type;
    }

    /**
     * Costruttore parziale
     * 
     * @param physicalName
     *        nome fisico
     * @param logicalName
     *        nome logico
     * @param id
     * 	      id della colonna
     */
    public Column(String physicalName, String logicalName, int id) {
	this.physicalName = physicalName;
	this.logicalName = logicalName;
	this.id = id;
    }

    /**
     * Restituisce il nome fisico della colonna con il quale possiamo generare
     * filtri sui dati
     * 
     * @return il nome fisico della colonna
     */
    public String getPhysicalName() {
	return physicalName;
    }

    /**
     * Restituisce il nome logico della colonna con il quale possiamo presentare
     * i dati agli utenti, se presente. Altrimenti si restituisce il nome fisico
     * 
     * @return nome logico. Se non presente si restituisce il nome fisico
     */
    public String getLogicalName() {
	return (logicalName != null) ? logicalName : physicalName;
    }
    
    /**
     * Espone il numero di elementi distinti per la colonna
     * @return il numero di elementi distinti
     */
    public Integer getDifferentDistinctCount() {
	return differentDistinctCount;
    }

    /**
     * Imposta l'id della colonna. Nel caso si tratti di una colonna di tipo dimensionale 
     * l'id corrisponde all'id della dimensione. Lo stesso vale per le misure 
     * 
     * @param id
     */
    public void setId(int id) {
	this.id = id;
    }
    
    /**
     * 
     * @return l'id della colonna
     */
    public int getId(){
	return id;
    }
    
    /**
     * 
     * @return il tipo della colonna
     */
    public ColumnType getType(){
	return type;
    }

    /**
     * imposta il tipo della colonna
     * @param type	tipo della colonna
     */
    public void setType(ColumnType type) {
	this.type = type;
    }
    
    /**
     * imposta il nome logico
     * @param logicalName	nome logico
     */
    public void setLogicalName(String logicalName){
	this.logicalName = logicalName;
    }
    
    /**
     * imposta il nome fisico
     * @param physicalName	nome fisico
     */
    public void setPhysicalName(String physicalName){
	this.physicalName = physicalName;
    }
    

    /**
     * Imposta il numero di elementi distinti per la colonna
     * @param differentDistinctCount	numero di elementi distinti
     */
    public void setDifferentDistinctCount(Integer differentDistinctCount){
	this.differentDistinctCount = differentDistinctCount;
    }

    /**
     * Eumerato che rappresenta i possibili tipi che può prendere una colonna
     *
     */
    public enum ColumnType {
	MEASURE, DIMENSION, GENERIC_COLUMN, OTHER;
    }

}
