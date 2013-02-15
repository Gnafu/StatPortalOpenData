(function($) {
	
	
	$(document).ready(function() {
		var $downloadarea = $('div.download-area-form'),
			$license = $('input#edit-license'),
			$dialog = $downloadarea.dialog({autoOpen: false, 
				closeText: Drupal.t('Chiudi'), 
				modal: true, 
				title: Drupal.t('Scarica il dato in formato '),
				position: 'center',
				drabbable: 'true',
				width: 360,
				open: function(event, ui) { 
					$license.prop('checked', false);
					$('#messages').remove();
					Recaptcha.reload();
				},
				resizable: true
			});
		/**
		 * Download button handler.
		 */
		$('div.cool_button').click(function() {
			var type = $(this).attr('data-value'),
				$download_form = $('input[value="' + type + '"]'),
				prev_type = $('input[name="export_type"]:checked').val(),
				type_label = $(this).text().trim();
			
			if(type==='link') {
				$(this).find('a').trigger('click');
				return;
			}
			
			// selection changed?
			if(prev_type!==type) { 
				$download_form.prop('checked', true);
			}
			
			if(!$dialog.dialog( "isOpen" ))
				$dialog.dialog('option', { title: Drupal.t('Scarica il dato in formato ') + type_label} );
				
				$dialog.dialog('open');
		});
		
		$('#edit-download').click(function(evt) {
			$downloadarea.dialog('close');
		});
	});
	
})(jQuery);