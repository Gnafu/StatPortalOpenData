    	<div id="tabs-etl" class="tab-etl">
			<div id="customEventStepsETL"></div>
			<div id="loadDataFormContainer">
				<div id="loadDataForm">
					<div id="loadDataFormLoading"></div>
					<fieldset class="fieldsetLoadDataWizard">
						<legend>Caricamento file</legend>
						<div id="visibleFileUpload">
							<div id="uploadFileInfoBox">
								<!-- error container -->
								<div id="message-error-etl-formatFile" class="messages error"></div>
								<!-- fine container -->
							
								<!-- inizio info container -->
								<div id="fileFormatInfoBox-xls" class="fileFormatInfoBox message-info messages">
									&Egrave; supportato il caricamento di una tabella semplice (nel caso di documento con fogli multipli verr&agrave; caricato il primo) <br />
									I nomi delle colonne devono essere disposti nella prima riga e non ci devono essere duplicati. <br /> 
									Limite massimo di 65.536 righe.<br />
									I numeri non devono avere il separatore delle migliaia.
									Per l'aggancio territoriale delle colonne &egrave; necessario fornire i codici ISTAT di comuni, province o regioni oppure i nomi ufficiali (ISTAT).
								</div>
								
								<div id="fileFormatInfoBox-xlsx" class="fileFormatInfoBox message-info messages">
								&Egrave; supportato il caricamento di una tabella semplice (nel caso di documento con fogli multipli verr&agrave; caricato il primo) <br />
									I nomi delle colonne devono essere disposti nella prima riga e non ci devono essere duplicati. <br />
									I numeri non devono avere il separatore delle migliaia.
									Per l'aggancio territoriale delle colonne &egrave; necessario fornire i codici ISTAT di comuni, province o regioni oppure i nomi ufficiali (ISTAT).
								</div>
								
								<div id="fileFormatInfoBox-dbf" class="fileFormatInfoBox message-info messages">
									I nomi delle colonne devono essere disposti nella prima riga e non ci devono essere duplicati. <br />
									Vengono supportai anche i formati DBase III e DBase V.
									Per l'aggancio territoriale delle colonne &egrave; necessario fornire i codici ISTAT di comuni, province o regioni oppure i nomi ufficiali (ISTAT).
								</div>
								
								<div id="fileFormatInfoBox-csv" class="fileFormatInfoBox message-info messages">
									&Egrave; supportato il caricamento di una tabella semplice. 
									I nomi delle colonne devono essere disposti nella prima riga e non ci devono essere duplicati. <br />
									I numeri non devono avere il separatore delle migliaia. <br />
									Carattere di quotazione dei testi/numeri: nessuno. <br />
									Per l'aggancio territoriale delle colonne &egrave; necessario fornire i codici ISTAT di comuni, province o regioni oppure i nomi ufficiali (ISTAT).
								</div>
								
								<div id="fileFormatInfoBox-mdb" class="fileFormatInfoBox message-info messages">
									Viene caricata la prima tabella del database.<br />
									Per l'aggancio territoriale delle colonne &egrave; necessario fornire i codici ISTAT di comuni, province o regioni oppure i nomi ufficiali (ISTAT).
								</div>
								
								<div id="fileFormatInfoBox-zip" class="fileFormatInfoBox message-info messages">
									&Egrave; necessario caricare un file .zip contenente almeno i seguenti file necessari: 
									<ul>
										<li>.shp</li>
										<li>.shx</li>
										<li>.dbf</li>
										<li>.prj</li>
									</ul>
									Le coordinate del dato devono essere Geografiche WGS84 (non proiettato).<br />
									Per l'aggancio territoriale delle colonne &egrave; necessario fornire i codici ISTAT di comuni, province o regioni oppure i nomi ufficiali (ISTAT).
								</div>
								
								<div id="fileFormatInfoBox-kml" class="fileFormatInfoBox message-info messages">
									Il file kml caricato sar&agrave; visualizzabile in mappa 2D e 3D
								</div>
								
								<!-- fine info container -->
								
								<br/>
								Formato del file:  
								<select id="fileFormatSelection" onChange="StatPortalOpenData.ETL.onChangeFileFormatInfoBox(this);">
									<option selected="selected" value="empty">Seleziona un formato</option>
									<option value="xls">File Excel 2003 ( .xls)</option>
								  	<option value="xlsx">File  Excel 2007 (.xlsx)</option>
								  	<option value="dbf">File Dbase IV (.dbf)</option>
								  	<option value="csv">File Testo CSV (.csv, .txt)</option>
								  	<option value="kml">File KML (.kml)</option>
								  	<option value="mdb">File Access 2003 (.mdb)</option>
								  	<option value="zip">ESRI Shapefile</option>
								</select>
								<br/><br/>
								<div id="csv-delimiter-container">Delimitatore del file CSV:
									<select id="csv-delimiter" name="csv-delimiter">
										<option selected="selected" value=";">Punto e virgola (;)</option>
										<option value=",">Virgola (,)</option>
									  	<option value="t">Tabulazione</option>
									  	<option value=" ">Spazio</option>
									</select>
								</div>	
								<br />
							</div>
							<?php 
								$upd_form_state = array('node' => (isset($node) ? $node : false));
								$upd_form = drupal_build_form('metadata_dataupload_form', $upd_form_state);
								print render($upd_form);
							?>
						</div>
					</fieldset>
					<fieldset class="fieldsetLoadDataWizard">
					<legend>Selezione delle colonne</legend>
						<div class="message-info messages">
							Scegliere quali colonne si vogliono importare ed impostarne le etichette.</br>
							Sono ammessi spazi nelle etichette. </br>
							Non sono ammesse etichette duplicate.</br>
						</div>
						<div id="visibleFileInfo">
						</div>
					</fieldset>
					<fieldset class="fieldsetLoadDataWizard">
						<legend>Tipologia delle colonne</legend>
						<div id="etl-warnings-container" style="display:none;"></div>
						<div id="error-info" style="display:none;" class="messages error"></div>
						<div id="columnTypeInfoBox" class="message-info messages">
							Per ogni colonna indicare la tipologia al fine di migliorare la visualizzazione del dato.</br>
							Nel caso di colonna statistica &egrave; necessario anche indicare l'eventuale numero di decimali che si intende visualizzare. </br>
						</div>
						<span><a class="etl-linkable" href="javascript:StatPortalOpenData.ETL.setAllColumnsAsGenerics();">Annulla suggerimenti</a>&nbsp;(Imposta tutte le colonne come generiche)</span>
						<div id="visibleDetailsFileInfo">
						</div>
					</fieldset>
					<fieldset class="fieldsetLoadDataWizard">
						<legend>Scelta visualizzatori</legend>
						<div id="viewerChoice">
						<div class="messages status">Dato caricato correttamente!</div> 
						<br/>
						<div class="">
						<b>Come visualizzare il dato: </b></div>
						<?php 
							$viewer_form_state = array();
							$viewer_form = drupal_build_form('metadata_datainfo_form', $viewer_form_state);
							print render($viewer_form);
						?>
						</div>
					</fieldset>
					<div id="etl-button-for-warnings">
						<a href="#" onclick="StatPortalOpenData.ETL.dummyPrevious();" id="stepFittPrev" class="prev">&lt; Indietro</a>
						<a href="#" onclick="StatPortalOpenData.ETL.dummyNext();" id="stepFittNext" class="next">Avanti &gt;</a>
					</div>
				</div>
			</div>
		</div>