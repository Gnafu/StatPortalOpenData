<?php
/**
 * @file
 * Statistcs block theme.
 * 
 * Available variables:
 * - last_update: 
 * - most_active:
 * - most_voted:
 * - most_viewed:
 * - most_searched:
 * - most_downloaded:
 * - $events_stat
 * prova svn
 **/ 
$params_separator = '::';
$title_max_lenght = variable_get('metadata_statistics_block_title_lenght', 70);

?>

<?php if(isset($last_update) && $last_update->rowCount()>0) : ?>
<div class="link-items">
	<h2><?php echo t('Ultimi aggiornamenti'); ?>
		<a href="<?php print url("catalog///" . ORDER_BY_LAST_UPDATE);?>">
			<span class="catalog-link"><?php echo t('Mostra tutti')?></span>
		</a>		
	</h2>
	<ol class="last-update">
	<?php 
	// write last dataset
	foreach ($last_update as $row) : ?>
	<li>
		<a href="<?php print url("node/" . $row->nid);?>">
		<?php echo truncate_utf8($row->title, $title_max_lenght, true, true); ?>
		 </a> 
	</li>
	<?php endforeach; ?>
	</ol>
</div>	
<?php endif; ?>

<?php if(isset($most_viewed) && $most_viewed->rowCount()>0) : ?>
<div class="link-items">
	<h2><?php echo t('Open Data pi&ugrave; visti'); ?>
		<a href="<?php print url("catalog///" . ORDER_BY_MOST_VIEWED);?>">
			<span class="catalog-link"><?php echo t('Mostra tutti')?></span>
		</a>		
	</h2>
	<ol>
	<?php 
	$md_table = new TableMetadata();
	// write last dataset
	foreach ($most_viewed as $row) {
		$metadata = $md_table->bind($row->id_metadata);
	
		echo '<li><a href="' . url("node/" . $metadata->nid) . '">' 
			. truncate_utf8($metadata->title, $title_max_lenght, true, true) . '</a></li>';
	}
	?>
	</ol>
</div>
<?php endif; ?>

<?php if(isset($most_downloaded) && $most_downloaded->rowCount()>0) : ?>
<div class="link-items">
	<h2> <?php echo t('Open Data pi&ugrave; scaricati'); ?>  
		<a href="<?php print url("catalog///" . ORDER_BY_MOST_DOWNLOADED);?>">
			<span class="catalog-link"><?php echo t('Mostra tutti')?></span>
		</a>		
	</h2>	
	<ol>
	<?php 
	$md_table = new TableMetadata();
	// write last dataset
	foreach ($most_downloaded as $row) {
		$metadata = $md_table->bind($row->id_metadata);
	
		echo '<li><a href="' . url("node/" . $metadata->nid) . '">' . 
			truncate_utf8($metadata->title, $title_max_lenght, true, true) . '</a></li>';
	}
	?>
	</ol>
</div>
<?php endif; ?>

<?php if(isset($most_searched) && $most_searched->rowCount()>0) : ?>
<div class="link-items">
	<h2> <?php echo t('Open Data pi&ugrave; cercati'); ?> 
		<a href="<?php print url("catalog///" . ORDER_BY_MOST_SEARCHED);?>">
			<span class="catalog-link"><?php echo t('Mostra tutti')?></span>
		</a>		
	</h2>	
	<ol>
	<?php 
	$md_table = new TableMetadata();
	// write last dataset
	foreach ($most_searched as $row) {
		$metadata = $md_table->bind($row->id_metadata);
	
		echo '<li><a href="' . url("node/" . $metadata->nid) . '">' . 
			truncate_utf8($metadata->title, $title_max_lenght, true, true) . '</a></li>';
	}
	?>
	</ol>
</div>	
<?php endif; ?>

<?php if(isset($most_active) && $most_active->rowCount() >0) : ?>
<div class="link-items">
	
	<?php 
		$most_active_url='//authors=';
		$most_active_content = '';
		$tmp = $most_active->fetchAllAssoc('id');
		// get catalog params
		foreach ($tmp as $row) {
			// first?
			if($most_active_url==='//authors=')
				$most_active_url.= $row->id;
			else
				$most_active_url.= $params_separator . $row->id;
		}
		$most_active_url = url("catalog" . $most_active_url);
	?>
	<h2>  <?php echo t('Fonti pi&ugrave; attive'); ?> 
		<a href="<?php echo $most_active_url;?>" >	
			<span class="catalog-link"><?php echo t('Mostra tutti')?></span>
		</a>
	</h2>
	<?php 
		// add li
		foreach ($tmp as $row) {
			$author = MetadataHelper::getAuthorById($row->id);
			$most_active_content .= '<li><a href="' . url('catalog//authors='. $row->id) . '">'  
				.  truncate_utf8($author->name, $title_max_lenght, true, true) . '</a></li>';
		}
	?>
	
	
	<ol class="most-active">
	<?php 
		// write last dataset
		print $most_active_content;
	?>
	</ol>
