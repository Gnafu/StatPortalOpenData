<?php
/**
 * Template file.
 * 
 * @param stdClass $rows table alias row to fetch
 * @param array $active_taxonomies active taxonomies id (tid)
 * @param array $active_authors active authors id
 */
if(!isset($rows))
	return;


?>

<p style="padding-left: 8px;" class="description">
La pagina mostra tutti i dati aperti suddivisi secondo le diverse categorie e parole chiave.
</p>

<ul>

<?php while($row=$rows->fetch()) : ?>

	<?php 
		$pos = strpos($row->source, 'term=');
		$tax_term = $pos!=false ? substr($row->source, strpos($row->source, 'term=')+5) : $pos;
		
		$author_id=false;
		if($tax_term==false)
			$author_id = substr($row->source, strpos($row->source, 'authors=')+8);
		
		if(($tax_term!=false && in_array($tax_term, $active_taxonomies)) || 
			($author_id!=false && in_array($author_id, $active_authors))) : ?>
			<li><a href="<?php echo url($row->alias, array('absolute'=>true)); ?>"><?php echo $row->alias; ?></a></li>
		<?php endif; ?>
		

<?php endwhile; ?>
</ul>
<?php 