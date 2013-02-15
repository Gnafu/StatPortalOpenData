<?php
/**
 * Metadata statistics helper.
 * Uset to get statistics by metadata_events.
 *
 * @author a.mistretta
 */
class MetadataStatisticsHelper {

	/**
	 * Get lat updated (insert/updated) metadata.
	 * @param int $max_element limit result row count.
	 * @return object query result. Supported row fields: nid, title.
	 *
	 *  select *
	 *	from metadata
	 *	ORDER BY last_update DESC
	 *	LIMIT $max_element
	 */
	public static function getLastUpdatedMetadata($max_element=5) {
		$query_res = NULL;

		try {
			$query_res = db_select('metadata', 'm')
			->condition('status', TableMetadata::APPROVED, '=')
			->condition('visibility', TableMetadata::VISIBLE, '=')
			->fields('m', array('nid', 'title'))
			->orderBy('last_update', 'DESC')
			->range(0, $max_element)
			->execute();

		} catch (Exception $e) {
			watchdog_exception('metadata', $e, 'Get last update metadata!');
		}

		return $query_res;
	}

	/**
	 * Get most voted author.
	 *
	 * @param int $max_element limit results.
	 * @return object query result. Supported row fields: id_author, toto_vote_count, tot_avg_vote
	 */
	public static function getMostVotedAuthors($max_element=5) {
		$query_res = NULL;

		try {
			$query_res = db_select('view_most_voted_author', 'v')
			->fields('v', array('id_author', 'tot_vote_count', 'tot_avg_vote'))
			->orderBy('tot_vote_count', 'DESC')
			->orderBy('tot_avg_vote', 'DESC')
			->range(0, $max_element)
			->execute();
		} catch (Exception $e) {
			watchdog_exception('metadata', $e, 'Get last voted authors!');
		}

		return $query_res;
	}

	/**
	 * Get most active author.
	 *
	 * @param int $max_element limit results.
	 * @return object query result. Supported row fields: id, tot.
	 */
	public static function getMostActiveAuthors($max_element=5) {
		$query_res = NULL;

		try {
			$query_res = db_select('view_most_active_author', 'v')
			->fields('v', array('id', 'tot'))
			->orderBy('tot', 'DESC')
			->range(0, $max_element)
			->execute();
		} catch (Exception $e) {
			watchdog_exception('metadata', $e, 'Get last voted authors!');
		}

		return $query_res;
	}

	/**
	 * Get most viewed metadata.
	 *
	 * @param int $max_element limit results.
	 * @return object query result. Supported row fields: id_metadata, tot.
	 *
	 * @see MetadataStatisticsHelper::_getTopMetadataForEvent
	 *
	 *  select id_metadata, tot
	 *	from view_metadata_events
	 *	where id_event_type=0
	 *	ORDER BY tot DESC, id_metadata DESC
	 *	LIMIT $max_element;
	 */
	public static function getMostViewedMetadata($max_element=5) {
		return self::_getTopMetadataForEvent($max_element, TableMetadataEvents::VIEW_EVTTYPE);
	}

	/**
	 * Get most viewed metadata.
	 *
	 * @param int $max_element limit results.
	 * @return object query result. Supported row fields: id_metadata, tot.
	 *
	 * @see MetadataStatisticsHelper::_getTopMetadataForEvent
	 */
	public static function getMostSearchedMetadata($max_element=5) {
		return self::_getTopMetadataForEvent($max_element, TableMetadataEvents::SEARCH_EVTTYPE);
	}

