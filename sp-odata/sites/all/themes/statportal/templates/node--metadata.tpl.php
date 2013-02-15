<?php
/**
 * @file
 * Default theme implementation to display a node.
 *
 * Available variables:
 * - $title: the (sanitized) title of the node.
 * - $content: An array of node items. Use render($content) to print them all,
 *   or print a subset such as render($content['field_example']). Use
 *   hide($content['field_example']) to temporarily suppress the printing of a
 *   given element.
 * - $user_picture: The node author's picture from user-picture.tpl.php.
 * - $date: Formatted creation date. Preprocess functions can reformat it by
 *   calling format_date() with the desired parameters on the $created variable.
 * - $name: Themed username of node author output from theme_username().
 * - $node_url: Direct url of the current node.
 * - $display_submitted: Whether submission information should be displayed.
 * - $submitted: Submission information created from $name and $date during
 *   template_preprocess_node().
 * - $classes: String of classes that can be used to style contextually through
 *   CSS. It can be manipulated through the variable $classes_array from
 *   preprocess functions. The default values can be one or more of the
 *   following:
 *   - node: The current template type, i.e., "theming hook".
 *   - node-[type]: The current node type. For example, if the node is a
 *     "Blog entry" it would result in "node-blog". Note that the machine
 *     name will often be in a short form of the human readable label.
 *   - node-teaser: Nodes in teaser form.
 *   - node-preview: Nodes in preview mode.
 *   The following are controlled through the node publishing options.
 *   - node-promoted: Nodes promoted to the front page.
 *   - node-sticky: Nodes ordered above other non-sticky nodes in teaser
 *     listings.
 *   - node-unpublished: Unpublished nodes visible only to administrators.
 * - $title_prefix (array): An array containing additional output populated by
 *   modules, intended to be displayed in front of the main title tag that
 *   appears in the template.
 * - $title_suffix (array): An array containing additional output populated by
 *   modules, intended to be displayed after the main title tag that appears in
 *   the template.
 *
 * Other variables:
 * - $node: Full node object. Contains data that may not be safe.
 * - $type: Node type, i.e. story, page, blog, etc.
 * - $comment_count: Number of comments attached to the node.
 * - $uid: User ID of the node author.
 * - $created: Time the node was published formatted in Unix timestamp.
 * - $classes_array: Array of html class attribute values. It is flattened
 *   into a string within the variable $classes.
 * - $zebra: Outputs either "even" or "odd". Useful for zebra striping in
 *   teaser listings.
 * - $id: Position of the node. Increments each time it's output.
 *
 * Node status variables:
 * - $view_mode: View mode, e.g. 'full', 'teaser'...
 * - $teaser: Flag for the teaser state (shortcut for $view_mode == 'teaser').
 * - $page: Flag for the full page state.
 * - $promote: Flag for front page promotion state.
 * - $sticky: Flags for sticky post setting.
 * - $status: Flag for published status.
 * - $comment: State of comment settings for the node.
 * - $readmore: Flags true if the teaser content of the node cannot hold the
 *   main body content.
 * - $is_front: Flags true when presented in the front page.
 * - $logged_in: Flags true when the current user is a logged-in member.
 * - $is_admin: Flags true when the current user is an administrator.
 *
 * Field variables: for each field instance attached to the node a corresponding
 * variable is defined, e.g. $node->body becomes $body. When needing to access
 * a field's raw values, developers/themers are strongly encouraged to use these
 * variables. Otherwise they will have to explicitly specify the desired field
 * language, e.g. $node->body['en'], thus overriding any language negotiation
 * rule that was previously applied.
 *
 * @see template_preprocess()
 * @see template_preprocess_node()
 * @see template_process()
 */

/**
 * NOTA: per personalizzare l'ordinamento dei vari field si possono sfruttare le funzioni
 * hide($content[field_type]) per nascondere temporaneamente o definitivamente il field (imposta un ) [set printed = true]
 * show($content[field_type]) per mostrare il field [set printed = false] 
 * print render($content); stampa su output tutti i field su cui non è stata mai invocata una hide 
 * 							o quelli sui cui è stata invocata una show. [render di tutti i field con printed==false]
 **/ 

global $base_url;
?>
<!-- TEMPLATES -->
<script id="dimensionTemplate" type="text/x-jquery-tmpl">
	<option class="field dimension" data-alias="${getAlias()}" data-id="${getId()}" name="${getName()}">${getAlias()}</option>
</script>
<script id="measureTemplate" type="text/x-jquery-tmpl">
	<option class="field measure" data-alias="${getAlias()}" data-id="${getId()}" name="${getName()}">${getAlias()}</option>
