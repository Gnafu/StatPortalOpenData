package it.sister.extension.classification;

import it.sister.utils.STStyleGenerator;
import it.sister.utils.STUtils;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.rest.AbstractCatalogResource;
import org.geoserver.catalog.rest.SLDFormat;
import org.geoserver.rest.RestletException;
import org.geoserver.rest.format.DataFormat;
import org.geoserver.rest.format.StringFormat;
import org.geotools.brewer.color.ColorBrewer;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.DataUtilities;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.factory.GeoTools;
import org.geotools.filter.function.Classifier;
import org.geotools.filter.function.RangedClassifier;
import org.geotools.geometry.jts.Geometries;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.Fill;
import org.geotools.styling.PolygonSymbolizerImpl;
import org.geotools.styling.Rule;
import org.geotools.styling.Stroke;
import org.geotools.styling.Style;
import org.geotools.styling.StyleFactory;
import org.geotools.styling.Symbolizer;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.expression.Function;
import org.opengis.filter.expression.PropertyName;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;

public class ClassificationResource extends AbstractCatalogResource {

	public ClassificationResource(Context context, Request request,
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
		String stile = createStyle();
		return stile;
	}

	private String createStyle() throws Exception {
		
		// Get request parameters
		String wsName = getAttribute("workspace");
		String ftName = getAttribute("featureType");
		String fieldName = getAttribute("field");
		String numClassesStr = getAttribute("classes");
		String paletteName = getAttribute("palette");		
		String filter = getAttribute("filter");		
		
		String borderColor = getAttribute("borderColor");
		String borderWidth = getAttribute("borderWidth");
		String borderStyle = getAttribute("borderStyle");

		NamespaceInfo namespace = catalog.getNamespaceByPrefix(wsName);
		FeatureTypeInfo featureType = catalog.getFeatureTypeByName(namespace,
				ftName);
		// get datastoreInfo
		DataStoreInfo dsInfo = featureType.getStore();
		DataStore dataStore = DataStoreFinder.getDataStore(dsInfo
				.getConnectionParameters());
				
		// get collection of data
		if (STUtils.isNullOrEmpty(filter)) {
			filter = "include";
		}
		
		SimpleFeatureCollection collection;
		try {
			collection = STUtils.getFeaturesCollection(dataStore,
					featureType.getNativeName(), filter);
			if (collection.isEmpty()) {
				collection = STUtils.getFeaturesCollection(dataStore,
						featureType.getNativeName(), "include");
			}
		} catch (Exception e) {
			collection = STUtils.getFeaturesCollection(dataStore,
					featureType.getNativeName(), "include");
		}
		
		
		if (borderColor!= null && borderWidth != null && fieldName != null){
			
			Double bWidth = 0.05;		
			try{
				bWidth=Double.parseDouble(borderWidth);
			}
			catch (NumberFormatException nfe) {
				STUtils.writeDebugMessage("strokeWidth - Number format exception");
			}
			Color bColor = getColorFromParam(borderColor);
						
			return createClassificationStyle(wsName, ftName, fieldName,
					numClassesStr, paletteName, bColor, bWidth, borderStyle, dataStore, collection);
		}
		else if (fieldName == null)
		{			
			return createDefaultStyle(wsName, ftName, borderColor,
					borderWidth,  borderStyle, dataStore, collection);
		}
		else
		{		
			return createClassificationStyle(wsName, ftName, fieldName,
					numClassesStr, paletteName, dataStore, collection);
		}
	}
	

