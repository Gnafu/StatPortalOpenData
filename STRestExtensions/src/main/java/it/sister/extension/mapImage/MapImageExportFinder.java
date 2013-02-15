package it.sister.extension.mapImage;

import it.sister.extension.mapImage.MapImageExportResource;

import it.sister.utils.STUtils;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.rest.AbstractCatalogFinder;
import org.geoserver.rest.RestletException;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Resource;


public class MapImageExportFinder extends AbstractCatalogFinder {

	 public MapImageExportFinder(Catalog catalog) {
	        super(catalog);
	    }
	 
	 @Override
	 public Resource findTarget(Request request, Response response) {
	        //Get request parameters
	        try{		    
	        }
	        catch(RestletException re){
	        	throw re;
	        }
	        catch(Exception e){	        	
	        	STUtils.writeErrorMessage(e.getMessage());
	        	throw new RestletException(e.getMessage(), Status.CLIENT_ERROR_NOT_FOUND );
	        }
	        return new MapImageExportResource( null, request, response, catalog);
	    }
}
