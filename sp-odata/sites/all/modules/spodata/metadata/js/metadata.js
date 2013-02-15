(function($){
	
	$(document).ready(function() {
		
		var md_title = $('span#field-author-label').html(),
			md_lastupdate = $('span#lastupdate').html();
		
		
		$('span#author-subtitle').html(md_title);
		$('span#lastupdate-subtitle').html(md_lastupdate);
		
		
		/**
		 * Gestione link catalogo dati
		 */
		$('div#left-column ul.links a, div#right-column ul.links a').each(function(idx, element) {
			element.href = element.href.replace('taxonomy', 'catalog').replace('term/', '/term=');
			element.title = Drupal.t('Click per vedere tutti i dati con il tag corrente');
		});
		
		
		$('div#left-column ul.links a, div#right-column ul.links a').hover(
				function() { $(this).css('cursor', 'pointer'); }, 
				function() { $(this).css('cursor', 'auto'); }
		);
	});
	
})(jQuery);