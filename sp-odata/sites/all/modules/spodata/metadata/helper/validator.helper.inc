<?php
/**
 * Implements metadata form validation used on save or update.
 *
 * @author a.mistretta
 */
class MetadataValidator
{
	private $current = NULL;

	const BASETYPE_APPLICATION = 1;
	const BASETYPE_DATA = 0;
	/**
	 * Costrunctor
	 */
	public function __construct() {

	}

	/**
	 * Node validation.
	 *
	 * @param object $node node to validate
	 * @see form_set_error
	 */
	public function validate(&$node)
	{
		if(is_null($node) || is_array($node) )
		{
			return false;
		}

		$this->current = $node;

		// author validation
		$isValidAuthor = $this->validate_author();
		if(!$isValidAuthor)
		{
			form_set_error(t('author'), t('Il campo Fonte &egrave; obbligatorio') );
		}

		// relation validation
		if(isset($node->relation))
		{
			$relations = explode('##END##', $node->relation);
			$rel_count = count($relations)-1;//last result is empty
			$relation_array = array();
			/// url validation
			for ($i=0; $i<$rel_count; $i++)
			{
				list($label, $link) = explode( ':::', $relations[$i]);

				$trimmed_url = trim($link);
				if(!empty($trimmed_url))
				{
					$safe_url = $this->validate_URL($trimmed_url);
					if(!$safe_url)
					{
						// url not valid => set error
						form_set_error(t('relation'), t('Invalid link format: ') . $trimmed_url);
					}
					else
						$relation_array[] = array( 'label' => $label, 'link' => $link);
				}
			}
			$node->relation = $relation_array;
		}

		$node->ds_link = trim($node->ds_link);
		if(isset($node->ds_link) && !empty($node->ds_link) && !$this->validate_URL($node->ds_link))
			form_set_error(t('Link alla sorgente del dato'), t('Formato non valido: ') . $node->ds_link);


		// check attachemnts
		$isValidAttachments = $this->validate_attachment($node);

		return $isValidAuthor;
	}

	/**
	 * Implements author validation.
	 */
	private function validate_author() {
		$id = isset($this->current->metadata_author) ? $this->current->metadata_author : NULL;
		// esiste l'opzione?
		return !is_null($id) ? $id=='new' || ($id>=0 && TableAuthor::author_exists($id) ) : false;
	}

	/**
	 * URL validation.
	 * @return string safe URL.
	 */
	private function validate_URL($url)
	{
		$safe_url = check_url($url);

		if(valid_url($url, true))
			return $safe_url;
		else
			return false;
	}

	/**
	 * Check metadata attachment.
	 *
	 * @param node object $node
	 * @param boolean true on success, false otherwise.
	 */
	private function validate_attachment($node) {
		if($node->basetype==self::BASETYPE_DATA) {
			return $this->validate_data_attachment($node);
		}
		else if($node->basetype==self::BASETYPE_APPLICATION) {
			return $this->validate_application_attachment($node);
		}
		else {
			form_set_error('metadata_node_form', 'Tipo allegato non supportato');
			return false;
		}

	}

	/**
	 * Data md/raw attachment validation
	 * @param object $node
	 */
	private function validate_data_attachment($node) {
		$result = true;
		$is_geographic = 'md_is_geographic';
		$is_alphanumeric = 'md_is_alphanumeric';

		// se entrambi false -> scheda senza dati e con allegati
		if((bool)$node->md_is_geographic || (bool)$node->md_is_alphanumeric ||
				(bool)$node->md_is_structured || (bool)$node->md_is_raw) {
			// odata url validation
			$odata_url = 'odata-url';
			if(!isset($node->odata_url) || empty($node->odata_url)) {
				form_set_error('metadata_node_form', t('Impossibile generare un URL OData.'));
				$result = false;
			}

			// save file if no error
			if(!isset($node->nid) && (!isset($node->fid) || $node->fid<=0)) {
				form_set_error('metadata_node_form', t('Allegato non presente: caricare il file allegato con i dati.'));
			}

			// owner enabled viewer
			if( !(bool)$node->viewer['hasGrid'] && !(bool)$node->viewer['has2DMap'] &&
					!(bool)$node->viewer['has3DMap'] && !(bool)$node->viewer['hasChart']) {
				form_set_error('metadata_node_form', t('Selezionare almento un visualizzatore.'));
				$result = false;
			}
		}
		else {

			// @TODO gestire gli allegati di una scheda - report
			// 		$node->attached_files
			// 			form_set_error('metadata_node_form', t('Allegati ancora da gestire.'));
			$result = $this->validate_application_attachment($node);
		}
	}

	/**
	 * MD Application validation.
	 *
	 * @param object $node
	 */
	private function validate_application_attachment($node) {
		// application info
		// $node->app-name
		// $node->app-description
		// $node->app-web
		// $node->app-format
		return false;
	}
}