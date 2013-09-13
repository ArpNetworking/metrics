function Gauge(placeholderName, configuration) {
    this.placeholderName = placeholderName;

    var self = this; // for internal d3 functions

    this.configure = function (configuration) {
        this.config = configuration;

        this.config.controlSize = this.config.size * 0.9;

        this.config.raduis = this.config.controlSize * 0.97 / 2;
        this.config.cx = this.config.controlSize / 2;
        this.config.cy = this.config.controlSize / 2;

        this.config.min = undefined != configuration.min ? configuration.min : 0;
        this.config.max = undefined != configuration.max ? configuration.max : 100;
        this.config.range = this.config.max - this.config.min;

        this.config.majorTicks = configuration.majorTicks || 5;
        this.config.minorTicks = configuration.minorTicks || 2;

        this.config.greenColor = configuration.greenColor || "#109618";
        this.config.yellowColor = configuration.yellowColor || "#FF9900";
        this.config.redColor = configuration.redColor || "#DC3912";

        this.config.transitionDuration = configuration.transitionDuration || 500;
        this.config.autoMax = configuration.autoMax || true;
        this.config.yellowZones = [
            { from: this.config.min + this.config.range * 0.75, to: this.config.min + this.config.range * 0.9 }
        ];
        this.config.redZones = [
            { from: this.config.min + this.config.range * 0.9, to: this.config.max }
        ];
    }

    this.render = function (suppressRedraw) {
        d3.select("#" + this.placeholderName).select("svg").remove();
        this.body = d3.select("#" + this.placeholderName)
            .append("svg:svg")
            .attr("class", "gauge")
            .attr("width", this.config.controlSize)
            .attr("height", this.config.controlSize);

        var defs = this.body.append("svg:defs");
        var gradient = defs.append("svg:radialGradient")
            .attr("id", "bggradient");

        gradient.append("svg:stop")
            .attr("offset", "92%")
            .attr("stop-color", "#000")
            .attr("stop-opacity", 1);

        gradient.append("svg:stop")
            .attr("offset", "96%")
            .attr("stop-color", "#ccc")
            .attr("stop-opacity", 1);

        gradient.append("svg:stop")
            .attr("offset", "100%")
            .attr("stop-color", "#111")
            .attr("stop-opacity", 1);

        var needleGradient = defs.append("svg:radialGradient")
            .attr("id", "needleGradient");

        needleGradient.append("svg:stop")
            .attr("offset", "0%")
            .attr("stop-color", "#666")
            .attr("stop-opacity", 1);

        needleGradient.append("svg:stop")
            .attr("offset", "100%")
            .attr("stop-color", "#333")
            .attr("stop-opacity", 1);

        this.body.append("svg:circle")
            .attr("cx", this.config.cx)
            .attr("cy", this.config.cy)
            .attr("r", this.config.raduis)
            .style("fill", "url(#bggradient)")
            .style("stroke", "#000")
            .style("stroke-width", "0.5px");

        this.body.append("svg:circle")
            .attr("cx", this.config.cx)
            .attr("cy", this.config.cy)
            .attr("r", 0.9 * this.config.raduis)
            .style("fill", "#fff")
            .style("stroke", "#e0e0e0")
            .style("stroke-width", "2px");

        for (var index in this.config.greenZones) {
            this.drawBand(this.config.greenZones[index].from, this.config.greenZones[index].to, self.config.greenColor);
        }

        for (var index in this.config.yellowZones) {
            this.drawBand(this.config.yellowZones[index].from, this.config.yellowZones[index].to, self.config.yellowColor);
        }

        for (var index in this.config.redZones) {
            this.drawBand(this.config.redZones[index].from, this.config.redZones[index].to, self.config.redColor);
        }

        if (undefined != this.config.label) {
            var fontSize = Math.round(this.config.controlSize / 9);
            this.body.append("svg:text")
                .attr("x", this.config.cx)
                .attr("y", this.config.cy / 2 + fontSize / 2)
                .attr("dy", fontSize / 2)
                .attr("text-anchor", "middle")
                .text(this.config.label)
                .style("font-size", fontSize + "px")
                .style("fill", "#333")
                .style("stroke-width", "0px");
        }

        var fontSize = Math.round(this.config.controlSize / 16);
        var majorDelta = this.config.range / (this.config.majorTicks - 1);
        for (var major = this.config.min; major <= this.config.max; major += majorDelta) {
            var minorDelta = majorDelta / this.config.minorTicks;
            for (var minor = major + minorDelta; minor < Math.min(major + majorDelta, this.config.max); minor += minorDelta) {
                var point1 = this.valueToPoint(minor, 0.75);
                var point2 = this.valueToPoint(minor, 0.85);

                this.body.append("svg:line")
                    .attr("x1", point1.x)
                    .attr("y1", point1.y)
                    .attr("x2", point2.x)
                    .attr("y2", point2.y)
                    .style("stroke", "#666")
                    .style("stroke-width", "1px");
            }

            var point1 = this.valueToPoint(major, 0.7);
            var point2 = this.valueToPoint(major, 0.85);

            this.body.append("svg:line")
                .attr("x1", point1.x)
                .attr("y1", point1.y)
                .attr("x2", point2.x)
                .attr("y2", point2.y)
                .style("stroke", "#333")
                .style("stroke-width", "3px");

            if (major == this.config.min || major == this.config.max) {
                var point = this.valueToPoint(major, 0.63);

                this.body.append("svg:text")
                    .attr("x", point.x)
                    .attr("y", point.y)
                    .attr("dy", fontSize / 3)
                    .attr("text-anchor", major == this.config.min ? "start" : "end")
                    .text(major)
                    .style("font-size", fontSize + "px")
                    .style("fill", "#333")
                    .style("stroke-width", "0px");
            }
        }

        var pointerContainer = this.body.append("svg:g").attr("class", "pointerContainer");

        var midValue = (this.config.min + this.config.max) / 2;

        var pointerPath = this.buildPointerPath(midValue);

        var pointerLine = d3.svg.line()
            .x(function (d) {
                return d.x
            })
            .y(function (d) {
                return d.y
            })
            .interpolate("basis");

        pointerContainer.selectAll("path")
            .data([pointerPath])
            .enter()
            .append("svg:path")
            .attr("d", pointerLine)
            .style("fill", "#dc3912")
            .style("stroke", "#c63310")
            .style("fill-opacity", 0.7)

        pointerContainer.append("svg:circle")
            .attr("cx", this.config.cx)
            .attr("cy", this.config.cy)
            .attr("r", 0.12 * this.config.raduis)
            .style("fill", "url(#needleGradient)")
            .style("stroke", "#666")
            .style("opacity", 1);

        var fontSize = Math.round(this.config.controlSize / 10);
        pointerContainer.selectAll("text")
            .data([midValue])
            .enter()
            .append("svg:text")
            .attr("x", this.config.cx)
            .attr("y", this.config.controlSize - this.config.cy / 4 - (0.7 * fontSize))
            .attr("dy", fontSize / 2)
            .attr("text-anchor", "middle")
            .style("font-size", fontSize + "px")
            .style("fill", "#000")
            .style("stroke-width", "0px");

        if (!suppressRedraw) {
            this.redraw(this.config.min, 0);
        }
    }

    this.buildPointerPath = function (value) {
        var delta = this.config.range / 13;

        var head = valueToPoint(value, 0.85);
        var head1 = valueToPoint(value - delta, 0.12);
        var head2 = valueToPoint(value + delta, 0.12);

        var tailValue = value - (this.config.range * (1 / (270 / 360)) / 2);
        var tail = valueToPoint(tailValue, 0.28);
        var tail1 = valueToPoint(tailValue - delta, 0.12);
        var tail2 = valueToPoint(tailValue + delta, 0.12);

        return [head, head1, tail2, tail, tail1, head2, head];

        function valueToPoint(value, factor) {
            var point = self.valueToPoint(value, factor);
            point.x -= self.config.cx;
            point.y -= self.config.cy;
            return point;
        }
    }

    this.drawBand = function (start, end, color) {
        if (0 >= end - start) return;

        this.body.append("svg:path")
            .style("fill", color)
            .attr("d", d3.svg.arc()
                .startAngle(this.valueToRadians(start))
                .endAngle(this.valueToRadians(end))
                .innerRadius(0.65 * this.config.raduis)
                .outerRadius(0.85 * this.config.raduis))
            .attr("transform", function () {
                return "translate(" + self.config.cx + ", " + self.config.cy + ") rotate(270)"
            });
    }

    this.redraw = function (value, transitionDuration) {
        var sinceAdjustMax = self._sinceAdjustMax || 0;

        if (value > self.config.max) {
            sinceAdjustMax = 0;
            self.config.max = self.config.max * 1.3;
            var roundingFactor = Math.pow(10, Math.floor(Math.log(self.config.max) / Math.LN10) - 1);
            self.config.max = Math.round(self.config.max / roundingFactor) * roundingFactor;
            self.configure(self.config);
            self.render(true);
        } else {
            sinceAdjustMax = sinceAdjustMax + 1;
        }

        if (sinceAdjustMax == 20) {
            self.config.max = self.config.max * 0.9;
            var roundingFactor = Math.pow(10, Math.floor(Math.log(self.config.max) / Math.LN10) - 1);
            self.config.max = Math.round(self.config.max / roundingFactor) * roundingFactor;
            sinceAdjustMax = 0;
            self._sinceAdjustMax = sinceAdjustMax;
            self.configure(self.config);
            self.render(true);
        }

        self._sinceAdjustMax = sinceAdjustMax;

        var pointerContainer = this.body.select(".pointerContainer");
        pointerContainer.selectAll("text").text(Math.round(value));

        var pointer = pointerContainer.selectAll("path");
        pointer.transition()
            .duration(undefined != transitionDuration ? transitionDuration : this.config.transitionDuration)
            //.delay(0)
            .ease("linear")
            //.attr("transform", function(d)
            .attrTween("transform", function () {
                var pointerValue = value;
                if (value > self.config.max) pointerValue = self.config.max + 0.02 * self.config.range;
                else if (value < self.config.min) pointerValue = self.config.min - 0.02 * self.config.range;
                var targetRotation = (self.valueToDegrees(pointerValue) - 90);
                var currentRotation = self._currentRotation || targetRotation;

                return function (step) {
                    var rotation = currentRotation + (targetRotation - currentRotation) * step;
                    self._currentRotation = rotation;
                    return "translate(" + self.config.cx + ", " + self.config.cy + ") rotate(" + rotation + ")";
                }
            });
    }

    this.valueToDegrees = function (value) {
        // thanks @closealert
        //return value / this.config.range * 270 - 45;
        return value / this.config.range * 270 - (this.config.min / this.config.range * 270 + 45);
    }

    this.valueToRadians = function (value) {
        return this.valueToDegrees(value) * Math.PI / 180;
    }

    this.valueToPoint = function (value, factor) {
        return {    x: this.config.cx - this.config.raduis * factor * Math.cos(this.valueToRadians(value)),
            y: this.config.cy - this.config.raduis * factor * Math.sin(this.valueToRadians(value))        };
    }

    // initialization
    this.configure(configuration);
}