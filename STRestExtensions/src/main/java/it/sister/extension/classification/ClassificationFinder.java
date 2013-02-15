package it.sister.extension.classification;

import it.sister.utils.STUtils;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.rest.AbstractCatalogFinder;
import org.geoserver.rest.RestletException;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Resource;

public class ClassificationFinder extends AbstractCatalogFinder {

    public ClassificationFinder(Catalog catalog) {
        super(catalog);
    }

    @Override
    public Resource findTarget(Request request, Response response) {
        //Get request parameters
    	STUtils.writeDebugMessage("findTarget");
        String wsName = getAttribute(request, "workspace");
        String ftName = getAttribute(request, "featureType");
       
        try{
        	//check workspace
	        if(!STUtils.workspaceExists(catalog, wsName)){
	            throw new RestletException( "No such workspace: " + wsName, Status.CLIENT_ERROR_NOT_FOUND );
	        }
	        //check layer
	        if(ftName!= null){
	        	if(!STUtils.featureTypeExists(catalog, wsName, ftName)){
	        		throw new RestletException( "No such feature type: "+wsName+" , "+ftName, Status.CLIENT_ERROR_NOT_FOUND );
	        	}
	        }
        }
        catch(RestletException re){
        	throw re;
        }
        catch(Exception e){
        	String msg="Error verifying workspace '" + wsName + "' or feature type '" + ftName + "'";
        	STUtils.writeErrorMessage(msg + " " + e.getMessage());
        	throw new RestletException(msg, Status.CLIENT_ERROR_NOT_FOUND );
        }
        return new ClassificationResource( null, request, response, catalog);        
    }
}
