package it.sister.utils;

import java.awt.Color;

import org.geotools.brewer.color.StyleGenerator;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.filter.IllegalFilterException;
import org.geotools.filter.function.Classifier;
import org.geotools.filter.function.RangedClassifier;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.Fill;
import org.geotools.styling.Graphic;
import org.geotools.styling.Mark;
import org.geotools.styling.Rule;
import org.geotools.styling.Stroke;
import org.geotools.styling.StyleBuilder;
import org.geotools.styling.StyleFactory;
import org.geotools.styling.Symbolizer;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.expression.Expression;

import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

public class STStyleGenerator {

	public FilterFactory2 filterFactory;
	public StyleFactory styleFactory;
	public StyleBuilder styleBuilder;
	
	public STStyleGenerator() {
		filterFactory = null;	    
		styleFactory = null;
		styleBuilder =  null;
	}
	
	public STStyleGenerator(FilterFactory2 ff,StyleFactory sf) {
		filterFactory = ff;
		styleFactory = sf;
		styleBuilder = new StyleBuilder(styleFactory, filterFactory);
	}
	
	
	public FeatureTypeStyle createFeatureTypeStyle(StyleFactory sf,Classifier classifier,Expression expression, Color[] colors, String typeId,
		        GeometryDescriptor geometryAttrType, double opacity, Stroke defaultStroke){
		   FeatureTypeStyle fts = sf.createFeatureTypeStyle();	
		   RangedClassifier ranged = (RangedClassifier) classifier;

	       Object localMin = null;
	       Object localMax = null;
	      
	       for (int i = 0; i < ranged.getSize(); i++) {
	           // obtain min/max values
	           localMin = ranged.getMin(i);
	           localMax = ranged.getMax(i);

	           Rule rule = createRuleRanged(ranged, expression, localMin, localMax,geometryAttrType, i, colors, opacity, defaultStroke);
	           fts.rules().add(rule);
	       }
	       return fts;
	   }
	   
	   private Rule createRuleRanged(RangedClassifier classifier, Expression expression, Object localMin, Object localMax, GeometryDescriptor geometryAttrType, int i,
		       Color[] colors, double opacity, Stroke defaultStroke)
		       throws IllegalFilterException {
		        // 1.0 --> 1
		        // (this makes our styleExpressions more readable. Note that the
		        // filter always converts to double, so it doesn't care what we
		        // do).
		   localMin = chopInteger(localMin);
		   localMax = chopInteger(localMax);

		   // generate a title
		   String title = classifier.getTitle(i);

		   // construct filters
		   Filter filter = null;

		
		       Filter lowBoundFilter = null;
		       Filter hiBoundFilter = null;
		            
		       if(localMin != null) {
		    	   if (i == 0) {
		    		   lowBoundFilter = filterFactory.greaterOrEqual(expression, filterFactory.literal(localMin));
		    	   } else {
		    		   lowBoundFilter = filterFactory.greater(expression, filterFactory.literal(localMin));
		    	   }
		    	}
	            if(localMax != null) {	            	            
	            	hiBoundFilter = filterFactory.lessOrEqual(expression, filterFactory.literal(localMax));	                
	            }

	            if ((localMin != null) && (localMax != null)) {
	                filter = filterFactory.and(lowBoundFilter, hiBoundFilter);
	            } else if ((localMin == null) && (localMax != null)) {
	                filter = hiBoundFilter;
	            } else if ((localMin != null) && (localMax == null)) {
	                filter = lowBoundFilter;
	            }	      

	        // create a symbolizer
	        Symbolizer symb = createSymbolizer(geometryAttrType, colors[i], opacity, defaultStroke);

	        // create a rule
	        Rule rule = styleBuilder.createRule(symb);
	        rule.setFilter(filter);
	        rule.setTitle(title);
	        rule.setName(getRuleName(i + 1));

	        return rule;
	   }
	   
	   /**
	     * Creates a symbolizer for the given geometry
	     *
	     * @param sb
	     * @param geometryAttrType
	     * @param color
	     * @param opacity
	     * @param defaultStroke stroke used for borders
	     *
	     */
	    private org.geotools.styling.Symbolizer createSymbolizer(GeometryDescriptor geometryAttrType, Color color,
	        double opacity, Stroke defaultStroke) {
	    	org.geotools.styling.Symbolizer symb;

	        if (defaultStroke == null) {
	            defaultStroke = styleBuilder.createStroke(color, 1, opacity);
	        }

	        if ((geometryAttrType.getType().getBinding() == MultiPolygon.class)
	                || (geometryAttrType.getType().getBinding() == Polygon.class)) {
	            Fill fill = styleBuilder.createFill(color, opacity);
	            symb = styleBuilder.createPolygonSymbolizer(defaultStroke, fill);
	        } else if (geometryAttrType.getType().getBinding() == LineString.class) {
	            symb = styleBuilder.createLineSymbolizer(color);
	        } else if ((geometryAttrType.getType().getBinding() == MultiPoint.class)
	                || (geometryAttrType.getType().getBinding() == Point.class)) {
	            Fill fill = styleBuilder.createFill(color, opacity);
	            Mark square = styleBuilder.createMark(StyleBuilder.MARK_SQUARE, fill, defaultStroke);
	            Graphic graphic = styleBuilder.createGraphic(null, square, null); //, 1, 4, 0);
	            symb = styleBuilder.createPointSymbolizer(graphic);
	           
	        } else {
	            //we don't know what the heck you are, *snip snip* you're a line.
	            symb = styleBuilder.createLineSymbolizer(color);
	        }

	        return symb;
	    }
	   
	   /**
	    * Truncates an unneeded trailing decimal zero (1.0 --> 1) by converting to
	    * an Integer object.
	    *
	    * @param value
	    *
	    * @return Integer(value) if applicable
	    */
	   private static Object chopInteger(Object value) {
	       if ((value instanceof Number) && (value.toString().endsWith(".0"))) {
	           return new Integer(((Number) value).intValue());
	       } else {
	           return value;
	       }
	   }
	   
	   /**
	    * Generates a quick name for each rule with a leading zero.
	    *
	    * @param count
	    *
	    */
	   private static String getRuleName(int count) {
	       String strVal = new Integer(count).toString();

	       if (strVal.length() == 1) {
	           return "rule0" + strVal;
	       } else {
	           return "rule" + strVal;
	       }
	   }
	   	  
}
