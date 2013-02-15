import java.io.BufferedReader;



import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.NumberFormat;
import java.util.ArrayList;


import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import java.util.Set;
import java.util.zip.GZIPInputStream;

import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.ejb.criteria.expression.function.AggregationFunction;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.io.ClassPathResource;


import it.sister.statportal.odata.domain.Column;
import it.sister.statportal.odata.domain.DbTable;
import it.sister.statportal.odata.domain.Column.ColumnType;
import it.sister.statportal.odata.domain.IRepository.MinMax;
import it.sister.statportal.odata.domain.IRepository.MinMaxCount;
import it.sister.statportal.odata.domain.MdData;
import it.sister.statportal.odata.domain.MdData.FIELD_TO_SORT;
import it.sister.statportal.odata.domain.MdData.SORTING_DIRECTION;
import it.sister.statportal.odata.domain.MdDataDim;
import it.sister.statportal.odata.domain.MdDataFiles;
import it.sister.statportal.odata.domain.MdDataFilesPK;
import it.sister.statportal.odata.domain.MdGenericColumn;
import it.sister.statportal.odata.domain.MdHierNode;
import it.sister.statportal.odata.domain.MdHierarchy;
import it.sister.statportal.odata.domain.MdLayer;
import it.sister.statportal.odata.domain.MdMeasureFields;
import it.sister.statportal.odata.domain.MdRelHierNode;
import it.sister.statportal.odata.domain.MdRelHierNodePK;
import it.sister.statportal.odata.domain.MdRelLayerNode;
import it.sister.statportal.odata.domain.MeasureAggregation;
import it.sister.statportal.odata.domain.OdataDomainException;
import it.sister.statportal.odata.domain.MeasureAggregation.AggregateFunctions;
import it.sister.statportal.odata.domain.RepositoryFactory;
import it.sister.statportal.odata.domain.Row;
import it.sister.statportal.odata.repository.PostgreSQLRepository;
import it.sister.statportal.odata.utility.DBUtils;

import java.lang.instrument.Instrumentation;

public class Test {
    
    /**
     * @param args
     * @throws Throwable 
     */
    public static void main(String[] args) throws Throwable {
	ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
		"classpath*:META-INF/spring/applicationContext.xml");
	
    }
    
    
}
