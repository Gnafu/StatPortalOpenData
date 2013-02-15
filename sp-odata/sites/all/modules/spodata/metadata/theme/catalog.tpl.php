<?php
/**
 * @file
 * Catalog theme.
 * 
 * Available variables:
 * - $rows: search result rows.
 * - $
 **/

drupal_add_library('metadata', 'catalog');
// drupal_add_js('var StatPortalOpenData = StatPortalOpenData || {};StatPortalOpenData.Catalog = StatPortalOpenData.Catalog || {};','inline');
drupal_add_js('StatPortalOpenData.Catalog.mdcount_by_term = ' . $mdcount_by_term, 'inline');
?>
<div class="order-by-label"><span><?php echo t('Ordinati per'); ?></span>
<?php 
print theme_select(array( 'element' => array(
		'#type' => 'select',
		'#options' => get_order_types(),
		'#title' => t('Ordinati per'),
		'#name' => 'order_by',
		'#value' => $rows['#page_order_type'],
)));

$ajax_animation = theme_image(array( 
		'path' => drupal_get_path('module', 'metadata') . '/img/ajax-search-animation.gif',
				'alt' => t('Ricerca in corso'),
				'attributes' => '',
		));
?>
</div>

<div id="match_count">
<span class="count"></span><span> <?php echo t('dati')?></span><span class="filtered-search"> <?php echo t('per')?></span>
</div>
<div class="ajax-animation" ><p></p> <?php echo $ajax_animation ; ?> </div>
<?php
print '<div id="search-filters-summary"></div>';
global $user;
if(user_access('administrator')) : ?>
<div class="admin-ajax-animation" style="display: none;"><p>Aggiornamento in corso ...</p> <?php echo $ajax_animation ; ?> </div>
<div id="administrator-panel">
	<span id="select-all">Seleziona tutto</span> <span id="deselect">Deseleziona tutto</span> <span id="switch-selection">Inverti selezione</span>
	<!-- action panel -->
	<div class="descendent-menu"><div class="action-menu-label">Azioni</div>
		<div class="action-menu-icon ui-state-default ui-corner-all" title="Seleziona un'azione da eseguire sulla selezione corrente">
			<span class="ui-icon ui-icon-circle-triangle-s"></span>
		</div>
		<ul class="action-items">
			<li id="task-approved">Approva</li>
			<li id="task-rejected">Scarta</li>
			<li id="task-edit">Contatta autore</li>
			<li id="task-hide">Nascondi</li>
			<li id="task-show">Rendi visibile</li>
		</ul>
	</div>
</div>
<div id="task-messages" style="display:none;"><div class="section clearfix">

</div></div>

<?php 
	$form_state = array();
	$admform = drupal_build_form('metadata_admtools_form', $form_state);
	print render($admform);
?>

<?php endif;
print render($rows);

