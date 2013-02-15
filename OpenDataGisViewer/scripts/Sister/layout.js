$(document).ready(function () {	
	//Per richieste crossdomain 
	jQuery.support.cors = true;
	
	//Layout
	var mainLayout = $('body').layout({ 
		applyDefaultStyles: true,		
		north__size:0,north__closable:false,north__resizable:false,
		east__size:300,east__closable:false,east__resizable:false,
		south__size:45,south__closable:false,south__resizable:false
	});
	
	// qTip plugin
	$('#btnPrev[title], #btnNext[title], button[title]').qtip({ 
		style: { 
			name: 'light', 
			tip: true			
		}
	});
	
	$("#overviewPanelBtn").live("click",function(){
		if($(".olControlOverviewMapElement").is(":visible")){
			 closeOverviewMap();
		}else{
			 openOverviewMap();
		}
	});
	
	hideLegendPanel();
	hideLegendBorderPanel();
	
	initMap();
	
});

$(function() {
	$.format.locale({
	    number: {
	        groupingSeparator: '.',
	        decimalSeparator: ','
	    }
	});
	
	
	//base maps
	$( "#street" ).button()
	.click(function() {
		map.setBaseLayer(gmap);
	});
	
	$( "#sat" ).button()
	.click(function() {
		map.setBaseLayer(gsat);
	});
	
	$( "#hyb" ).button()
	.click(function() {
		map.setBaseLayer(ghyb);
	});
	
	$( "#phy" ).button()
	.click(function() {
		map.setBaseLayer(gphy);
	});
			
	$( "#base_maps" ).buttonset();
		
	if ((mapType != "undefined") &&(mapType !="") ){
		$("#"+mapType).button().click();
	}
	else{
		$("#street").button().click();
	}
});

/*autocomplete variabile da tematizzare*/
(function( $ ) {
	$.widget( "ui.combobox", {
		_create: function() {
			var self = this,
				select = this.element.hide(),
				selected = select.children( ":selected" ),
				value = selected.val() ? selected.text() : "";
			var input = this.input = $( "<input>" )
				.insertAfter( select )
				.val( value )
				.autocomplete({
					delay: 0,
					minLength: 0,
					source: function( request, response ) {
						var matcher = new RegExp( $.ui.autocomplete.escapeRegex(request.term), "i" );
						response( select.children( "option" ).map(function() {
							var text = $( this ).text();
							if ( this.value && ( !request.term || matcher.test(text) ) )
								return {
									label: text.replace(
										new RegExp(
											"(?![^&;]+;)(?!<[^<>]*)(" +
											$.ui.autocomplete.escapeRegex(request.term) +
											")(?![^<>]*>)(?![^&;]+;)", "gi"
										), "<strong>$1</strong>" ),
									value: text,
									option: this
								};
						}) );
					},
					select: function( event, ui ) {						
						ui.item.option.selected = true;
						self._trigger( "selected", event, {
							item: ui.item.option
						});						
						legendDoClassify();
					},
					change: function( event, ui ) {						
						if ( !ui.item ) {
							var matcher = new RegExp( "^" + $.ui.autocomplete.escapeRegex( $(this).val() ) + "$", "i" ),
								valid = false;
							select.children( "option" ).each(function() {
								if ( $( this ).text().match( matcher ) ) {
									this.selected = valid = true;
									return false;
								}
							});
							if ( !valid ) {
								// remove invalid value, as it didn't match anything
								$( this ).val( "" );
								select.val( "" );
								input.data( "autocomplete" ).term = "";
								return false;
							}
						}
					}
				})
				.addClass( "ui-widget ui-widget-content ui-corner-left" );
			
			input.data( "autocomplete" )._renderItem = function( ul, item ) {
				return $( "<li></li>" )
					.data( "item.autocomplete", item )
					.append( "<a>" + item.label + "</a>" )
					.appendTo( ul );
			};
			
			this.button = $( "<button type='button'>&nbsp;</button>" )
				.attr( "tabIndex", -1 )
				.attr( "title", "Mostra tutte" )
				.insertAfter( input )
				.button({
					icons: {
						primary: "ui-icon-triangle-1-s"
					},
					text: false
				})
				.removeClass( "ui-corner-all" )
				.addClass( "ui-corner-right ui-button-icon" )
				.click(function() {
					// close if already visible
					if ( input.autocomplete( "widget" ).is( ":visible" ) ) {
						input.autocomplete( "close" );
						return;
					}

					$( this ).blur();

					// pass empty string as value to search for, displaying all results
					input.autocomplete( "search", "" );
					input.focus();
				});
		},
		_init: function(){
			this.input.val(this.element.find("option:selected").text());
		},
		destroy: function() {
			this.input.remove();
			this.button.remove();
			this.element.show();
			$.Widget.prototype.destroy.call( this );
		}			
	});
})( jQuery );


$.widget( "custom.catcomplete", $.ui.autocomplete, {
	_renderMenu: function( ul, items ) {
		var self = this,
			currentCategory = "";
		$.each( items, function( index, item ) {
			if ( item.category != currentCategory ) {
				ul.append( "<li class='ui-autocomplete-category'>" + item.category + "</li>" );
				currentCategory = item.category;
			}
			self._renderItem( ul, item );
		});
	}
});
