<?php 

include_once 'tables/table.metadata.inc';

function ajax_catalog_term_updater($type = 'ajax') {
	if($type != 'ajax') {
		$output = t("Operation not supported.");
		return;
	}
	
	$result = array( 'type' => 'term-count' );
// 	$hasKeyTerms = $params['keys']!='';
	
	try {
	
		$helper = new MetadataSearchHelper();
		$search_query = $helper->getSearchQuery();
		// search result isn't empty
		if(!is_null($search_query)) {
		
			if($hasKeyTerms) {
				$search_query->addField('i', 'sid', 'nid');
			}
			
			$query = db_select('taxonomy_index', 'TI');
			$query->fields('TI', array('tid'));
			$query->addExpression('count(*)', 'TOT');
			$query->innerJoin($search_query, 'SQ', 'TI.nid=SQ.nid');
			$query->groupBy('TI.tid');
			
			$res = $query->execute();
			
			foreach ($res as $row) {
				$result['term'][$row->tid] = $row->tot;
			}
			
			$query = db_select('metadata', 'M');
			$query->fields('A', array('id'));
			$query->addExpression('count(*)', 'TOT');
			$query->innerJoin('author', 'A', 'M.id_author=A.id');
			$query->innerJoin($search_query, 'SQ', 'M.nid=SQ.nid');
			$query->groupBy('A.id');
			
			$res = $query->execute();
			
			foreach ($res as $row) {
				$result['author'][$row->id] = $row->tot;
			}
			
			global $user;
			if(user_access('administrator')) {
			
				$query = db_select('metadata', 'M')
					->fields('M', array('status'));
				$query->addExpression('count(*)', 'TOT');
				$query->innerJoin($search_query, 'SQ', 'M.nid=SQ.nid');
				$query->groupBy('M.status');
				
				$res = $query->execute();
				
				foreach ($res as $row) {
					$result['admin']['status_' . $row->status] = $row->tot;
				}
				
				$query = db_select('metadata', 'M')
				->fields('M', array('visibility'));
				$query->addExpression('count(*)', 'TOT');
				$query->innerJoin($search_query, 'SQ', 'M.nid=SQ.nid');
				$query->groupBy('M.visibility');
				
				$res = $query->execute();
				
				foreach ($res as $row) {
					$result['admin']['visibility_' . $row->visibility] = $row->tot;
				}
			}
		}
	}
	catch ( Exception $ex) {
		$msg = 'Error getting term result count: ' . $query->__toString();
		watchdog_exception('metadata', $ex, $msg);
		$result['error'] = $msg;
	}
	
	// esegue la query per ottenere il numero di risultati per ciascun termine della tassonomia
	
	drupal_json_output($result);
}


/**
 * Implement server side search. AJAX method.
 *
 * @param string $type request type.
 */
function ajax_metadata_advanced_search($type = 'ajax')
{
	if ($type == 'ajax') {

		// result
		$search_result;

		// execute search query
		try {
			// execute search
			$helper = new MetadataSearchHelper();
			$search_result = $helper->search();
		}
		catch (Exception $ex) {
			// set empty results
			$search_result = array('page_count'=>0, 'match_count'=>0);
			// log error
			watchdog_exception('Metadata -  Search', $ex, 'Error executing catalog query: ' . $ex->getMessage());
		}

		// used by pager
		$page_count = isset($search_result['page_count']) ? $search_result['page_count'] : 0;
		$match_count = isset($search_result['match_count']) ? $search_result['match_count'] : 0;
		unset($search_result['match_count']);
		unset($search_result['page_count']);

		// prepare search result
		$output = metadata_search_page($search_result);
		$output['#page_count'] = $page_count;
		$output['#match_count'] = $match_count;
		$output['#page_order_type'] = $orderBy;
		// add container

		// theme and return result page
		ajax_deliver($output);
	}
	else
	{
		$output = t("Operation not supported.");
	}
}