</script>
<script id="fieldTemplate" type="text/x-jquery-tmpl">
	<option class="field genericColumn" data-id="${getId()}" name="${getName()}">${getAlias()}</option>
</script>
<?php

// autore che ha caricato la scheda
$publishes = user_load($node->uid);
$keywords = array();
?>
<div id="initial_viewer_loading" class="initial_viewer_loading"></div>
<div id="node-<?php print $node->nid; ?>"
	class="<?php print $classes; ?> clearfix" <?php print $attributes; ?> itemscope itemtype="http://schema.org/CreativeWork">
	
	<div id="dataset-container" class="initial_metadata_loading" data-id="<?php echo $node->data_attach_params->odata_id; ?>">
		<img width="25" height="25" class="viewer_img_link_button" alt="Ottieni link" title="Ottieni link" onclick="StatPortalOpenData.ODataUtility.toggleExportLink();" src="/sites/all/modules/spodata/metadata/img/link.png" />
		<img width="25" height="25" id="viewer_img_button" class="viewer_img_button" alt="Espandi a tutto schermo" title="Espandi a tutto schermo" onclick="StatPortalOpenData.ODataUtility.toFullscreen();" src="/sites/all/modules/spodata/metadata/viewer/commons/img/fullscreen.png" />
		<ul id="tab_parent_ul">
			<li id="Descrizione" class="Descripton_img"><a href="#main-description"><?php echo t('Descrizione'); ?></a></li>
			<?php
			if($node->hasGrid){
			?>
			<li id="Tabella" class="Table_img">
			<?php echo l(t('Tabella'), $base_url.'/'.drupal_get_path('module', 'metadata') . '/viewer/multidimensional_viewer/multidimensional_viewer.php?id='.$node->data_attach_params->odata_id.''); ?>
			</li>
			<?php 
			}
			// se il dato è geografico si mostra il tab relativo alla mappa 
			if($node->hasMap2D ){
			?>

			<li id="Mappa" class="Map_img">
			<?php echo l(t('Mappa'), $base_url.'/'.drupal_get_path('module', 'metadata') . '/viewer/geo_viewer/geo_viewer.php?id='.$node->data_attach_params->odata_id); ?>
			</li>
			
			<?php
			} 
			if($node->hasMap3D){
			?>
			<li id="Mappa3D" class="Earth_img">
			
			<?php echo l(t('Mappa 3D'), $base_url.'/'.drupal_get_path('module', 'metadata') . '/viewer/earth_viewer/earth_viewer.php?id='.$node->data_attach_params->odata_id.''); ?>
			
			</li>
			<?php
			}
			// se il dato è strutturato (quindi ci sono dimensioni) si mostra il grafico
			if($node->hasGraph){
			?>
			<li id="Grafico" class="Graphics_img">
			<?php echo l(t('Grafico'), $base_url.'/'.drupal_get_path('module', 'metadata') . '/viewer/graph_viewer/graph_viewer.php?id='.$node->data_attach_params->odata_id.''); ?>		
			</li>
			<?php 
			}
			?>
			<?php if($node->hasGrid || $node->hasMap2D || $node->hasMap3D || $node->hasGraph) :?>
				<li id="Scarica" class="Download_img"><a href="#download-area"><?php echo t('Scarica'); ?></a></li>
			<?php endif; ?>
			
			<li id="Commenti" class="Comment_img"><a href="#comments-area"><?php echo t('Commenti'); ?></a></li>
			
		</ul>
		
		<div id="main-description" >
			<div id="left-column">
				<!-- autore -->
				<div class="field metadata-author" itemprop="author" itemscope itemtype="http://schema.org/Organization">
					<div class='field-label'>
						<?php print t('FONTE'); ?>
					</div>
					<?php 
					$aid = $node->author['id'];
					$author_url = url(drupal_get_path_alias("catalog//authors=$aid"), array('absolute' => TRUE));
					?>
					<a href="<?php echo $author_url; ?>">
						<span id="field-author-label" itemprop="name"><?php echo  $node->author['name']; ?> </span></a><br>
					<?php
						print l(t("Sito web"), $node->author['website'], array(
							'attributes' => array ('class' => 'author-website', 'rel' => 'nofollow',
							'itemprop' => 'url', 'target' => '_blank'),
						));
						print "/";
						print l(t("Indirizzo mail"), 'mailto:'.$node->author['mail'], array(
								'attributes' => array ('class' => 'author-mail-address', 'rel' => 'nofollow',
								'itemprop' => 'email'),
						));
					?>
					
				</div>
				
				<!-- fivestars -->
					<!-- http://support.google.com/webmasters/bin/answer.py?hl=en&answer=176035  -->
					<span itemprop="aggregateRating" itemscope itemtype="http://schema.org/AggregateRating">
						<meta itemprop="description" content="Votato dagli utenti"/>	
						<meta itemprop="bestRating" content="100"/>
						<?php 
							$vote = isset($content['metadata_fivestars'][0]) && isset($content['metadata_fivestars'][0]['vote']['#values']['average']) ? $content['metadata_fivestars'][0]['vote']['#values']['average'] : 70; 
							$vote_count = isset($content['metadata_fivestars'][0]) && isset($content['metadata_fivestars'][0]['vote']['#values']['count']) ? $content['metadata_fivestars'][0]['vote']['#values']['count'] : 1;
						?>						
						<meta itemprop="ratingValue" content="<?php echo $vote;?>"/>
						<meta itemprop="ratingCount" content="<?php echo $vote_count;?>"/>
						<meta itemprop="reviewCount" content="<?php echo $vote_count;?>"/>
						<meta itemprop="worstRating" content="0"/>
					</span>
				<?php 
					$content['metadata_fivestars']['#title'] =  t('VALUTAZIONE');
					print render($content['metadata_fivestars']);
				?> 
				
				<!-- Dettaglio dato -->
				<div class="metadata-details">
					
					<!-- Creation date -->
					<div class="field metadata-last-change"> 
						<div class='field-label'>
							<?php print t('DATA CREAZIONE'); ?>
						</div>
						<?php print '<span id="creationdate">' . format_date($node->created, 'custom', 'd/M/Y') . '</span>'; ?>
					</div>
					
					<!-- Last update -->
					<div class="field metadata-last-change"> 
						<div class='field-label'>
							<?php print t('AGGIORNATO AL'); ?>
						</div>
						<?php print '<span id="lastupdate">' . format_date($node->changed, 'custom', 'd/M/Y') . '</span>' ; ?>
					</div>
					
					
					<!-- Numero allegati -->
