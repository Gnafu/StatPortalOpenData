<?php 
include_once 'table.relation.inc';
include_once 'table.metadata_attributes.inc';

/**
 * @author a.mistretta
 *
 * @file Implements table metadata class.
 * 
 **/

class TableMetadata
{
	// current row
	private $metadata_row;
	
	// metadata visibility
	const VISIBLE = 1;
	const HIDDEN = 0;
	// metadata status
	const NOT_APPROVED = 0;
	const APPROVED = 1;
	const REJECTED = 2;
	
	public function bind($id) {
		$row = db_select('metadata', 'm')
		->fields('m')
		->condition('nid', $id, '=')
		->execute();
		
		$this->metadata_row = (object)$row->fetchAssoc();
		
		return $this->metadata_row;
	}
	
	/**
	 * Insert new row in metadata.
	 * @param object $node
	 */
	public function insert($node) {
		$this->metadata_row = new stdClass();
		$this->metadata_row->nid = $node->nid;
		$this->metadata_row->vid = $node->vid;
		$this->metadata_row->title = $node->title;
		$this->metadata_row->identifier = $node->identifier;
		$this->metadata_row->description = isset($node->body['und'][0]['value']) ? $node->body['und'][0]['value'] : '';
		
		if(trim($node->metadata_author)=='new') {
			$this->metadata_row->id_author = TableAuthor::create_if_not_exists($node->author_name, $node->uid, $node);
		} 
		else { 
			$this->metadata_row->id_author=$node->metadata_author;
		}
		$this->metadata_row->id_publisher = $node->uid;
		$this->metadata_row->creation_date = $node->created;
		$this->metadata_row->last_update = $node->created;
		$this->metadata_row->note = $node->note;
		$this->metadata_row->status = self::NOT_APPROVED;
		$this->metadata_row->visibility = self::HIDDEN;

		// insert new row
		if(drupal_write_record('metadata', $this->metadata_row)!=SAVED_NEW)
			throw  new Exception("Error saving metadata", '999');
		
		$tbl_relation = new TableRelation();
		
		if(isset($node->ds_link) && !empty($node->ds_link)) {
			if(!isset($node->ds_label))
				$node->ds_label = 'Download del dato originale';
		
			$tbl_relation->insert($node->nid, $node->ds_link, $node->ds_label, 1);
		}
		
		// add metadata datasource link. 
		if(!is_array($node->relation)) {
			$node->relation = TableRelation::StringToArray($node->relation);
		} 
		// add metadata relation.
		foreach ($node->relation as $link) {
			$tbl_relation->insert($node->nid, check_url($link['link']),  $link['label']);
		}
		
		$tbl_attach = new TableAttach();
		
		// add data/application attachment to metadata
		if($node->basetype==MetadataValidator::BASETYPE_DATA) {
			$_type = $node->md_is_structured ? TableAttach::TYPE_DATA_MD : TableAttach::TYPE_DATA_RAW;
			// has data attachment
			if(isset($node->fid) && !empty($node->fid) && $node->fid>0)
				$tbl_attach->addAttachToMetadata($node, $_type, $node->uid, $node->odata_url, NULL, $node->extra_note);
// 			else 
// 				watchdog('metadata', 'Una scheda dati senza allegato principale è stata caricata.');
		}
		else if($node->basetype==MetadataValidator::BASETYPE_APPLICATION) {
			// @todo gestire scheda formato applicazione
			form_set_error('metadata_node_form', t('Scheda con allegato applicazione non gestito'));
		}
		
		// add extra attachment
		if(isset($node->extra_attachments)) {
			// fetch attachments
			for($idx=0;$idx<count($node->extra_attachments);$idx++) {
				$uploaded_file = $node->extra_attachments[$idx];
				// if file uploaded on server tmp directory
				if($uploaded_file['status']=='done') {
					// move file by temp folder on report folder
					$_source = $uploaded_file['tmppath'];
					$_destination = file_stream_wrapper_uri_normalize('public://metadata_updfile/report/' . $node->nid . '_' . $uploaded_file['name']);
					
					$_destination = file_unmanaged_move($_source, $_destination, FILE_EXISTS_RENAME);
					$_file = false;
					if($_destination) {
						$_file = plupload_file_uri_to_object($_destination);
						$_file = file_save($_file);
					}
					
					if($_file) {
// 						$obj_file = file_save($obj_file);
						$tbl_attach->addAttachToMetadata($node, TableAttach::TYPE_REPORT, $node->uid, $_destination, $_file->fid);
					}
					else {
						form_set_error('metadata_node_form', t('Errore durante la copia del file ' . $uploaded_file['name']));
					}
				}
				else {
					form_set_error('metadata_node_form', t('Errore durante il trasferimento del file ' . $node->extra_attachments[$idx]));
				}
			}
		}
		
		// add attributes
		$tbl_md_attributes = new TableMetadataAttributes();
		$tbl_md_attributes->insert($node->nid, (bool)$node->viewer['hasGrid'], (bool)$node->viewer['has2DMap'], (bool)$node->viewer['has3DMap'], (bool)$node->viewer['hasChart']);
		
	}
	
