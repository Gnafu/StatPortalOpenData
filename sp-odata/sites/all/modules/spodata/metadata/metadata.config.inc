<?php
/**
 * @file
 * Metadata configuration
 */

define('MODULE', 'metadata');

define('ORDER_BY_RELEVANCE', 0);
define('ORDER_BY_TITLE', 1);
define('ORDER_BY_TYPE', 2);
define('ORDER_BY_FORMAT', 3);
define('ORDER_BY_RATING', 4);
define('ORDER_BY_MOST_DOWNLOADED', 5);
define('ORDER_BY_LAST_UPDATE', 6);
define('ORDER_BY_MOST_VIEWED', 7);
define('ORDER_BY_MOST_SEARCHED', 8);

function get_order_types() {
	return $search_type = array (
			ORDER_BY_RELEVANCE => t('Rilevanza'),
			ORDER_BY_TITLE => t('Titolo'),
			ORDER_BY_RATING => t('Voto utenti'),
			ORDER_BY_LAST_UPDATE => t('Data aggiornamento'),
			ORDER_BY_MOST_VIEWED => t('Numero visualizzazioni'),
			ORDER_BY_MOST_SEARCHED => t('Numero ricerche'),
			ORDER_BY_MOST_DOWNLOADED => t('Numero download'),
			ORDER_BY_TYPE => t('Tipo'),
			ORDER_BY_FORMAT => t('Formato'),
	);
}

function get_metadata_term() {
	return array(
			// lu_category terms
			'metadata_lu_category' => array( 'Ambiente', 'Ecologia', 'Popolazione', 'Altro'),
			// lu_type terms
			'metadata_lu_type' => array( 'Demografici', 'Altro' ),
			// metadata_lu_license
			'metadata_lu_license' => array('CC BY - Libero riuso citando la fonte', 
					'CC BY-NC - No uso commerciale', 
					'CC BY-NC-ND - No uso commerciale, no opere derivate', 
					'CC BY-NC-SA - No uso commerciale, condivisione con la stessa licenza', 
					'CC BY-ND - No opere derivate', 
					'CC BY-SA - Condivisione con la stessa licenza', 
					'IODL v.2 - Italian OpenData License 2.0 (simile a CC BY)', 
					'Non specificato', 
					'ODbL - Open Data Commons Open Database License',
					'IODL v.1 - Italian OpenData License 1.0 (compatibile CC BY SA e ODbL)',
					'CC Zero - No Copyright'),
			// lu_format terms
			'metadata_lu_format' => array( 
				'Dato' => array( 
					'Tabellare',
					'Cartografico',
				),
				'File' => array(
					'CSV',
					'XML/Atom',
					'JSON',
					'OData',
					'XLS',
					'XLSX',
					'MDB',
					'SHP',
					'DBF',
					'PDF',
					'DOC',
					'KML',
					'Image',	
				),
				'Applicazione' => array(
					'Web',
					'Desktop',
				),
				'Non Definito'
			),
			// lu_data_sources
			'metadata_lu_datasources' => array(
				'DatiOpen.it',		
			),	
	);
}

function metadata_get_licenses()
{
	return array( 
			'CC BY - Libero riuso citando la fonte' => array(
					'alias' => 'Attribuzione',
					'link' => 'http://creativecommons.org/licenses/by/3.0',
					'logo' => 'CC_BY_80x15.png'
					),
			'CC BY-NC - No uso commerciale' => array(
					'alias' => 'Attribuzione - Non commerciale',
					'link' => 'http://creativecommons.org/licenses/by-nc/3.0',
					'logo' => 'CC_BY-NC_80x15.png'
					),
			'CC BY-NC-ND - No uso commerciale, no opere derivate' => array(
					'alias' => 'Attribuzione - Non commerciale - Non opere derivate',
					'link' => 'http://creativecommons.org/licenses/by-nc-nd/3.0',
					'logo' => 'CC_BY-NC-ND_80x15.png'
					),
			'CC BY-NC-SA - No uso commerciale, condivisione con la stessa licenza' => array(
					'alias' => 'Attribuzione - Non commerciale - Condividi allo stesso modo',
					'link' => 'http://creativecommons.org/licenses/by-nc-sa/3.0',
					'logo' => 'CC_BY-ND-SA_80x15.png'
					),
			'CC BY-ND - No opere derivate' => array(
					'alias' => 'Attribuzione - Non opere derivate',
					'link' => 'http://creativecommons.org/licenses/by-nd/3.0',
					'logo' => 'CC_BY-ND_80x15.png'
					),
			'CC BY-SA - Condivisione con la stessa licenza' => array(
					'alias' => 'Attribuzione - Condividi allo stesso modo',
					'link' => 'http://creativecommons.org/licenses/by-sa/3.0',
					'logo' => 'CC_BY-SA_80x15.png'
					),
			'IODL v.2 - Italian OpenData License 2.0 (simile a CC BY)' => array(
					'alias' => 'Italian Open Data License 2.0',
					'link' => 'http://www.dati.gov.it/iodl/2.0/',
					'logo' => 'IO-DL_80X15.png'
			),
			'IODL v.1 - Italian OpenData License 1.0 (compatibile CC BY SA e ODbL)' => array(
					'alias' => 'Italian Open Data License',
					'link' => 'http://www.formez.it/iodl/',
					'logo' => 'IO-DL_80X15.png'
					),
			'Non specificato' => array(
					'alias' => '',
					'link' => '',
					'logo' => 'NoLicense_80X15.png'
					),
			'ODbL - Open Data Commons Open Database License' => array(
					'alias' => 'Open Data Commons Open Database License',
					'link' => 'http://opendatacommons.org/licenses/odbl/',
					'logo' => 'IO-DL_ODbL_80X15.png'
					),
			'CC Zero - No Copyright' => array(
					'alias' => 'CC Zero - No Copyright',
					'link' => 'http://wiki.creativecommons.org/CC0',
					'logo' => 'cc-zero.png'
			),
	);
}

function metadata_get_default_authors()
{
	return array (
			array(
					'name' => 'Provincia di Livorno',
					'logo' => 'logo_livorno.png',
					'website' => 'www.provincia.livorno.it',
					'mail' => 'urp@provincia.livorno.it'
			),
			array(
					'name' => 'Provincia di Pisa',
					'logo' => 'logo_pisa.png',
					'website' => 'www.provincia.pisa.it',
					'mail' => 'protocollo@provpisa.pcertificata.it '
			),
			array(
					'name' => 'Comune di Livorno',
					'logo' => 'logo_clivorno.png',
					'website' => 'www.comune.livorno.it',
					'mail' => 'urp@comune.livorno.it'
			),
			array(
					'name' => 'Comune di Pisa',
					'logo' => 'logo_livorno.png',
					'website' => 'www.comune.pisa.it',
					'mail' => 'urp@comune.pisa.it'
			),
			array(
					'name' => 'StatPortal',
					'logo' => 'prodotto_StatPortal.png',
					'website' => 'http://www.sister.it/tabid/290/Default.aspx',
					'mail' => 'info@sister.it',
			),
	);
}


function metadata_get_fields()
{
	return $fields = array('nid' ,
		'title',
		'identifier',
		'id_creator',
		'rights',
		'note',
		'description',
	);	
}
