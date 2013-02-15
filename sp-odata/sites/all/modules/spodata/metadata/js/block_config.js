(function($){
	
	$(document).ready(function() {
		var current_max_item = $('input[name="max_item"]').val(),
			min_value = 0,
			max_value = 20;
		
		$('input[name="max_item"]').change(function(evt){
			var new_value = Number($(this).val());
			
			if(isNaN(new_value)) { 
				$(this).val(current_max_item);
				return;
			}
			else {
				
				if(new_value>max_value)
					new_value=max_value;
				
				if(new_value<min_value)
					new_value = min_value;
				
				current_max_item = new_value;
			}
			
			for(var i=0; i<current_max_item; i++) {
				$('#edit-item-'+i).removeClass('invisible');
			}
			
			for(;i<max_value;i++)
				$('#edit-item-'+i).addClass('invisible');
		});
		
	});
	
})(jQuery);