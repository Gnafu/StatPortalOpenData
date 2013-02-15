(function($){
	
$(document).ready(function() {
	
	$('.action').click(function(evt) {
		var $this = $(this),
			author = {},
			author_id = $this.attr('data-id'),
			row = "item-" + author_id;

		author.logo = $("#" + row + "> .logo > img").attr('src');
		author.author = $("#" +row + "> .author").text();
		author.website = $("#" +row + "> .website").text();
		author.email = $("#" +row + "> .email").text();
		author.id = author_id;
			
		if($this.hasClass('edit')) {
			initEditForm(author);
			
			$('#edit-logo2-remove-button').trigger('mousedown');
			$('#image-preview').show();
			$('.form-item-logo2').addClass('edit').hide();
			
			$('#metadata-edit-author-form').dialog({
				'title': Drupal.t('Modifica la Fonte'),
				'width': 450,
				'draggable': true
			});
			
			
		}
		else if($this.hasClass('delete')) {
			var $form = $('#delete-author-handler');
			$form.find('#author-id-to-delete').val(author.id);
			
			$('#delete-author-handler').dialog({
				'title': Drupal.t('Cancellazione Fonte'),
				'width': 450,
				'draggable': true
			});
		} 
		
	});
	
	
	/**
	 * Init form items and show this on dialog.
	 */
	function initEditForm(opt) {
		var $form = $('#metadata-edit-author-form');
		
		$form.find('#author-id').val(opt.id);
		$form.find('#author-name').val(opt.author);
		$form.find('#author-logo').val(opt.logo);
		$form.find('#author-logo-file').val(opt.logo);
		$form.find('#author-website').val(opt.website);
		$form.find('#author-email').val(opt.email);
		$('#image-preview').attr('src', opt.logo);
		
	}
	
	
	$('#add-author').click(function(evt) {
		var author = { id: -1, logo: '', author: '', website: '', email: '' };
		
		evt.preventDefault();
		
		initEditForm(author);
		
		$('#edit-logo2-remove-button').trigger('mousedown');
		$('#image-preview').hide();
		$('.form-item-logo2').removeClass('edit').show();
		
		$('#metadata-edit-author-form').dialog({
			'title': Drupal.t('Aggiungi una nuova fonte'),
			'width': 450,
			'draggable': true
		});
		
	});

	$('#image-preview').click(function() {
		$('.form-item-logo2').toggle();
	});
	
	$('#author-website').change(function(evt) {
		var url = $(this).val(),
			rex = /^http:\/\//i;
		
		if(!rex.test(url)) {
			$(this).val('http://'+url);
		}
	});
	
	$('input[name="replacement_type"]').change( function(evt) {
		if($(this).attr('id')==="edit-replacement-type-replace-with") {
			$('.form-item-metadata-author').show();
		}
		else {
			$('.form-item-metadata-author').hide();
		}
	});
});	
	
})(jQuery);