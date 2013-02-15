package it.sister.statportal.odata.proxy.rdf;

import java.util.List;
import java.util.Map;

import it.sister.statportal.odata.domain.Column;
import it.sister.statportal.odata.domain.OdataDomainException;
import it.sister.statportal.odata.domain.Row;

/**
 * Classe astratta che si occupa di trasformare il contenuto di un dato nei vari pezzi di un file RDF.
 * I vari formatter concreti ridefiniranno i metodi del formatter astratto a seconda del tipo di 
 * formato rdf scelto.
 *
 */
public abstract class RdfFormatter {

	/**
	 * Restituisce l'estensione che dovranno avere i file creati dal formatter
	 * @return l'estensione che dovranno avere i file creati dal formatter
	 */
	public abstract String getExtension();
	
	/**
	 * Fornisce l'header del file rdf
	 * @param name il nome del dato
	 * @param tableName il nome della tabella
	 * @param pkExists flag che indica se è una tabella con chiave
	 * @param columns le colonne della tabella
	 * @param d2rqServer l'indirizzo del server d2rq
	 * @return l'header del file rdf
	 * @throws OdataDomainException
	 */
	public String getHeader(String name, String tableName, boolean pkExists, List<Column> columns, String d2rqServer) throws OdataDomainException {
		return "";
	}
	
	/**
	 * Fornisce la formattazione di un gruppo di righe in un formato rdf
	 * @param tableName il nome della tabella
	 * @param pkExists flag che indica se è una tabella con chiave
	 * @param rows il gruppo di righe da formattare
	 * @param primaryKeys l'insieme di colonne che formano la chiave primaria
	 * @param columns le colonne della tabella
	 * @return la formattazione di un gruppo di righe in un formato rdf
	 * @throws OdataDomainException
	 */
	public String formatRows(String tableName, boolean pkExists, List<Row> rows, List<Map<String, Object>> primaryKeys, List<Column> columns) throws OdataDomainException{
		StringBuilder builder = new StringBuilder();
		for(Row row : rows){
			builder.append(this.formatRow(tableName, pkExists, row, primaryKeys, columns));
		}
		return builder.toString();
	}
	
	/**
	 * Fornisce la formattazione di una riga in un formato rdf
	 * @param tableName il nome della tabella
	 * @param pkExists flag che indica se è una tabella con chiave
	 * @param row la riga da formattare
	 * @param primaryKeys l'insieme di colonne che formano la chiave primaria
	 * @param columns le colonne della tabella
	 * @return la formattazione di una riga in un formato rdf
	 * @throws OdataDomainException
	 */
	protected String formatRow(String tableName, boolean pkExists, Row row, List<Map<String, Object>> primaryKeys, List<Column> columns) throws OdataDomainException{
		return "";
	}

	/**
	 * Fornisce il footer del file rdf
	 * @return il footer per il file rdf
	 */
	public String getFooter(){
		return "";
	}
	
	
}
