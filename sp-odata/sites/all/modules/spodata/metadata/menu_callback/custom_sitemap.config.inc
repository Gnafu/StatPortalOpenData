<?php
/**
 * Configurazione output sitemap.
 * 
 * Istruzioni
 * Aggiungere un elemento per ogni voce di menu (Menu Principale) che si vuole aggiungere alla sitemap.
 * Per ogni voce di menu si dovrà aggiungere un'entry con la seguente struttura:
 * 
 * '<item_menu_name>' => array(
 *				'changefreq' => '<change_freq>',
 *				'priority' => '<priority>',
 *		),
 *
 *	Dove:
 *	- <item_menu_name> è il nome, visualizzato all'utente finale, della voce di menu;
 *	- <change_freq> uno dei valori concessi dallo schema (always, hourly, daily, weekly, monthly, yearly, never);
 *	- <priority> un valore nell'intervallo [0.0, 1.0]. Questa voce serve ad indicare ai motori di ricerca le pagine 
 *		che reputi più importanti per i crawler.
 */

global $sitemap_configuration;


$sitemap_configuration = array(
		'Home' => array(
				'changefreq' => 'hourly',
				'priority' => '0.9',
		),
		'Esplora dati' => array(
				'changefreq' => 'always',
				'priority' => '1',
		),
		'Il progetto' => array(
				'changefreq' => 'never',
				'priority' => '0.8',
		),
		'Chi siamo' => array(
				'changefreq' => 'never',
				'priority' => '0.4',
		),
		'Blog' => array(
				'changefreq' => 'daily',
				'priority' => '0.7',
		),
		'Forum' => array(
				'changefreq' => 'daily',
				'priority' => '0.7',
		),
		'FAQ' => array(
				'changefreq' => 'monthly',
				'priority' => '0.4',
		),
		'Links' => array(
				'changefreq' => 'monthly',
				'priority' => '0.3',
		),
		'Cerca nel sito' => array(
				'changefreq' => 'monthly',
				'priority' => '0.3',
		),
		'Contatti' => array(
				'changefreq' => 'monthly',
				'priority' => '0.3',
		),
		// aggiunge alla sitemap tutte le schede dato visibili
		'Scheda dato' => array(
				'changefreq' => 'monthly',
				'priority' => 1,
		),
		// aggiunge alla sitemap il link con l'elenco di collegamenti al catalogo dati con filtri
		'opendata_list' => array(
				'changefreq' => 'daily',
				'priority' => 0.5,
		),
);