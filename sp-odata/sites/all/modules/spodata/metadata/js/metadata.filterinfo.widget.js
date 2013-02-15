(function($){
	
	var FilterInfoWidget = function() {
		// private vars and method
		var defaultOpt = { 
				separator: '<span class="separator"> o </span>', 
				treeSeparator: '<span class="separator"> &gt;&gt; </span>',
				tree: false
			},
			init,
			options,
			filters = {},
			widget,
		// private function
		buildTreeSummaryText = function(treeLevel, selectedItem, vocabulary) {
				// label del nodo corrente
			var filter = treeLevel.tid===0 ? '' : selectedItem[treeLevel.tid],
				children_filter_array = [],		
				// numero di elementi ottenuti con la ricorsione
				temp, 
				i;
			
			// scorro i figli del nodo corrente
			for(i=0; i<treeLevel.childrens.length; i++) {
				// passo ricorsivo
				temp = buildTreeSummaryText(MetadataSearch.Terms[vocabulary][treeLevel.childrens[i]], selectedItem, vocabulary);
				// se ho ottenuto qualcosa dalla ricorsione aggiungo
				if(temp!=undefined && temp != null && temp.length>0 ) {
					$.merge(children_filter_array, temp);
				}
			}
			
			// se non ha figli aggiungo il tag del nodo corrente e lo restituisco
			if(children_filter_array.length===0 && filter!=undefined) {
			    children_filter_array.push(filter);
			}
			else {
				// remove span and add separator
				if(filter!=undefined && $.trim(filter)!=='')
					filter = '<span class="summary-term">' + filter.match(/\w+/i)[0] + '</span> <span class="tree-level-separator"/>' + 
						 '<img src="http://' + location.host + '/sites/all/modules/spodata/metadata/img/tree-tags-separator.png" class="tree-level-separator" alt="child tag">';
					
				// aggiungo in tag padre ai figli e restituisco l'array
				for(i=0; i<children_filter_array.length;i++)
					children_filter_array[i] = (filter==undefined ? '' : filter) + children_filter_array[i];
			}
			
			
			return children_filter_array;
		},
		/**
		 * get summary.
		 */
		loadSummaryText = function loadSummaryText(lastInputChecked) {
			var count=0;
			// delete previous.
			filters = { };
			// foreach input container
			for(var i=0; i<options.inputContainers.length;i++) {
				checkedInput = $(options.inputContainers[i].selector + ' input:checked');
				filters[options.inputContainers[i].label] = '';
				
				// if tree
				if(options.inputContainers[i].tree!=undefined && options.inputContainers[i].tree) {
					var tid,
						selectedTid = {count: 0},
						label,
						temp='',
						or_separator = ' <span class="separator"> o </span> ';
					// get checked options
					for(var j=0; j<checkedInput.length;j++) {
						tid = $(checkedInput[j]).val();
						label = $(checkedInput[j]).next('label').html();
						
						selectedTid[tid] = label;
						selectedTid.count++;
					}
					// build summary
					if(selectedTid.count>0) {
						filters[options.inputContainers[i].label] = 
							buildTreeSummaryText(MetadataSearch.Terms[options.inputContainers[i].voc][0], 
																	selectedTid, options.inputContainers[i].voc);
						
						for(var j=0;j<filters[options.inputContainers[i].label].length; j++)
							temp += (j!=0 ? or_separator : '') + filters[options.inputContainers[i].label][j];
						   
						filters[options.inputContainers[i].label]= temp;
						count++;
					}
				}
				else {
					
					// get checked options
					for(var j=0; j<checkedInput.length;j++) {
						var term = $(checkedInput[j]).next('label').html();
						
						if(j!==0)
							filters[options.inputContainers[i].label] += options.separator;
						// add a checked term
						filters[options.inputContainers[i].label] += '<span class="summury-term">' + 
								$(checkedInput[j]).siblings('label').html() + '</span>';
						// checked input count
						count++;
					}
				}
			}
			
			return count>0;
		},
		/**
		 * Update filter summary content.
		 */
		writeContent = function writeContent(lastInputChecked) {
			var tmp_content = '',
			// clean and load new summary.
				filterExists = loadSummaryText(lastInputChecked);
			
			if(filterExists) {
				//tmp_content = '<div class="summary-filter"><span class="summary-filter-title">Filtrati per</span></div>';
				$('.filtered-search').show(0);
			}
			else {
				$('.filtered-search').hide(0);
			}
			// foreach input into filter container => add checked input to filter summary.
			for(var i=0; i<options.inputContainers.length;i++) {
				if(filters[options.inputContainers[i].label]!=undefined && filters[options.inputContainers[i].label]!=='') {
					tmp_content += '<div class="summary-filter"><span class="summary-filter-label">' + options.inputContainers[i].label +
						':</span>' + filters[options.inputContainers[i].label] + '</div>';
				}
			}
			
			$(widget).html(tmp_content);
		},
		/**
		 * Initialize widget.
		 * 
		 * @params string widgetSelector selector to filter summary container.
		 * @params object opt widget options. 
		 * 					{ inputContainers : [  
		 *		            	{ selector : 'input_container_selector', label: 'label showed into summary' },
		 *						... 
		 *					}
		 */ 
		init = function init(widgetSelector, opt) {
			var checkedInput;
			
			options = $.extend(defaultOpt, opt, true);
			widget = widgetSelector;
			
			if(options.inputContainers!=undefined && $.isArray(options.inputContainers)) {
				
				// foreach input into filter container => bind change event handler.
				for(var i=0; i<options.inputContainers.length;i++) {
					$(options.inputContainers[i].selector + ' input').change(updateFilter);
				}
				
				// write content
				writeContent();
				
				// update summary on 
				$('[name="term_updater"]').bind('onTermCountUpdated', updateFilter);
			}
		},
		// callback function
		updateFilter = function updateFilter() {
			writeContent($(this));
		};
		
		// public 
		return {
			init : init
		};
	};
	
	$(document).ready(function() {
		var widgetOptions = { 
				inputContainers : [  
				                   { selector : '.term-reference-tree-level', label: 'Formato', tree: true, voc: 'metadata_lu_format' },
				                   { selector : '#edit-metadata-author', label: 'Fonte' },
				                   { selector : '#edit-metadata-filter-metadata-lu-type', label: 'Parole chiave' },
				                   { selector : '#edit-metadata-filter-metadata-lu-license', label: 'Licenza' },
				                   { selector : '#edit-metadata-filter-metadata-lu-category', label: 'Tematica' },
				                   { selector : '#edit-metadata-filter-metadata-lu-datasources', label: 'Banca dati' }
				                     ],  
		};
		
		// start widget
		FilterInfoWidget().init('#search-filters-summary', widgetOptions);
	});
	
})(jQuery);