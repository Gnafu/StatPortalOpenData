<?php
drupal_add_css(drupal_get_path('theme', 'seven') . '/upd.metadata.css', array('type' => 'file', 'group' => CSS_THEME));

$js_obj = 'var StatPortalOpenData = StatPortalOpenData || {}; StatPortalOpenData.UPLOAD_PATH = "' . str_replace('\\', '\\\\', realpath('.')) . '/sites/default/files/metadata_updfile";';
drupal_add_js($js_obj,'inline');

if(!drupal_add_library('metadata', 'etl_wizard'))
	echo t('Errore nel caricamento delle librerie Javascript');
?>
<script id="columnInfoTemplate" type="text/x-jquery-tmpl">
	Colonna ${idx}:
	<input class="etl_checkField" checked="true" type="checkbox" id="cb_${idName}" title="Importa se selezionato" {{if (!isExcludable) }} disabled {{/if}} />
	<input type="text" id="tb_${idName}" value="${name}" maxlength="100" size="100" class="tb_labelField" title="Etichetta" /> 
	<br /> 
</script>

<script id="warningInfoTemplate" type="text/x-jquery-tmpl">
	<div class="etl_warning">${message}</div>
</script>

<script id="columnDetailsTemplate" type="text/x-jquery-tmpl">
<div class="rowColumnDetailsContainer">
	<span class="dataLoadAliasLabel" title="${alias}">${alias}:</span>
	<select class="ddl_columnType" onChange="StatPortalOpenData.ETL.typeColumnChange(this);" id="ddl_columnType_${idName}" data-type="${type}">
    <option {{if (type === 'NOTHING' || type === 'UNKNOWN')}} selected="selected" {{/if}} value="generica">Generica</option>
	<optgroup label="-------------"></optgroup>
    <option {{if ((type === 'NUMBER' || type === 'FLOAT') && inferedDimension !== 'LATITUDE' && inferedDimension !== 'LONGITUDE')}} selected="selected" {{/if}} value="statistica">Statistica (numero)</option>
	<optgroup label="-------------"></optgroup>
	<?php
	
	$count = count($etlDimensions);
	
	for ($i = 0; $i < $count; $i++) {
		echo "<option {{if (inferedDimension === '" . $etlDimensions[$i]->inferedDimension . "')}} selected=\"selected\" {{/if}} value=\"" . $etlDimensions[$i]->idHierNode . "\">" . $etlDimensions[$i]->name . "</option>\n";
	}
	
	?>
	{{if (!$item.isShapeFile)}}
	<optgroup label="-------------"></optgroup>
	<option {{if (inferedDimension === 'LATITUDE')}} selected="selected" {{/if}} value="latitudine">Latitudine</option>
	<option {{if (inferedDimension === 'LONGITUDE')}} selected="selected" {{/if}} value="longitudine">Longitudine</option>
	{{/if}}
	</select>
	<select class="ddl_numberOfDecimal" id="ddl_numberOfDecimal_${idName}" style="display:{{if ((inferedDimension === 'NOTHING' || inferedDimension === 'UNKNOWN') && (type === 'NUMBER' || type === 'FLOAT'))}} inline {{else}} none {{/if}};">
	<option {{if (decimalPositions === 0)}} selected="selected" {{/if}} value="0">Intero</option>
	<option {{if (decimalPositions === 1)}} selected="selected" {{/if}} value="1">1 decimale</option>
	<option {{if (decimalPositions === 2)}} selected="selected" {{/if}} value="2">2 decimali</option>
	<option {{if (decimalPositions === 3)}} selected="selected" {{/if}} value="3">3 decimali</option>
	<option {{if (decimalPositions === 4)}} selected="selected" {{/if}} value="4">4 decimali</option>
	{{if (decimalPositions > 4)}} <option selected="selected" value="${decimalPositions}">${decimalPositions} decimali</option> {{/if}}
	</select>
	<span class="spanColumnErrorMessage"></span>
	<span class="spanColumnInfoMessage"></span>
