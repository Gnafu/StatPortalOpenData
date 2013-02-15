<?php
class EtlDimensionConfiguration
{
	const tableName = 'etl_dimension_config';
	public $inferedDimension;
	public $idHierNode;
	public $name;
	
	/**
	 * Ctor 
	 * @param string $infered_dim
	 * @param int $id_hier_node
	 * @param string $name
	 */
	function __construct() {
		if(!isset($this->idHierNode) && isset($this->id_hier_node)) {
			$this->idHierNode = $this->id_hier_node;
			unset($this->id_hier_node);
		}
		if(!isset($this->inferedDimension) && isset($this->infered_dimension)) {
			$this->inferedDimension = $this->infered_dimension;
			unset($this->infered_dimension);
		}
	}
	
	/**
	 * Load ETL dimension configuration by database.
	 * @return array of etl_dimension_config row.
	 */
	public static function loadConfig() {
		$result = array();
		
		try {
			$rows = db_select(self::tableName, 'c')
				->fields('c', array('infered_dimension', 'id_hier_node', 'name'))
				->execute();
			
			while ($row = $rows->fetchObject('EtlDimensionConfiguration')) {
				
				$result[] = $row;
			}
		} catch (Exception $e) {
			watchdog_exception('metadata', $e, 'Configurazione non trovata');
		}
		
		return $result;
	}
}