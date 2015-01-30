define(['durandal/composition','jquery', 'knockout'], function(composition, $, ko) {
  var ctor = function() { };

  ctor.prototype.activate = function(settings) {
    this.settings = settings;
  };

  var simulatedObservable = (function() {
    var timer = null;
    var items = [];

    var check = function() {
      items = items.filter(function(item) {
        return !!item.elem.parents('html').length;
      });
      if (items.length === 0) {
        clearInterval(timer);
        timer = null;
        return;
      }
      items.forEach(function(item) {
      item.obs(item.getter());
      });
    };

    return function(elem, getter) {
      var obs = ko.observable(getter());
      items.push({ obs: obs, getter: getter, elem: $(elem) });
      if (timer === null) {
        timer = setInterval(check, 100);
      }
      return obs;
    };
  })();

  ko.bindingHandlers.virtualScroll = {
    init: function(element, valueAccessor, allBindingsAccessor,
                   viewModel, context) {
      var clone = $(element).clone();
      $(element).empty();
      var config = ko.utils.unwrapObservable(valueAccessor());
      var rowHeight = ko.utils.unwrapObservable(config.rowHeight);

      ko.computed(function() {
        var newHeight = config.rows().length * rowHeight
        $(element).css({
          height: newHeight
        });
        var parent = $(element).parent();
        if( parent.scrollTop == (parent.scrollHeight - parent.offsetHeight))
        {
        }
        parent.scrollTop(newHeight);
      });


      var offset = simulatedObservable(element, function() {
        return $(element).offset().top - $(element).parent().offset().top;
      });

      var windowHeight = simulatedObservable(element, function() {
        return $(element).parent().innerHeight();
      });

      var created = {};

      var refresh = function() {
        var o = offset();
        var data = config.rows();
        var top = Math.max(0, Math.floor(-o / rowHeight) - 10);
        var bottom = Math.min(data.length, Math.ceil(-o / rowHeight) + Math.ceil((windowHeight()) / rowHeight) + 10) + 1;

        for (var row = top; row < bottom; row++) {
          if (!created[row]) {
            var rowDiv = $('<div></div>');
            rowDiv.css({
              position: 'absolute',
              height: config.rowHeight,
              left: 0,
              right: 0,
              top: row * config.rowHeight
            });
            rowDiv.append(clone.clone().children());
            ko.applyBindingsToDescendants(
              context.createChildContext(data[row]), rowDiv[0]);
            created[row] = rowDiv;
            $(element).append(rowDiv);
          }
        }

        Object.keys(created).forEach(function(rowNum) {
          if (rowNum < top || rowNum >= bottom) {
            created[rowNum].remove();
            delete created[rowNum];
          }
        });
      };

      ko.computed(refresh);

      return { controlsDescendantBindings: true };
    }
  };


  return ctor;
});
