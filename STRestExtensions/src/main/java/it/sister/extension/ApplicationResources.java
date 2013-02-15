// <editor-fold defaultstate="collapsed" desc="Description">

/*
 * ApplicationProperties
 *
 *
 *
 */

// </editor-fold>
package it.sister.extension;

// <editor-fold defaultstate="collapsed" desc="import statements">
import java.text.MessageFormat;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

// </editor-fold>

/**
 *
 * 
 */
public class ApplicationResources {

	// <editor-fold defaultstate="collapsed" desc="Fields">

	// </editor-fold>

	// <editor-fold defaultstate="collapsed" desc="Costructor">
	// </editor-fold>

	// <editor-fold defaultstate="collapsed" desc="Public Methods">

	/**
	 * 
	 * @param propertyName
	 * @param param
	 * @return Valore di una proprietà 
	 */
	public static String getApplicationProperty(String propertyName,
			String param) {

		ResourceBundle appResources = PropertyResourceBundle.getBundle("STRestExtensionsResources");

		if (param == null || param.equals("")) {
			return getPropertyValue(appResources, propertyName, null);
		} else {
			String[] p = new String[1];
			p[0] = param;

			return getPropertyValue(appResources, propertyName, p);
		}

	}

	/**
	 * 
	 * @param propertyName
	 * @param params
	 * @return Valore di una proprietà 
	 */
	public static String getApplicationProperty(String propertyName,
			String[] params) {

		ResourceBundle appResources = PropertyResourceBundle.getBundle("it.sister.extension.STRestExtensionsResources");

		return getPropertyValue(appResources, propertyName, params);

	}

	// </editor-fold>

	// <editor-fold defaultstate="collapsed" desc="Private Methods">
	private static String getPropertyValue(ResourceBundle resource,
			String propertyName, String[] params) {

		if (params == null) {
			return resource.getString(propertyName);
		} else {
			String template = resource.getString(propertyName);
			return MessageFormat.format(template, (Object[]) params);

		}

	}
	// </editor-fold>
}
