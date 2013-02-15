<?php
include_once('../helper/federation.helper.inc');

/**
 * Federated download support.
 * Item menu callback
 */
function metadata_federated_download() {
	if($_SERVER['REQUEST_METHOD']!='POST') {
		echo 'Access denied.';
		return;
	}
	
	//	get POST parameter	
	$metadata_uid = isset($_REQUEST['mid']) ? $_REQUEST['mid'] : '';
	$fid = isset($_REQUEST['fid']) ? $_REQUEST['fid'] : -1;
	$hash = isset($_REQUEST['h']) ? $_REQUEST['h'] : '';
	
	// 	check hash
	if(FederationHelper::isValidAttachHash($hash, $metadata_uid, $fid)) {
		$file_info = file_load($fid);
		if($file_info==false) {
			echo t('File not found.');
		}
		else {
			// get file extension by attach table
			$file_ext = db_select('attach', 'A')
				->fields('A', array('file_format'))
				->condition('A.fid', $fid, '=')
				->execute()
				->fetchField();
			
			// prepare output header
			drupal_add_http_header("Content-type", "text/plain");
			drupal_add_http_header("Cache-Control", 'no-cache');
			drupal_add_http_header("Content-disposition", 'attachment; filename="out.'. $file_ext . '"');
			
			// clean output
			$out = ob_get_clean();
			ob_clean();
			flush();
			// write responce
			$readed = readfile($file_info->uri);
			if($readed==false) {
				echo t("Creazione dell'export fallita.");
			}
		}
	}
	else {
		echo 'Access denied!';
	}
	
	exit;
}