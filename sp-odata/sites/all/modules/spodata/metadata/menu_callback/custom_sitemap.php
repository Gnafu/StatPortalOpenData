<?php
include_once 'custom_sitemap.config.inc';

/**
 * Item menu callback. Sitemap page creation.
 */
function metadata_sitemap() {

	// prepare output header
	drupal_add_http_header("Content-type", "text/plain");//"application/force-download"
	drupal_add_http_header("Cache-Control", 'no-cache');
	drupal_add_http_header("Content-disposition", "inline; filename=\"sitemap.xml\"");
	
	// clean output
	$out = ob_get_clean();
	ob_clean();
	flush();
	
	$menu_link = menu_load_links('main-menu');
	
	$md_url_query = db_select('metadata', 'M')
		->fields('M', array('nid', 'identifier', 'last_update'))
		->condition('visibility', 1, '=')
		->condition('status', 1, '=');
	
	$md_url_query->addExpression("(TIMESTAMP 'epoch' + last_update * INTERVAL '1 second')::date", 'formatted_date');
	
		
	$md_url = $md_url_query->execute();
	
	write_metadata_sitemap($menu_link, $md_url	);
	exit;
}

/**
 * Write sitemap on output stream.
 * 
 * @param array $menu_link An array of menu links
 * @param array $md_url An array of metadata item.
 */
function write_metadata_sitemap($menu_link, $md_url) {
	
	global $sitemap_configuration;
	
	echo '<?xml version="1.0" encoding="UTF-8"?>' . 
		 "\n<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">";
	
	// '<url> <loc>http://www.esempio.it/</loc> <lastmod>2005-01-01</lastmod> <changefreq>monthly</changefreq> <priority>0.8</priority> </url>';
	
	// add item menu links
	$link_count = count($menu_link);
	foreach ($menu_link as $link_info) {
		$path = drupal_get_path_alias($link_info['link_path']);
		$url = url($path, array('absolute' => true));
		
		if(isset($sitemap_configuration[$link_info['link_title']])) {
			$freq = isset($sitemap_configuration[$link_info['link_title']]['changefreq']) ? $sitemap_configuration[$link_info['link_title']]['changefreq'] : 'weekly';
			$priority = isset($sitemap_configuration[$link_info['link_title']]['priority']) ? $sitemap_configuration[$link_info['link_title']]['priority'] : 0.5;		
			
			echo "\n\t<url> <loc>$url</loc> <changefreq>$freq</changefreq> <priority>$priority</priority> </url>";//<lastmod>2005-01-01</lastmod>
		}
	}
	
	// add metadata page links
	$freq = isset($sitemap_configuration['Scheda dato']['changefreq']) ? $sitemap_configuration['Scheda dato']['changefreq'] : 'weekly';
	$priority = isset($sitemap_configuration['Scheda dato']['priority']) ? $sitemap_configuration['Scheda dato']['priority'] : 0.5;
	
	foreach ($md_url as $metadata) {
		$path = drupal_get_path_alias('node/' . $metadata->nid);
		$url = url($path, array('absolute' => true));
		$date = $metadata->formatted_date;
		
		if(isset($sitemap_configuration['Scheda dato'])) {
			echo "\n\t<url> <loc>$url</loc> <lastmod>$date</lastmod> <changefreq>$freq</changefreq> <priority>$priority</priority> </url>";
		}
	}
	
	// add opendata_list
	if(isset($sitemap_configuration['opendata_list'])) {
		$path = drupal_get_path_alias('opendata_list');
		$url = url($path, array('absolute' => true));
		$freq = isset($sitemap_configuration['opendata_list']['changefreq']) ? $sitemap_configuration['opendata_list']['changefreq'] : 'daily';
		$priority = isset($sitemap_configuration['opendata_list']['priority']) ? $sitemap_configuration['opendata_list']['priority'] : 1;
		echo "\n\t<url> <loc>$url</loc> <changefreq>daily</changefreq> <priority>$priority</priority> </url>";
	}
	echo "\n</urlset>";
}