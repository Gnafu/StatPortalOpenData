package it.sister.statportal.odata.proxy;

import java.io.File;
import java.io.FileFilter;

import org.joda.time.DateTime;

/**
 * Filtro da usare per ottenere i file di una directory modificati prima di una certa data
 *
 */
public class DateTimeFileFilter implements FileFilter {

	private DateTime notAfter;

	/**
	 * Costruisce il filtro
	 * @param notAfter soglia oltre la quale non prendere i file
	 */
	public DateTimeFileFilter(DateTime notAfter) {
		this.notAfter = notAfter;
	}

	@Override
	public boolean accept(File file) {		
		DateTime lastModified = new DateTime(file.lastModified());
		return file.isFile() && notAfter.isAfter(lastModified);
	}
}