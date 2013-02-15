<?php
/**
 * @author a.mistretta
 *
 * @file Implements table attach class.
 *
 **/
//include "table.metadata_preview.inc";


/**
 * TableAttach class.
 * 
 **/
class TableAttach
{
	const TYPE_DATA_MD = 0;
	const TYPE_DATA_RAW = 1;
	const TYPE_REPORT = 2;
	const TYPE_APPLICATION = 3;
	
	// true if node has structured data attachment 
	private $nodeHasStructuredDataAttachment = false;
	// true if node has raw data attachment
	private $nodeHasRawDataAttachment = false;
	
	public static $type_mapping = array (
			self::TYPE_DATA_MD => 'data_md',
			self::TYPE_DATA_RAW => 'data_raw',
			self::TYPE_REPORT => 'report',
			self::TYPE_APPLICATION => 'application',
	);


	/**
	 * Add an attach to metadata.
	 *
	 * @param object $node
	 * @param int $type
	 * @param int $id_publisher
	 * @param string $url
	 * @param int $fid attachment file id on file_manged table.
	 * @param string $data_description data format description
	 */
	public function addAttachToMetadata($node, $type, $id_publisher, $url, $extra_attach_fid=NULL, $data_description=NULL)
	{
		if( $type!=self::TYPE_DATA_MD && $type!=self::TYPE_DATA_RAW &&
				$type!=self::TYPE_APPLICATION && $type!=self::TYPE_REPORT )
			throw new Exception('Attach type (' . $type . ') is not valid!');

		$file;
		// save file. if not exists throw exception (problem in validation!).
		if(is_null($extra_attach_fid))
			$file = $this->save_attached_file($node->fid);
		else
			$file = $this->save_attached_file($extra_attach_fid);
		
		// create row object
		$attach_row = new stdClass();
		$attach_row->id_metadata = $node->nid;
		$attach_row->id_attach_type = $type;
		$attach_row->creation_date = format_date(time(), 'custom', 'l j F Y');
		$attach_row->last_update = $attach_row->creation_date;
		$attach_row->id_publisher = $id_publisher;
		$attach_row->vid = $node->vid;
		if(is_null($extra_attach_fid) && !is_null($node->fid)) {
			$attach_row->fid = $node->fid;
			if(is_object($file))
				$attach_row->file_format = $this->get_file_format($file);
		}
		else if(!is_null($extra_attach_fid)) {
			$attach_row->fid = $extra_attach_fid;
			// todo rimane da gestire il set di file_format
		}
		else {
			throw  new Exception("Missing file id", '111');
		}
		
		$attach_row->data_description = $data_description;
		
		// save record
		if(drupal_write_record('attach', $attach_row)!=SAVED_NEW)
			throw  new Exception("Error saving metadata", '999');
		
		// create row object
		$spec_attach = new stdClass();
		$attach_row->id_attach = $attach_row->id;
		$attach_row->url = $url;
		// save record				
		if(drupal_write_record(TableAttach::$type_mapping[$type], $attach_row)!=SAVED_NEW)
			throw  new Exception("Error saving metadata", '999');
	}
	
	/**
	 * Update data_description field of attach row related to $odata_url. 
	 * (Only data_md and data_row) 
	 * 
	 * @param string $odata_url
	 * @param int $attach_type
	 * @param string $data_description
	 */
	public function updateAttachDataDescription($odata_url, $attach_type, $data_description) {
		if(self::$type_mapping[$attach_type]) {
			$q_result = db_select(self::$type_mapping[$attach_type], 'EA')
				->fields('EA', array('id_attach'))
				->condition('url', $odata_url, 'ILIKE')
				->execute();
			
			$attach_id = $q_result->fetchField();
			
			if($attach_id) {
				$attach_row = new stdClass();
				$attach_row->id = $attach_id;
				$attach_row->data_description = $data_description;
				
				// update record
				db_update('attach')
					->condition('id', $attach_id, '=')
					->fields(array( 'data_description' => $data_description, ))
					->execute();
			}
		}
	}
	
