<?php 
if(!isset($fid))
	return;
?>


<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML+RDFa 1.0//EN" "http://www.w3.org/MarkUp/DTD/xhtml-rdfa-1.dtd">
<html xmlns="http://www.w3.org/1999/xhtml"	xml:lang="EN" version="XHTML+RDFa 1.0" >

<?php

$module_path = drupal_get_path('module', 'metadata');
$system_path = drupal_get_path('module', 'system');
$theme_path = drupal_get_path('theme','statportal');

?>

<head>

<title>Pagina download</title>
<style type="text/css" media="all">
	@import url(<?php echo file_create_url($system_path . "/system.base.css");?>);
	@import url(<?php echo file_create_url($system_path . "/system.messages.css");?>);
	@import url(<?php echo file_create_url($theme_path . "/css/style.css");?>);
	@import url(<?php echo file_create_url($theme_path . "/css/metadata.css");?>);
	@import url(<?php echo file_create_url($module_path . '/style/spodata_jqtheme/jquery.ui.all.css');?>);
</style>

<script type="text/javascript"
	src="<?php echo file_create_url($module_path . '/js/jquery-1.7.1.min.js'); ?>"></script>
<script type="text/javascript"
	src="<?php echo file_create_url($module_path . '/js/ui/jquery.ui.core.min.js'); ?>"></script>
<script type="text/javascript"
	src="<?php echo file_create_url($module_path . '/js/ui/jquery.ui.widget.min.js'); ?>"></script>
<script type="text/javascript"
	src="<?php echo file_create_url($module_path . '/js/ui/jquery.ui.dialog.min.js'); ?>"></script>
<script type="text/javascript"
	src="<?php echo file_create_url($module_path . '/js/ui/jquery.ui.mouse.min.js'); ?>"></script>
<script type="text/javascript"
	src="<?php echo file_create_url($module_path . '/js/ui/jquery.ui.draggable.min.js'); ?>"></script>
<script type="text/javascript"
	src="<?php echo file_create_url(base_path() . 'misc/jquery.once.js'); ?>"></script>
<script type="text/javascript"
	src="<?php echo file_create_url(base_path() . 'misc/drupal.js'); ?>"></script>
<script type="text/javascript"
	src="<?php echo file_create_url(base_path() . 'misc/form.js'); ?>"></script>
<script type="text/javascript"
	src="<?php echo file_create_url($module_path . '/js/metadata.download.manager.js'); ?>"></script>
</head>
<body class="<?php print $classes; ?>" <?php print $attributes;?>>
	<?php 
	// output validation errors
	$messages = drupal_get_messages(null, true);
	if(count($messages)>0 || isset($url)): ?>
		<div id="messages">
			<div class="section clearfix">
				<?php foreach ($messages as $type => $type_messages) : ?>
				<div class="messages <?php echo $type; ?>">
					<h2 class="element-invisible"><?php echo "$type messages"; ?></h2>
					<ul>
						<?php foreach ($type_messages as $idx => $msg) : ?>
							<li><?php echo $msg; ?></li>
						<?php endforeach; ?>
					</ul>
				</div>
				<?php endforeach; ?>
				<?php if(isset($url)) : ?>
				<div class="messages status">
					<h2 class="element-invisible"><?php echo "odata url"; ?></h2>
					<ul>
						<li>
							<span><?php echo t('Url OData') ?></span>
							<input style="width: 98%" type="text" value="<?php echo $url; ?>" onclick="this.select();" />
						</li>
					</ul>
				</div>
				<?php endif; ?>
			</div>
		</div>
	<?php endif; ?>
	<div id="messages">
		
	</div>
	
	<div id="download-area">
		<?php

		if(isset($fid) && $fid>=0) : ?>
		<div>
			<div class='cool_button' data-value="fid">
				<?php print t(strtoupper($file_format)); ?>
			</div>
			<div class='type-download-description'>
				<?php print t('Formato originale del file.'); ?>
			</div>
		</div>
		<?php endif;?>
		<div>
			<div class='cool_button' data-value="csv">
				<?php print t('CSV'); ?>
			</div>
			<div class='type-download-description'>
				<?php print t('Formato per i file di testo
						aperto e standard apribile con qualsiasi lettore di file testuali (es.
						"Blocco note" o "Excel"). Il separatore tra i valori utilizzato
						&egrave; il "tab".'); ?>
			</div>
		</div>
		<div>
			<div class='cool_button' data-value="atom">
				<?php print t('XML/Atom'); ?>
			</div>
			<div class='type-download-description'>
				<?php print t('Formato per file di testo aperto
						e standard, ha una struttura predisposta per permettere la lettura dei
						dati ad altri software generici. In particolare il formato XML Atom
						&egrave; adatto per la sottoscrizione di contenuti web, come blog o
						testate giornalistiche.'); ?>
			</div>
		</div>
		<div>
			<div class='cool_button' data-value="json">
				<?php print t('JSON'); ?>
			</div>
			<div class='type-download-description'>
				<?php print t('Formato aperto adatto per lo
						scambio dei dati in applicazioni client-server di ultima generazione.
						&Egrave; basato sul linguaggio JavaScript.'); ?>
			</div>
		</div>
		<div>
			<div class='cool_button' data-value="odata">
				<?php print t('OData'); ?>
			</div>
			<div class='type-download-description'>
				<?php print t('Protocollo aperto che nasce per
						standardizzare i meccanismi di accesso e di consumo dei dati
						attraverso l\'utilizzo di tecnologie web ampiamente diffuse come l\'HTTP
						ed il protocollo ATOM.'); ?>
			</div>
		</div>
		<div>
			<div class='cool_button' data-value="lod">
				<?php print t('RDF/XML'); ?>
			</div>
			<div class='type-download-description'>
				<?php print t('Formato che permette lo scambio e il riutilizzo di metadati strutturati e consente l\'interoperabilit&agrave; tra applicazioni che si scambiano informazioni sul Web. &Egrave; inoltre il formato di riferimento per i Linked Open Data.'); ?>
			</div>
		</div>

		<?php if(isset($node->ds_relation) && count($node->ds_relation)>0): ?>
		<?php
		foreach($node->ds_relation as $link)
		{
			$url = $link->url;
			$label = (isset($link->label) ? $link->label : $link->url);

			?>
		<div>
			<a href="<?php echo $url; ?>" rel="nofollow" target="_blank">
				<div class="cool_button" data-value="link">
					<?php print t('Sorgente'); ?>
				</div>
			</a>
			<div class='type-download-description'>
				<?php print t('La sorgente del dato &egrave; la
						pagina da cui &egrave; stato scaricato il dato originale.'); ?>
			</div>
		</div>
		<?php
		}
		?>

		<?php endif; ?>

		<div class="download-area-form" style="display: none;">
			<?php
				print render($download_form);
			?>

		</div>
	</div>
</body>
</html>
