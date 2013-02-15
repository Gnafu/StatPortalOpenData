package it.sister.extension.publish;

import it.sister.utils.DBManager;
import it.sister.utils.STUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.rest.AbstractCatalogResource;
import org.geoserver.rest.format.DataFormat;
import org.geoserver.rest.format.StringFormat;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;

public class JoinTableResource extends AbstractCatalogResource {

	private DBManager dbManager;	

	public JoinTableResource(Context context, Request request,
			Response response, Catalog catalog) {
		super(context, request, response, String.class, catalog);
	}

	@Override
	protected List<DataFormat> createSupportedFormats(Request request,
			Response response) {
		List<DataFormat> formats = new ArrayList<DataFormat>();
		formats.add(new StringFormat(MediaType.TEXT_XML));
		return formats;
	}

	@Override
	protected String handleObjectGet() throws Exception {		
		dbManager = new DBManager();
		String viewName = joinTable();
		return "<JoinTable><ViewName>" + viewName + "</ViewName></JoinTable>";
	}

	private String joinTable() {
		try {		
			// Get request parameters
			String wsName = getAttribute("workspace");
			String dsName = getAttribute("dataStore");
			String layerTableName = getAttribute("layerTableName");
			String dataTableName = getAttribute("dataTableName");
			String joinLayerField = getAttribute("joinLayerField");
			String joinTableField = getAttribute("joinTableField");
			String reqParams = "layerTableName: " + layerTableName
					+ " dataTableName: " + dataTableName + " joinLayerField: "
					+ joinLayerField + " joinTableField: " + joinTableField;

			String viewName = "geo_" + layerTableName + "_" + dataTableName;
			// connect to database			
			// Get connection properties			
			WorkspaceInfo wsInfo = catalog.getWorkspaceByName(wsName);
			DataStoreInfo dsInfo = catalog.getDataStoreByName(wsInfo, dsName);
			String host = (String)dsInfo.getConnectionParameters().get("host");	
			String port = dsInfo.getConnectionParameters().get("port").toString();			
			String database = (String)dsInfo.getConnectionParameters().get("database");			
			String user = (String)dsInfo.getConnectionParameters().get("user");
			String passwd = (String)dsInfo.getConnectionParameters().get("passwd");
			
			if (dbManager.createPostgreSQLConnection(host,port,database,user,passwd)){
				if(dbManager.viewExists(viewName)){
					int numRec = dbManager.getNumRecords(viewName);
					if (numRec > 0) {
						return viewName;
					}
				}else{					
					//create the view
					//get field names
					LinkedList<String> layerTableFields = dbManager.getTableFields(layerTableName);
					LinkedList<String> dataTableFields = dbManager.getTableFields(dataTableName);
					String selectFields = "";
					int c = 0;
					for (Iterator<String> lFieldsIter = layerTableFields.iterator(); lFieldsIter
							.hasNext();) {
						String layerFieldName = (String) lFieldsIter.next();
						if (c > 0) {
							if (!layerFieldName.equalsIgnoreCase("the_geom"))
								selectFields += ", l.\"" + layerFieldName
										+ "\" AS \"" + layerTableName + "_"
										+ layerFieldName + "\"";
							else
								selectFields += ", l.\"" + layerFieldName + "\"";
						} else {
							if (!layerFieldName.equalsIgnoreCase("the_geom"))
								selectFields += "l.\"" + layerFieldName
										+ "\" AS \"" + layerTableName + "_"
										+ layerFieldName + "\"";
							else
								selectFields += "l.\"" + layerFieldName + "\"";
						}
						c++;
					}
					selectFields += " ";
					for (Iterator<String> dFieldsIter = dataTableFields.iterator(); dFieldsIter
							.hasNext();) {
						String dataFieldName = (String) dFieldsIter.next();
						selectFields += ", d.\"" + dataFieldName + "\"";
					}
					String viewDefinition = "SELECT " + selectFields + " FROM \""
							+ layerTableName + "\" l JOIN \"" + dataTableName
							+ "\" d " + "ON l.\"" + joinLayerField + "\" = d.\""
							+ joinTableField + "\"";
					STUtils.writeDebugMessage("JoinTable: definizione della vista: " + viewDefinition);
					if (dbManager.createView(viewName, viewDefinition)) {
						// Aggiungo la riga alla tabella 'geometry_columns' di postgis se non è gia stata inserita
						if(!dbManager.geometryColumnsRowExists(viewName)){
							String getSrid = "(select srid from geometry_columns where f_table_name='"
									+ layerTableName + "')";
							String getType = "(select type from geometry_columns where f_table_name='"
									+ layerTableName + "')";
							String query = "insert into geometry_columns(f_table_catalog,f_table_schema,f_table_name,f_geometry_column,coord_dimension,srid,type) values ('','public','"
									+ viewName
									+ "','the_geom',2,"
									+ getSrid
									+ ","
									+ getType + " )";
							if (dbManager.executeQuery(query)) {
								int numRec = dbManager.getNumRecords(viewName);
								if (numRec > 0) {
									return viewName;
								}
							}
						}else{
							int numRec = dbManager.getNumRecords(viewName);
							if (numRec > 0) {
								return viewName;
							}
						}
					}
				}
			}
			return "";
		} catch (Exception e) {
			STUtils.writeDebugMessage("Errore durante joinTable: "+ e.getMessage());
			e.printStackTrace();
			return "";
		} finally {
			if (dbManager != null)
				dbManager.CloseConnection();
		}
	}
}