	/**
	 * Set file status.
	 * @param int $fid file id
	 * @throws Exception
	 */
	private function save_attached_file($fid) {
		// save file if no error
		if(isset($fid) && $fid>0) {
			// Load the file via file.fid.
			$file = file_load($fid);
			if($file) {
				// Change status to permanent.
				$file->status = FILE_STATUS_PERMANENT;
				//TODO: aggiungere controllo caratteri speciali
				// Save.
				file_save($file);
				// Record that the module (in this example, user module) is using the file.
				file_usage_add($file, 'metadata', 'metadata', $fid);
			}
			
			return $file;
		}
		else {
			throw  new Exception("Error saving metadata file attchment", '999');
		}
		
		return false;
	}
	
	/**
	 * Get all metadata attachments.
	 * 
	 * @param int $node_id node id.
	 * @return array array of attach row
	 */
	public function getExtendedMDAttach($node_id, $bind_preview=true)
	{
		$res = db_select('attach', 'a')
			->fields('a')
			->condition('id_metadata', $node_id, '=')
			->execute();
		
		$rows = $res->fetchAllAssoc('id');
		
		$this->bindMDSpecInfo($rows);
		
		if($bind_preview)
			$this->bindMDPreview($rows);

		return $rows;
	}
	
	/**
	 * Delete all metadata attachment
	 * 
	 * @param object $node a metadata node object.
	 */
	public function delete($node)
	{
		// load preview and specified attach rows
		$attachments = $this->getExtendedMDAttach($node->nid);
		
		$id_attachments = array();
		$id_preview = array();
		foreach ($attachments as $id => $attach)
		{
			array_push($id_attachments, $attach->id);
			
			foreach ($attach->previews as $preview)
			{
				array_push($id_preview, $preview->id);
			}
		
			// delete specified attach row
			$res = db_delete(TableAttach::$type_mapping[$attach->id_attach_type])
				->condition('id_attach', $attach->id, '=')
				->execute();
			
			// eseguo la richiesta di cancellazione al servizio ETL
			if($attach->id_attach_type==self::TYPE_DATA_MD || $attach->id_attach_type==self::TYPE_DATA_RAW) {
				if(isset($attach->type_info) && isset($attach->type_info->odata_id)) {
					$this->ETLAttachDelete($attach->type_info->odata_id);
				}
				
			}
		}
		
		if(count($id_preview)>0)
		{	
			$res = db_delete('metadata_preview')
				->condition('id', $id_preview, 'IN')
				->execute();
		}
		
		if(count($id_attachments)>0)
		{
			$res = db_delete('attach')
					->condition('id', $id_attachments, 'IN')
					->execute();
		}
		
	}
	
	/**
	 * Delete an attach.
	 * @param int $attach_id
	 */
	public function deleteAttach($attach_id, $new_fid=NULL) {
		
		// get attach type
		$res = db_select('attach', 'a')
			->fields('a')
			->condition('id', $attach_id, '=')
			->execute();
		
		$attach_row = $res->fetchObject();
		if($attach_row) {
			
			$attach_type = $attach_row->id_attach_type;

			$query_res = db_select(self::$type_mapping[$attach_type], 'a')
				->fields('a')
				->condition('id_attach', $attach_id, '=')
				->execute();
			
			$extend_attach_row = $query_res->fetchObject();
			
			// delete attach child table row
			$res = db_delete(self::$type_mapping[$attach_type])
				->condition('id_attach', $attach_id, '=')
				->execute();
			
			// delete attach row 
			$res = db_delete('attach')
				->condition('id', $attach_id, '=')
				->execute();
			
			// ETL Delete ??
			if($attach_type==self::TYPE_DATA_MD || $attach_type==self::TYPE_DATA_RAW  )
			{	
				// check odata_id and delete
				$uid_domain = array();
				if(preg_match('/[a-z0-9]{8}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{12}@[a-z0-9]+/', $extend_attach_row->url, $uid_domain)) 
				{
					$this->ETLAttachDelete($uid_domain[0]);
				}
				else {
					watchdog('metadata', 'ETLDelete non invocata: formato url non valido.',NULL,WATCHDOG_INFO);
				}
			}
			// cancellazione file: se la riga di attach contine un fid valido ed è diverso dal nuovo
			if(isset($attach_row->fid) && isset($attach_row->fid)>0 && $new_fid!=$attach_row->fid) {
				// Load the file via file.fid. and delete
				$file = file_load($attach_row->fid);
				if($file && file_exists(drupal_realpath($file->uri)))
					file_delete($file, true);
			}
			
			return true;
		}
		else 
			return false;
	}
	
	
	/**
	 * True if exists structured attachment in previous loaded
	 * node attachment.
	 */
	public function nodeHasStructuredAttach() {
		return $this->nodeHasStructuredDataAttachment;
	}
	
