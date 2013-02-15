<?php
/**
 * @author a.mistretta
 *
 * @file Implements table relation class that contains metadata external links.
 *
 **/

class TableMetadataEvents
{
	// Metadata Event type
	const VIEW_EVTTYPE = 0;
	const SEARCH_EVTTYPE = 1;
	const INSERT_EVTTYPE = 2;
	const DELETE_EVTTYPE = 3;
	const DOWNLOAD_EVTTYPE = 4;
	const table_name = 'metadata_events';
	const count_view_name = 'view_metadata_relevance';
	var $count_view_column_name = array(
				self::VIEW_EVTTYPE => 'view_count',
				self::SEARCH_EVTTYPE => 'search_count',
				self::DOWNLOAD_EVTTYPE => 'download_count',
			);

	/**
	 * Add event row.
	 * 
	 * @param int $evt_id
	 * @param int $metadata_id
	 * @throws Exception on error
	 */
	public function insert($evt_id, $metadata_id) {
		
		if(is_array($metadata_id)) {
			try {
				$query = db_insert(self::table_name)->fields(array('id_metadata', 'id_event_type', 'date'));
				foreach ($metadata_id as $id) {
					$query->values(array($id, $evt_id, REQUEST_TIME));
				}
				$query->execute();
			} catch (Exception $ex) {
				throw  new Exception("Error saving metadata events: " . $evt_id, '999');
			}
		}
		else {
			
		$row = new stdClass();
		$row->id_event_type = $evt_id;
		$row->id_metadata = $metadata_id;
		$row->date = REQUEST_TIME; 

		if(drupal_write_record(self::table_name, $row)!=SAVED_NEW) {
			throw  new Exception("Error saving metadata event: " . $evt_id, '999');
		}
		}
	}
	
	/**
	 * Get metadata view\search\download event count.
	 * 
	 * @param int $metadata_id metadata identifier
	 * @param mixed $evt_type event type(int) to get a single value or 'ALL' to get all events count.
	 * @return mixed event/s count (int or object)
	 */
	public function getMetadataEventsCount($metadata_id, $evt_type = 'ALL') {
		$result = 0;
		
		if(!is_null($metadata_id)) {
			try {
				// se è stato specificato un tipo di evento valido
				if(isset($evt_type) && is_numeric($evt_type) && array_key_exists($evt_type, $this->count_view_column_name) ) {
					$result = db_select(self::count_view_name, 'E')
					->fields('E', array($this->count_view_column_name[$evt_type]))
					->condition('nid', $metadata_id, '=')
					->execute()->fetchField();
				}
				else
				{
					$result = db_select(self::count_view_name, 'E')
					->fields('E', array())
					->condition('nid', $metadata_id, '=')
					->execute()->fetchAllAssoc('nid');
				}
				
			} catch (Exception $ex) {
				$result = 0;
				watchdog_exception('metadata', $ex, "Error get metadata events count: " . $evt_id);
			}
		} 
		
		return $result;
	}
	
	/**
	 * Truncate metadata_events table.
	 * 
	 * @throws Exception on error
	 */
	public function clean() {
		try {
			db_truncate(self::table_name, array())->execute();
		}
		catch (Exception $ex) {
			throw  new Exception("Error saving metadata event: " . $evt_id, '999', $ex);
		}
	}
	
	/**
	 * Get metadata created count by uid (user id).
	 * 
	 * @param int $uid user id
	 * @return array 
	 */
	public static function getMetadataCreationEvent($uid) {
// 		SELECT count(*) day_count, to_char(to_timestamp(E.date) , 'YYYY-MM-DD') evt_date 
// 		FROM metadata M INNER JOIN metadata_events E ON M.nid=E.id_metadata
// 		where id_event_type=2 and id_publisher=1
// 		GROUP BY evt_date
// 		ORDER BY evt_date asc
		$data = array();
		try {		
			$query = db_select('metadata', 'M');
			$query->innerJoin(self::table_name, 'E', ' M.nid=E.id_metadata');		
			$query->addExpression('count(*)', ' day_count');
			$query->addExpression("to_char(to_timestamp(E.date) , 'YYYY-MM-DD')", 'evt_date');
			$query
				->condition('id_event_type', self::INSERT_EVTTYPE, '=')
				->condition('id_publisher', $uid, '=')
				->where("E.date > (date_part('epoch'::text, now()))::integer - (date_part('epoch'::text, '6 months'::interval))::integer")
				// todo aggiungere condizione su intervallo temporale
				->groupBy('evt_date')
				->orderBy('evt_date', 'ASC');
			
			$result = $query->execute();
			
			// todo inizializzare con query per ottenere il numero di schede create precedentemente 
			$count = 0;
			
			foreach ($result as $row) {
				$count += $row->day_count;
				$data[] = array(
							'date' => $row->evt_date,
							'tot' => $count
						);
			}
		}
		catch (Exception $ex) {
			watchdog_exception('DB Error', $ex);
		}

		return $data;
	}
	
	/**
	 * Get relevance. 
	 * 
	 * @param int $uid user identifier
	 * @return array that contains relevance row object.
	 */
	public static function getWeekRelevance($uid) {
		try {		
			$rows = db_query("SELECT A.*, 0.3*A.view_count+0.6*A.search_count+0.9*A.download_count relevance
					FROM (
					SELECT S.week, S.id_publisher, S.evt_count as search_count, 
						CASE WHEN V.evt_count is NULL THEN 0 ELSE V.evt_count END as view_count,  
						CASE WHEN D.evt_count IS NULL THEN 0 ELSE D.evt_count END as download_count
					from 
					(
					-- search events
					SELECT * FROM {view_metadata_events_week}
					where id_event_type=1 and week > date_part('week'::text, to_timestamp(date_part('epoch'::text, now())::integer - (date_part('epoch'::text, '6 months'::interval))::integer))
					) S
					LEFT JOIN (
					-- view events
					SELECT * FROM {view_metadata_events_week}
					where id_event_type=0 and week > date_part('week'::text, to_timestamp(date_part('epoch'::text, now())::integer - (date_part('epoch'::text, '6 months'::interval))::integer))
					) V ON V.id_publisher = S.id_publisher AND V.week = S.week
					LEFT JOIN(
					-- download events
					SELECT * FROM {view_metadata_events_week}
					where id_event_type=4 and week > date_part('week'::text, to_timestamp(date_part('epoch'::text, now())::integer - (date_part('epoch'::text, '6 months'::interval))::integer))
					) D ON V.id_publisher = D.id_publisher AND V.week = D.week
					where S.id_publisher= :uid ) A
					
					order by A.week", array(':uid'=> $uid));
		
			if($rows)		
				return $rows->fetchAll();
			else
				return array();
		}
		catch(Exception $ex) {
			watchdog_exception('DB Error', $ex);
			return array();
		}
	}
}