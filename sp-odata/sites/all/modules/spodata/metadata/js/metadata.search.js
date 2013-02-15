(function($){
	
	/* function to fix the -10000 pixel limit of jquery.animate */
	$.fx.prototype.cur = function(){
	    if ( this.elem[this.prop] != null && (!this.elem.style || this.elem.style[this.prop] == null) ) {
	      return this.elem[ this.prop ];
	    }
	    var r = parseFloat( jQuery.css( this.elem, this.prop ) );
	    return typeof r == 'undefined' ? 0 : r;
	};
	
	function terms_count_init() {
//		$('form#catalog_form label').each(//'#edit-main label' 
//			function (idx, label) { 
//				var obj_label = $(label),
//					text = obj_label.html();
//				obj_label.html(text + '<span class="term-count">(0)</span>');
//				obj_label.attr('title', obj_label.text());
//			}
//		);
	};
	
	/**
	 * Update data count by term.
	 */
	function updateTermsCount(termcount) {
		var current,
			tot_termcount = StatPortalOpenData.Catalog.mdcount_by_term,
			start = '(',
			separator = '<span class="taxonomy-term-count"> di ',
			end = '</span>)';

		// init/reset term count
		$('label > span.term-count').each( function(idx, element) {
		    current = $(element);
		    current.html('(0/0)');
		    current.parent().attr('title', current.text());
		    current = current.parent();
		    current.parent().hide();
		});
		
		//TODO: ASPETTANDO LA MODIFICA LATO SERVER DEGLI ID NON SI VISUALIZZANO I
		// NUMERI NEL BOX DEL FORMATO
		if(termcount['term']!=undefined) {	
			// taxonomy terms -> set term count
			for ( var term in tot_termcount['term']) {
				var value = termcount['term'][term]===undefined ? 0 : termcount['term'][term];
				var current = $('#term-' + term);
				
				if(current.length > 0){
					current.html(start + value + separator + tot_termcount['term'][term] + end);
					current.parent().attr('title', current.text());
					current.parent().parent().show();
				}
			}
		}
			
		if(termcount['author']!=undefined) {
			// author 
			for ( var term in tot_termcount['author']) {
				var value = termcount['author'][term]===undefined ? 0 : termcount['author'][term];
				var current = $('#author-' + term);
				var totForAuth = tot_termcount['author'][term];
				current.html(start + value + separator + totForAuth + end);
				current.parent().attr('title', '(' + value + '/' + totForAuth + ')');
				current.parent().parent().show();
			}
		}
		
		// update term count for admin
		if(typeof(termcount['admin']) != 'undefined' ){
			for (var term in tot_termcount['admin']) {
				var value = termcount['admin'][term]===undefined ? 0 : termcount['admin'][term];
				var valueTot = tot_termcount['admin'][term]===undefined ? 0 : tot_termcount['admin'][term];
				var current = $('#' + term);
				current.html(start + value + separator + valueTot + end);
				current.parent().attr('title', '(' + value + '/' + valueTot + ')');
			}
		}
		
		// show all format tree node
		$('.search-filters div[class*="form-item-metadata-filter-metadata-lu-format-tree"]').css('display', 'inline');
		$('div.search-filters div.criterion > div').css('visibility', 'visible');
		// show admin filters
		$('#edit-metadata-filter-administration-tools>div').show();
		
		$('.search-filters .form-item').show();
		
		// Trigger event onTermCountUpdated.
		$('[name="term_updater"]').trigger('onTermCountUpdated');
		
	}
	
	$(document).ready(function() {
		var lastPage=0,
			pending_request = 0;
		
		terms_count_init();
		// load terms count by server
		$('[name="term_updater"]').trigger('onUpdateResult');
				
		$('div#match_count>span.count').html($('input[name="match_count"]').val());
		$('li.pager-last').find('a').attr('data-lastpage', $('input[name="page_count"]').val());
				
		Drupal.ajax.prototype.beforeSubmit = function(xmlhttprequest, options) {
			
			pending_request++;
			
			// nessuna azione da eseguire per la chiamata ajax_catalog_term_updater
			if(this.callback==='ajax_catalog_term_updater')
				return;
			
			if(this.callback==='ajax_admtask_callback') {
				$('div.admin-ajax-animation').show();
			}else{
				$('div.ajax-animation').show();
			}
			
			var curent_page = Number($('input[name="page"]').val()),
				i = 0;
					    
		    // if page changed
			if (lastPage !== curent_page) {
				// update lastPage number
				lastPage = Number(curent_page);
			} else {
				// reset lastPage number + reset 'input[name="page"]' to show first page
				lastPage = 0;
				$('input[name="page"]').val(lastPage);
				
				// update sended page number
				for(i=0; i< xmlhttprequest.length; i++) {
					if(xmlhttprequest[i].name==='page') {
						xmlhttprequest[i].value = lastPage;
						break;
					}
				}
			}
			
			$('.messages.error').remove();
			$('input.error').removeClass('error');
		};
		

		Drupal.ajax.prototype.success = function(response, status) {
				
			pending_request--;
			
			// Remove the progress element.
			if (this.progress.element) {
				$(this.progress.element).remove();
			}
			if (this.progress.object) {
				this.progress.object.stopMonitoring();
			}
			$(this.element).removeClass('progress-disabled')
					.removeAttr('disabled');

			Drupal.freezeHeight();
			
			
			if(response.type==='admin-task') {
				var div_message = '';
				$('#task-messages>.clearfix').html('');
				if(response.error) {
					div_message = '<div class="messages error">'+response.d+'</div>';
				}
				else {
					div_message = '<div class="messages status">'+response.d+'</div>';
				}
				

				$('#task-messages').show().delay(2500).slideUp().find('.clearfix').append(div_message);
			}
			else if( response.type!=='term-count') {
				
				for ( var i in response) {
					if (response[i]['command']
							&& this.commands[response[i]['command']]) {
						
						if(response[i]['command'] == 'insert'){
							if(response[i]['data'] != ''){
								$("#metadata-catalog").html('');
								$("#metadata-catalog").append(response[i]['data']);
							}
						}else{
							this.commands[response[i]['command']](this, response[i], status);
						}
					}
				}
	
				
				if(pending_request <= 0) {
					updateTermsCount(response);
				}
				// Reattach behaviors, if they were detached in
				// beforeSerialize(). The
				// attachBehaviors() called on the new content from
				// processing the response
				// commands is not sufficient, because behaviors
				// from the entire form need
				// to be reattached.
				if (this.form) {
					var settings = this.settings || Drupal.settings;
					Drupal.attachBehaviors(this.form, settings);
				}
	
				Drupal.unfreezeHeight();
				
				// update match count and page count
				$('div#match_count>span.count').html($('input[name="match_count"]').val());
				$('li.pager-last').find('a').attr('data-lastpage', $('input[name="page_count"]').val());
	
				// Remove any response-specific settings so they don't get used on the next
				// call by mistake.
				this.settings = null;
				
				$('[name="term_updater"]').trigger('onUpdateResult');
			}
			else {
				updateTermsCount(response);
			}
			
			if(pending_request <= 0) {
				$('div.ajax-animation, div.admin-ajax-animation').hide();
				pending_request = 0;
			}
		};
		
		Drupal.ajax.prototype.error = function(response, uri) { 
			if(console!=undefined && typeof console.log==='function')
				console.log(uri, ':', response);
			
			$('div.ajax-animation, div.admin-ajax-animation').hide();
			
			if(console!=undefined && typeof console.log==='function')
				Drupal.t('Errore nella comunicazione con il server. Se l\'errore persiste contattare l\'amministratore.');
		};
		  		
		
		$('div.content').on('click', 'ul.pager>li',  function(evt) { 
			evt.preventDefault; 
			
			var list_item = $(this), 
				link = list_item.find('a'),
				page = Number(link.html());
			
			if(isNaN(page))
			{
				if(list_item.hasClass('pager-first'))
				{
					page = '0';
				}
				else if(list_item.hasClass('pager-previous'))
				{
					page = Number($('input[name="page"]').val())-1;
				}
				else if(list_item.hasClass('pager-next'))
				{
					page = Number($('input[name="page"]').val())+1;
				}
				else if(list_item.hasClass('pager-last'))
				{
					page = $('li.pager-last').find('a').attr('data-lastpage');
				}
				
			}
			else
			{
				page -= 1;
			}
			
			if(link.length===1)
			{
				$('input[name="page"]').val(page);
				// force form submit
				$('input[name="keys"]').blur();
			}
			
			return false; 
		});
		
		$('div#content').on('change', 'select[name="order_by"]', function(evt) {
			var new_order = $(this).val();
			
			$('input[name="order_by"]').val(new_order);
			$('input[name="keys"]').blur();
		});
		
		var filter_widget_count = 0;
		
		$('div#edit-filter-accordion').accordion({
			collapsible: true,
			autoHeight: false,
			active: false
		});
		
		$('ul.pager').on('click', '> li', function() {
			$('ul.pager > li.pager-current').removeClass('pager-current');
			$(this).addClass('pager-current');
		});
		
		/**
		 * Show confirmation dialog and return result.
		 */
		function showConfirDialog() {
			return confirm('Vuoi togliere dai filtri il tag selezionato?');
		};
		
		/**
		 * Set filter after click on active page element.
		 */
		$('div#main').on('click', '.action-filter', function(evt) {
			var input_id = $(this).attr('data-input'),
				voc = $(this).attr('data-vocabulary'),
				checkbox = $('input[value="' + input_id + '"][id*="' + voc + '"]');// ('#' + input_id + '[]');
			
			evt.preventDefault();
			
			// aggiunge/elimina il filtro corrispondente
			if(checkbox.length!=0) {
				if(checkbox.attr('checked')==='checked') {
					// user confirmation
					if(showConfirDialog()) {
						checkbox.removeAttr('checked').trigger('change');
					}
				}
				else
					checkbox.attr('checked', 'checked').trigger('change');
			}
			else {
				// log error on chrome
				if(console!=undefined && typeof(console.log)==='function')
					console.error('Missing filter input: ', input_id);
			}
			
		})
		.on( 'mouseenter', '.action-filter',
			function() {
				$(this).css('cursor', 'pointer');
			})
		.on( 'mouseout', '.action-filter', function() {
				$(this).css('cursor', 'auto');
			}
		);
		
		// add tooltip text on filters label.
		$('label.option').each(function() {
			var title = $(this).html();
			
			$(this).attr('title', title);
		});
		
		// todo trovare soluzioni + efficiente
		// $('input[name="keys"]').blur();
	});
})(jQuery);