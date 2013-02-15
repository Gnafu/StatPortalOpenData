<?php


/**
 * Item menu callback. Yield the download page.
 * @param int $fid file id
 * @param string $file_format file format
 * @param int $nid node id
 */
function metadata_download_page($fid, $file_format ,$nid, $mDataId, $showurl=NULL) {
	$node = node_load($nid);
	if($node) {
		$node = metadata_load(array($node));
		$form_state = array(
				'attachments' => array( $mDataId ),
				'nid' => $nid,
				'fid' => isset($fid) ? $fid : -1,
				'metadata_license' => array(taxonomy_term_load($node[0]->metadata_license['und'][0]['tid'])),
				'author' => $node[0]->author['name'],
				'form_action' => $node[0]->identifier,
		);
		$form = drupal_build_form('metadata_download_form', $form_state);
		
		switch (strtolower($file_format)) {
			case 'xls':
				$file_format_label = 'Excel 2003';
			break;
			case 'xlsx':
				$file_format_label = 'Excel 2007';
				break;
			case 'dbf':
				$file_format_label = 'DBase IV';
				break;
			case 'txt':
			case 'csv':
				$file_format_label = 'File di testo/CSV';
				break;
			case 'mdb':
				$file_format_label = 'Access 2003';
				break;
			case 'zip':
				$file_format_label = 'Archivio';
				break;
			default:
				$file_format_label=$file_format;
			break;
		}
		
		if(isset($showurl)) {
			// get odata services
			$odata_services = MetadataHelper::get_services()->fetchAllAssoc('service_name');
			if( isset($odata_services['ODATA4J_SERVICE']) &&
					isset($odata_services['ODATA4J_SERVICE']->service_url) &&
					!empty($odata_services['ODATA4J_SERVICE']->service_url)) {
				// MdData('7c4058d2-9b5c-4232-bcc2-884da70c6055@datiopen')/DataRows?\$skip=0&\$top=10
				$showurl = $odata_services['ODATA4J_SERVICE']->service_url."MdData('$showurl')/DataRows?\$skip=0&\$top=10";
			}
			else {
				$showurl = NULL;
			}
				
		}
		
		print theme('metadata_download', array('fid' => $fid, 'file_format' => $file_format_label, 'node' => $node[0], 'mDataId' => $mDataId, 'download_form' => $form, 'url' => $showurl));
	}
	else {
		echo 'Nessun allegato da scaricare';
	}
}