	/**
	 * Add new author.
	 * 
	 * @param string $author_name author name.
	 * @param int $uid user id. 
	 */
	private function addNewAuthor($author_name, $uid) {
		
// 		Table
		
		
	}
	
	/**
	 * Delete a metadata row
	 * 
	 * @param int $node_id identifier of row to delete.
	 */
	public function  delete($node_id)
	{
		// delete metadata relation
		$tbl_relation = new TableRelation();
		$tbl_relation->delete($node_id);
		
		// delete metadata
		$res = db_delete('metadata')
			->condition('nid', $node_id, '=')
			->execute();
	}
	
	/**
	 * Update metadata row.
	 * 
	 * @param object $node
	 */
	public function update($node) {

		$this->metadata_row = new stdClass();
		$this->metadata_row->nid = $node->nid;
		$this->metadata_row->vid = $node->vid;
		$this->metadata_row->title = $node->title;
		//$this->metadata_row->identifier = 'TODO'; questo campo non cambia
		$this->metadata_row->description = isset($node->body['und'][0]['value']) ? $node->body['und'][0]['value'] : '';
		global $user;
		
		if(trim($node->metadata_author)=='new') {
			$this->metadata_row->id_author = TableAuthor::create_if_not_exists($node->author_name, $user->uid, $node);
		}
		else {
			$this->metadata_row->id_author = $node->metadata_author;
		}
		
		$this->metadata_row->id_publisher = $node->uid;
		$this->metadata_row->creation_date = $node->created;
		$this->metadata_row->last_update = $node->changed;
		$this->metadata_row->note = $node->note;
		
		if(drupal_write_record('metadata', $this->metadata_row, array('nid') )!=SAVED_UPDATED)
			throw  new Exception("Error update metadata row", '999');

		$tbl_relation = new TableRelation();
		$tbl_relation->update($node->nid, $node->relation);
		
		if(isset($node->ds_link)) {
			$ds_link = new stdClass();
			$ds_link->url = $node->ds_link;
			$ds_link->label = isset($node->ds_label) ? $node->ds_label : $node->ds_link; 
			
			$tbl_relation->update_data_source($node->nid, $ds_link);
		}
		
		$tbl_attach = new TableAttach();
		// add data/application attachment to metadata
		if($node->basetype==MetadataValidator::BASETYPE_DATA) {
			$_type = $node->md_is_structured ? TableAttach::TYPE_DATA_MD : TableAttach::TYPE_DATA_RAW;
			
			// devo cancellare il precedente allegato?
			if( isset($node->old_odata_url) && isset($node->old_odata_attach_id) &&
				!empty($node->old_odata_url) && !empty($node->old_odata_attach_id) &&
				isset($node->odata_url) && $node->old_odata_url!=$node->odata_url ) {
				$tbl_attach->deleteAttach($node->old_odata_attach_id, $node->fid);
				
			}
			
			if(!isset($node->old_odata_url))
				$node->old_odata_url = 'missed';
			
			// has data attachment
			if(isset($node->fid) && !empty($node->fid) && $node->fid>0 &&
			  isset($node->odata_url) && $node->old_odata_url!=$node->odata_url )
				$tbl_attach->addAttachToMetadata($node, $_type, $node->uid, $node->odata_url, NULL, $node->extra_note);
			else 
				$tbl_attach->updateAttachDataDescription($node->odata_url, $_type, $node->extra_note);
				
			// add extra attachment
			if(isset($node->extra_attachments)) {
				// fetch attachments
				for($idx=0;$idx<count($node->extra_attachments);$idx++) {
					$uploaded_file = $node->extra_attachments[$idx];
					// if file uploaded on server tmp directory
					if($uploaded_file['status']=='done') {
						// move file by temp folder on report folder
						$_source = $uploaded_file['tmppath'];
						$_destination = file_stream_wrapper_uri_normalize('public://metadata_updfile/report/' . $node->nid . '_' . $uploaded_file['name']);
			
						$_destination = file_unmanaged_move($_source, $_destination, FILE_EXISTS_RENAME);
						$_file = false;
						if($_destination) {
							$_file = plupload_file_uri_to_object($_destination);
							$_file = file_save($_file);
						}
			
						if($_file) {
							// 						$obj_file = file_save($obj_file);
							$tbl_attach->addAttachToMetadata($node, TableAttach::TYPE_REPORT, $node->uid, $_destination, $_file->fid);
						}
						else {
							form_set_error('metadata_node_form', t('Errore durante la copia del file ' . $uploaded_file['name']));
						}
					}
					else {
						form_set_error('metadata_node_form', t('Errore durante il trasferimento del file ' . $node->extra_attachments[$idx]));
					}
				}
			}
			// remove deleted attachment
			if(isset($node->deleted_attached_files) && !empty($node->deleted_attached_files)) {
				$to_delete = split(';', $node->deleted_attached_files);
				foreach ($to_delete as $id_att) {
					if(!empty($id_att)) {
						if(!$tbl_attach->deleteAttach($id_att)) {
							watchdog('metadata', 'Errore durante l\'eliminazione dell\'allegato (' . $id_att . ')!', NULL, WATCHDOG_ERROR );
						}
					}
				}
			}
			
			// add attributes
			$tbl_md_attributes = new TableMetadataAttributes();
			$tbl_md_attributes->update($node->nid, (bool)$node->viewer['hasGrid'], (bool)$node->viewer['has2DMap'], (bool)$node->viewer['has3DMap'], (bool)$node->viewer['hasChart']);
			
		}
		else if($node->basetype==MetadataValidator::BASETYPE_APPLICATION) {
			// @todo gestire scheda formato applicazione
			form_set_error('metadata_node_form', t('Scheda con allegato applicazione non gestito'));
		}
	}
	
