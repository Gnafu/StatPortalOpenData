<?php
/**
 * @file
 * Custom taxonomy field theme implementation to display hierarchical terms.
 *
 * Available variables:
 * - $terms: array of term.
 * - $vocabulary: The vocabulary id.
 *
 */
$voc_tree = NULL;

// taxonomy reference base url
$base_url = '?q=taxonomy/term/';
// title attribute value
$title = t('Click per impostare un filtro su questo tag');

$themed_separator_img = theme_image(
		array( 
			'path' => drupal_get_path('module', 'metadata') . '/img/tree-tags-separator.png',
			'alt' => t('child tag'),
			'attributes' => '',
		));

if(isset($terms) && count($terms)>0)
{ ?>
<div class="taxonomy-horizontal-tree">
	<span><?php print t('Formato:');?></span>
	<ul class="taxonomy-horizontal-tree">
	<?php 
		$first = true;
		$vid = variable_get('metadata_lu_format', null);
		$voc = taxonomy_vocabulary_load($vid);
		$file_term = array_values(taxonomy_get_term_by_name(variable_get('metadata_fileformat_file', 'File'), $voc->machine_name));
		foreach($terms as $term) : ?>
			<?php if(!isset($file_term) || $term['taxonomy_term']->name != $file_term[0]->name) : ?>
				<li>
				<?php
	
				$tid = $term['taxonomy_term']->tid;
				$voc = str_replace('_', '-', $term['taxonomy_term']->vocabulary_machine_name);
				$tname =  $term['taxonomy_term']->name;
				
				if($first) {
					$first = false;
				}
				else {
					echo $themed_separator_img;
				}
				?> 
					<span>
						<a class="action-filter" ref="nofollow" title="<?php echo $title; ?>" 
							data-input="<?php echo $tid ?>" data-vocabulary="<?php echo $voc; ?>"
							href="<?php echo $base_url . $tid;  ?>"> 
							<?php echo $tname; ?> 
						</a>
					</span>
				</li>
			<?php
			endif; 
		endforeach;
	?>
  </ul>
</div>
<?php 
}