	private Color getColorFromParam(String borderColor)
	{
		Color strokeColor = Color.BLACK;
		
		try{
			String[] colorArg = borderColor.split("_");
			if (colorArg.length==3)
			{
				try{
					strokeColor=new Color(Integer.parseInt(colorArg[0]), Integer.parseInt(colorArg[1]), Integer.parseInt(colorArg[2]));
				}catch(NumberFormatException nfe){
					STUtils.writeDebugMessage("colorArg - Number format exception");
				}
			}
		}
		catch(NumberFormatException nfe){
			STUtils.writeDebugMessage("colorArg - Number format exception; _ missing");
		}
				
		return strokeColor;
			
	}
	/**
	 * @param wsName
	 * @param ftName
	 * @param borderColor
	 * @param borderWidth	 
	 * @param dataStore
	 * @param collection
	 * @return
	 */	
	private String createDefaultStyle(String wsName, String ftName,
			String borderColor, String borderWidth, String borderStyle,
			DataStore dataStore, SimpleFeatureCollection collection) throws Exception {
		
		//STUtils.writeDebugMessage("createDefaultStyle - borderColor: "+borderColor + ", borderWidth: " + borderWidth);
		StyleFactory styleFactory = CommonFactoryFinder.getStyleFactory(GeoTools.getDefaultHints());
		//STUtils.writeDebugMessage("createDefaultStyle - getStyleFactory");
		FilterFactory2 filterFactory = CommonFactoryFinder.getFilterFactory2(null);
		//STUtils.writeDebugMessage("createDefaultStyle - getFilterFactory2");
		//Imposta stile di default
		StyleInfo styleInfo = STUtils.getDefaultStyle(catalog, collection);		
		
		//STUtils.writeDebugMessage("createDefaultStyle - getDefaultStyle");
		Style _style = styleInfo.getStyle();
		//STUtils.writeDebugMessage("createDefaultStyle - getStyle");
		_style.setName(wsName + ":" + ftName);
		
		float strokeWidth = 1;	
		try{
			strokeWidth=Float.parseFloat(borderWidth);
		}
		catch (NumberFormatException nfe) {
			STUtils.writeErrorMessage("strokeWidth - Number format exception");
		}
		Color strokeColor = getColorFromParam(borderColor);
			
		Rule defRule = _style.featureTypeStyles().get(0).rules().get(0);
		//STUtils.writeDebugMessage("createDefaultStyle - get default Rule");
		
		Symbolizer oldSymb = defRule.getSymbolizers()[0];
		
		//STUtils.writeDebugMessage("createDefaultStyle - get default Symbolizer");
		
		Geometries geom = STUtils.getGeometryType(catalog, collection);
		
		//STUtils.writeDebugMessage("createDefaultStyle - getGeometryType");
		
		Symbolizer sym = getSymbolizer(styleFactory,filterFactory,strokeColor, strokeWidth, borderStyle, geom, oldSymb);
		
		//STUtils.writeDebugMessage("createDefaultStyle - getSymbolizer");
		
		defRule.symbolizers().clear();
		defRule.symbolizers().add(sym);
	
		String sldBody = createStyleInfo(_style);
		
		//STUtils.writeDebugMessage("createDefaultStyle - getSymbolizer");
		
		return sldBody;
		
	}
	
	/**
	 * 	
	 * @param sf
	 * @param ff
	 * @param borderColor
	 * @param lineWidth
	 * @param lineStyle
	 * @param geometries
	 * @param oldSymb
	 * @return
	 */
	private Symbolizer getSymbolizer(StyleFactory sf, FilterFactory ff, Color borderColor, float lineWidth, String lineStyle, Geometries geometries, Symbolizer oldSymb) {
		 		 	
	        Symbolizer symbolizer = null;
	        Fill fill = null;       
	        	        	        
	        Stroke stroke = sf.createStroke(ff.literal(borderColor), ff.literal(lineWidth), ff.literal(0));
	        //se la dimensione del bordo è >0 allora lo faccio vedere
	        if (lineWidth>0)
	        {
	        	stroke =  sf.createStroke(ff.literal(borderColor), ff.literal(lineWidth));	
	        }
	        
	        try{
	        	stroke = setStyleBorder(stroke, lineStyle, lineWidth);
	        }
	        catch (Exception e) {
				// TODO: handle exception
	        	STUtils.writeErrorMessage("SetStyleBorder error: "+e.getMessage());
			}
	        
	        switch (geometries) {
	            case POLYGON:
	            case MULTIPOLYGON:
	            	PolygonSymbolizerImpl polyOldSymb = (PolygonSymbolizerImpl)oldSymb;	            	
	                fill = polyOldSymb.getFill();
	                symbolizer = sf.createPolygonSymbolizer(stroke, fill, oldSymb.getGeometryPropertyName());
	                break;

	            case LINESTRING:
	            case MULTILINESTRING:
	                symbolizer = sf.createLineSymbolizer(stroke, oldSymb.getGeometryPropertyName());
	                break;

	            case POINT:
	            case MULTIPOINT:
	            	symbolizer = oldSymb;
	        }

	        return symbolizer;
	    }
	
