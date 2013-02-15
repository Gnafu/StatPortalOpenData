package it.sister.extension.extent;

import it.sister.utils.STUtils;

import java.util.ArrayList;
import java.util.List;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.rest.AbstractCatalogResource;
import org.geoserver.rest.format.DataFormat;
import org.geoserver.rest.format.StringFormat;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.styling.StyleFactory;
import org.opengis.filter.FilterFactory;
import org.opengis.referencing.operation.TransformException;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;

public class ExtentResource extends AbstractCatalogResource {

    private DataStore dataStore = null;

    private FeatureTypeInfo featureType = null;
    
    static StyleFactory styleFactory = CommonFactoryFinder.getStyleFactory(null);

    static FilterFactory filterFactory = CommonFactoryFinder.getFilterFactory(null);

    public ExtentResource(Context context, Request request, Response response,
            Catalog catalog) {
        super(context, request, response, String.class, catalog);        
    }
    
    @Override
    protected List<DataFormat> createSupportedFormats(Request request, Response response) {
        List<DataFormat> formats = new ArrayList<DataFormat>();
        formats.add(new StringFormat( MediaType.TEXT_XML ));
        return formats;
    }   
    
    @Override
    protected String handleObjectGet() throws Exception {
    	String extent = getExtent();
        return extent;
    }

    /**
     * 
     * @return
     * @throws Exception
     */
    private String getExtent() throws Exception{
    	// Get request parameters
        String wsName = getAttribute("workspace");
        String ftName = getAttribute("featureType");
        String filter = getAttribute("filter");
                
        NamespaceInfo namespace = catalog.getNamespaceByPrefix(wsName);
        featureType = catalog.getFeatureTypeByName(namespace, ftName);
        
        // get datastoreInfo
        DataStoreInfo dsInfo = featureType.getStore();
        dataStore = DataStoreFinder.getDataStore(dsInfo.getConnectionParameters());
        // get collection of data
        if(STUtils.isNullOrEmpty(filter)){
        	filter = "include";
        }
        SimpleFeatureCollection collection;
        try{
        	collection = STUtils.getFeaturesCollection(dataStore, featureType.getNativeName(), filter); 
        	if(collection.isEmpty()){
            	collection = STUtils.getFeaturesCollection(dataStore, featureType.getNativeName(), "include");             			
            }
        }catch(Exception e){
        	collection = STUtils.getFeaturesCollection(dataStore, featureType.getNativeName(), "include");
        }
        
        ReferencedEnvelope nativeEnvelope = collection.getBounds();
       
        //verifica sistema di riferimento
        ReferencedEnvelope declaredEnvelope=null;
        try{
        	declaredEnvelope = (ReferencedEnvelope)nativeEnvelope.toBounds(featureType.getCRS());
        }catch(TransformException te){
        	if(collection!=null){
            	collection=null;
            }
        	if( dataStore != null ){
            	dataStore.dispose();
            	dataStore = null;
        	}
        	return "";
        }
        if(collection!=null){        	
        	collection=null;
        }
        if( dataStore != null ){
        	dataStore.dispose();
        	dataStore = null;
    	}
        
        if(declaredEnvelope!=null){
        	return "<Envelope><xmin>" + declaredEnvelope.getMinX() + "</xmin><xmax>" + declaredEnvelope.getMaxX() +"</xmax><ymin>" + declaredEnvelope.getMinY() +"</ymin><ymax>" + declaredEnvelope.getMaxY() +"</ymax></Envelope>";
        }else{
        	return "";
        }        	
    }
    
   
}
