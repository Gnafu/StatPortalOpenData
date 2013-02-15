package it.sister.statportal.odata.etl;

import java.io.File;
import java.io.FilenameFilter;

/**
 * Filtro sui nomi dei file che accetta un file se l'estensione coincide con l'input al
 * netto delle maiuscole/minuscole
 *
 */
public class ExtensionFilter implements FilenameFilter {

	private String extension;

	/**
	 * Costruisce il filtro
	 * @param extension l'estensione richiesta
	 */
	public ExtensionFilter(String extension) {
		this.extension = extension;
	}

	public boolean accept(File directory, String name) {
		return name.toUpperCase().endsWith("."+extension.toUpperCase());
	}
}