	/**
	 * @param wsName
	 * @param ftName
	 * @param fieldName
	 * @param numClassesStr
	 * @param paletteName
	 * @param dataStore
	 * @param collection
	 * @return
	 */
	private String createClassificationStyle(String wsName, String ftName,
			String fieldName, String numClassesStr, String paletteName,
			DataStore dataStore, SimpleFeatureCollection collection) {
	
		return createClassificationStyle(wsName,ftName,fieldName,numClassesStr,paletteName,Color.BLACK, 0.005,"solid", dataStore,collection);
	}
	
	/**
	 * @param wsName
	 * @param ftName
	 * @param fieldName
	 * @param numClassesStr
	 * @param paletteName
	 * @param bColor
	 * @param bWidth 
	 * @param dataStore
	 * @param collection
	 * @return
	 */
	private String createClassificationStyle(String wsName, String ftName,
			String fieldName, String numClassesStr, String paletteName,Color bColor, Double bWidth, 
			String bStyle, DataStore dataStore, SimpleFeatureCollection collection) {
	
		int numClasses = 5;
		if (!STUtils.isNullOrEmpty(numClassesStr)) {
			try {
				numClasses = Integer.parseInt(numClassesStr);
			} catch (Exception e) {
				numClasses = 5;
			}
		}
		if (STUtils.isNullOrEmpty(paletteName)) {
			paletteName = "BuGn";
		}
		// generate style
		SimpleFeature[] features = STUtils.getFeaturesArray(collection);
		if (features != null) {
			Style _style = classify(features, collection.getSchema(),
					fieldName, numClasses, paletteName, bColor, bWidth, bStyle);
			_style.setName(wsName + ":" + ftName);

			String sldBody = createStyleInfo(_style);

			sldBody = roundMinMaxValues(sldBody);

			if (collection != null) {
				collection = null;
			}
			if (dataStore != null) {
				dataStore.dispose();
				dataStore = null;
			}
			return sldBody;
		} else {
			return "";
		}
	}

	/**
	 * @param sldBody
	 * @return
	 */
	private String roundMinMaxValues(String sldBody) {
		try {
			// Arrotondo il primo e l'ultimo valore a 2 decimali
			String[] literals = sldBody.split("<ogc:Literal>");
			String minValueStr = literals[1].substring(0,
					literals[1].indexOf("<"));
			String maxValueStr = literals[literals.length - 1].substring(0,
					literals[literals.length - 1].indexOf("<"));
			STUtils.writeDebugMessage("CLASSIFICATOR: Valore minimo: "
					+ minValueStr);
			STUtils.writeDebugMessage("CLASSIFICATOR: Valore massimo: "
					+ maxValueStr);
			Double minValueDbl = Double.parseDouble(minValueStr) - 0.01;
			Double maxValueDbl = Double.parseDouble(maxValueStr) + 0.01;
			NumberFormat roundingFormatter = NumberFormat
					.getNumberInstance(Locale.ENGLISH);
			roundingFormatter.setMaximumFractionDigits(2);
			roundingFormatter.setGroupingUsed(false);
			roundingFormatter.setRoundingMode(RoundingMode.FLOOR);
			String roundedMinValue = roundingFormatter.format(minValueDbl);
			STUtils.writeDebugMessage("CLASSIFICATOR: Valore minimo arrotondato: "
					+ roundedMinValue);
			roundingFormatter.setRoundingMode(RoundingMode.CEILING);
			String roundedMaxValue = roundingFormatter.format(maxValueDbl);
			STUtils.writeDebugMessage("CLASSIFICATOR: Valore massimo arrotondato: "
					+ roundedMaxValue);
			// aggiorno stile
			// sldBody = sldBody.replace("<sld:Title>" + minValueStr,
			// "<sld:Title>" + roundedMinValue);
			sldBody = sldBody.replaceFirst("<ogc:Literal>" + minValueStr
					+ "</ogc:Literal>", "<ogc:Literal>" + roundedMinValue
					+ "</ogc:Literal>");
			//sldBody = sldBody.replaceAll("<sld:Name>name","<sld:Name>di prova");
			int k = sldBody.lastIndexOf("<ogc:Literal>" + maxValueStr
					+ "</ogc:Literal>");
			String bodyRepl = sldBody.substring(k).replace(
					"<ogc:Literal>" + maxValueStr + "</ogc:Literal>",
					"<ogc:Literal>" + roundedMaxValue + "</ogc:Literal>");
			sldBody = sldBody.replace(sldBody.substring(k), bodyRepl);			
		} catch (Exception e) {
			STUtils.writeDebugMessage("Errore durante arrotondamento primo e ultimo valore: "
					+ e.getMessage());
		}
		return sldBody;
	}