/**
 * Implement server side search. AJAX method.
 *
 * @param string $type request type.
 */
function ajax_metadata_adm_task($type = 'ajax') {
	if ($type == 'ajax' && user_access('administrator')) {
		$task = $_POST['task'];
		$metadata_ids = $_POST['mids'];
		$req_status = false;
		
		switch ($task) {
			case 'task-approved':
				if($req_status=TableMetadata::setStatusToApproved($metadata_ids)) {
					$output = t('Le schede selezionate sono state Approvate');
				}
				else {
					$output = t('Si &egrave; verificato un errore. Operazione non eseguita.');
				}
			break;
				
			case 'task-rejected':
				if($req_status=TableMetadata::setStatusToRejected($metadata_ids)) {
					$output = t('Lo stato delle schede selezionate &egrave; stato impostato a Scartato');
				}
				else {
					$output = t('Si &egrave; verificato un errore. Operazione non eseguita.');
				}
			break;
			
			case 'task-contact-author':
				
				if( !isset($_POST['subject']) || !isset($_POST['body']) || 
					empty($_POST['subject']) || empty($_POST['body']) 
				) {
					$output = ('Impostare l\'oggetto e il corpo del messaggio da inviare');					
				} 
				else {
					//todo controllare esito invio
					if($req_status=_send_mail_to_metadata_owner($metadata_ids, $_POST['subject'], $_POST['body'])) {
						$output = t('Email inviate con successo');
					}
					else {
						$output = t('Si &egrave; verificato un errore.');
					}
				} 
					
					
			
			break;
			
			case 'task-hide':
				if($req_status=TableMetadata::setVisibilityToHidden($metadata_ids)) {
					$output = t('Le schede selezionate non sono pi&uacute; visibili.');
				}
				else {
					$output = t('Si &egrave; verificato un errore. Operazione non eseguita.');
				}
			break;
			
			case 'task-show':
				if($req_status=TableMetadata::setVisibilityToVisible($metadata_ids)) {
					$output = t('Le schede selezionate sono visibili.');
				}
				else {
					$output = t('Si &egrave; verificato un errore. Operazione non eseguita.');
				}
			break;
			
			default:
				$output = t("Operazione non riconosciuta.");
			break;
		}
		
	}
	else
	{
		$output = t("Operazione non consentita.");
	}
	
	$json_output = array( 'd' =>$output, 'type' => 'admin-task', 'error' => !$req_status);
		
	drupal_json_output($json_output);
}

/**
 * Send email to metadata owner.
 * 
 * @param string $metadata_ids metadata id (ex: nid;nid;nid)
 */
function _send_mail_to_metadata_owner($metadata_ids, $subject, $body) {
	// associative array. key=owner value=message_info
	$email = array();
	
	// get metadata owner mail address
	$nodes = node_load_multiple(explode(';', $metadata_ids));
	
	// get owner mail
	foreach ($nodes as $key => $node) {
		// get owner mail address if not yet loaded
		if(!isset($email[$node->uid])) {
			$owner = user_load($node->uid);
			
			$email[$key] = array(
						'to' => $owner->mail,
						'lang' => user_preferred_language($owner),
						'metadata' => array( $node ),
						'md_link' => url('node/' . $node->nid, array('absolute' => TRUE))
					);
		}
		else {
			$email[$key]['metadata'][] = $node;
			$email[$key]['metadata']['md_link'] .= ', ' . url('node/' . $node->nid, array('absolute' => TRUE));
		}
	}
	$site_email = variable_get('site_mail', '');
	 
	
	// send message
	foreach ($email as $uid => $message_info) {
		$_body = $body;
		$_body .= ' Schede coinvolte: ' . $message_info['md_link'];
		
		$params = array('body' => $_body, 'subject' => $subject);
		
		$message = drupal_mail('metadata', 'edit_mail', $message_info['to'], $message_info['lang'],
						$params, $site_email, TRUE);
		
		if(!$message['result'])
			return false;
	}
	
	return true;
}