	/**
	 * True if exists raw attachment in previous loaded
	 * node attachment.
	 */
	public function nodeHasRawAttach() {
		return $this->nodeHasRawDataAttachment;
	}
	
	/**
	 * True if exists a data attachment (raw or structured) in previous loaded
	 * node attachment.
	 */
	public function nodeHasDataAttach() {
		return $this->nodeHasRawDataAttachment ||$this->nodeHasStructuredDataAttachment ;
	}
	
	/**
	 * Bind metadata_preview to each attachment row.
	 * 
	 * @param array $rows attachment rows.
	 */
	private function bindMDPreview(&$rows)
	{
		foreach ($rows as $i => $val)
		{
			$res = db_select('metadata_preview', 'm')
			->fields('m')
			->condition('id_attach', $val->id, '=')
			->execute();
			
			$rows[$i]->previews = $res->fetchAllAssoc('id');
		}
	}
	
	/**
	 * Bind attach to data_md, data_raw, report or application row.
	 */
	private function bindMDSpecInfo(&$rows)
	{
		// la tabella in cui cercare le info relative ai singoli attach
		$spec_table;
		
		// scorre gli attach
		foreach ($rows as $i => $val)
		{
			// set table to query
			switch ($val->id_attach_type) {
				case self::TYPE_DATA_MD:
					$spec_table="data_md";
					$this->nodeHasStructuredDataAttachment=true;
					break;
				case self::TYPE_DATA_RAW :
					$spec_table="data_raw";
					$this->nodeHasRawDataAttachment=true;
					break;
				case self::TYPE_REPORT:
					$spec_table="report";
					break;
				case self::TYPE_APPLICATION:
					$spec_table="application";
					break;
				default:
					throw new Exception("Bad attach format", 1);
					break;
			}
			
			// query execute
			$res = db_select($spec_table, 't')
				->fields('t')
				->condition('id_attach', $rows[$i]->id, '=')
				->execute();

			// aggiunge i dettagli della tabella child alla riga di attach
			//$rows[$i]->type_info = $res->fetchAssoc('id');
			foreach ($res as $value) {
				$rows[$i]->type_info = $value;
				
				$uid_domain = array();
				if(preg_match('/[a-z0-9]{8}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{12}@[a-z0-9]+/', $value->url, $uid_domain)) {//-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{12}\'\)
					$rows[$i]->type_info->odata_id = $uid_domain[0];
				}
				else if ( strpos($value->url, 'public://metadata_updfile/report/')===FALSE ){
					$rows[$i]->type_info->error = t('URL dati non valido');
				}
				
				
// 				$start = strpos($value->url, '(');
// 				$end = strpos($value->url, ')');
// 				$rows[$i]->type_info->odata_id = substr($value->url, $start+1, $end-$start-1); 
			}
		}
	}
	
	/**
	 * Get a table name between [data_md, data_raw, report, application] 
	 * @param unknown_type $type
	 */
	private function getTableNameByAttachType($type)
	{
		
	}
	
	/**
	 * 	  
	 * @param stdClass $node node object.
	 * @return true if node type is alphanumeric; false othrewise.
	 */
	public static function isAlphanumeric($node) {
		
		$alphanumeric_term_text = variable_get('metadata_format_alphanumeric', 'Alfanumerici');
		return self::hasFormatTerm($node, $alphanumeric_term_text);
	}
	
