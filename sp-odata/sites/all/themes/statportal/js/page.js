(function($) {
	window.getWindowSize = function getWindowSize()
	{

		var myWidth = 0, myHeight = 0;
		if( typeof( window.innerWidth ) == 'number' ) 
		{
			//Non-IE
			myWidth = window.innerWidth;
			myHeight = window.innerHeight;
		} 
		else if( document.documentElement && ( document.documentElement.clientWidth || document.documentElement.clientHeight ) ) 
		{
			//IE 6+ in 'standards compliant mode'
			myWidth = document.documentElement.clientWidth;
			myHeight = document.documentElement.clientHeight;
		} 
		else if( document.body && ( document.body.clientWidth || document.body.clientHeight ) ) 
		{
			//IE 4 compatible
			myWidth = document.body.clientWidth;
			myHeight = document.body.clientHeight;
		}
		
		return {'height' : myHeight-15, 'width': myWidth-30 };
	};
	
	function resize_content() {
		var window_size = window.getWindowSize(),
		content = $('#main-wrapper'),
		content_height = content.height(),
		footer_height = $('#footer-wrapper').height(),
		header_height = $('#header').height(),
		menu_height = $('#main-menu').height(),
		// admin
		toolbar = $('#toolbar'),
		toolbar_height = toolbar.length===0 ? 0 : toolbar.height(),
		page_height = content_height+footer_height+header_height+menu_height+toolbar_height,
		offset = window_size.height-page_height;
	
		if(offset>0 && !/http:\/\/[\w\.]+\/\w{2}\/faq/i.test(document.URL)) {
			content.height(content_height+offset);
		}
	}
	
	$(document).ready(function() {
		resize_content();
	});
	
	$(window).resize( resize_content);
	
})(jQuery);