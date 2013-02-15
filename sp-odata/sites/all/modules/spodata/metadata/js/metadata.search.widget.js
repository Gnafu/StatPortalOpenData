(function($){
	
	$(document).ready(function() {
		
		/**
		 * Search box handler
		 */
		$('input#main-search-button').click(function(evt) {
			evt.preventDefault();
			var keys = $('input#main-search-bar').attr('value'), 
				pattern=/\?q=/i,
				rewrite = true;
			
			if(pattern.test(document.location.href))
				rewrite = false;
			
//			document.URL
			//document.location.href = document.location.href = Drupal.settings.basePath + ( rewrite ? '' : '?q=' ) + Drupal.settings.pathPrefix + 'catalog' + '/' + keys;
                        document.location.href = 'http://' + document.location.host + Drupal.settings.basePath + ( rewrite ? '' : '?q=' ) + Drupal.settings.pathPrefix + 'catalog' + '/' + keys;
		});
		/**
		 * Search box handler
		 */
		var searchBoxDefault = 'Inserisci il testo per la ricerca';
		$('input#main-search-bar').focus(function(){
			if($.trim($(this).attr("value"))==searchBoxDefault) {
				$(this).addClass('no-italic');
				$(this).attr("value", ""); 
			}
		}).blur(function(){ 
			if($.trim($(this).attr("value"))=="") {
				$(this).removeClass('no-italic');
				$(this).attr("value", searchBoxDefault);
			}
		}).keypress(function(e)
                {
                    code= (e.keyCode ? e.keyCode : e.which);
		    if (code == 13) 
            	    {
              		e.preventDefault();
              		$('#main-search-button').trigger('click');
                    }
               });
		
	});
})(jQuery);