	/**
	 *
	 * @param stdClass $node node object.
	 * @return true if node type is raw; false othrewise (structured).
	 */
	public static function isRaw($node) {
		$alphanumeric_term_text = variable_get('metadata_format_raw', 'Raw');
		return self::hasFormatTerm($node, $alphanumeric_term_text);
	}
	
	/**
	 *
	 * @param stdClass $node node object.
	 * @return true if node type is structured; false othrewise (structured).
	 */
	public static function isStructured($node) {
		$alphanumeric_term_text = variable_get('metadata_format_structured', 'Strutturati');
		return self::hasFormatTerm($node, $alphanumeric_term_text);
	}
	
	/**
	 *
	 * @param stdClass $node node object.
	 * @return true if node type is geographic; false othrewise.
	 */
	public static function isGeographic($node) {
		$term_text = variable_get('metadata_format_geographic', 'Geografici');
		return self::hasFormatTerm($node, $term_text);
	}
	
	/**
	 * Check if node has input format term.
	 * 
	 * @param object $node node object.
	 * @param string $term taxonomy term string
	 * @return true if node has term; false otherwise.
	 */
	private static function hasFormatTerm($node, $term)  {
		$term = taxonomy_get_term_by_name($term);
		// contains id of getted terms.
		$tid = array();
		foreach ($term as $key => $value) {
			$tid[$value->tid] = $value->tid;
		}
		// check if node as term
		foreach ($node->metadata_format['und'] as $key => $value) {
			if(in_array($value['tid'], $tid))
				return true;
		}
		
		return false;
	}
	
	/**
	 * Run delete task on OPENDATA_ETL service. 
	 * @param stdClass $attach
	 */
	private function ETLAttachDelete($odata_id) {
		try {
				$odata_uid = substr($odata_id, 0, strpos($odata_id, '@'));
				$service = MetadataHelper::get_services('OPENDATA_ETL')->fetchAllAssoc('service_name');
				if(isset($service['OPENDATA_ETL'])) {
					// call soap service UTILITY_AND_MAP_SERVICE?wsdl
					$client = new SoapClient($service['OPENDATA_ETL']->service_url."?wsdl");
					$result = $client->deleteData(array('dataUid'=>$odata_uid));
					if(!$result->deleteDataReturn)
						watchdog('module', 'ETL: Errore nella cancellazione del dato ' . $odata_uid, NULL, WATCHDOG_CRITICAL);
				}
			}
			catch (Exception $ex) {
				watchdog_exception('module', $ex, 'Errore di cancellazione nell\'ETL per il dato ' . $odata_uid . '. ' . $ex->getMessage(), WATCHDOG_CRITICAL);
			}
	}
	
	/**
	 * Get file extension
	 * @param unknown_type $file
	 */
	private function get_file_format($file) {
		return pathinfo($file->filename, PATHINFO_EXTENSION);
	}
}

/**
 * DataAttach class
 * Istance of DataAttach classes stores general info of
 * structured/raw data attachment.
 *  
 */
