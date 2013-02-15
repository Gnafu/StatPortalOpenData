(function($) {
	
	var LinkManager = function() {
		// private vars and method
		var init,
			options,
			ui_button,
			ui_inputlink,
			ui_inputlabel,
			ui_container,
			ui_relation,
			ui_bt_submit,
		// private function
		/**
		 * Add button click handler function.
		 */
		_add_button_click_handler = function _add_button_click_handler(evt) {
			evt.preventDefault();
			
			var link = $.trim(ui_inputlink.attr('value')),
				label = $.trim(ui_inputlabel.attr('value')),
				url_regex = /(ftp|http|https):\/\/(\w+:{0,1}\w*@)?(\S+)(:[0-9]+)?(\/|\/([\w#!:.?+=&%@!\-\/]))?/,
				// get and validate user input
				new_element = _get_link();
			
			if(new_element===false)
				return; // si potrebbe mostrare un msg di errore
			
			// aggiunta link alla lista
			ui_container.children('ol').append(new_element);
		},
		/**
		 * Init options validator.
		 * @return true on success.
		 */
		_options_validation = function _options_validation(opt) {
			return (opt.button!==undefined && opt.link_input!==undefined && 
					opt.label_input && opt.link_area!==undefined);
		},
		/**
		 * Get link to add.
		 */
		_get_link = function() {
			var link = $.trim(ui_inputlink.attr('value')),
				label = $.trim(ui_inputlabel.attr('value'));
		
			// add prot if missing
			if(link.substring(0, 7)!=='http://' && link.substring(0, 8)!=='https://')
				link = 'http://' + link;
			
			// link validation
			if(!_validate_url(link)) {
				ui_inputlink.addClass('error');
				return false;
			}
			
			if(label==='')
				label = link;			
			
			return _create_li(label,link);
		},
		/**
		 * Change event handler on input element.
		 * Compute url validation.
		 * @param evt object event object.
		 */
		_on_link_changed = function _on_link_changed(evt) {
			var url = $.trim(ui_inputlink.attr('value'));
			
			if(url.substring(0, 7)!=='http://')
				url = 'http://' + url;
			
			_validate_url(url);
		},
		/**
		 * Compute url validation.
		 * @param url
		 * @return true on succes validation, false otherwise (set error status on link input).
		 */
		_validate_url = function(url) {
			var url_regex = /(ftp|http|https):\/\/(\w+:{0,1}\w*@)?(\S+)(:[0-9]+)?(\/|\/([\w#!:.?+=&%@!\-\/]))?/,
			validation_ok = true;
			
			// validazione link
			if(!url_regex.test(url)) {
				ui_inputlink.addClass('error');
				validation_ok = false;
			}
			else {
				ui_inputlink.removeClass('error');
			}
			
			return validation_ok;
		},
		/**
		 * Remove added link.
		 */
		_remove_link = function _remove_link(evt, ui) {
			$(this).parent().remove();
		},
		/**
		 * 
		 */
		_set_relation = function _set_relation() {
			var items = ui_container.find('li'),
				value = '',
				first = true;
			
			$.each(items, function(idx, li) {
				var link = $(li).find('span.link>a').attr('href'),
					label = $(li).children('span.label').html();
				
				value += label+':::' + link + '##END##';

			});
			
			ui_relation.val(value);
			
			return this;
		},
		_show_relation = function() {
			var links = ui_relation.val().split('##END##');
			
			for(var i=0; i<links.length; i++) {
				var link_info = links[i].split(':::'),
					li = link_info.length==2 ? _create_li(link_info[0], link_info[1]) : '';
				
				ui_container.children('ol').append(li);
			} 
		},
		/**
		 * Create a li element.
		 * @param string label link label
		 * @param string link url
		 * @return string html li item containing label/link and remove icon.
		 */
		_create_li = function _create_li(label, link) {
			li = "";
			
			li += '<li><span class="label">' + label + '</span>';
			li += '<span class="link"><a target="_blank" href="' + link + '">' + link + '</a></span>';
			li += '<span class="bt-remove-link ui-icon ui-icon-close" title="Rimuovi il link"></span></li>';
			
			return li;
		},
		/**
		 * Initialize widget.
		 * 
		 * @params object opt widget options. 
		 * 					{
		 *						button: '#bt-add-relation',
		 *						link_input: '#edit-link',
		 *						label_input: '#edit-label',
		 *						link_area: '#links-container'
		 *					}
		 */ 
		init = function init(opt) {
			// check options
			if(_options_validation(opt)) {
				options=opt;
			}
			else
			{
				if(typeof(console.error)==='function')
					console.error('LinkManager: error on init, bad configuration.');
				return;
			}	
			
			// load button element and add click handler
			ui_button = $(options.button);
			if(ui_button.length==0) {
				console.error('Missing add link button');
				return;
			}
			else
				ui_button.click(_add_button_click_handler);
			
			// load input link
			ui_inputlink = $(options.link_input);
			if(ui_inputlink.length==0) {
				console.error('Missing link input element');
				return;
			}
			// add change evt handler.
			ui_inputlink.change(_on_link_changed);
			
			
			// load input label			
			ui_inputlabel = $(options.label_input);
			if(ui_inputlabel.length==0) {
				console.error('Missing link input label');
				return;
			}
			// load link summary			
			ui_inputlabel = $(options.label_input);
			if(ui_inputlabel.length==0) {
				console.error('Missing link input label');
				return;
			}
			
			ui_container = $(options.link_area);
			if(ui_inputlabel.length==0) {
				console.error('Missing link input label');
				return;
			} 
			else {
				ui_container.append('<ol></ol>');
			}
			
			ui_bt_submit = $(options.submit_button);
			if(ui_bt_submit.length==0) {
				console.error('Missing submit button');
				return;
			}
			
			ui_relation = $(options.relation);
			if(ui_relation.length==0) {
				console.error('Missing relation input hidden');
				return;
			}
			else {
				_show_relation();
			}
			
			// add remove link handler
			$(ui_container).on('mouseup', '.bt-remove-link', _remove_link);
			
			ui_bt_submit.bind('click', _set_relation);

		};
		
		// public 
		return {
			init : init
		};
	};
	/**
	 * Implements binding between input element.
	 * options.direction = [OneWay || TwoWay]
	 * options.source = input source
	 * options.target = input target
	 * 
	 * If options.direction is OneWay => binding [source]->[target].
	 * If options.direction is TwoWay => binding [source]<->[target].
	 */
	InputBinding = function() {
		var default_opt = { direction: 'OneWay', source: undefined, target: undefined, live: false, event_type: 'change'},
			opt,
			/**
			 * init function
			 */
			init = function init(options) {
				opt = $.extend(default_opt, options, true);
				
				// check if source and target are defined
				if(opt.source!=undefined && opt.target!=undefined) {
					// copy source val to target
					if($(opt.target).attr('type')==='checkbox') {
						var status = $(opt.target).is(':checked');
						$(opt.source).attr('checked', status);
					}
					else {
						$(opt.source).val($(opt.target).val());
					}
					
					// default
					_binding();
					
					if(opt.direction==='TwoWay') {
						_inverse_binding();
					}
						
				}
			},
			/**
			 * OneWay binding: source->target
			 */
			_binding = function _binding() {
				if(!opt.live)
					$(opt.source).bind(opt.event_type, _on_change_handler);
				else
					$('div#page').on(opt.event_type, opt.source, _on_change_handler);
			},
			/**
			 * TwoWay: target->source
			 */
			_inverse_binding = function _inverse_binding() {
				$(opt.target).change(_on_change_handler);
			},
			/**
			 * Change event handler.
			 */
			_on_change_handler = function _on_change_handler(event) {
				var current_value = $(this).val();
				
				if(opt.direction==='OneWay') {
					if($(this).attr('type')==='checkbox') {
						var status = $(this).is(':checked');
						$(opt.target).attr('checked', status);
					}
					$(opt.target).val(current_value);
				}
				else {
					// update target
					if(_is_source(this)) {
						if($(this).attr('type')==='checkbox') {
							var status = $(this).is(':checked');
							$(opt.target).attr('checked', status);
						}
						else {
							$(opt.target).val(current_value);
						}
					}
					// update source
					else {
						if($(this).attr('type')==='checkbox') {
							var status = $(this).is(':checked');
							$(opt.source).attr('checked', status);
						}
						else {
							$(opt.source).val(current_value);
						}
					}
				}
			},
			/**
			 * Check if element is target.
			 * 
			 */
			_is_dest = function(element) {
				return $(element)===$(opt.target);
			};
			/**
			 * Check if element is source.
			 */
			_is_source = function(element) {
				return $(element)===$(opt.source);
			};
		// public methods
		return {
			init: init
		};
	};
	
	$(document).ready(function() {
		var conf = {
				button: '#bt-add-relation',
				link_input: '#edit-link',
				label_input: '#edit-label',
				link_area: '#links-container',
				relation: '#relation',
				submit_button: '#edit-submit',
		},
		uploader = undefined;
		
		LinkManager().init(conf);
		
		// si controlla il path del file sia all'inizio che ad ogni cambiamento 
		// del modulo dell'upload
		StatPortalOpenData.ETL.showCsvDelimiterAndNextButtonIfNecessary();
		
		jQuery('#visibleFileUpload').on('ajaxComplete',function(e, xhr, settings) {
			StatPortalOpenData.ETL.showCsvDelimiterAndNextButtonIfNecessary();
		});
		
		$('div.tab-wrapper').tabs({
			create: function(event, ui) 
			{ 
				// TODO: da cancellare la riga successiva
				// $('#tabs-edit-label, .tabs-edit-label-class').hide();
				
				// se si tratta di modifica della scheda bisogna nascondere il tab 
				// Modifica etichette nel caso in cui non ci sia alcun dato
				// associato
				if($('#etl-page-type').val() == 'edit-metadata'){
					
					StatPortalOpenData.ETL.initializeDataEditing();
					
					if($('#uid_odata_attach').val() == '-1'){
						// non ci sono dati associati. Si nasconde il tab
						//$('#tabs-etl, .tabs-etl-class').hide();
						$('#tabs-edit-label, .tabs-edit-label-class').hide();
					}
				}
			},
			select: function(event, ui)
			{
				var idTabSelected = ui.tab.hash,
			    	$delete_btn = $('#edit-delete-copy');
				
				$delete_btn.hide();
				
				if(typeof(idTabSelected) != 'undefined'){
					// se viene selezionato il tab modifica etichette richiede la lista delle etichette
					if(idTabSelected == '#tabs-edit-label'){
						StatPortalOpenData.ETL.getAliases();
					}
					else if(idTabSelected === "#tabs-attachment") {
						/// AM BUG FIXING
						if($.browser.msie && ($.browser.version==='8.0' || $.browser.version==='7.0')) {
							 var $this = $('#edit-attached-files');

						      // Merge the default settings and the element settings to get a full
						      // settings object to pass to the Plupload library for this element.
							 var id = $this.attr('id'),
					          settings = Drupal.settings,
					          defaultSettings = settings.plupload['_default'] ? settings.plupload['_default'] : {},
					          elementSettings = (id && settings.plupload[id]) ? settings.plupload[id] : {},
					    	  pluploadSettings = $.extend({}, defaultSettings, elementSettings);

						      // Do additional requirements testing to prevent a less than ideal runtime
						      // from being used. For example, the Plupload library treats Firefox 3.5
						      // as supporting HTML 5, but this is incorrect, because Firefox 3.5
						      // doesn't support the 'multiple' attribute for file input controls. So,
						      // if settings.plupload._requirements.html5.mozilla = '1.9.2', then we
						      // remove 'html5' from pluploadSettings.runtimes if $.browser.mozilla is
						      // true and if $.browser.version is less than '1.9.2'.
						      if (settings.plupload['_requirements'] && pluploadSettings.runtimes) {
						        var runtimes = pluploadSettings.runtimes.split(',');
						        var filteredRuntimes = [];
						        for (var i = 0; i < runtimes.length; i++) {
						          var includeRuntime = true;
						          if (settings.plupload['_requirements'][runtimes[i]]) {
						            var requirements = settings.plupload['_requirements'][runtimes[i]];
						            for (var browser in requirements) {
						              if ($.browser[browser] && Drupal.plupload.compareVersions($.browser.version, requirements[browser]) < 0) {
						                includeRuntime = false;
						              }
						            }
						          }
						          if (includeRuntime) {
						            filteredRuntimes.push(runtimes[i]);
						          }
						        }
						        pluploadSettings.runtimes = filteredRuntimes.join(',');
						      }

						      // Initialize Plupload for this element.
						      $this.pluploadQueue(pluploadSettings);
						      uploader = initPluploadPlugin();
						}
						
						
					    var element = $('.form-item-attached-files').detach(),
					    	$current_list = $('ol.current-attaches'),
					    	current = $.parseJSON($('input[name="current_attached_files"]').val()),
					    	attach = undefined,
					    	count = 0;
					    //reset attachment list
					    $current_list.html('');
					    $(ui.panel).append(element);
					    if(current!==null && jQuery.isArray(current)) {
					    	for(var i=0; i<current.length;i++) {
				    			$current_list.append('<li><a href="'+current[i].url+'" target="_blank">'+
				    					current[i].name + '</a>' +
				    					'<span class="bt-remove-attach ui-icon ui-icon-close" data-id="'+current[i].id+'" title="Rimuovi l\'allegato"></span>' +
				    					'</li>');
				    			count++;
					    	}
					    	if(uploader!=undefined) {
						    	for(i=0; i<uploader.files.length; i++) {
						    		$current_list.append('<li>'+ uploader.files[i].name +
											'<span class="bt-remove-attach ui-icon ui-icon-close" data-new="true" data-id="'+ uploader.files[i].id +'" title="Rimuovi l\'allegato"></span>' +
											'</li>');
						    		count++;
						    	}
					    	}
					    }
					    
					    if(count===0) {
					    	$current_list.append('<li id="message">'+Drupal.t('Nessun allegato')+'</li>');
					    }
					}
					else if(idTabSelected==='#tabs-description') {
						$delete_btn.show();
					}
				}
		    }
//			select: function(event, ui) {
//				// gestione tab non attivi 
////				if(ui.hasClass('disabled'))
////					return false;
////				else 
////					return true;
//			},
			
		}).show();
		
		if(!($.browser.msie && $.browser.version==="8.0")) {
			uploader = initPluploadPlugin();
		}
			
		
		function initPluploadPlugin() {
			var upd_handler = $('#edit-attached-files').pluploadQueue();
			upd_handler.bind('FileUploaded', function(up, file, res) { 
				var $current_list = $('ol.current-attaches');
				$current_list.find('#message').remove();
				$current_list.append('<li>'+ file.name +
						'<span class="bt-remove-attach ui-icon ui-icon-close" data-new="true" data-id="'+ file.id +'" title="Rimuovi l\'allegato"></span>' +
						'</li>');
			});
			
			$('ol.current-attaches').on('click', '.bt-remove-attach', function(evt) {
				var $this = $(this),
					id = $this.attr('data-id'),
					li = $this.parent();
				
				if($this.attr('data-new')) {
					upd_handler.removeFile(new plupload.File(id));
				}
				else {
					var deleted_files = $('input[name="deleted_attached_files"]'); 
					deleted_files.val( deleted_files.val() + id + ';');
				}
				
				$(li).remove();
				
				if($('ol.current-attaches > li').length===0) {
					$('ol.current-attaches').append('<li id="message">'+Drupal.t('Nessun allegato')+'</li>');
				}
			});
			
			return upd_handler;
		}
		
		$('div.tab-wrapper').tabs( 'option', "disable" , [1, 2, 3] );
		
		
		// Binding application info 
		InputBinding().init({source: '#v-app-name', target: '#edit-app-name'});
		InputBinding().init({source: '#v-app-description', target: '#edit-app-description'});
		InputBinding().init({source: '#v-app-type', target: '#edit-app-format'});
		InputBinding().init({source: '#v-app-web', target: '#edit-app-web'});
		
		// Binding data info
//		InputBinding().init({source: '[name="data_file[fid]"]', target: '#odata-file-fid', live: true});
		InputBinding().init({source: '#tb-odata-url', target: '#edit-odata-url'});
		// Binding viewer info
		InputBinding().init({source: '#cb-viewer-table', target: '#edit-viewer-hasgrid'});
		InputBinding().init({source: '#cb-viewer-graph', target: '#edit-viewer-haschart'});
		InputBinding().init({source: '#cb-viewer-map3d', target: '#edit-viewer-has3dmap'});
		InputBinding().init({source: '#cb-viewer-map2d', target: '#edit-viewer-has2dmap'});
		// data type
		InputBinding().init({source: '#cb-type-structured', target: '#edit-md-is-structured'});
		InputBinding().init({source: '#cb-type-raw', target: '#edit-md-is-raw'});
		InputBinding().init({source: '#cb-type-alphanumeric', target: '#edit-md-is-alphanumeric'});
		InputBinding().init({source: '#cb-type-geographic', target: '#edit-md-is-geographic'});
		InputBinding().init({source: '#cb-file-format', target: '#file-format'});

		$('#edit-submit-copy').mouseup(function() {
			var fid = $('[name="data_file[fid]"]').val();
			
			if(form_validation()) {
				
			    var element = $('.form-item-attached-files').detach();
			    $('#metadata-node-form').append(element);
				    
				
				$('#odata-file-fid').val(fid);
			
				$('#edit-submit').trigger('click');
			}
			else {
				alert(Drupal.t('Completare tutti i campi obbligatori.'));
			}
		});
		
		$('#edit-delete-copy').mouseup(function() {
			$('input#edit-delete').trigger('click');
		});
		
		/**
		 * Check required fields.
		 */
		function form_validation() {
			var input_required_fields = $('input.required'),
				select_required_fields = $('select.required'),
				checkbox_required_container = $('div.required'),
				i=0,
				$current = undefined,
				checked_count = 0;
			
			for(;i<input_required_fields.length; i++) {
				$current = $(input_required_fields[i]);
				if($.trim($current.val())==='') {
					$current.addClass('error');
					return false; // exit on first validation error
				}
				else {
					$current.removeClass('error');
				}
			}
			
			for(i=0;i<select_required_fields.length; i++) {
				$current = $(select_required_fields[i]);
				
				if($current.val()==='-1') {
					$current.addClass('error');
					return false; // exit on first validation error
				}
				else {
					$current.removeClass('error');
				}
			}
			
			for(i=0;i<checkbox_required_container.length;i++) {
				$current = $(checkbox_required_container[i]);
				checked_count = $current.find('input:checked').length;
				
				if(checked_count<=0) {
					$current.addClass('error');
					return false; // exit on first validation error
				}
				else {
					$current.removeClass('error');
				}
			}
			
			return true;
		}
		
		// ds link validation
		$('[name="ds_link"]').change(function() {
			var url_regex = /(ftp|http|https):\/\/(\w+:{0,1}\w*@)?(\S+)(:[0-9]+)?(\/|\/([\w#!:.?+=&%@!\-\/]))?/,
				url = $.trim($(this).val());
			
			if(!/^(ftp|http|https):\/\//i.test(url) && url!=='')
				url = 'http://' + url;
				
			// validazione url
			if(!url_regex.test(url) && url!=='') {
				$(this).addClass('error');
			}
			else {
				$(this).removeClass('error');
				$(this).val(url);
			}
		});
		
		$('#edit-basetype input').change(function(evt) { 
			// load application
			$('.tab-application').toggle();
			$('.tab-etl').toggle();
		});
		
		$('#step0-next').click(function(evt) {
			var offset = Number($('#edit-basetype input').val());
			$('div.tab-wrapper').tabs('select', offset + Number($(this).attr('data-next')));
		});
		
		$('#ui-autocomplete-author').autocomplete({ 
			source: StatPortalOpenData.authors, 
			minLength: 0,
			select: function(event, ui) {
				$(this).removeClass('error');
				$('#author-id').val(ui.item.aid);
			},
			change: function(event, ui) {
				var new_author = $(this).val();
				
				for(var i=0; i<StatPortalOpenData.authors.length;i++) {
					if(StatPortalOpenData.authors[i].label==new_author) {
						return;
					}
				}
				
				if(/^[a-z0-9-\s\xE0\xE1\xE8\xE9\xEC\xED\xF2\xF3\xF9\xFA'()]+$/i.test(new_author)) {
					$('#author-id').val(new_author);
					$(this).removeClass('error');
					$('#author-id').val('new');
				}
				else {
					alert('La fonte inserita contiene caratteri non validi. Correggere il problema prima di salvare la scheda dato.');
					$(this).addClass('error');
				}
			}
			
		})
//		.focus(function(evt){            
//            $(this).autocomplete( "search", "" );
//        })
        .click(function(evt) { $(this).autocomplete( "search", $(this).val() ); });
	});
	
})(jQuery);