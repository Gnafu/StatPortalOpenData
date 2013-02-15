<?php
/**
 * @file FederationHelper implementation.
 */

/**
 * Class FederationHelper define federation utility functions.
 *
 * @author a.mistretta
 */
class FederationHelper {

	CONST alg = 'md5';

	public function HashAlgorithm() {
		return self::alg;
	}


	/**
	 * loadMetadataInfo return
	 *
	 * @param integer $nid
	 * @return array: array('metadata_uid', 'local', 'system_uid', 'system_base_url')
	 */
	public static function getMetadataInfo($nid) {
		$metadata_uid = NULL;
		$local = true;
		$system_uid = NULL;
		$system_base_url = NULL;

		$row = db_select('metadata', 'm')
		->fields('m', array('local', 'uid'))
		->condition('nid', $nid, '=')
		->execute()
		->fetchAssoc();

		// get metadata uid
		if(isset($row) && isset($row['uid'])) {
			$metadata_uid = $row['uid'];
		}

		// if metadata is not local => get system uid
		if(isset($row) && isset($row['local']) && !$row['local']) {
			// get system uid
			$local = false;
			try {
				list($system_uid, $system_base_url) = self::getMetadataOrigin($nid);
			}
			catch (Exception $e) {
				watchdog_exception('federation', $e);
			}
		}

		return array($metadata_uid,$local,$system_uid,$system_base_url);
	}

	/**
	 * Get system uid by metadata node id.
	 *
	 * @param string $uid
	 * @return string: origin system uid (this is a site unique identifier)
	 * @throws Exception on error or origin not found.
	 */
	public static function getMetadataOrigin($nid) {
		$field = 'metadata_datasources';
		$origin = NULL;
		$origin_url = NULL;
		// 		get node datasource taxonomy term
		$node = entity_load('node', array($nid));
		$node = array_values($node);
		if($node===FALSE || empty($node)) {
			throw new Exception('Metadata not found');
		}

		// get datasource term name
		$datasource = field_get_items('node', $node[0], $field);
		$ds_count = count($datasource);

		if (is_array($datasource)) {
			if ($ds_count > 1) {
				throw new Exception('La scheda è associata a più datasource.');
			}
			foreach ($datasource as $item) {
				$term = $item['tid'];
			}
		}
		else {
			throw new Exception('Datasource not found.');
		}

		// 		read drupal variable: get origin uid by datasource term
		if(isset($term)) {
			$system_info = db_select('{federation}', 'f')
			->condition('tid', $term, '=')
			->fields('f', array('uid', 'url'))
			->execute()
			->fetchAssoc();
			if($system_info) {
				$origin = $system_info['uid'];
				$origin_url = $system_info['url'];
			}
		}

		if(!isset($term) || is_null($origin)) {
			throw new Exception('Datasource not found');
		}

		return array($origin, $origin_url);
	}

	/**
	 * Get hash.
	 *
	 * @param string $uid_metadata: metadata unique identifier
	 * @param integer $attach_fid: attachment file id
	 * @param string $system_uid: system unique identifier
	 * @return string: hash
	 */
	public static function getAttachHash($uid_metadata, $attach_fid, $system_uid) {
		return hash(self::alg, $uid_metadata . $fid . $system_uid);
	}

	/**
	 * Compare hash parameter with metadata attach hash.
	 *
	 * @param string $hash: to compare
	 * @param string $uid_metadata: metadata unique identifier
	 * @param integer $attach_fid: attach file id
	 * @return bool: true on success, false otherwise.
	 */
	public static function isValidAttachHash($hash, $uid_metadata, $attach_fid) {
		$local_system_uid = db_select('federation', 'f')
		->condition('local', 1, '=')
		->fields('f', array('uid'))
		->execute()
		->fetchColumn();
		$hash_2 = self::getAttachHash($uid_metadata, $attach_fid, $local_system_uid);

		return strcasecmp($hash, $hash_2)===0;
	}
}