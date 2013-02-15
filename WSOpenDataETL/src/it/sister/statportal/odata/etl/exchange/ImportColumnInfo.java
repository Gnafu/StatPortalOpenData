package it.sister.statportal.odata.etl.exchange;

/**
 * Superclasse per le classi DimensionInfo, MeasureInfo, GenericColumnInfo
 *
 */
public abstract class ImportColumnInfo implements IImportColumnInfo{
	
	//posizione della colonna
	protected int pos;
	
	//usato solo negli excel
	protected int columnIndex;
	
	/**
	 * Restituisce il tipo della colonna
	 * @return
	 */
	public abstract ColumnType getColumnType();
	
	@Override
	public int getPos(){
		return this.pos;
	}
	
	@Override
	public void setPos(int value){
		this.pos = value;
	}
	
	@Override
	public abstract String getColumnName();

	@Override
	public abstract void setColumnName(String value);
	
	@Override
	public int getColumnIndex(){
		return this.columnIndex;
	}
	
	@Override
	public void setColumnIndex(int value){
		this.columnIndex = value;
	}
}
