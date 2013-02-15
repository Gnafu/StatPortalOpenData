package it.sister.statportal.odata;

/**
 * Enumerato con i nomi delle tabelle corrispondenti ai tipi di entità che si possono generare
 *
 */
public enum Entities {
	MD_DATA,
	MD_DATA_DIM,
	MD_HIER_NODE,
	MD_MEASURE_FIELDS,
	MD_HIERARCHY,
	MD_LU_HIER_TYPE,
	DB_TABLE,
	UNKNOWN;
	
	/**
	 * Mappa il parametro in un elemento dell'enumerato.
	 * Ci serve per mappare i nomi degli entitySet in un tipo dell'enumerato.
	 * @param entitySetName il nome dell'entityset
	 * @return il valore dell'enumerato corrispondente o UNKNOWN se non riesce a mapparlo
	 */
	public static Entities parse(final String entitySetName)
	{
		Entities entity;
		if(entitySetName.compareTo("MdData") == 0){
			entity = Entities.MD_DATA;
		} else if(entitySetName.compareTo("MdDataDim") == 0){
			entity = Entities.MD_DATA_DIM;
		} else if(entitySetName.compareTo("MdHierNode") == 0){
			entity = Entities.MD_HIER_NODE;
		} else if(entitySetName.compareTo("MdMeasureFields") == 0){
			entity = Entities.MD_MEASURE_FIELDS;
		} else if(entitySetName.compareTo("MdHierarchy") == 0){
			entity = Entities.MD_HIERARCHY;
		} else if(entitySetName.compareTo("MdLuHierType") == 0){
			entity = Entities.MD_LU_HIER_TYPE;
		} else if(entitySetName.compareTo("DbTable") == 0){
			entity = Entities.DB_TABLE;
		} else{
			entity = Entities.UNKNOWN;
		}
		return entity;
	}
}
