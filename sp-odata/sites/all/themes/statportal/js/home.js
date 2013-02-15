(function($) {
	
	$(document).ready(function(){ 
		// start slider
		$('#slidingFeatures').jshowoff({
			effect: 'slideLeft',
			hoverPause: true,
			controls: false,
			cssClass: 'slider',
			animatePause: true,
			speed: 7000,
			changeSpeed: 1000,
		});
		
		// hide loading animation
		$('.ajax-loading-animation-container').hide(100);
		

	});
	
} )(jQuery);