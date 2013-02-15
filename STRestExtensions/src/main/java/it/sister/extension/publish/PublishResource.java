package it.sister.extension.publish;

import it.sister.utils.STUtils;

import java.util.ArrayList;
import java.util.List;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ProjectionPolicy;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.rest.AbstractCatalogResource;
import org.geoserver.rest.format.DataFormat;
import org.geoserver.rest.format.StringFormat;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;

public class PublishResource extends AbstractCatalogResource {

	public PublishResource(Context context, Request request, Response response,
			Catalog catalog) {
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
		String layer = publishTable();
		return layer;
	}

	private String publishTable() throws Exception {
		// Get request parameters
		String wsName = getAttribute("workspace");
		String dsName = getAttribute("dataStore");
		String tableName = getAttribute("tableName");
		String layerName = getAttribute("layerName");
		if(layerName==null){
			layerName = tableName;
        }
		String baseLayerName=layerName;
		
		NamespaceInfo namespace = catalog.getNamespaceByPrefix(wsName);
		WorkspaceInfo wsInfo = catalog.getWorkspaceByName(wsName);
		DataStoreInfo dsInfo = catalog.getDataStoreByName(wsInfo, dsName);

		DataStore dataStore = DataStoreFinder.getDataStore(dsInfo.getConnectionParameters());
		if (dataStore != null) {			
			//check layer
	        if(layerName!= null){	        	
	        	if(STUtils.featureTypeExists(catalog, wsName, layerName)){

	        		NamespaceInfo ns = catalog.getNamespaceByPrefix(wsName);	        
	        		FeatureTypeInfo fti = catalog.getFeatureTypeByName(namespace,layerName);
	        		if(fti.getNativeName().equals(tableName)){
	        			STUtils.writeDebugMessage("Publish: Il layer '" + layerName + "' è già pubblicato in geoserver quindi non viene ripubblicato");
	        			return "<Layer><WorkspaceName>" + wsName + "</WorkspaceName><LayerName>" + layerName + "</LayerName></Layer>";
	        		}else{
	        			STUtils.writeDebugMessage("Publish: Il layer '" + layerName + "' esiste già in geoserver");
	        			//esiste un layer con lo stesso nome ma la tabella pubblicata è diversa
	        			int count=1;
	        			layerName = baseLayerName + "_" + String.valueOf(count);
	    	        	while (STUtils.featureTypeExists(catalog, wsName, layerName)) {
	    					layerName = baseLayerName + "_" + String.valueOf(count+1);
	    				}
	        		}
	        	}
	        }
	       
			LayerInfo layer = createLayer(tableName, layerName,namespace, dsInfo, dataStore);
			if( dataStore != null ){
	        	dataStore.dispose();
	        	dataStore = null;
	    	}
			if(layer!=null){				
				return "<Layer><WorkspaceName>" + wsName + "</WorkspaceName><LayerName>" + layer.getName() + "</LayerName></Layer>";
			}
		}
		return "";
	}

	/**
	 * @param tableName
	 * @param layerName
	 * @param namespace
	 * @param dsInfo
	 * @param dataStore
	 * @return
	 * @throws Exception
	 */
	private LayerInfo createLayer(String tableName, String layerName,
			NamespaceInfo namespace, DataStoreInfo dsInfo, DataStore dataStore){
		
		LayerInfo layer=null;
		FeatureTypeInfo featureTypeInfo=null;
		try {					
			//Crea FeatureType
			featureTypeInfo = createFeatureType(tableName, layerName,namespace, dsInfo);	
			//Recupera collezione dati
			SimpleFeatureCollection collection = STUtils.getFeaturesCollection(dataStore, featureTypeInfo.getNativeName(), "include"); 
			//Imposta envelope
			setFeatureTypeBoundingBox(featureTypeInfo, collection);
			//Imposta stile di default
			StyleInfo style = STUtils.getDefaultStyle(catalog, collection);			
			//Imposta le proprietà del layer
			layer = setLayerProperties(featureTypeInfo, style);
			if(layer!=null){
				//aggiunge featureType al catalogo
				catalog.add(featureTypeInfo);
				catalog.add(layer);	
			}
		} catch (TransformException te) {
			STUtils.writeErrorMessage(te.getMessage());
			return null;
		} catch (Exception e) {
			STUtils.writeErrorMessage(e.getMessage());
			return null;
		}
		return layer;
	}

	/**
	 * @param featureTypeInfo
	 * @param style
	 * @return
	 */
	private LayerInfo setLayerProperties(FeatureTypeInfo featureTypeInfo,StyleInfo style) {
		LayerInfo layer;
		layer=catalog.getFactory().createLayer();
		layer.setDefaultStyle(style);
		layer.setResource(featureTypeInfo);
		layer.setQueryable(true);
		layer.setEnabled(true);
		layer.setAdvertised(true);
		return layer;
	}

	/**
	 * @param featureTypeInfo
	 * @param collection
	 * @throws TransformException
	 */
	private void setFeatureTypeBoundingBox(FeatureTypeInfo featureTypeInfo,
			SimpleFeatureCollection collection) throws TransformException {
		ReferencedEnvelope nativeEnvelope = collection.getBounds();
		CoordinateReferenceSystem nativeCRS = nativeEnvelope.getCoordinateReferenceSystem();
		featureTypeInfo.setNativeCRS(nativeCRS);
		featureTypeInfo.setLatLonBoundingBox(nativeEnvelope);

		// verificare sistema di riferimento
		ReferencedEnvelope declaredEnvelope = null;
		declaredEnvelope = (ReferencedEnvelope) nativeEnvelope.toBounds(featureTypeInfo.getCRS());
		featureTypeInfo.setNativeBoundingBox(declaredEnvelope);
	}

	/**
	 * @param tableName
	 * @param layerName
	 * @param namespace
	 * @param dsInfo
	 * @return
	 */
	private FeatureTypeInfo createFeatureType(String tableName,
			String layerName, NamespaceInfo namespace, DataStoreInfo dsInfo) {
		FeatureTypeInfo featureTypeInfo;
		featureTypeInfo = catalog.getFactory().createFeatureType();
		featureTypeInfo.setNamespace(namespace);
		featureTypeInfo.setStore(dsInfo);
		featureTypeInfo.setEnabled(true);
		featureTypeInfo.setAdvertised(true);
		featureTypeInfo.setNativeName(tableName);
		featureTypeInfo.setName(layerName);
		featureTypeInfo.setMaxFeatures(0);
		featureTypeInfo.setNumDecimals(0);
		featureTypeInfo.setTitle(layerName);
		featureTypeInfo.setProjectionPolicy(ProjectionPolicy.REPROJECT_TO_DECLARED);
		featureTypeInfo.setSRS("EPSG:900913");
		return featureTypeInfo;
	}

	

}
