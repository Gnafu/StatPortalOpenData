(function($){
	
	/**
	 * Selection handlers.
	 **/
	$('div#main').on( 'click', '.metadata-action-select', function() {
		$(this).hide().hide().next().show().parent().parent().addClass('selected');
	})
	.on( 'click', '.metadata-action-deselect', function() {
		$(this).hide().hide().prev().show().parent().parent().removeClass('selected');
		
	});
	
	$('#select-all').click(function() {
		$('.metadata-search-result').addClass('selected');
		$('.metadata-action-select').hide();
		$('.metadata-action-deselect').show();
	});
	
	$('#deselect').click(function() {
		$('.metadata-search-result').removeClass('selected');
		$('.metadata-action-select').show();
		$('.metadata-action-deselect').hide();
	});
	
	$('#switch-selection').click(function() {
		$('.metadata-search-result').toggleClass('selected');
		$('.metadata-action-select').toggle();
		$('.metadata-action-deselect').toggle();
	});
	
	$('.descendent-menu').click(function() {
		$('.action-items').toggle();
	});
	
	/**
	 * Adm action handler.
	 */
	$('ul.action-items>li').click(function() {
		var action = $(this).attr('id'),
			selected_items = [];
		
		$.each($('.search-results>li.selected'), function(idx, value) {
			selected_items.push($.trim($(value).attr('data-id')));
		});
		
		switch(action) {
			case 'task-approved':
				sendTask('approved', selected_items);
			break;
			case 'task-rejected':
				sendTask('rejected', selected_items);
			break;
			case 'task-show':
				sendTask('show', selected_items);
			break;
			case 'task-hide':
				sendTask('hide', selected_items);
			break;
			case 'task-edit':
				sendEditTask(selected_items);
			break;
			default:
				alert('Azione non ancora implementata');
			break;
		}
		
	});
	
	/**
	 * Send approved task.
	 */
	function sendTask(task, selected_items) {
		$('input[name="task"]').val('task-' + task);
		$('input[name="mids"]').val(selected_items.join(';'));
		
		$('#adm-task-handler').trigger('onExecuteTask');
	}
		
	/**
	 * Show mail form to send edit request task.
	 */
	function sendEditTask(selected_items) {
		$('input[name="task"]').val('task-contact-author');
		$('input[name="mids"]').val(selected_items.join(';'));
		
		$('#mail-box').dialog({ 
			title: Drupal.t('Compila e Invia la richiesta'),
			modal : true,
			width : 500
		});
		
//		$('#adm-task-handler').trigger('onExecuteTask');
	}
	
	$('#edit-send').click(function() { 
		var subject = $('#mail-subject').val(),
			body = $('#mail-body').val();
		
		$('[name="subject"]').val(subject);
		$('[name="body"]').val(body);
		
		$('#mail-box').dialog('close');
		
		$('#adm-task-handler').trigger('onExecuteTask');
	});
	
	$('#edit-metadata-filter-administration-tools').on('mousedown', 'input', function(evt) {
		var  checked = $('#edit-metadata-filter-administration-tools input:checked').length;
		
		if(checked===1 && $(this).is(':checked')) {
			evt.preventDefault();
			alert('Non puoi deselezionare tutte le checkbox dell\'area "Filtri amministratore"');
		}
		
	});
	
})(jQuery);