	/**
	 * Get statistics.
	 *
	 * @return array associative array contained follow fields:
	 * 				- datasets_count: total number of datasets
	 * 				- applications_count: total number of application attachments.
	 * 				- reports_count: total number of reports attachments.
	 * 				- geographic_count: total number of geographic attachments.
	 * 				- alphanumerc_count: total number of alphanumeric attachments
	 * 				- view_count: total number of visualizations.
	 * 				- download_count: total number of download.
	 * 				- search_count: total number of search.
	 * 				- vote_count: total number of user votes.
	 * 				- authors_count: total number of authors.
	 *
	 */
	public static function getMainStatistics() {
		$result = array(
				'datasets_count' => 0,
				'applications_count' => 0,
				'reports_count' => 0,
				'geographic_count' => 0,
				'alphanumerc_count' => 0,
				'alphanumerc_count' => 0,
				'view_count' => 0,
				'download_count' => 0,
				'search_count' => 0,
				'vote_count' => 0,
				'authors_count' => 0,
		);

		try {
			// get total number of datasets.
			$query = db_select('metadata', 'm')
			->condition('status', TableMetadata::APPROVED, '=')
			->condition('visibility', TableMetadata::VISIBLE, '=');
			$query->addExpression('count(*)');
			$result['datasets_count'] = $query->execute()->fetchField();
		} catch (Exception $e) {
			watchdog_exception('metadata', $e);
		}

		try {
			// get total number of application attachment.
			$query = db_select('attach', 'a');
			$query->innerJoin('metadata', 'm', 'a.id_metadata=m.nid');
			$query->addExpression('count(*)');
			$query->condition('m.status', TableMetadata::APPROVED, '=')
			->condition('m.visibility', TableMetadata::VISIBLE, '=')
			->condition('a.id_attach_type', TableAttach::TYPE_APPLICATION, '=');
			$result['applications_count'] = $query->execute()->fetchField();
		} catch (Exception $e) {
			watchdog_exception('metadata', $e);
		}

		try {
			// get total number of report attachment.
			$query = db_select('attach', 'a');
			$query->innerJoin('metadata', 'm', 'a.id_metadata=m.nid');
			$query->addExpression('count(*)');
			$query->condition('m.status', TableMetadata::APPROVED, '=')
			->condition('m.visibility', TableMetadata::VISIBLE, '=')
			->condition('a.id_attach_type', TableAttach::TYPE_REPORT, '=');
			$result['reports_count'] = $query->execute()->fetchField();
		} catch (Exception $e) {
			watchdog_exception('metadata', $e);
		}

		try {

			// get total number of geographic attachments.
			// get geographic terms id
			$tids = array_keys(taxonomy_get_term_by_name('Cartografico'));
			// count geographic metadata.
			$query = db_select('taxonomy_index', 't')
			->condition('t.tid', $tids);
			$query->addExpression('count(*)');
			$query->innerJoin('metadata', 'm', 't.nid=m.nid');
			$query->condition('m.status', TableMetadata::APPROVED, '=')
			->condition('m.visibility', TableMetadata::VISIBLE, '=');

			$result['geographic_count'] = $query->execute()->fetchField();


		} catch (Exception $e) {
			watchdog_exception('metadata', $e);
		}

		try {
			// get total number of alphanumerc attachments.
			// get alphanumeric terms id
			$tids = array_keys(taxonomy_get_term_by_name('Tabellare'));
			// count alphanumeric metadata.
			$query = db_select('taxonomy_index', 't')
			->condition('tid', $tids);
			$query->addExpression('count(*)');
			$query->innerJoin('metadata', 'm', 't.nid=m.nid');
			$query->condition('m.visibility', TableMetadata::VISIBLE, '=')
			->condition('m.status', TableMetadata::APPROVED, '=');

			$result['alphanumerc_count'] = $query->execute()->fetchField();
		} catch (Exception $e) {
			watchdog_exception('metadata', $e);
		}

		try {
			// get total number of user visualization.
			$query = db_select('view_metadata_events', 'v')
			->condition('id_event_type',  TableMetadataEvents::VIEW_EVTTYPE)
			->groupBy('id_event_type');
			$query->addExpression('sum(tot)');

			$result['view_count'] = $query->execute()->fetchField();
		} catch (Exception $e) {
			watchdog_exception('metadata', $e);
		}

		try {
			// get total number of user download.
			$query = db_select('view_metadata_events', 'v')
			->condition('id_event_type',  TableMetadataEvents::DOWNLOAD_EVTTYPE)
			->groupBy('id_event_type');
			$query->addExpression('sum(tot)');

			$result['download_count'] = $query->execute()->fetchField();
		} catch (Exception $e) {
			watchdog_exception('metadata', $e);
		}

		try {
			// get total number of user search.
			$query = db_select('view_metadata_events', 'v')
			->condition('id_event_type',  TableMetadataEvents::SEARCH_EVTTYPE)
			->groupBy('id_event_type');


			$query->addExpression('sum(tot)');

			$result['search_count'] = $query->execute()->fetchField();
		} catch (Exception $e) {
			watchdog_exception('metadata', $e);
		}

		try {
			// get total number of user vote.
			$query = db_select('metadata', 'm')
			->condition('m.status', TableMetadata::APPROVED, '=')
			->condition('m.visibility', TableMetadata::VISIBLE, '=');
			$query->innerJoin('votingapi_vote', 'v', 'm.nid=v.entity_id');
			$query->addExpression('count(*)');

			$result['vote_count'] = $query->execute()->fetchField();
		} catch (Exception $e) {
			watchdog_exception('metadata', $e);
		}

		try {
			// get total number of active author.
			$subquery = db_select('metadata', 'M')
			->fields('M', array('id_author'))
			->condition('M.status', TableMetadata::APPROVED, '=')
			->condition('M.visibility', TableMetadata::VISIBLE, '=')
			->groupBy('id_author');

			$query = db_select('author', 'a');
			$query->addExpression('count(*)');
			$query->innerJoin($subquery, 'M', ' a.id=M.id_author');
			$result['authors_count'] = $query->execute()->fetchField();
		} catch (Exception $e) {
			watchdog_exception('metadata', $e);
		}

		return $result;
	}

	/**
	 * Get most downloaded metadata.
	 *
	 * @param int $max_element limit results.
	 * @return object query result. Supported row fields: id_metadata, tot.
	 *
	 * @see MetadataStatisticsHelper::_getTopMetadataForEvent
	 *
	 */
	public static function getMostDownloadedMetadata($max_element=5) {
		return self::_getTopMetadataForEvent($max_element, TableMetadataEvents::DOWNLOAD_EVTTYPE);
	}

	/**
	 * Get best metadata for input event type.
	 *
	 * @param int $max_element limit results.
	 * @param int $event_type metadata event type.
	 * @return object query result. Supported row fields: id_metadata, tot.
	 *
	 *
	 *  select id_metadata, tot
	 *	from view_metadata_events
	 *	where id_event_type=$event_type
	 *	ORDER BY tot DESC, id_metadata DESC
	 *	LIMIT $max_element;
	 */
	protected static function _getTopMetadataForEvent($max_element, $event_type) {
		$query_res = NULL;

		try {
			$query = db_select('view_metadata_events', 'v')
			->fields('v', array('id_metadata', 'tot'));
			$query->innerJoin('metadata', 'M', 'v.id_metadata=M.nid');
			// 				->condition('id_event_type',  $event_type)
			$query_res = $query->where("id_event_type=$event_type AND M.visibility=1 AND M.status=1")
			->orderBy('tot', 'DESC')
			->orderBy('id_metadata', 'DESC')
			->range(0, $max_element)
			->execute();
		} catch (Exception $e) {
			watchdog_exception('metadata', $e, 'Get last events!');
		}

		return $query_res;
	}


}
