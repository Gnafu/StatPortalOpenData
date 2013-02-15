package it.sister.statportal.odata.etl;

import java.io.File;
import java.io.FilenameFilter;

/**
 * Filtro sui nomi dei file basato sulla corrispondenza precisa al netto delle maiuscole/minuscole
 *
 */
public class ExactFilter implements FilenameFilter{
	
	String fileName;
	
	/**
	 * Crea il filtro 
	 * @param fileName nome del filtro
	 */
	public ExactFilter(String fileName) {
		this.fileName = fileName;
	}

	public boolean accept(File directory, String name) {
		return name.equalsIgnoreCase(this.fileName);
	}
}
