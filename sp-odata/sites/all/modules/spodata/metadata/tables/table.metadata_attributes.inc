<?php
/**
 * @author a.mistretta
 *
 * @file Implements table metadata_attributes class that contains metadata attributes.
 *
 **/

class TableMetadataAttributes
{
	/**
	 * metadata_attributes row insert.
	 *
	 * @param int $node_id node identifier
	 * @param bool $has_grid
	 * @param bool $has_2dmap
	 * @param bool $has_3dmap
	 * @param bool $has_chart
	 * @throws Exception
	 */
	public function insert($node_id, $has_grid, $has_2dmap, $has_3dmap, $has_chart) {
		watchdog('metadata_notice', 'Aggiungo gli attributi del metadata');
		
		$row = new stdClass();
		$row->id_metadata = (int)$node_id;
		$row->has_grid = (int)$has_grid;
		$row->has_2dmap = (int)$has_2dmap;
		$row->has_3dmap = (int)$has_3dmap;
		$row->has_chart = (int)$has_chart;

		if(drupal_write_record("metadata_attributes", $row)!=SAVED_NEW) {
			throw  new Exception("Error saving metadata_attributes relation in metadata node: " . $node_id, '999');
		}
	}
	
	/**
	 * metadata_attributes row update.
	 *
	 * @param int $node_id node identifier
	 * @param bool $has_grid
	 * @param bool $has_2dmap
	 * @param bool $has_3dmap
	 * @param bool $has_chart
	 * @throws Exception
	 */
	public function update($node_id, $has_grid, $has_2dmap, $has_3dmap, $has_chart) {
		// load row
		$query = db_select('metadata_attributes', 'A')
			->fields('A')
			->condition('A.id_metadata', $node_id, '=')
			->execute();
		$row = $query->fetchObject();
		
		// set new metadata attributes
		$row->has_grid = (int)$has_grid;
		$row->has_2dmap = (int)$has_2dmap;
		$row->has_3dmap = (int)$has_3dmap;
		$row->has_chart = (int)$has_chart;
		
		if(drupal_write_record("metadata_attributes", $row, 'id')!=SAVED_UPDATED) {
			throw  new Exception("Error updating metadata_attributes relation in metadata node: " . $node_id, '999');
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
			$res = db_delete('metadata_attributes')
			->condition('id_metadata', $id_node, '=')
			->execute();
		} catch (Exception $e) {
			throw  new Exception("Error deleting metadata attributes.", '999', $e);
		}
	
	}
	
/**
	 * Get all metadata relation.
	 * 
	 * @param int $node_id id metadata node
	 */
	public function getMDAttributes($node_id)
	{
		$row = db_select('metadata_attributes', 'r')
			->fields('r')
			->condition('id_metadata', $node_id, '=')
			->execute();
		
		$rowset = $row->fetchAllAssoc('id');
		
		if(count($rowset)<=0) {
			// supporto scheda senza dati
			$rowset[0] = new stdClass();
			$rowset[0]->id = 0;
			$rowset[0]->id_metadata = $node_id;
			$rowset[0]->has_grid = 0;
			$rowset[0]->has_2dmap = 0;
			$rowset[0]->has_3dmap = 0;
			$rowset[0]->has_chart = 0; 
		}
		
		return $rowset;
	}
	
}