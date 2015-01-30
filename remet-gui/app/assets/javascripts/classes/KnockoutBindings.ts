///<reference path="../libs/jqueryui/jqueryui.d.ts"/>
///<reference path="../libs/jqueryui/jqrangeslider.d.ts"/>
///<reference path="../libs/knockout/knockout.d.ts"/>
///<reference path="../libs/typeahead/typeahead.d.ts" />
///<reference path="./BrowseNode.ts"/>
///<amd-dependency path="jquery.ui"/>
///<amd-dependency path="jqrangeslider"/>
///<amd-dependency path="typeahead" />

import ko = require('knockout');
import $ = require('jquery');

module kobindings {
    ko.bindingHandlers['slider'] = {
        init: function(element, valueAccessor) {
            // First get the latest data that we're bound to
            var value = valueAccessor();
            var valueUnwrapped: any = ko.utils.unwrapObservable(value);
            $(element).rangeSlider(valueUnwrapped);
            $(element).bind("valuesChanging", function (e, data) {
                valueUnwrapped.slide(e, data);
            });
        }
    };

    ko.bindingHandlers['slide'] = {
        update: function(element, valueAccessor, allBindingsAccessor) {
            var shouldShow = ko.utils.unwrapObservable(valueAccessor());
            var bindings = allBindingsAccessor();
            var direction = ko.utils.unwrapObservable(bindings.direction);
            var duration = ko.utils.unwrapObservable<number>(bindings.duration) || 400;
            var after: Function = ko.utils.unwrapObservable<Function>(bindings.after);

            var effectOptions = { "direction": direction };

            if (shouldShow) {
                after();
                $(element).show("slide", effectOptions, duration);
            } else {
                $(element).hide("slide", effectOptions, duration, after);
            }

        }
    };

    ko.bindingHandlers['tooltip'] = {
        init: function(element, valueAccessor) {
            var value = valueAccessor();
            var valueUnwrapped = ko.utils.unwrapObservable(value);
            //TODO: tooltip this
        }
    };

    ko.bindingHandlers['stackdrag'] = {
        init: function(element, valueAccessor) {
            var thisLevel = $(element).parent().children();
            var value = valueAccessor();
            var valueUnwrapped = ko.utils.unwrapObservable(value);

            $.each(thisLevel, function(index, e) { $(e).draggable(valueUnwrapped); });
        }
    };

    ko.bindingHandlers['legendBlock'] = {
        init: function(element, valueAccessor) {
            // First get the latest data that we're bound to
            var value = valueAccessor();

            // Next, whether or not the supplied model property is observable, get its current value
            var valueUnwrapped = ko.utils.unwrapObservable(value);

            var context = element.getContext('2d');

            context.beginPath();
            context.rect(3, 3, element.width - 6, element.height - 6);
            context.fillStyle = valueUnwrapped;
            context.fill();
            context.lineWidth = 2;
            context.strokeStyle = '#F0F0F0';
            context.stroke();
        },
        update: function(element, valueAccessor) {
            // First get the latest data that we're bound to
            var value = valueAccessor();

            // Next, whether or not the supplied model property is observable, get its current value
            var valueUnwrapped = ko.utils.unwrapObservable(value);

            var context = element.getContext('2d');
            context.clearRect(0, 0, element.width, element.height);
            context.beginPath();
            context.rect(3, 3, element.width - 6, element.height - 6);
            context.fillStyle = valueUnwrapped;
            context.fill();
            context.lineWidth = 2;
            context.strokeStyle = '#F0F0F0';
            context.stroke();
        }
    };

    ko.bindingHandlers["typeahead"] = {
        init: function(element, valueAccessor, allValuesAccessor) {
            var value = valueAccessor();
            var valueUnwrapped: any = ko.utils.unwrapObservable(value);

            var ta = $(element).typeahead(valueUnwrapped.options.opt, valueUnwrapped.options.source);

            if (valueUnwrapped.value !== undefined) {
                ta.data().ttTypeahead.input.onSync("queryChanged", () => {
                    valueUnwrapped.value(ta.typeahead('val'));
                });

                ta.on('typeahead:autocompleted', () => {
                    valueUnwrapped.value(ta.typeahead('val'));
                });

                ta.on('typeahead:selected', () => {
                    valueUnwrapped.value(ta.typeahead('val'));
                });

                //Hack to handle the clearing of the query
                valueUnwrapped.value.subscribe((newValue) => {
                    if (newValue == "") {
                        ta.typeahead('val', '');
                    }
                });
            }
        }
    }
}

export = kobindings;