	/**
	 * 
	 * @param featuresCollection
	 * @param schema
	 * @param fieldName
	 * @param numClasses
	 * @param paletteName
	 * @param bColor
	 * @param bWidth
	 * @param bStyle
	 * @return
	 */
	private Style classify(SimpleFeature[] featuresCollection,SimpleFeatureType schema, String fieldName,
            int numClasses, String paletteName, Color bColor, Double bWidth, String bStyle) {
    	Style style=null;
    	StyleFactory styleFactory = CommonFactoryFinder.getStyleFactory(GeoTools.getDefaultHints());
    	FilterFactory2 filterFactory = CommonFactoryFinder.getFilterFactory2(null);
    	STStyleGenerator myStyle = new STStyleGenerator(filterFactory, styleFactory);
    	try{
    		ColorBrewer brewer = ColorBrewer.instance();
	        PropertyName propertyExpression = filterFactory.property(fieldName);
	        
	        Function classify = filterFactory.function("Jenks", propertyExpression,
	                filterFactory.literal(numClasses));
	        
	        //use in Memory collection to avoid <idle> in transaction problem   
	        SimpleFeatureCollection memory = DataUtilities.collection( featuresCollection );        
	        Classifier groups = (Classifier) classify.evaluate(memory);
	        
	        if((groups.getSize()>1)){
	        	RangedClassifier rGroups = (RangedClassifier) groups;
	        	Double min = (Double) rGroups.getMin(0);
	        	Double max = (Double) rGroups.getMax(rGroups.getSize()-1);
		        //Caso particolare
	        	if(min.equals(max)){	        		
	        		Comparable<Double> newMin[] = new Comparable[1];
	        	    Comparable<Double> newMax[] = new Comparable[1];        	   
	        	    newMin[0] = (Double)rGroups.getMin(0);
	        	    newMax[0] = (Double)rGroups.getMax(0);    
	        	    groups = new RangedClassifier(newMin, newMax);        	
	        	    groups.setTitle(0, min.toString()); 
	        	}
	        	else
	        	{	
	        		//toglie i doppioni     	        		
	        		Comparable<Double> tmpMin[] = new Comparable[rGroups.getSize()];
	        		Comparable<Double> tmpMax[] = new Comparable[rGroups.getSize()];
	        		tmpMin[0] = ((Double)rGroups.getMin(0));
	        		tmpMax[0] = ((Double)rGroups.getMax(0));
	        		int c=1;
	        	    for (int i = 1; i < rGroups.getSize(); i++) {
	        	    	Double minValue =  (Double)rGroups.getMin(i);
	        	    	Double maxValue =  (Double)rGroups.getMax(i);
	        	    	if ( ( ( (Double)rGroups.getMin(i-1)).doubleValue()!=minValue.doubleValue()) || (((Double)rGroups.getMax(i-1)).doubleValue() != maxValue.doubleValue()))  
	        	    	{	        	    		
	        	    		tmpMin[c] = minValue;
	        	    		tmpMax[c] = maxValue;
	        	    		c++;
	        	    	}	        	    
	        	    }
	        	    	    
	        	    Comparable<Double> newMin[] = new Comparable[c];
	        	    Comparable<Double> newMax[] = new Comparable[c];
	        	    
	        	    if (tmpMax.length != c){
	        	    	for (int i = 0; i < c; i++) {	        	    	
	        	    		newMin[i] = tmpMin[i];
	        	    		newMax[i] = tmpMax[i];	 
	        	    	}	        	    
	        	    }	        	    
	        	    else{
	        	    	newMin = tmpMin;	        	    	
	        	    	newMax = tmpMax;
	        	    }
	        	    	
	        	    groups = new RangedClassifier(newMin, newMax);	     	      
	        	}
	        }
	        
	        
	        boolean invertedColors = false;

	        if (paletteName.contains("Inverted")){
	        	invertedColors = true;
	        	paletteName = paletteName.replaceAll("Inverted", "");
	        }

	        //look up a predefined palette from color brewer
	        Color[] colors = brewer.getPalette(paletteName).getColors(groups.getSize());
	
	        if (invertedColors){	
	        	List<Color> asList = Arrays.asList(colors);
	        	Collections.reverse( asList );
	        	colors = asList.toArray(colors);
	        }
	        
	        // ask StyleGenerator to make a set of rules for the Classifier
	        // assigning features the correct color based on height
	         
	        Stroke stroke = styleFactory.createStroke(filterFactory.literal(bColor),filterFactory.literal(bWidth),filterFactory.literal(0));
	       
	        if (bWidth>0){
	        	stroke = styleFactory.createStroke(
	                filterFactory.literal(bColor),
	                filterFactory.literal(bWidth)
	                );
	        }
	        	        
	        try{
	        	stroke = setStyleBorder(stroke,bStyle, bWidth.floatValue());	        	
	        }
	        catch (Exception e) {
				// TODO: handle exception
	        	STUtils.writeErrorMessage("SetStyleBorder error: "+e.getMessage());
			}
	        
	        FeatureTypeStyle fts = myStyle.createFeatureTypeStyle(styleFactory, groups, propertyExpression, colors, "Generated FeatureTypeStyle", schema.getGeometryDescriptor(), 0.95, stroke);
	  	    
	        style = styleFactory.createStyle();   
	        style.featureTypeStyles().add(fts);
	        
	        featuresCollection=null;
	        memory = null;
	        classify = null;
    	}catch(Exception e){    
    		style = styleFactory.getDefaultStyle();
    	}
        return style;
    }

