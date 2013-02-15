<?php
/**
 * @author a.mistretta
 *
 * @file Implements table relation class that contains metadata external links.
 *
 **/

class TableRelation
{
	/**
	 * Relation row insert.
	 *
	 * @param int $node_id node identifier
	 * @param string $url url 
	 * @param string $label lebel (Optional)
	 * @param int $type 0=>'other links' 1=>'Data source' (Optional)
	 */
	public function insert($node_id, $url, $label = NULL, $type=0) {
		$row = new stdClass();
		$row->id_metadata = $node_id;
		$row->url = $url;
		$row->label = $label;
		$row->data_source = $type;

		if(drupal_write_record("relation", $row)!=SAVED_NEW) {
			throw  new Exception("Error saving metadata relation: " . $url, '999');
		}
	}
	
	/**
	 * Add all url to node relation.
	 *
	 * @param int $node_id node identifier
	 * @param array $relations relation info array
	 */
	private function insertRowsArray($node_id, $relations) {
		foreach ($relations as $key => $rel) {
			$this->insert($node_id, $rel['link'], $rel['label']);
		}
	}
	
	/**
	 * Add all url to node relation.
	 *
	 * @param int $node_id node identifier
	 * @param array $url url array
	 * @param array $label lebels array
	 */
	private function insertRows($node_id, $url, $label = NULL) {
		
		if(!is_array($url))
			return; 
		
		$url_count = count($url);
		
		for($i=0; $i<$url_count; $i++)
		{
			$this->insert($node_id, $url[$i]);
		}
	}
	
	/**
	 * Delete all metadata relations of node with id=$id_node.
	 * 
	 * @param int $id_node node identifier.
	 */
	public function delete($id_node)
	{
		try {
			$res = db_delete('relation')
				->condition('id_metadata', $id_node, '=')
				->execute();
		} catch (Exception $e) {
			throw  new Exception("Error deleting metadata relation.", '999', $e);
		}
		
	}
	
	/**
	 * Get all metadata relation.
	 * 
	 * @param int $node_id id metadata node
	 * @param int $status [0,1] (Optional)
	 */
	public function getMDRelations($node_id, $data_source=null)
	{
		$query = db_select('relation', 'r')
			->fields('r')
			->condition('id_metadata', $node_id, '=');
		
		// add condition
		if (!is_null($data_source)) {
			$query->condition('data_source', $data_source, '=');
		}
		
		$result = $query->execute();
		
		return $result->fetchAllAssoc('id');
	}
	
	/**
	 * Deserialize relations.
	 * 
	 * @param unknown_type $relations
	 */
	public static function StringToArray($relations) {
		$new_relations = array();
		// relation conversion strint -> array.
		if(!is_array($relations))
			$tmp_relations = explode("##END##", $relations);
		foreach($tmp_relations as $id => $rel) {
			if(strpos($rel, ':::')!==false) {
				list($label, $url) = explode(':::', $rel);
				$new_relations[$url] = array('label' => $label, 'link' => $url );
			}
		}
		
		return $new_relations;
	}
	
	/**
	 * Update data_source relation of metadata with id=$nid. 
	 * 
	 * @param int $nid metadata node identifier
	 * @param object $rel a data source relation
	 * 
	 * @throws Exception throw Exception on insert/update/delete error.
	 */
	public function update_data_source($nid, $rel) {
		// get all relation with data_source=1
		$old_relations = $this->getMDRelations($nid, 1);
		
		if(isset($rel) && is_object($rel) && isset($rel->url) && !empty($rel->url)) {
			
			if(!isset($rel->label))
				$rel->label = $rel->url;

			if(count($old_relations)>0) {
				// update old relation
				$old_relations_key = array_keys($old_relations);
				$old_relations[$old_relations_key[0]]->url = $rel->url;
				$old_relations[$old_relations_key[0]]->label = $rel->label;
				// update
				if(drupal_write_record('relation', $old_relations[$old_relations_key[0]], 'id')!=SAVED_UPDATED)
					throw new Exception(t('Si &egrave; verificato un problema nel salvataggio del link alla sorgente dati'), 999);
			}
			else {
				// add relation
				$rel->id_metadata = $nid;
				$rel->data_source = 1;
				// insert
				if(drupal_write_record('relation', $rel)!=SAVED_NEW) 
					throw new Exception(t('Si &egrave; verificato un problema nel salvataggio del link alla sorgente dati'), 998);
			}
		}
		else if(count($old_relations)>0) {
			// delete relation
			$old_relations_key = array_keys($old_relations);
			
			// delete
			$deleted = db_delete('relation')
				->condition('id', $old_relations[$old_relations_key[0]]->id)
				->execute();
			
			if($deleted<=0) {
				throw new Exception(t('Si &egrave; verificato un problema durante l\'eliminazione del link alla sorgente dati'), 997);
			}
			
		}
	}

	/**
	 * Update relation rows: 
	 * - create if not exists;
	 * - delete row if missing in relations.
	 * - update if url already exists.
	 * 
	 * @param int $node_id
	 * @param mixed $relations
	 */
	public function update($node_id, $relations='') {
		
		// get all relation with data_source=0
		$old_relations = $this->getMDRelations($node_id, 0);
		//$new_relations = $relations;
		$changes = array('HANDLED' => array(), 'UPD' => array(), 'INS' => array(), 'DEL' => array());
		
		// relation conversion strint -> array.
		$new_relations = self::StringToArray($relations);
		
		// scorre le relazioni esistenti		
		foreach($old_relations as $id => $rel)
		{
			$changes['HANDLED'][] = $rel->url;
			
			// la vecchia relation è tra le nuove?
			if(in_array($rel->url, $new_relations))
			{
				// in previsione di gestire label faccio update. In realtà attualmente non sarebbe necessario
				$changes['UPD'][] = $new_relations[$rel->url];
				
			}
			else
			{
				// rel non è più presente nelle relazioni della scheda
				$changes['DEL'][] = $id;
			}
		}
		
		// scorre le nuove relation per verificare se ne abbiamo delle nuove da inserire
		foreach ($new_relations as $rel) {
			if(!in_array($rel, $changes['HANDLED']) && !empty($rel)) {
				$changes['INS'][] = $rel;
			}
		}
		
		$this->deleteRows($changes['DEL']);
		//$this->insertRows($node_id, $changes['INS']);
		$this->insertRowsArray($node_id, $changes['INS']);
	}
	
	/**
	 * Delete metadata relation row with id=$id_rel.
	 *
	 * @param int $id_rel relation  row identifier to delete.
	 */
	public function deleteRelation($id_rel)
	{
		try {
			$res = db_delete('relation')
			->condition('id', $id_rel, '=')
			->execute();
		}
		catch (Exception $ex) {
			throw new Exception("Error deleting relation with id=" . $id_rel, "999", $ex);
		}
		
	}
	
	/**
	 * Delete metadata relation rows.
	 *
	 * @param array $id_rows relation identifier array.
	 */
	private function deleteRows($id_rows)
	{
		if(!is_array($id_rows) || count($id_rows)==0)
			return;
		
		try {
			$res = db_delete('relation')
			->condition('id', $id_rows, 'IN')
			->execute();
		}
		catch (Exception $ex) {
			throw new Exception('Error deleting metadata relation rows:' . $filter, '999', $ex);
		}
		
	}
	
}