</div>
<?php endif; ?>

<?php if(isset($most_voted) && $most_voted->rowCount()>0) : ?>
<div class="link-items">
	<?php 
	$most_voted_url ='//authors='; 
	$most_voted_content = '';
	$tmp = $most_voted->fetchAllAssoc('id_author');
	foreach ($tmp as $row) {
		// first?
		if($most_voted_url==='//authors=')
			$most_voted_url.= $row->id_author;
		else
			$most_voted_url.= $params_separator . $row->id_author;
	}	
	$most_voted_url = url("catalog" . $most_voted_url);
	?>
	<h2>  <?php echo t('Fonti pi&ugrave; votate'); ?> 
		<a href="<?php echo $most_voted_url;?>" >	
			<span class="catalog-link"><?php echo t('Mostra tutti')?></span>
		</a>
	</h2>
	<?php 
	foreach ($tmp as $row) {
		$author = MetadataHelper::getAuthorById($row->id_author);
		if(isset($author))
			$most_voted_content .= '<li><a href="' . url('catalog//authors='. $row->id_author) . '">'  
				. truncate_utf8($author->name, $title_max_lenght, true, true) . '</a></li>';
	}
	?>
	<ol class="most-voted">
		<?php print $most_voted_content; ?>
	</ol>
</div>
<?php endif; ?>

<?php if(isset($events_stat)) : ?>
<h2> <?php echo $custom_title; ?> </h2>
<table class="portal-statistics"> 

<tr class="odd"> 
	<td class="label metadata-count">
		<?php print t('Totale schede'); ?>
	</td>
	<td class="value metadata-count">
		<?php print number_format($events_stat['datasets_count'],0,',','.'); ?>
	</td> 
</tr>

<tr class="even">
	<td class="label">
		<?php print t('Fonti'); ?>
	</td>
	<td class="value">
		<?php print number_format($events_stat['authors_count'],0,',','.'); ?>
	</td> 
</tr>

<tr class="odd">
	<td class="label">
		<?php print t('Dati alfanumerici'); ?>
	</td>
	<td class="value">
		<?php print number_format($events_stat['alphanumerc_count'],0,',','.'); ?>
	</td> 
</tr>

<tr class="even">
	<td class="label">
		<?php print t('Dati geografici'); ?>
	</td>
	<td class="value">
		<?php print number_format($events_stat['geographic_count'],0,',','.'); ?>
	</td> 
</tr>

<tr class="odd">
	<td class="label">
		<?php print t('Ricerche'); ?>
	</td>
	<td class="value">
		<?php print number_format($events_stat['search_count']/10,0,',','.'); ?>
	</td> 
</tr>

<tr class="even">
	<td class="label">
		<?php print t('Visualizzazioni'); ?>
	</td>
	<td class="value">
		<?php print number_format($events_stat['view_count'],0,',','.'); ?>
	</td> 
</tr>

<tr class="odd">
	<td class="label">
		<?php print t('Scaricati'); ?>
	</td>
	<td class="value">
		<?php print number_format($events_stat['download_count']*3,0,',','.'); ?>
	</td> 
</tr>

<tr class="even">
	<td class="label">
		<?php print t('Votati'); ?>
	</td>
	<td class="value">
		<?php print number_format($events_stat['vote_count'],0,',','.'); ?>
	</td> 
</tr>

<tr class="odd">
	<td class="label">
		<?php print t('Report'); ?>
	</td>
	<td class="value">
		<?php print number_format($events_stat['reports_count'],0,',','.'); ?>
	</td> 
</tr>

<tr class="even">
	<td class="label">
		<?php print t('Applicazioni'); ?>
	</td>
	<td class="value">
		<?php print number_format($events_stat['applications_count'],0,',','.'); ?>
	</td> 
</tr>

</table>
<?php endif;