	/**
	 * 
	 * @param style
	 * @return
	 */
	private String createStyleInfo(Style style) {
		ByteArrayOutputStream out = null;
		try {
			// serialize the file to the styles directory
			out = new ByteArrayOutputStream();
			SLDFormat format = new SLDFormat(true);
			format.toRepresentation(style).write(out);
			out.flush();
			out.close();
		} catch (IOException e) {
			throw new RestletException("Error creating style",
					Status.SERVER_ERROR_INTERNAL, e);
		}
		return out.toString();
	}

	/**
	 * add border style
	 * @param stroke
	 * @param lineStyle
	 * @param bWidth
	 * @return
	 */
	private Stroke setStyleBorder(Stroke stroke, String lineStyle, float bWidth){		
		try{			
			if (lineStyle.equals("dashed")){
	        	float[] dashPattern = {(float)3.0 * bWidth, (float) 4.0 * bWidth};
	        	stroke.setDashArray(dashPattern);
	        }
	        else if (lineStyle.equals("dotted")){
	        	float[] dashPattern = {(float)1.0 * bWidth, (float) 4.0* bWidth};
	        	stroke.setDashArray(dashPattern);
	        }
		}
		catch (Exception e) {
			// TODO: handle exception
			STUtils.writeErrorMessage("setStyleBorder error: " + e.getMessage());
		}
		 return stroke;
	}
}
