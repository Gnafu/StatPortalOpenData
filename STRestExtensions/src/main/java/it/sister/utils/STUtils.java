/**
 * 
 */
package it.sister.utils;

import java.util.logging.Logger;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geotools.data.DataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.filter.text.cql2.CQL;
import org.geotools.geometry.jts.Geometries;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.filter.Filter;

/**
 * 
 */
public class STUtils {

	private static Logger LOGGER = Logging
			.getLogger("org.geoserver.catalog.rest");

	/**
	 * 
	 * @param catalog
	 * @param workspaceName
	 * @return true if workspace exists, false otherwise
	 */
	public static boolean workspaceExists(Catalog catalog, String workspaceName)
			throws Exception {
		if (catalog.getWorkspaceByName(workspaceName) != null) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Indicate if a datastore exists in the default workspace
	 * 
	 * @param catalog
	 * @param dataStoreName
	 * @return
	 */
	public static boolean dataStoreExists(Catalog catalog, String dataStoreName)
			throws Exception {
		return dataStoreExists(catalog,
				catalog.getDefaultWorkspace().getName(), dataStoreName);
	}

	/**
	 * Indicate if a datastore exists in the specified workspace
	 * 
	 * @param catalog
	 * @param workspaceName
	 * @param dataStoreName
	 * @return
	 */
	public static boolean dataStoreExists(Catalog catalog,
			String workspaceName, String dataStoreName) throws Exception {
		if (catalog.getDataStoreByName(workspaceName, dataStoreName) != null) {
			return true;
		} else {
			return false;
		}
	}

	public static boolean featureTypeExists(Catalog catalog,
			String workspaceName, String featureTypeName) throws Exception {
		NamespaceInfo namespace = catalog.getNamespaceByPrefix(workspaceName);
		if (namespace != null
				&& catalog.getFeatureTypeByName(namespace, featureTypeName) != null) {
			return true;
		} else {
			return false;
		}
	}

	public static boolean styleExists(Catalog catalog, String styleName)
			throws Exception {
		StyleInfo sInfo = catalog.getStyleByName(styleName);
		if (sInfo != null) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * 
	 * @param value
	 *            , string to verify
	 * @return true if string is null or empty, false otherwise
	 */
	public static boolean isNullOrEmpty(String value) {
		if (value == null || value == "")
			return true;
		return false;
	}

	/**
	 * 
	 * @param dataStore
	 *            , the datastore
	 * @param featureTypeName
	 *            , the name of the table in the datastore
	 * @param filterTxt
	 *            , the filter string to apply to collection of data
	 * @return collection of filtered features
	 * @throws Exception
	 */
	public static SimpleFeatureCollection getFeaturesCollection(
			DataStore dataStore, String featureTypeName, String filterTxt)
			throws Exception {
		SimpleFeatureSource source = dataStore
				.getFeatureSource(featureTypeName);
		Filter filter = CQL.toFilter(filterTxt);
		SimpleFeatureCollection features = source.getFeatures(filter);
		return features;
	}

	/**
	 * Get features collection as array
	 * 
	 * @param features
	 * @return Array of feature
	 */
	public static SimpleFeature[] getFeaturesArray(
			SimpleFeatureCollection features) {
		if (features != null) {
			SimpleFeature[] featuresArray = null;
			SimpleFeatureIterator iterator = features.features();
			try {
				featuresArray = new SimpleFeature[features.size()];
				int count = 0;
				while (iterator.hasNext()) {
					SimpleFeature feature = iterator.next();
					featuresArray[count] = feature;
					count++;
				}
				return featuresArray;
			} catch (Exception e) {
				STUtils.writeDebugMessage(e.getMessage());
				return null;
			} finally {
				iterator.close();
			}
		}
		return null;
	}

	/**
	 * Log an info message
	 * 
	 * @param message
	 */
	public static void writeDebugMessage(String message) {
		LOGGER.info(message);
	}

	/**
	 * Log an error message
	 * 
	 * @param message
	 */
	public static void writeErrorMessage(String message) {
		LOGGER.severe(message);
	}

	/**
	 * @param featureCollection
	 * @return
	 */
	public static StyleInfo getDefaultStyle(Catalog catalog,
			SimpleFeatureCollection featureCollection) {
		
		Geometries geom = getGeometryType(catalog,featureCollection);
		
		if (geom != null) {
			switch (geom) {
			case POINT:
				return catalog.getStyleByName(StyleInfo.DEFAULT_POINT);
			case MULTIPOINT:
				return catalog.getStyleByName(StyleInfo.DEFAULT_POINT);
			case LINESTRING:
				return catalog.getStyleByName(StyleInfo.DEFAULT_LINE);
			case MULTILINESTRING:
				return catalog.getStyleByName(StyleInfo.DEFAULT_LINE);
			case POLYGON:
				return catalog.getStyleByName(StyleInfo.DEFAULT_POLYGON);
			case MULTIPOLYGON:
				return catalog.getStyleByName(StyleInfo.DEFAULT_POLYGON);
			}
		}
		return null;
	}
	
	public static Geometries getGeometryType(Catalog catalog,
			SimpleFeatureCollection featureCollection) {
		// check geometry type
		SimpleFeatureIterator iterator = featureCollection.features();
		com.vividsolutions.jts.geom.Geometry geometry = null;
		try {
			while (iterator.hasNext()) {
				SimpleFeature feature = iterator.next();
				geometry = (com.vividsolutions.jts.geom.Geometry) feature
						.getDefaultGeometry();
				break;
			}
		}
		catch (Exception e) {
			STUtils.writeErrorMessage(e.getMessage());
		}
		finally {
		}

		if (geometry != null) {
			return Geometries.get(geometry);			
		}
		return null;
	}
}
