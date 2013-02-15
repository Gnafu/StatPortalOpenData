package it.sister.statportal.odata.proxy.rdf;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import it.sister.statportal.odata.domain.Column;
import it.sister.statportal.odata.domain.MdData;
import it.sister.statportal.odata.domain.OdataDomainException;
import it.sister.statportal.odata.domain.Row;
import it.sister.statportal.odata.utility.DBUtils;

/**
 * Classe che si occupa di esportare un dato di StatPortal in formato RDF
 *
 */
public class RdfWriter {

	/**
	 * Formattatore del file
	 */
	protected RdfFormatter formatter;
	
	/**
	 * Costruisce il writer
	 * @param formatter il formatter da usare
	 */
	public RdfWriter(RdfFormatter formatter){		
		this.formatter = formatter;
	}
	
	/**
	 * Esporta un dato in un formato rdf scrivendolo nella cartella specificata
	 * @param data il dato da esportare
	 * @param directory la directory in cui scrivere il file
	 * @param d2rqServer indirizzo del server d2rq
	 * @throws IOException 
	 * @throws OdataDomainException 
	 */
	public File writeFile(MdData data, File directory, String d2rqServer) throws IOException, OdataDomainException{
		//controllo se la directory esiste
		if(!directory.exists()){
			throw new FileNotFoundException("Invalid directory");
		}
		//poi genero il file
		File exportFile = new File(directory+"/"+data.getUid()+"."+formatter.getExtension());		
		//se il file non esiste oppure esiste ma è vecchio cancello il file e lo riscrivo, altrimenti lascio tutto com'è
		if(!exportFile.exists() || exportFile.lastModified() < data.getLastUpdate().getTime()){
			exportFile.delete();
			FileOutputStream fileStream = null;
			PrintWriter printWriter = null;
			try{
				List<Map<String, Object>> primaryKeys = DBUtils.getPrimaryKeys(data.getTableName());
				String name = data.getName();
				String tableName = data.getTableName();
				boolean pkExists = data.pkEsists();
				List<Column> columns = data.getColumns();
				fileStream = new FileOutputStream(exportFile);
				printWriter = new PrintWriter(fileStream);
				printWriter.println(formatter.getHeader(name, tableName, pkExists, columns, d2rqServer));
				//ciclo sulle righe a blocchi di un certo numero di righe per volta
				int block = 1000;
				int rowCount = data.getCountRows();
				int read = 0;
				int toRead = Math.min(block, rowCount - read);
				while(toRead > 0){
					List<Row> rows = null;
					if(data.pkEsists()){
						rows = data.getRows(read, toRead, "");
					}else{
						List<Map<String, Object>> mapList = DBUtils.executeQueryForList("SELECT * FROM view_"+data.getTableName()+" OFFSET "+read+" LIMIT "+toRead);
						rows = new ArrayList<Row>();
						for(Map<String,Object> map : mapList){
							Row row = new Row();
							for(String key : map.keySet()){
								String value = map.get(key) != null ? map.get(key).toString() : null;
								row.addValue(key, value);
							}
							rows.add(row);
						}
					}
					printWriter.println(formatter.formatRows(tableName, pkExists, rows, primaryKeys, columns));
					read += toRead;
					toRead = Math.min(block, rowCount - read);
				}				
				printWriter.println(formatter.getFooter());
			}
			finally{
				if(printWriter != null){
					printWriter.close();
				}
				if(fileStream != null){
					fileStream.close();
				}				
			}
			
		}
		return exportFile;
	}
	
}
