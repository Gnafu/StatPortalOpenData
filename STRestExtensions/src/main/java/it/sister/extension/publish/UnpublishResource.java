package it.sister.extension.publish;

import it.sister.utils.STUtils;

import java.util.ArrayList;
import java.util.List;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.rest.AbstractCatalogResource;
import org.geoserver.rest.format.DataFormat;
import org.geoserver.rest.format.StringFormat;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;

public class UnpublishResource extends AbstractCatalogResource {

	public UnpublishResource(Context context, Request request, Response response,
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
		String layer = unpublishLayer();
		STUtils.writeDebugMessage("Rimosso Layer: " + layer);
		return layer;
	}

	private String unpublishLayer() throws Exception {
		// Get request parameters
		String wsName = getAttribute("workspace");		
		String layerName = getAttribute("layerName");
			
		LayerInfo layer = catalog.getLayerByName(layerName);
		if(layer==null){
			NamespaceInfo ns = catalog.getNamespaceByPrefix(wsName);
			ResourceInfo resource = catalog.getFeatureTypeByName(ns, layerName);
			if(resource!=null){
				catalog.remove(resource);
				
			}else{
				return "";
			}
		}else{
			catalog.remove(layer);
		}
		return "<Layer><WorkspaceName>" + wsName + "</WorkspaceName><LayerName>" + layerName+ "</LayerName></Layer>";
	}

}
