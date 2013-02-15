package it.sister.statportal.odata.domain;

import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.roo.addon.dbre.RooDbManaged;
import org.springframework.roo.addon.entity.RooEntity;
import org.springframework.roo.addon.javabean.RooJavaBean;
import org.springframework.roo.addon.tostring.RooToString;

@RooJavaBean
@RooToString
@RooEntity(versionField = "", table = "DB_TABLE", schema = "public")
@RooDbManaged(automaticallyDelete = true)
public class DbTable implements IData {

    public DbTable() {

    }

    public DbTable(String dbName, String description, Date lastUpdate,
	    String name, Integer numRows, String tableName) {
	setDbName(dbName);
	setDescription(description);
	setLastUpdate(lastUpdate);
	setName(name);
	setNumRows(numRows);
	setTableName(tableName);
    }

    @Override
    public List<Column> getColumns() throws OdataDomainException {
	CheckTableNameNotNull();
	return RepositoryFactory.getRepository().getTableColumns(
		this.getTableName());
    }

    @Override
    public List<Row> getRows() throws OdataDomainException {
	CheckTableNameNotNull();
	return RepositoryFactory.getRepository().getTableRows(
		this.getTableName(), getColumns());

    }

    @Override
    public List<Row> getRows(int startIndex, int count, String filter)
	    throws OdataDomainException {
	CheckTableNameNotNull();
	return RepositoryFactory.getRepository().getTableRows(
		this.getTableName(), getColumns(), startIndex, count, filter);
    }

    @Override
    public int getCountRows() throws OdataDomainException {
	return getCountRows(null);
    }

    /**
     * Funzione di utilità che controlla che il dato abbia impostato il nome
     * della tabella. Se non impostato lancia eccezione
     * 
     * @throws OdataDomainException
     */
    private void CheckTableNameNotNull() throws OdataDomainException {
	if (this.getTableName() == null) {
	    throw new OdataDomainException(
		    "Nome della tabella del dato non definita");
	}
    }

    @Override
    public int getCountRows(String filter) throws OdataDomainException {
	CheckTableNameNotNull();
	return RepositoryFactory.getRepository().getCountRows(this.getTableName(), null, null, -1, -1, false, filter, null, null, false);
    }

    @Override
    public String getDescriptionField(String physicalName) {
	return physicalName;
    }



}
