<?php
/**
 * @author a.mistretta
 *
 * @file Implements table metadata class.
 *
 **/

class TableAuthor
{
	const APPROVED = 1;
	const NOT_APPROVED = 0;
	/**
	 * Get author rows.
	 * 
	 * @return array key/value array, where key is author id and value is [name, logo, website, mail]
	 */
	public static function getAuthors()
	{
		$authors_array = array();
		
		$authors = db_select('author', 'a')
		->fields('a', array())->orderBy('name', 'ASC')
		->execute();
		
		
		foreach ($authors as $author) {
			$authors_array[$author->id] = array(
					'name' => $author->name, 
					'logo' => $author->logo,
					'website' => $author->website,
					'mail' => $author->mail,
					);
		}
		
		return $authors_array;
	}
	
	/**
	 * Get metadata author.
	 * @param int $author_id node id.
	 * @return array that contains metadata author; false on error.
	 */
	public static function getAuthor($author_id)
	{
		$row = db_select('author', 'a')
		->fields('a')
		->condition('id', $author_id, '=')
		->orderBy('a.name')
		->execute();
		
		return $row->fetchAssoc();
	}
	
	/**
	 * Insert or update an author row.
	 * 
	 * @param string $name
	 * @param string $logo
	 * @param string $website
	 * @param string $email
	 * @param int $id
	 * 
	 * @return true on success, false otherwise.
	 */
	public static function update_or_insert($name,$logo,$website,$email, $id=NULL) {
		$row = new stdClass();
		
		$row->name = trim($name);
		
		if($logo) {
			$row->logo = trim($logo);
		}
		
		$row->website = trim($website);
		$row->mail = trim($email);
		
		if (!isset($id)) {
			
			return drupal_write_record('author', $row)==SAVED_NEW;
		}
		else {
			$row->id = $id;
			$row->status = self::APPROVED;
			return drupal_write_record('author', $row, 'id')==SAVED_UPDATED;
		}
	}
	
	/**
	 * If author not already exists add new author row.
	 * 
	 * @param string $author_name author name
	 * @param int $uid user id
	 * 
	 * @return int author id (created or existed).
	 * 			
	 */
	public static function create_if_not_exists($author_name, $uid, $node) {
		
		$author_name = trim($author_name);
		$query_res = db_select('author', 'A')
			->fields('A')
			->condition('name', "%$author_name%", 'ILIKE')
			->execute();
		
		// todo migliorare il detect di autori esistenti
		if($query_res->rowCount()==1) {
			// author already exists
			$row = $query_res->fetchAssoc();
			return $row['id'];
		}
		else {
			// add new author
			$date = new DateTime();
			
			$row->name = $author_name;
			$row->creation_date = strtotime("now");
			$row->logo = 'default_logo.png';
			$row->website = NULL;
			$row->mail = NULL;
			$row->uid = $uid;
			$row->status=self::NOT_APPROVED;
			
			
			if(drupal_write_record('author', $row)==SAVED_NEW) {
				
				// assigned action execution
				$action = trigger_get_assigned_actions('metadata_author_added');
				if(count($action)) {
					actions_do(key($action), $node, array());
				}
				
				return $row->id;
			} else {
				throw new Exception("Non è stato possibile aggiungere la fonte $author_name");
			}
		}
		
	}
	
	/**
	 * Check if author exists.
	 * 
	 * @param int $author_id author id
	 * @return true if author exists, false otherwise
	 **/
	public static function author_exists($author_id) {
		
		if(!isset($author_id) || !is_numeric($author_id))
			return false;
		
		
		$query = db_select('author', 'A')
			->condition('id', $author_id, '=');

		$query->addExpression('count(*)', 'tot');
		
		$found = $query->execute()->fetchColumn();
		
		return $found==1;
	}
	
	/**
	 * Delete an author and all metadata related.
	 * @param int $author_id author identifier to delete.
	 * @throws Exception when operation fails.
	 */
	public static function delete_author_and_related_metadata($author_id) {
		$transaction = db_transaction('author_delete');
		try {
			// get metadata id related to deleteing author
			$res = db_select('metadata', 'M')
				->fields('M', array('nid'))
				->condition('id_author', $author_id,'=')
				->execute();
			
			$nids = $res->fetchAll();
			// delete related metadata
			node_delete_multiple($nids);
			
			// author delete
			db_delete('author')
				->condition('id', $author_id, '=')
				->execute();
			
		} catch (Exception $e) {
			$transaction->rollback();
			throw $e;
		}
		
		// $transaction goes out of scope here.  Unless the it was rolled back, it
		// gets automatically commited here.
	}
	
	/**
	 * Delete an author and replace it with specified author.
	 * @param int $author_id author identifier to delete
	 * @param int $replacement_author_id replacement author identifier.
	 * @throws Exception when operation fails 
	 */
	public static function delete_and_replace_with($author_id, $replacement_author_id) {
		
		try {
			// update metadata related to deleting author with $replacement_author_id
			db_update('metadata')
				->fields(array('id_author' => $replacement_author_id))
				->condition('id_author', $author_id, '=')
				->execute();
				
			
			// delete author
			// author delete
			db_delete('author')
			->condition('id', $author_id, '=')
			->execute();
			
		} catch (Exception $e) {
			$transaction->rollback();
			throw $e;
		}
	}
}