</div>
</script>
<style>
.navbar {
padding-top: 10px;
text-align: right;
}
</style>

  <div id="branding" class="clearfix">
    <?php print $breadcrumb; ?>
    <?php print render($title_prefix); ?>
    <?php if ($title): ?>
      <h1 class="page-title"><?php print $title; ?></h1>
    <?php endif; ?>
    <?php print render($title_suffix); ?>
    <?php print render($primary_local_tasks); ?>
  </div>
  <input id="etl-page-type" type="hidden" name="etl-page-type" value="add-metadata" />
  <div id="page">
    <?php if ($secondary_local_tasks): ?>
      <div class="tabs-secondary clearfix"><ul class="tabs secondary"><?php print render($secondary_local_tasks); ?></ul></div>
    <?php endif; ?>

    <div id="content" class="clearfix">
      <div class="element-invisible"><a id="main-content"></a></div>
      <?php if ($messages): ?>
        <div id="console" class="clearfix"><?php print $messages; ?></div>
      <?php endif; ?>
      <?php if ($page['help']): ?>
        <div id="help">
          <?php print render($page['help']); ?>
        </div>
      <?php endif; ?>
      <?php if ($action_links): ?><ul class="action-links"><?php print render($action_links); ?></ul><?php endif; ?>
      	
    	<div class="tab-wrapper" style="display: none;">
    	<ul>
			<li><a href="#tabs-description"><?php echo t('Scheda'); ?></a></li>
			<li class="tab-etl"><a class="disabled" href="#tabs-etl"><?php echo t('Dato'); ?></a></li>
			<li class="tab-application"><a class="disabled" href="#tabs-application"><?php echo t('Applicazioni'); ?></a></li>
			<li><a class="disabled" href="#tabs-attachment"><?php echo t('Allegati'); ?></a></li>
		</ul>
    	<div id="tabs-description">
    	<?php 
     		hide($page['content']['system_main']['attached_files']);
			print render($page['content']['system_main']);
    	?>
    		<div class="navbar" >
    			<button class="bt-next" data-next="1" id="step0-next" title="<?php echo t('Conferma i dati inseriti e avanza allo step successivo'); ?>"><?php echo t('Avanti'); ?></button>
    		</div>
    	</div>
    	<?php 
    		include('includes/etl.inc');
    	?>
		<div id="tabs-application" class="tab-application">
		<fieldset>
			<p>
			<label><?php echo t('Nome applicazione'); ?></label>
			<input id="v-app-name" type="text" name="application-name" value="<?php echo t('Inserisci il nome dell\'applicazione.'); ?>"/>
			</p>
			<p>
			<label><?php echo t('Indirizzo web'); ?></label>
			<input id="v-app-web" type="text" name="application-url" value="<?php echo t('Inserisci l\'indirizzo web dell\'applicazione.'); ?>"/>
			</p>
			<p>
			<label><?php echo t('Tipologia'); ?></label>
			<select id="v-app-type" name="application-type">
				<option value="0"><?php echo t('Desktop');?></option>
				<option value="1"><?php echo t('Mobile');?></option>
				<option value="2"><?php echo t('Web');?></option>
			</select>
			</p>
			<p>
			<label><?php echo t('Descrizione'); ?></label>
			<textarea id="v-app-description" name="application-description" rows="20" cols="90"><?php echo t('Inserisci informazioni aggiuntive sull\'applicazione.'); ?></textarea>
			</p>
			<p>
				<button id="upd-application"><?php echo t('Salva');?></button>
			</p>
		</fieldset>
		</div>
		
		<div id="tabs-attachment">
			<?php 
// 				$attach_form_state = array();
// 				$attach_form = drupal_build_form('metadata_attachmentupload_form', $attach_form_state);
// 				print render($attach_form);
				
				print render($page['content']['system_main']['attached_files']);
				
				
			?>
		</div>
		
		
		</div>
    </div>
	
	
    
<!--     <div class="additional-settings"> -->
	<?php // print render($page['content']['system_main']['additional_settings']); ?>	
<!-- 	</div> -->
	
<!-- 	<div class="metadata-creation-complete"> -->
	<?php // print render($page['content']['system_main']['actions']);?>
<!-- 	</div> -->
	
	<div class="form-actions" id="edit-actions-copy">
		<input type="submit" id="edit-submit-copy" name="op" value="Salva" class="form-submit" /><!-- disabled="disabled"  -->
		<input type="submit" id="edit-preview-copy" name="op" value="Anteprima" class="form-submit" disabled="disabled">
	</div>
	
	<div id="footer">
      <?php print $feed_icons; ?>
    </div>
  </div>