<!-- 					<div class="field metadata-attach-counter"> -->
<!-- 						<div class="field-label"> -->
							<?php 
// 								print '' . t('ALLEGATI') 
// 							?>
<!-- 						</div> -->
						
						<?php 
// 							print '<span id="attachment-count">' . (isset($node->attachments) ? count($node->attachments) : 0) . '</span>'; 
// 						?>
<!-- 					</div> -->
					
					<!-- Categoria -->
					<?php 
						foreach ($content['metadata_category']['#items'] as $key => $value) {
							$content['metadata_category'][$key]['#attributes'] = array( 'rel' => 'nofollow' );
							$keywords[] = $content['metadata_category'][$key]['#title'];
						}				
					
						$content['metadata_category']['#title'] = t('TEMATICA');
						print  render($content['metadata_category']); 
					?>
					
					<!-- Parole chiave -->
					<?php 
						if (isset($content['metadata_type'])) {
							foreach ($content['metadata_type']['#items'] as $key => $value) {
								$content['metadata_type'][$key]['#attributes'] = array( 'rel' => 'nofollow' );
								$keywords[] = $content['metadata_type'][$key]['#title'];
							}
						
							$content['metadata_type']['#title'] = t('PAROLE CHIAVE');
							print render($content['metadata_type']);
						}
					?>
					
					<!-- Tipologia allegati -->
					<?php 
						foreach ($content['metadata_format']['#items'] as $key => $value) {
							$content['metadata_format'][$key]['#attributes'] = array( 'rel' => 'nofollow' );
						}
					
						$content['metadata_format']['#title'] = t('FORMATO');
						print render($content['metadata_format']); 
					?>					
					
				</div>

			</div>
			<div id="main">
				<div class="description">
					<div class="field-label">
						<?php print t('Nome:'); ?>
					</div>
					<span itemprop="name"><?php  print render($node->title); ?></span>
				</div>
				
				<div class="description">
					<div class="field-label">
						<?php print t('Descrizione:');?>
					</div>
					<span itemprop="description"><?php print render($content['body']); ?></span>
				</div>
								
				
				<?php  if(isset($node->attachments) && count($node->attachments) > 0) {
					$_extra_attach = '';
					foreach ($node->attachments as $a) {
							if ($a->id_attach_type==2) {
								$_extra_attach .= '<li>';
								$f = file_load($a->fid);
								$_extra_attach .= l(substr($f->filename, strpos($f->filename, '_')+1), file_create_url($f->uri), 
												array('attributes' => array( 'target' => '_blank', 'rel' => 'nofollow')));
								$_extra_attach .= '</li>';
							}							
					}
				} ?>
				<?php  if(isset($_extra_attach) && !empty($_extra_attach)) : ?>
					<div class="extra-attach-link">
						<div class="field-label">
						<?php echo t('Allegati: '); ?>
						</div>
						<ul>
							<?php echo $_extra_attach; ?>
						</ul>
					</div>	
				<?php endif; ?>
				
				<?php if(isset($node->note) && !empty($node->note)): ?>
				<div class="note" >
						<div class="field-label">
							<?php print t('note:'); ?>
						</div>
						<div class="last-change">
							<?php print $node->note; ?>
						</div>
				</div>
				<?php endif; ?>
				
				<?php if(isset($node->ds_relation) && count($node->ds_relation)>0): ?>
					<div class="external-link">
						<div class="field-label">
						<?php echo t('Link al file\pagina del dato di origine: '); ?>
						</div>
						<ul>
							<?php
							foreach($node->ds_relation as $link)
							{
								$url = $link->url;
								$label = (isset($link->label) ? $link->label : $link->url);
								
								print '<li>' . 
									l($label, $url , array(
												'attributes' => array( 'target' => '_blank', 'rel' => 'nofollow'
														)) ) 
								. '</li>';
							}
							?>
						</ul>
					</div>
				<?php endif; ?>
				
				<?php if(isset($node->relation) && count($node->relation)>0): ?>
					<div class="external-link">
						<div class="field-label">
						<?php echo t('Altri link: '); ?>
						</div>
						<ul>
							<?php
							foreach($node->relation as $link)
							{
								//$url = (strpos($link->url, "http://") ? '' : "http://") . $link->url;
								$url = $link->url;
								$label = ((isset($link->label) && !empty($link->label)) ? $link->label : $link->url);
								
								print '<li>' . 
									l($label, $url , array(
												'attributes' => array( 'target' => '_blank', 'rel' => 'nofollow'
														)) ) 
								. '</li>';
								// links comments body metadata_category metadata_type metadata_format metadata_license metadata_fivestars
							}
							?>
						</ul>
						
						
					</div>
				<?php endif; ?>		
				
				<?php if(isset($node->admin_properties) && !empty($node->admin_properties)) : ?>
				<div class='field-label'>
					<?php print  t('Stato').' :'; ?>
				</div>
				<div id="admin-settings">
					<?php print $node->admin_properties; ?>
				</div>
				<?php endif; ?>	
			</div>
			<div id="right-column">
				
				<div class='field-label'>
					<?php 
						print  t('PUBBLICATO DA') ; 
					?>
				</div>
				
				<div class="metadata-publisher">
				<?php if(isset($publishes->picture)) : ?>
					<img src="/sites/default/files/styles/thumbnail/public/pictures/<?php echo $publishes->picture->filename; ?>" class="publisher-image" />
				<?php endif; ?>
					<p class="publisher-label">
						<?php print theme_link(	array( 'path' => 'user/' . $publishes->uid, 
														'text' => $publishes->name, 
														'options' => array(
																'attributes' => array(), 
																'html' => true,
																'rel' => 'nofollow',
															) 
											)); ?>
					</p>
				</div>
				<!-- Licenza -->
					<?php 
					$content['metadata_license']['#title'] = t('LICENZE');
						print render($content['metadata_license']); 
					?>
				<!-- Datasource -->
				<div class="field-datasource">
				<?php 
					if(isset($content['metadata_datasources']))
						print  render($content['metadata_datasources']);
				?>
				</div>
				
				<!-- event statistics -->
				<div class="field-event-statistics">
				<?php 
					if(isset($node->events_count) && count($node->events_count)==1 ) : ?>
						<div class='field-label'>
						<?php 
							print  t('STATISTICHE') ; 
						?>
						</div>
						<table id="md-event-statistics">
							<tbody>
								<tr title="<?php echo t('Numero visualizzazioni');?>">
									<td><?php echo t('Visualizzato'); ?></td>
									<td><?php echo $node->events_count[$node->nid]->view_count; ?></td>
								</tr>
								<tr title="<?php echo t('Numero visualizzazioni nel catalogo dati');?>">
									<td><?php echo t('Ricercato'); ?></td>
									<td><?php echo isset($node->events_count[$node->nid]->search_count) ? intval($node->events_count[$node->nid]->search_count/5) : 0; ?></td>
								</tr>
								<tr title="<?php echo t('Numero download');?>">
									<td><?php echo t('Scaricato'); ?></td>
									<td><?php echo $node->events_count[$node->nid]->download_count; ?></td>
								</tr>
							</tbody>
						</table>
						
				<?php
					endif; 
				?>
				</div>
				
				<div class='field-label'>
					<?php
						if(isset($node->md_related) && is_array($node->md_related)) {
							unset($node->md_related['match_count']);
							unset($node->md_related['page_count']);
							
							if(count($node->md_related)>0)
								print  t('OPENDATA SIMILI');
						}
					?>
				</div>
				<div class="related-metadata">
					<ul>
					<?php 
						
						foreach ($node->md_related as $value) {
							// mi assicuro di non visualizzare tra i dataset correlati
							// quello attualmente visualizzato.
							if($value['node']->nid!=$node->nid) {
								print "<li>" . 
									theme_link(	array( 'path' => $value['link'], 
														'text' => $value['title'], 
														'options' => array(
																'attributes' => array(), 
																'html' => true) 
											)) . 
								"</li>";
							}
						}
					?>
					</ul>
				</div>
			</div>
			<div style="clear: both;"></div>
			
			<div id="show-complete-metadata">
				<?php if($node->data_attach_params->data_description!=false) : ?>
				<!-- INIZIO RENDERIZZAZIONE INFO SULLA STRUTTURA/CONTENUTO DEL FILE -->
				<div id="dataDescriptionContainer">
					<a name="dettagli-sul-dato"></a>
					<span class="data-complete-info">Informazioni aggiuntive sul dato</span>
					Numero di campi: <?php echo $node->data_attach_params->data_description->fieldCount;?><br />
					Numero di righe: <?php echo $node->data_attach_params->data_description->rowsCount;?><br />

					<?php $columns = $node->data_attach_params->data_description->columns; ?>
					<table style='table-layout:fixed;padding:10px;'>
						<tr>
							<th class="thDataDescription" width="80">Campo</th>
							<th class="thDataDescription" width="80">Tipo</th>
							<th class="thDataDescription" width="150">Valori diversi</th>
							<th class="thDataDescription">Contenuti (10 pi&ugrave; frequenti)</th>
						</tr>
						<?php
						for($i = 0; $i < count($columns); ++$i) :
							if(!is_null($columns[$i])) : ?> 
							<tr class="trDataDescription">
							<td width="80"><?php echo $columns[$i]->fieldName; ?> </td>
							<td width="80"><?php echo $columns[$i]->fieldType; ?> </td>
							<td width="150"><?php echo $columns[$i]->differentValuesCount . ' (da ' . $columns[$i]->differentValuesMin . ' a ' . $columns[$i]->differentValuesMax; ?>)</td> 
							<td><?php echo $columns[$i]->firstTenContents; ?> </td>
							</tr>
						<?php endif; endfor;?>
					</table>
				</div>
				<?php endif; ?>
				
				<!-- FINE RENDERIZZAZIONE INFO SULLA STRUTTURA/CONTENUTO DEL FILE -->
				<?php if($node->data_attach_params->data_description==false && $node->data_attach_params->odata_id!=-1 && !$node->data_attach_params->error): ?>
					<?php 
						$cleanurl = variable_get('clean_url', 0);
						$token = $cleanurl ? '?' : '&amp;'; 
					?>
					<a href="<?php echo url($node->identifier, array('absolute'=>true)) . $token .'metadati#dettagli-sul-dato'; ?>" title="Link al metadato">
						Maggiori dettagli su questo open data
					</a>
				<?php elseif ($node->data_attach_params->error)  :?>
					<?php echo $node->data_attach_params->getErrorMessage(); ?>
				<?php endif; ?>
			</div>
		</div>

		<?php if($node->hasGrid || $node->hasMap2D || $node->hasMap3D || $node->hasGraph) :?>
			<div id="download-area">
				<iframe src="<?php 
					echo url('metadata_download_page/' . (isset($node->data_attach_params->fid) ? $node->data_attach_params->fid : -1) . '/' . 
							$node->data_attach_params->file_format . "/$nid/" . $node->data_attach_params->odata_id, array( 'absolute' => true,));
				?>"> </iframe>
			</div>
		<?php endif; ?>

		<div id="comments-area" >
		
			<?php 		
				print render($content['links']); 		
				print render($content['comments']); 
			 ?>
		</div>
	</div>

	<meta itemprop="keywords" content="<?php echo implode(',', $keywords); ?>" />
</div>
<?php 
