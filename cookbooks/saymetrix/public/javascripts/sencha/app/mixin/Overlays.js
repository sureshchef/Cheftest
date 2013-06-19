Ext.define('SF.mixin.Overlays', {
    
    
    /* CUSTOM OVERLAY FUNCTIONS
    ******************************************************/
    showAllLayersInList: false,
    dropdownExpanderExists: false,
   
    addCustomLayerControls: function(layers) {
        if (layers.length > 0) {
            var $this = this;
            var optionLabelText = "Overlays";
            var controlDiv = document.createElement('DIV');
            // Setting padding to 5px will offset the control from the edge of the map.
            controlDiv.style.padding = '5px';

            var controlUI = document.createElement('div');
            controlUI.className = 'controlUI';
            controlDiv.appendChild(controlUI);

            var controlText = document.createElement('div');
            controlText.className = 'controlText';
            controlText.innerHTML = optionLabelText;
            controlUI.appendChild(controlText);

            var dropdown = document.createElement('DIV');
            dropdown.className = 'dropdown';
            controlUI.appendChild(dropdown);
            
            //figure out big the list should be
            var minNumLayers = 3;
            var noToAlwaysShow = 0;
            for (var i = 0; i < layers.length; i++) {
                if(layers[i].alwaysShow)
                    noToAlwaysShow++;
            }
            if (noToAlwaysShow > minNumLayers)
                minNumLayers = noToAlwaysShow;
            
            for (var i = 0; i < layers.length; i++) {
                if (layers.length <= minNumLayers) {   //if there are <= minNumLayers then display all of them in the list, without the expander
                    layers[i].alwaysShow = true;
                    $this.showAllLayersInList = false;
                } else {
                    $this.dropdownExpanderExists = true;
                }
                if (layers[i].type == "group") {
                    this.addLayerGroupToDropDown(layers[i], dropdown);
                } else {
                    this.addLayerToDropDown(layers[i], dropdown);
                }
            }

            if ($this.dropdownExpanderExists) {
                //only show expander if set to true
                var dropdownExpander = document.createElement('div');
                dropdownExpander.className = 'dropdownExpander showing alwaysShows';    //'showing' and 'alwaysShows' will make sure it displays on mouseenter
                dropdownExpander.innerHTML = '<i class="icon-chevron-down"></i>';

                dropdown.appendChild(dropdownExpander);
                $(dropdownExpander).click(function(){
                    $this.showAllLayersInList = true;
                    //figure out the best height for the dropdown
                    var maxDropdownHeight = $this.adjustHeightOfOverlayControl();
                    $(dropdown).animate({height: maxDropdownHeight+"px"}, 150);
                    //show all the items in the dropdown and hide the expander
                    $(dropdown).children().addClass("showing");
                    $(dropdownExpander).removeClass("showing");
                });
            }

            //using jquery to do the hover stuff, as the google events were triggering several times
            $(controlUI)
            .mouseenter(function() {
                $(controlUI).addClass('getFocus');  //setting a timeout so that the controls don't pop out everytime you hover over it
                setTimeout(function() {
                    if ($(controlUI).hasClass('getFocus')) {
                        $(controlUI).addClass('hasFocus');
                        $this.adjustHeightOfOverlayControl();   //figure out best height for the dropdown (so user can see bottom if on small screen)
                        //animate other features
                        $(controlUI).animate({width: "175px"}, 200);
                        $(controlText).animate({paddingLeft: "28px"}, 200);
                    }
                }, 300);
            })
            .mouseleave(function() {
                $(controlUI).removeClass("getFocus");
                if($(controlUI).hasClass("hasFocus")) {
                    $(controlUI).removeClass("hasFocus");
                    //animate features back to orig
                    $(dropdown).animate({height: "0px"}, 200);
                    $(controlUI).animate({width: "75px"}, 200);
                    $(controlText).animate({paddingLeft: "11px"}, 200, function() { //as the last animation, it will be the one to do a callback function
                        $(dropdown).children().removeClass("showing");  //hide all items in the dropdown
                        $('.alwaysShows, .keep').addClass('showing');   //then re-show only the items that have the class 'alwaysShows' or 'keep'
                        $this.showAllLayersInList = false;
                    });
                }
            });
            controlDiv.index = 1;
            this.getMap().controls[google.maps.ControlPosition.TOP_RIGHT].push(controlDiv);
        }
    },
    
    addLayerToDropDown: function(layer, container) {
        var map = this.getMap();
        
        //Note: JQuery way of creating elements, possiby faster, possibly not
        //var layerContainer = $('<div>', {class: 'layerContainer', title: layer.name});
        //$(container).append(layerContainer);
        var layerContainer = document.createElement('DIV');
        layerContainer.className = 'layerContainer';
        layerContainer.title = layer.name;
        container.appendChild(layerContainer);
        if (layer.alwaysShow) {
            $(layerContainer).addClass('showing alwaysShows');
        }
        
        var layerIcon = document.createElement('DIV');
        layerIcon.className = 'layerIcon';
        layerContainer.appendChild(layerIcon);
        
        var layerLabel = document.createElement('DIV');
        layerLabel.className = 'layerLabel';
        layerLabel.innerHTML = layer.name;
        layerContainer.appendChild(layerLabel);
        
        if (layer.enabled) {
            $(layerContainer).addClass('active').addClass('keep');
            if (layer.type == 'imageOverlay') {
                map.overlayMapTypes.push(layer.layer);
            } else if (layer.type == "markers") {
                layer.layer.enable();
            } else {
                layer.layer.setMap(map);
            }
        }
        
        google.maps.event.addDomListener(layerContainer, 'click', function() {
            if (layer.type == 'imageOverlay') {
                if(map.overlayMapTypes.getLength() == 0) {
                    $(layerContainer).addClass('active').addClass('keep');
                    map.overlayMapTypes.push(layer.layer);
                } else {
                    $(layerContainer).removeClass('active').removeClass('keep');
                    map.overlayMapTypes.pop();
                }
            } else if (layer.type == "markers") {
                if (layer.layer.enabled) {
                    $(layerContainer).removeClass('active').removeClass('keep');
                    layer.layer.disable();
                } else {
                    $(layerContainer).addClass('active').addClass('keep');
                    layer.layer.enable();
                }
            } else {
                if(layer.layer.map == null) {
                    $(layerContainer).addClass('active').addClass('keep');
                    layer.layer.setMap(map);
                } else {
                    $(layerContainer).removeClass('active').removeClass('keep');
                    layer.layer.setMap(null);
                }
            }
        });
        google.maps.event.addListener(layer.layer, 'status_changed', function () {
            if (layer.layer.getStatus() != 'OK') {
                //something went wrong
                console.log('KML load: ' + layer.layer.getStatus());
                //display an error and remove layer
                $(layerContainer).removeClass('active');
                layer.layer.setMap(null);
                Ext.Msg.alert('Overlay Failed', 'The overlay \'' + layer.name + '\' failed to load properly.');
            }
        });
    },
    
    addLayerGroupToDropDown: function(layer, container) {
        var $this = this;
        
        /* CREATING DIVS
         ******************************************************/
        //main container
        var layerContainer = document.createElement('DIV');
        layerContainer.className = 'layerContainer groupContainer';
        layerContainer.title = layer.name;
        container.appendChild(layerContainer);
        $(layerContainer).attr('id', layer.id);
        if (layer.alwaysShow) {
            $(layerContainer).addClass('showing alwaysShows');
        }
        
        //container for the label
        var layerContainerInner = document.createElement('DIV');
        layerContainerInner.className = 'layerContainerInner';
        layerContainer.appendChild(layerContainerInner);
        
        //label icon
        var layerIcon = document.createElement('DIV');
        layerIcon.className = 'layerIcon';
        layerContainerInner.appendChild(layerIcon);
        
        //label text
        var layerLabel = document.createElement('DIV');
        layerLabel.className = 'layerLabel';
        layerLabel.innerHTML = layer.name;
        layerContainerInner.appendChild(layerLabel);
        
        //drop down icon
        var layerDropIcon = document.createElement('DIV');
        layerDropIcon.className = 'layerDropIcon';
        layerContainerInner.appendChild(layerDropIcon);
        
        var arrowleft = document.createElement('DIV');
        arrowleft.className = 'arrow-left';
        layerDropIcon.appendChild(arrowleft);
        var arrowdown = document.createElement('DIV');
        arrowdown.className = 'arrow-down';
        layerDropIcon.appendChild(arrowdown);
        
        /* CREATING DROPDOWN AREA
         ******************************************************/
        //drop down container
        var defaultOption = false, defaultOptionId = false;
        var layerGroupDropdown = document.createElement('div');
        layerGroupDropdown.className = 'layerGroupDropdown';
        for (var i = 0; i < layer.layers.length; i++) {
            var newLayer = this.addOptionToLayerGroup(i, layer.layers, layerGroupDropdown, layer.id);
            if (layer.layers[i].defaultOption) {
                defaultOptionId = i;
                defaultOption = newLayer;
            }
        }
        if (defaultOptionId === false) {
            defaultOptionId = 0;
            defaultOption = layer.layers[0];
        }
        $(layerContainer).attr('defaultOption', defaultOptionId);
        layerContainer.appendChild(layerGroupDropdown);
        $(layerGroupDropdown).attr('showing', 'no');
        $(layerGroupDropdown).attr('keepShowing', 'yes');
        $(arrowleft).addClass('showing');
        
        /* EVENT HANDLING
         ******************************************************/
        var hideDropdown = function() {
            $(arrowdown).removeClass('showing');
            $(arrowleft).addClass('showing');
            $(layerContainer).animate({height: "26px"}, 200);
            $this.adjustHeightOfOverlayControl();
        }
        
        var showDropdown = function() {
            $(arrowdown).addClass('showing');
            $(arrowleft).removeClass('showing');
            var dropheight = (($(layerGroupDropdown).children().length) * 18)+32;
            $(layerContainer).animate({height: dropheight+"px"}, 200);
            $this.adjustHeightOfOverlayControl();
        }
        
        //event handler to turn on/off deafultOption
        $(layerIcon).click(function(){
            labelClickFunction();
        });
        $(layerLabel).click(function(){
            labelClickFunction();
        });
        var labelClickFunction = function() {
            //check if have active class
            var source = $('#'+layer.id),
            //get current layer, using attribute defaultOption
            curLayerId = $('#'+layer.id).attr('defaultOption'),
            curLayer = layer.layers[curLayerId];
            if (source.hasClass('active')) {
                //disable it
                //remove classes from option
                $this.disableOverlay(curLayer.layer, curLayer.type, '#'+layer.id + ' .active');
                //remove label classes
                $('#'+layer.id).removeClass('active').removeClass('keep');
                //hide dropdown icon
                $(layerDropIcon).removeClass('showing');
                //hide dropdown if it is showing
                if ($(layerGroupDropdown).attr('showing') == "yes") {
                    $(layerGroupDropdown).attr('keepShowing', 'no');
                    hideDropdown();
                }
            } else {
                //enable it
                //add classes to option
                $this.enableOverlay(curLayer.layer, curLayer.type, '#'+layer.id + ' .' + curLayerId);
                //add classes to label
                $('#'+layer.id).addClass('active').addClass('keep');
                //show dropdown icon
                $(layerDropIcon).addClass('showing');
                //show dropdown if it is supposed to be showing
                if ($(layerGroupDropdown).attr('showing') == "yes") {
                    $(layerGroupDropdown).attr('keepShowing', 'yes');
                    showDropdown();
                }
            }
        }
        
        //event handler to show/hide the group contents
        $(layerDropIcon).click(function(){
            if ($(layerGroupDropdown).attr('showing') != "yes") {
                $(layerGroupDropdown).attr('showing', 'yes');
                showDropdown();
            } else {
                $(layerGroupDropdown).attr('showing', 'no');
                hideDropdown();
            }
        });
        
        /* ENABLED DEFAULTS
         ******************************************************/
        if (layer.enabled) {
            var source = $(layerContainer),
            enabledLayer = layer.layers[defaultOptionId];
            $this.enableOverlay(enabledLayer.layer, enabledLayer.type, defaultOption);
            source.addClass('active').addClass('keep');
            $(layerDropIcon).addClass('showing');
            enabledLayer.enabled = true;
        }
    },
    
    addOptionToLayerGroup: function(curLayer, layers, container, parentId) {
        var layer = layers[curLayer];
        var $this = this;
        
        var layerContainer = document.createElement('DIV');
        layerContainer.className = 'layerOptionContainer '+curLayer;
        layerContainer.title = layer.name;
        container.appendChild(layerContainer);
        
        var layerIcon = document.createElement('DIV');
        layerIcon.className = 'layerOptionIcon';
        layerContainer.appendChild(layerIcon);
        
        var layerLabel = document.createElement('DIV');
        layerLabel.className = 'layerOptionLabel';
        layerLabel.innerHTML = layer.name;
        layerContainer.appendChild(layerLabel);
        
        $(layerContainer).data('layerObj', layer);
        
        $(layerContainer).click(function() {
            if(!layer.enabled) {
                //first check if another layer is active (label.hasClass(active))
                if ($('#'+parentId).hasClass('active')) {
                    //another layer is enabled, it can be found using the attribute defaultOption
                    var enabledId = $('#'+parentId).attr('defaultOption');
                    var enabledLayer = layers[enabledId];
                    //disable the layer
                    //remove its classes
                    enabledLayer.enabled = false;
                    $this.disableOverlay(enabledLayer.layer, enabledLayer.type, '#'+parentId + ' .active');
                    //remove classes from label
                    $('#'+parentId).removeClass('active');
                }
                //enable me
                //add my classes
                layer.enabled = true;
                $this.enableOverlay(layer.layer, layer.type, layerContainer);
                //add classes to label
                $('#'+parentId).addClass('active').addClass('keep');
                //set myself as the defaultOption
                $('#'+parentId).attr('defaultOption', curLayer);
            }
        });
        
        google.maps.event.addListener(layer.layer, 'status_changed', function () {
            if (layer.layer.getStatus() != 'OK') {
                //something went wrong
                console.log('KML load: ' + layer.layer.getStatus());
                //display an error and remove layer
                $(layerContainer).removeClass('active').removeClass('keep');
                layer.layer.setMap(null);
                Ext.Msg.alert('Overlay Failed', 'The overlay \'' + layer.name + '\' failed to load properly.');
            }
        });
        return layerContainer;
    },
    
    enableOverlay: function (overlay, type, label) {
        var map = this.getMap();
        if (type == 'imageOverlay') {
            map.overlayMapTypes.push(overlay);
        } else {
            overlay.setMap(map);
        }
        $(label).addClass('active');
    },
    
    disableOverlay: function (overlay, type, label) {
        if (type == 'imageOverlay') {
            this.getMap().overlayMapTypes.pop();
        } else {
            overlay.setMap(null);
        }
        $(label).removeClass('active');
    },
    
    adjustHeightOfOverlayControl: function() {
        //get maxHeightOfMap
        var maxHeightOfMap = $('#map').height()-50;
        var heightFromLabelsToShow = 0;
        var numItems = 0;
        var extraHeightOfOpenDropdowns = 0;
        var finalHeight = 0;
        //if showAllLayersInList
        if (this.showAllLayersInList) {
            //get height of all layers together
            numItems = $('.controlUI .dropdown').children().length;
            heightFromLabelsToShow = ((numItems > 0) ? (numItems * 27) : 26);    //27 = height of each label (26 height, 1 border bottom)
            //if expander exists, add additional height for it.
            if (this.dropdownExpanderExists) {
                //remove 26 from height as its being counted as a child
                heightFromLabelsToShow -= 26;
            } else {
                heightFromLabelsToShow++;
            }
        } else {
            //get height of only layers to always show or to keep
            numItems = $('.alwaysShows, .keep').length;
            heightFromLabelsToShow = ((numItems > 0) ? (numItems * 27) : 26);    //27 = height of each label
            
            //if expander exists, add additional height for it.
            if (this.dropdownExpanderExists) {
                //add 19 for its height and remove 27 as its being counted as a child (net: 8)
                heightFromLabelsToShow -= 8;
            } else {
                heightFromLabelsToShow++;
            }
        }
        //then add any open dropdowns
        if ($('.layerGroupDropdown').length > 0) {
            $('.layerGroupDropdown').each(function(){
                if ($(this).attr('showing') == 'yes' && $(this).attr('keepShowing') == 'yes') {
                    extraHeightOfOpenDropdowns += (($(this).children().length) * 18)+6;
                }
            });
        }
        heightFromLabelsToShow += extraHeightOfOpenDropdowns;
        
        //make height = whatever is smallest
        finalHeight = ((heightFromLabelsToShow < maxHeightOfMap) ? heightFromLabelsToShow : maxHeightOfMap)-1;
        
        $('.controlUI .dropdown').animate({height: finalHeight+"px"}, 200);
    }
});


