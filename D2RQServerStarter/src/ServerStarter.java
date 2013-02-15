import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;

import org.springframework.context.support.ClassPathXmlApplicationContext;

import d2rq.generate_mapping;
import d2rq.server;
import it.sister.statportal.odata.domain.Column;
import it.sister.statportal.odata.domain.MdData;
import it.sister.statportal.odata.domain.OdataDomainException;
import it.sister.statportal.odata.utility.DBUtils;


public class ServerStarter {

	/**
	 * COMANDO PER FARLO PARTIRE
	 * nohup java -cp .:./* ServerStarter > /dev/null 2>&1 &
	 * @param args
	 */
	public static void main(String[] args) {
		
		new ClassPathXmlApplicationContext("classpath*:META-INF/applicationContext.xml");
		
		String ttlFile = readConfig("ttlFile");
		String serialField = readConfig("serialField");
		String prefixForTableWithoutPK = readConfig("prefixForTableWithoutPK");
		String jdbcStrConnection = readConfig("jdbcStrConnection");
		String dbUser = readConfig("dbUser");
		String dbPassword = readConfig("dbPassword");
		
		// hashmap che mantiene tutte le coppie <chiave, valore> che
		// vanno sostituite nel file di mapping
		HashMap<String, String> toReplace = new HashMap<String, String>();
		//hashmap che mette in relazione il nome della tabella di un dato con il nome del dato
		HashMap<String, String> tableNameToDataName = new HashMap<String, String>();
		//hashmap che mette in relazione il nome della tabella di un dato
		//con una tabella contenente le relazioni nome colonna - alias per tutte le sue colonne
		HashMap<String, HashMap<String, String>> tableNameToColumnTable = new HashMap<String, HashMap<String,String>>();
		
		// si scorrono tutti gli MdData per comporre la lista di tabelle per il mapping
		StringBuilder tablesList = new StringBuilder();
		for(MdData data : MdData.findAllMdDatas()){
		    String tableName = data.getTableName().toLowerCase().trim();
		    if(!data.getName().equals("")){
		    	tableNameToDataName.put(tableName, xmlEscapeText(data.getName()));
		    }
		    HashMap<String, String> columnNameToAlias = new HashMap<String, String>();
		    try {
				for(Column column : data.getColumns()){
					if(!column.getLogicalName().equals("")){
						columnNameToAlias.put(column.getPhysicalName(), xmlEscapeText(column.getLogicalName()));
					}
				}
			} catch (OdataDomainException e1) {
				e1.printStackTrace();
			}
		    tableNameToColumnTable.put(tableName, columnNameToAlias);
		    try{
		    	if(!mustCreateView(data)){
		    		tablesList.append(((tablesList.length() != 0) ? "," : "") + "" + tableName + "");
		    	}else{
		    		String viewName = prefixForTableWithoutPK + tableName;
		    		// si aggiornano le configurazioni da sostituire
		    		toReplace.put("d2rq:uriPattern \"" + viewName + "\";", "d2rq:uriPattern \"" + viewName + "/@@" + viewName + "." + serialField + "@@\";");
		    		// se non c'è la vista si genera
		    		if(!DBUtils.viewExists(viewName)){
		    			data.createViewWithSerialUID(viewName, false, serialField);
		    		}
		    		
		    		tablesList.append(((tablesList.length() != 0) ? "," : "") + "" + viewName + "");
		    	}
		    }catch(Exception e){
		    	// Non importa: probabilmente non esiste più la tabella e quindi è un dato vecchio
		    	// che su Drupal non è più accessibile 
		    }
		}

		String[] generate_mapping_args = new String[11];
		generate_mapping_args[0] = "-o";
		generate_mapping_args[1] = ttlFile;
		generate_mapping_args[2] = "--tables";
		generate_mapping_args[3] = tablesList.toString();
		generate_mapping_args[4] = "-d";
		generate_mapping_args[5] = "org.postgresql.Driver";
		generate_mapping_args[6] = "-u";
		generate_mapping_args[7] = dbUser;
		generate_mapping_args[8] = "-p";
		generate_mapping_args[9] = dbPassword;
		generate_mapping_args[10] = jdbcStrConnection;
		
		new generate_mapping().process(generate_mapping_args);
		
		// TODO: si deve aprire il mapping generato per modificare le chiavi primarie delle viste
		try {
			editMappingFile(ttlFile, toReplace, tableNameToDataName, tableNameToColumnTable);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		String serverBaseURI = readConfig("serverBaseURI");
		String verboseAndDebug = readConfig("verboseAndDebug");
		String port = readConfig("port");
		String[] server_args;
		if(verboseAndDebug.equalsIgnoreCase("true")){
			server_args = new String[]{"-b", serverBaseURI, "-pt", port, "--verbose", "--debug", ttlFile};
		}else{
			server_args = new String[]{"-b", serverBaseURI, "-pt", port, ttlFile};
		}
		
		
		// si fa partire il server
		new server().process(server_args);
	}
	
	/**
	 * Controlla se è necessario creare una vista per poter fare l'export rdf
	 * è necessario creare la vista se:
	 * 1) non esiste una chiave primaria
	 * 2) tra le colonne è presente la colonna the_geom
	 * @param data il dato
	 * @return true se bisogna creare la vista, false altrimenti
	 * @throws OdataDomainException
	 */
	private static boolean mustCreateView(MdData data) throws OdataDomainException{
		if(!data.pkEsists()){
			return true;
		}
		List<Column> columns = data.getColumns();
		for(Column column : columns){
			if(column.getPhysicalName().equalsIgnoreCase("the_geom")){
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Modifica il file ttl per aggiungerci le informazioni che ci servono:
	 * 1) la chiave primaria per quelle tabelle che non l'hanno
	 * 2) una label basata sul nome logico e non fisico del dato/colonna
	 * @param mapFilePath il file da modificare
	 * @param uriPatternMapping mapping per le chiavi
	 * @param tableNameToDataName tabella di corrispondenza nome tabella - nome dato
	 * @param tableNameToColumnTable tabella di corrispondenza nome tabella - nomi logici/fisici del dato
	 * @throws IOException
	 */
	private static void editMappingFile(String mapFilePath, HashMap<String, String> uriPatternMapping, 
			HashMap<String, String> tableNameToDataName, HashMap<String, HashMap<String, String>> tableNameToColumnTable) throws IOException{
        File f=new File(mapFilePath);
        FileInputStream fs = null;
        InputStreamReader in = null;
        BufferedReader br = null;
        StringBuffer sb = new StringBuffer();
        String textinLine;
        try {
        	fs = new FileInputStream(f);
            in = new InputStreamReader(fs);
            br = new BufferedReader(in);
            while(true){
            	textinLine=br.readLine();
                if(textinLine==null)
                    break;
                
				if(!textinLine.contains("# Sorry, I don't know which columns to put into the uriPattern") &&
				   !textinLine.contains("because the table doesn't have a primary key.") &&
				   !textinLine.contains("#     Please specify it manually.")){
					if(textinLine.contains("d2rq:uriPattern \"")){
						// si cerca se l'uriPattern è tra quelli da modificare
						if(uriPatternMapping.containsKey(textinLine.trim())){
							String newLine = uriPatternMapping.get(textinLine.trim());
							sb.append(newLine+"\n");
						}else{
							sb.append(textinLine+"\n");
						}
					} else if(textinLine.contains("d2rq:pattern \"")){
						sb.append(getPatternFromLine(textinLine, tableNameToDataName)+"\n");
					} else{
						if(textinLine.contains("d2rq:propertyDefinitionLabel")){
							sb.append(getColumnAliasFromLine(textinLine, tableNameToColumnTable)+"\n");
						}else if(textinLine.contains("d2rq:classDefinitionLabel")){
							sb.append(getDataNameFromLine(textinLine, tableNameToDataName)+"\n");
						}else{
							sb.append(textinLine+"\n");
						}
					}
				}
            }
            fs.close();
            in.close();
            br.close();
        } finally{
        	if(fs != null){
        		fs.close();
        	}        	
        }
        FileWriter fstream = null;
        try{
        	fstream = new FileWriter(f);
            BufferedWriter outobj = new BufferedWriter(fstream);
            outobj.write(sb.toString());
            outobj.close();
        } finally {
        	if(fstream != null){
        		fstream.close();
        	}
        }
    }
	
	/**
	 * Restituisce una stringa formata dall'intestazione d2rq:propertyDefinitionLabel e dal nome logico della colonna
	 * @param line una riga del file ttl
	 * @param tableNameToColumnTable tabella con il mapping {nome tabella dato - tabella di corrispondenza nomi fisici/logici delle sue colonne}
	 * @return una stringa formata dall'intestazione d2rq:propertyDefinitionLabel e dal nome logico della colonna
	 */
	protected static String getColumnAliasFromLine(final String line, final HashMap<String, HashMap<String, String>> tableNameToColumnTable){
		final String currentLabel = extractLabel(line);
		//se non c'era nessun valore lascio invariato
		if(currentLabel.equals("")){
			return line;
		}
		String[] splittedLabel = currentLabel.split(" ");
		//mi aspetto che la label corrente sia composta da nome tabella - spazio - nome fisico colonna
		if(splittedLabel.length != 2){
			return line;
		}
		//se non ho il nome della tabella nella tabella di corrispondenza lascio invariata la riga
		if(splittedLabel[0].startsWith("view_")){
			splittedLabel[0] = splittedLabel[0].substring(5);
		}
		if(!tableNameToColumnTable.containsKey(splittedLabel[0])){
			return line;
		}
		HashMap<String, String> columnNameToAlias = tableNameToColumnTable.get(splittedLabel[0]);
		if(columnNameToAlias == null || !columnNameToAlias.containsKey(splittedLabel[1])){
			return line;
		}
		return "\td2rq:propertyDefinitionLabel \""+columnNameToAlias.get(splittedLabel[1])+"\";";
	} 
	
	/**
	 * Restituisce una stringa formata dall'intestazione d2rq:classDefinitionLabel e dal nome del dato
	 * @param line una riga del file ttl
	 * @param tableNameToDataName tabella con il mapping {nome tabella dato - nome dato}
	 * @return una stringa formata dall'intestazione d2rq:classDefinitionLabel e dal nome del dato
	 */
	protected static String getDataNameFromLine(final String line, final HashMap<String, String> tableNameToDataName){
		String currentLabel = extractLabel(line);
		//se non c'era nessun valore lascio invariato
		if(currentLabel.equals("")){
			return line;
		}
		//se non ho il nome della tabella nella tabella di corrispondenza lascio invariata la riga
		if(currentLabel.startsWith("view_")){
			currentLabel = currentLabel.substring(5);
		}
		if(!tableNameToDataName.containsKey(currentLabel)){
			return line;
		}
		return "\td2rq:classDefinitionLabel \""+tableNameToDataName.get(currentLabel)+"\";";
	}

	/**
	 * Restituisce una stringa formata dall'intestazione d2rq:pattern e dal nome del dato concatenato con il percorso della chiave
	 * @param line una riga del file ttl
	 * @param tableNameToDataName tabella con il mapping {nome tabella dato - nome dato}
	 * @return una stringa formata dall'intestazione d2rq:pattern e dal nome del dato concatenato con il percorso della chiave
	 */
	protected static String getPatternFromLine(final String line, final HashMap<String, String> tableNameToDataName){
		String currentLabel = extractLabel(line);
		//se non c'era nessun valore lascio invariato
		if(currentLabel.equals("")){
			return line;
		}
		String tableName = "";
		if(currentLabel.indexOf(' ') != -1){
			tableName = currentLabel.substring(0, currentLabel.indexOf(' '));
		}
		//se non ho il nome della tabella nella tabella di corrispondenza lascio invariata la riga
		String toFind = tableName;
		if(tableName.startsWith("view_")){
			toFind = tableName.substring(5);
		}
		if(!tableNameToDataName.containsKey(toFind)){
			return line;
		}
		return line.replaceAll(toFind+" ", tableNameToDataName.get(toFind)+" ");
	}
	
	/**
	 * Estrae dalla linea il valore corrente della label
	 * @param line una riga del file ttl
	 * @return il valore corrente della label
	 */
	protected static String extractLabel(String line){
		final int from = line.indexOf('"');
		final int to = line.indexOf('"', from + 1);
		//se non trovo le informazioni restituisco la stringa vuota
		if(from == -1 || to == -1 || (to - (from + 1)) <= 0){
			return "";
		}
		return line.substring(from + 1, to);
	}
	
	/**
	 * Fa l'escape dei caratteri speciali xml sostituendoli con le relative codifiche
	 * @param text il testo da modificare
	 * @return il testo con i caratteri speciali sostituiti dalle loro codifiche
	 */
	protected static String xmlEscapeText(String text) {
		   StringBuilder sb = new StringBuilder();
		   for(int i = 0; i < text.length(); i++){
		      char c = text.charAt(i);
		      switch(c){
		      case '<': sb.append("&lt;"); break;
		      case '>': sb.append("&gt;"); break;
		      case '\"': sb.append("&quot;"); break;
		      case '&': sb.append("&amp;"); break;
		      case '\'': sb.append("&apos;"); break;
		      default:
		         if(c>0x7e) {
		            sb.append("&#"+((int)c)+";");
		         }else
		            sb.append(c);
		      }
		   }
		   return sb.toString();
		}
	
	
	/**
	 * Legge una chiave dal file di configurazione
	 * @param key il nome della chiave 
	 * @return il valore della chiave se è presente, null altrimenti
	 */
	private static String readConfig(String key) {
		try{			
			BufferedReader reader = null;
			try{
				reader = new BufferedReader(new java.io.FileReader("META-INF/config.txt"));
			}catch(Exception exception){
				reader = new BufferedReader(new java.io.FileReader("src/META-INF/config.txt"));
			}
			//BufferedReader reader = new BufferedReader(new java.io.FileReader("META-INF/config.txt"));
			String line = null;
			while((line = reader.readLine()) != null){
				String[] splittedLine = line.split("\t");
				if(splittedLine.length == 2){
					if(splittedLine[0].equals(key)){
						return splittedLine[1];
					}
				}
			}
			return null;
		}catch(Exception ex){
			return null;
		}
	}

}
