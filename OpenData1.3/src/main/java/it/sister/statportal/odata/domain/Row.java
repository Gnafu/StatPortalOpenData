package it.sister.statportal.odata.domain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * 
 * Classe che rappresenta una riga
 * 
 */
public class Row {

    /**
     * Insieme dei valori della riga
     */
    private HashMap<String, String> values;

    /**
     * Costruttore che inizializza un insieme di valori vuoto
     */
    public Row() {
	values = new HashMap<String, String>();
    }


    /**
     * Restituisce i valori di una riga
     * @return l'insieme dei valori della riga
     */
    public HashMap<String, String> getValues() {
	return values;
    }
    
    /**
     * Restituisce il valore della riga 
     * associato alla chiave passata
     * @param key
     * @return	il valore della riga 
     * associato alla chiave passata
     */
    public String getValue(String key){
	return values.get(key);
    }

    /**
     * Aggiunge un valore alla riga
     * @param key chiave da aggiungere
     * @param value valore da aggiungere
     */
    public void addValue(String key, String value) {
	values.put(key, value);
    }


}