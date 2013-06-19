Ext.define('SF.mixin.SearchTab', {
    autoScroll: true,
    bodyPadding: '10px 15px',
    
    validateDates: function(start, end, mustHaveEnd) {
        var startDate = $(start),
            startDateValue = startDate.val(),
            endDate = $(end),
            endDateValue = endDate.val(),
            dtRegex = /^\d{1,2}\/\d{1,2}\/\d{4}$/;
    
        function checkDate(dateValue) {
            if(!dtRegex.test(dateValue)) {
                return false;
            } else {
                if (!getDateFromString(dateValue)) {
                    return false;
                }
            }
            return true;
        }
        
        function getDateFromString(dateString) {
            var data = dateString.split('/'),
                dd = parseInt(data[0],10),
                mm = parseInt(data[1],10),
                yyyy = parseInt(data[2],10),
                date = new Date(yyyy,mm-1,dd);
            if (!((date.getFullYear() == yyyy) && (date.getMonth() == mm - 1) && (date.getDate() == dd))) {
                return false;
            }
            return date;
        }

        if (startDateValue === "") {
            if (endDateValue !== "") {
                startDate.parent().parent().siblings('.startError').children('.error-icon').attr('data-original-title','Cannot be empty if end date is set').show();
                Ext.getCmp('newSearch').isValid = false;
                return;
            }
        } else {
            if (!checkDate(startDateValue)) {
                startDate.parent().parent().siblings('.startError').children('.error-icon').attr('data-original-title','Please enter a date in the format: dd/mm/yyyy').show();
                Ext.getCmp('newSearch').isValid = false;
                return;
            }
            if (endDateValue !== "") {
                if (!checkDate(endDateValue)) {
                    endDate.parent().parent().siblings('.endError').children('.error-icon').attr('data-original-title','Please enter a date in the format: dd/mm/yyyy').show();
                    Ext.getCmp('newSearch').isValid = false;
                    return;
                }
                var from = getDateFromString(startDateValue);
                var to = getDateFromString(endDateValue);
                if (from > to) {
                    startDate.parent().parent().siblings('.startError').children('.error-icon').attr('data-original-title','Please ensure the start date comes before the end date').show();
                    Ext.getCmp('newSearch').isValid = false;
                    return;
                }
            }
            if (mustHaveEnd && endDateValue === "") {
                endDate.parent().siblings('.endError').children('.error-icon').attr('data-original-title','Cannot be empty if start date is set').show();
                Ext.getCmp('newSearch').isValid = false;
                return;
            }
        }
    },
    
    returnFilterAsJson: function(formContainer) {
        var filterAsJson = {};
        var serializedForm = $(formContainer + ' .search-form').serializeArray();
        $.each(serializedForm, function() {
            if (filterAsJson[this.name]) {
                if (!filterAsJson[this.name].push) {
                    filterAsJson[this.name] = [filterAsJson[this.name]];
                }
                filterAsJson[this.name].push(this.value || '');
            } else {
                filterAsJson[this.name] = this.value || '';
                filterAsJson[this.name] = [filterAsJson[this.name]];
            }
        });
        $.each(filterAsJson, function() {
            if(!filterAsJson["incidentTypes"]) {
                filterAsJson["incidentTypes"] = [];
            }
            if(!filterAsJson["accounts"]) {
                filterAsJson["accounts"] = [];
            } else {
                if(filterAsJson["accounts"][0] == "") {
                    filterAsJson["accounts"].splice(0,1);
                }
            }
            if(!filterAsJson["locationTech"]) {
                filterAsJson["locationTech"] = [];
            } else {
                if(filterAsJson["locationTech"][0] == "") {
                    filterAsJson["locationTech"].splice(0,1);
                }
            }
            if(!filterAsJson["position"]) {
                filterAsJson["position"] = [];
            } else {
                if(filterAsJson["position"][0] == "") {
                    filterAsJson["position"].splice(0,1);
                }
            }
            if(!filterAsJson["frequency"]) {
                filterAsJson["frequency"] = [];
            } else {
                if(filterAsJson["frequency"][0] == "") {
                    filterAsJson["frequency"].splice(0,1);
                }
            }
        });
        filterAsJson["cellID"] = filterAsJson["cellID"][0];
        filterAsJson["msisdn"] = filterAsJson["msisdn"][0];

        //lastly make sure the incidentTypes array thing is fine
        var newIncidentTypesArray = [];
        for(var i = 0; i<filterAsJson["incidentTypes"].length; i++) {
            if (filterAsJson["incidentTypes"][i].substring(1).indexOf(",") != -1) {
                newIncidentTypesArray = newIncidentTypesArray.concat(filterAsJson["incidentTypes"][i].substring(1).split(","));
            } else {
                newIncidentTypesArray.push(filterAsJson["incidentTypes"][i]);
            }
        }
        filterAsJson["incidentTypes"] = newIncidentTypesArray;
		
        return filterAsJson;
    }
});


