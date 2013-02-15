/* Created by jankoatwarpspeed.com */

(function($) {
    $.fn.formToWizard = function(options) {
        options = $.extend({  
            submitButton: ''  
        }, options); 
        
        var element = this;

        var steps = $(element).find("fieldset");
        var count = steps.size();
        var submmitButtonName = "#" + options.submitButton;
        $(submmitButtonName).hide();

        // 2
        $(element).before("<ul id='steps'></ul>");

        steps.each(function(i) {
            $(this).wrap("<div id='step" + i + "'></div>");
            $(this).append("<p id='step" + i + "commands' class='etl-wizard-step-buttons'></p>");

            // 2
            var name = $(this).find("legend").html();
            $("#steps").append("<li id='stepDesc" + i + "'>Passo " + (i + 1) + "<span>" + name + "</span></li>");

            if (i == 0) {
                createNextButton(i);
                selectStep(i);
            }
            else if (i == count - 1) {
                $("#step" + i).hide();
                createPrevButton(i);
            }
            else {
                $("#step" + i).hide();
                createPrevButton(i);
                createNextButton(i);
            }
        });

        function createPrevButton(i) {
            var stepName = "step" + i;
            $("#" + stepName + "commands").append("<a href='#' id='" + stepName + "Prev' class='prev'>< Indietro</a>");

            $("#" + stepName + "Prev").bind("click", function(e) {
                $("#" + stepName).hide();
                $("#step" + (i - 1)).show();
                $(submmitButtonName).hide();
                selectStep(i - 1);
            });
        }

        function createNextButton(i) {
            var stepName = "step" + i;
            $("#" + stepName + "commands").append("<a href='#' id='" + stepName + "Next' class='next'>Avanti ></a>");

            $("#" + stepName + "Next").bind("click", function(e) {
            	StatPortalOpenData.ETL.addLoading();
            	if(i == StatPortalOpenData.ETL.EXTRACT_INFO_INDEX){
            		StatPortalOpenData.ETL.extractInfo();
            	}else if(i == StatPortalOpenData.ETL.DETAILS_INFO_INDEX){
            		StatPortalOpenData.ETL.passToColumnsDetails();
            	}else if(i == StatPortalOpenData.ETL.LOAD_DATA_INDEX){
            		StatPortalOpenData.ETL.verifyUniquenessAndLoadData();
            	}
            	else
            	{
            		CustomEventStepsETLhandler(null, 1);
            	}
            });
        }
        
        $('#customEventStepsETL').unbind('CustomEventStepsETL', CustomEventStepsETLhandler);
        $('#customEventStepsETL').bind('CustomEventStepsETL', CustomEventStepsETLhandler);
        
        function CustomEventStepsETLhandler(event, i) {
        	$("#" + "step" + i).hide();
            $("#step" + (i + 1)).show();
            if (i + 2 == count)
                $(submmitButtonName).show();
            selectStep(i + 1);	
            // facciamo sparire il loading
            StatPortalOpenData.ETL.removeLoading();
        }

        function selectStep(i) {
            $("#steps li").removeClass("current");
            $("#stepDesc" + i).addClass("current");
        }

    }
})(jQuery); 