class DataAttach {
	/* File Managed ID */
	public $fid = -1;
	/* File format */
	public $file_format = 'CSV';
	/* OData ID */
	public $odata_id = -1;
	/* OData URL */
	public $odata_url;
	/* Error on load? */
	public $error = false;
	/* Error message */
	private $error_message;
	const DEFAULT_ERR_MSG = 'Nessun dato allegato alla scheda';
	public $data_description=false;
	/**
	 * Set error message.
	 * @param string $msg error message
	 */
	public function setErrorMessage($msg=NULL) {
		$this->error = true; 
		isset($msg) ? $this->error_message=$msg : $this->error_message=self::DEFAULT_ERR_MSG; 
	}
	/**
	 * Get error message.
	 * @return string error message
	 */
	public function getErrorMessage(){ return $this->error_message; }
	/**
	 * Retrieve Open Data additional info.
	 * 
	 * @return bool true on success, false otherwise.
	 */
	public function loadDataDescription() {
		if($this->odata_id!=-1) {
			$odata_services = MetadataHelper::get_services()->fetchAllAssoc('service_name');
			$result = drupal_http_request($odata_services['ODATA4J_SERVICE']->service_url."DataDescription?dataUid='".$this->odata_id."'&\$format=json");
			
			// se fallisce la richiesta al servizio non visualizziamo niente
			if($result->code == 200) {
				$obj_result = json_decode($result->data);
				if(isset($obj_result) && isset($obj_result->d) && isset($obj_result->d->jsonSerialization)) {
					$jsonSerialization = $obj_result->d->jsonSerialization;
					$this->data_description = json_decode($jsonSerialization);
					return true;
				}
			}
		}
		return $this->data_description=false;
	}
	/**
	 * Use this function to enable a metadata viewer on current page.
	 * 
	 * @param bool $dataviewer true to load a data viewer.
	 * @param string $oDataServices OData services info
	 */
	public function loadMetadataViewer($dataviewer=TRUE, $oDataServices=NULL) {
		drupal_add_css(drupal_get_path('theme', 'statportal') . '/css/metadata.css', array('type' => 'file', 'group' => CSS_THEME));
		if($dataviewer) {
			drupal_add_js('StatPortalOpenData.ODataServices = ' . $oDataServices, 'inline', array('type' => 'inline', 'weight' => -20, 'group' => -200 ));
			drupal_add_library('metadata', 'viewer');
			#grafici
			if (strpos($_SERVER['HTTP_USER_AGENT'], 'MSIE'))
			{
				if (!strpos($_SERVER['HTTP_USER_AGENT'], 'Opera'))
				{
					drupal_add_js(drupal_get_path('module', 'metadata') . '/viewer/commons/jquery_plugins/jqPlot/excanvas.js');
				}
			}
			
			if(isset($this->odata_url))
				drupal_add_js("StatPortalOpenData.RESOURCE_PATH=\"".$this->odata_url."\"", array('type' => 'inline', 'weight' => 0, 'group' => 300));
			else
				drupal_add_js("StatPortalOpenData.LOAD_ERROR='".$this->error_message."'", array('type' => 'inline', 'weight' => 0, 'group' => 300));
			
			drupal_add_js("jQuery(document).ready(function(){ StatPortalOpenData.ODataUtility.loadStateCompleted = false; StatPortalOpenData.ODataUtility.loadState('".$this->odata_id."'); });", array('type' => 'inline', 'weight' => 0, 'group' => 300));
		}
		else {
			// report without data attachment
			$_js_tab_init = '
			(function($){
				$(document).ready(function() {
					var tabToOpen = decodeURI( /t=(.+?)(&|$)/.exec(location.search)||[,null])[1],
					idToOpen = 0;
					
					// search tab to activate
					$("#tab_parent_ul li").each(function(index) {
						if(this.id === tabToOpen){
						idToOpen = index;
						return false;
						}
					});
					
					$("div#comments ul.pager a").each(function() {
						var oldHref = $(this).attr("href");
						$(this).attr("href", oldHref + "&t=Commenti");
					});
					
					// tab initialitazion
					$( "#dataset-container" ).tabs({
						selected: idToOpen,
						select: function(event, ui) {
							// si deve togliere la visualizzazione del link
							//StatPortalOpenData.ODataUtility.unbindAndRemoveExportLink();
							try{
								var tabSelectedId = $("#tab_parent_ul li")[ui.index].id;
								//window.location.hash = "#v=" + tabSelectedId;
								$("#viewer_img_button").css("visibility", "hidden");
							}catch(Exception){
								$("#viewer_img_button").css("visibility", "hidden");
							}
						},
						create: function(event, ui) {
							$("#initial_viewer_loading").removeClass("initial_viewer_loading");
							$("#dataset-container").removeClass("initial_metadata_loading");
							$("#metadata-header").removeClass("initial_metadata_loading");
						}
					});
				});
			})(jQuery)';
			
			drupal_add_js($_js_tab_init, 'inline');
			drupal_add_library('metadata', 'report_viewer');
		}
	}
}