	/**
	 * Update metadata visibility field to Visible.
	 * 
	 * @param sting $nids nodes id (nid;nid;nid)
	 */
	public static function setVisibilityToVisible($nids) {
		return self::setVisibility($nids, self::VISIBLE);
	}
	
	/**
	 * Update metadata visibility field to Hidden.
	 * 
	 * @param sting $nids nodes id (nid;nid;nid)
	 */
	public static function setVisibilityToHidden($nids) {
		return self::setVisibility($nids, self::HIDDEN);
	}
	
	/**
	 * Update metadata status field to Approved.
	 *
	 * @param sting $nids nodes id (nid;nid;nid)
	 */
	public static function setStatusToApproved($nids) {
		if(self::setVisibility($nids, self::VISIBLE)) {	
			return self::setState($nids, self::APPROVED);
		}
		else {
			return false;
		}
	}
	
	/**
	 * Update metadata status field to Rejected.
	 * 
	 * @param sting $nids nodes id (nid;nid;nid)
	 */
	public static function setStatusToRejected($nids) {
		if(self::setVisibility($nids, self::HIDDEN)) {
			return self::setState($nids, self::REJECTED);
		}
		else {
			return false;
		}
	}
	
	/**
	 * Set metadata state to $state.
	 * 
	 * @param string $nid node id
	 * @param int $state
	 */
	private static function setState($nid, $state) {
		try {
			$id = explode(';', $nid);
			
			return db_update('metadata')
					->fields(array('status'=> $state))
					->condition('nid', $id, 'IN')
					->execute();
			
		} catch (Exception $e) {
			watchdog_exception('metadata_adm', $e);
		}
		
		return false;
	}
	
	/**
	 * Set metadata visibility to $visibility.
	 * 
	 * @param int $nid node id
	 * @param int $visibility 
	 */
	private static function setVisibility($nid, $visibility) {
		try {
			$id = explode(';', $nid);
			
			switch ($visibility) {
				case self::VISIBLE:
//				rimpiazzato per problemi di indicizzazione delle taxonomy
// 					db_update('node')
// 						->condition('nid', $id, 'IN')
// 						->fields(array('status'=>NODE_PUBLISHED))
// 						->execute();
					$tmpnodes = node_load_multiple($id);
					foreach ($tmpnodes as $n) {
						if($n->status!=NODE_PUBLISHED) {
							$n->status = NODE_PUBLISHED;
							node_save($n);
							taxonomy_build_node_index(metadata_load($n->nid));
						}
					}
					
				break;
			
				default:
// 					db_update('node')
// 						->condition('nid', $id, 'IN')
// 						->fields(array('status'=>NODE_NOT_PUBLISHED))
// 						->execute();
					$tmpnodes = node_load_multiple($id);
					foreach ($tmpnodes as $n) {
						if($n->status!=NODE_NOT_PUBLISHED) {
							$n->status = NODE_NOT_PUBLISHED;
							node_save($n);
							taxonomy_build_node_index(metadata_load($n->nid));
						}
					}
				break;
			}
		
			return db_update('metadata')
					->fields(array('visibility'=> $visibility))
					->condition('nid', $id, 'IN')
					->execute();
		
		} catch (Exception $e) {
			watchdog_exception('metadata_adm', $e);
		}
		
		return false;
	} 
}
