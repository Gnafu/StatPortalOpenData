(function($) {
	$(document).ready(function() {
			var md_created = [],
				user_vote = [],
				relevance = [],
				i;
			
				function addChart(id_element, data, title, xlabel, ylabel) {
					$.jqplot(id_element, [ data ], {
										series : [ {
											showMarker : false
										} ],
										grid : {
											drawGridLines : false, // wether draw lines across the grid or not.
											gridLineColor : '#F7F7F7', // *Color of the grid lines.
											background : '#E9E9E9', // CSS color spec for background color of grid.
											borderColor : '#ddd', // CSS color spec for border around grid.
											borderWidth: 1.0,           // pixel width of border around grid.
									        shadow: false,               // draw a shadow for grid.
									        shadowAngle: 45,            // angle of the shadow.  Clockwise from x axis.
									        shadowOffset: 1.5,          // offset from the line of the shadow.
									        shadowWidth: 3,             // width of the stroke for the shadow.
									        shadowDepth: 3,             // Number of strokes to make when drawing shadow.
									                                    // Each stroke offset by shadowOffset from the last.
									        shadowAlpha: 0.07,           // Opacity of the shadow
									        renderer: $.jqplot.CanvasGridRenderer,  // renderer to use to draw the grid.
									        rendererOptions: {}         // options to pass to the renderer.  Note, the default
									                                    // CanvasGridRenderer takes no additional options.
										},
										title : title,
										axes : {
											xaxis : {
												// label:xlabel,
												labelRenderer : $.jqplot.CanvasAxisLabelRenderer,
												renderer : $.jqplot.DateAxisRenderer,
												tickOptions : {
													formatString : '%b %#d, %y'
												},

											},
											yaxis : {
												label : ylabel,
												labelRenderer : $.jqplot.CanvasAxisLabelRenderer
											}
										},
										cursor : {
											 style: 'crosshair',     // A CSS spec for the cursor type to change the
										                                // cursor to when over plot.
										        show: true,
										        showTooltip: true,      // show a tooltip showing cursor position.
										        followMouse: false,     // wether tooltip should follow the mouse or be stationary.
										        tooltipLocation: 'se',  // location of the tooltip either relative to the mouse
										                                // (followMouse=true) or relative to the plot.  One of
										                                // the compass directions, n, ne, e, se, etc.
										        tooltipOffset: 6,       // pixel offset of the tooltip from the mouse or the axes.
										        showTooltipGridPosition: false,     // show the grid pixel coordinates of the mouse
										                                            // in the tooltip.
										        showTooltipUnitPosition: true,      // show the coordinates in data units of the mouse
										                                            // in the tooltip.
										        tooltipFormatString: '%.4P',    // sprintf style format string for tooltip values.
										        useAxesFormatters: true,        // wether to use the same formatter and formatStrings
										                                        // as used by the axes, or to use the formatString
										                                        // specified on the cursor with sprintf.
										        tooltipAxesGroups: [],  // show only specified axes groups in tooltip.  Would specify like:
										                                // [['xaxis', 'yaxis'], ['xaxis', 'y2axis']].  By default, all axes
										                                // combinations with for the series in the plot are shown.
										},

									});
				}
				
				// add creation event chart
				for ( i = 0; i < StatPortalOpenData.Userprofile.created.length; i++) {
					md_created.push([StatPortalOpenData.Userprofile.created[i].date,
									StatPortalOpenData.Userprofile.created[i].tot ]);
				}
				if (md_created.length > 1)
					addChart('created-chart', md_created,Drupal.t('Schede totali'), Drupal.t('Data'), Drupal.t('Numero schede'));

				// add vote
				for ( i = 0; i < StatPortalOpenData.Userprofile.voted.length; i++) {
					user_vote.push([ StatPortalOpenData.Userprofile.voted[i].date,
									 StatPortalOpenData.Userprofile.voted[i].tot ]);
				}
				if (user_vote.length > 1)
					addChart('voted-chart', user_vote, Drupal.t('Valutazione'),Drupal.t('Data'), Drupal.t('Numero voti'));

				function w2date(year, wn, dayNb) {
					var j10 = new Date(year, 0, 10, 12, 0, 0), j4 = new Date(
							year, 0, 4, 12, 0, 0), mon1 = j4.getTime()
							- j10.getDay() * 86400000;
					return new Date(mon1 + ((wn - 1) * 7 + dayNb)
							* 86400000);
				};

				for ( i = 0; i < StatPortalOpenData.Userprofile.relevance.length; i++) {
					relevance.push([ w2date(2012, StatPortalOpenData.Userprofile.relevance[i].week,1),
									Number(StatPortalOpenData.Userprofile.relevance[i].relevance) ]);
				}
				if (relevance.length > 1)
					addChart('relevance-chart', relevance, Drupal.t('Rilevanza per settimana'),Drupal.t('Data'), Drupal.t('Rilevanza'));

			});
})(jQuery);