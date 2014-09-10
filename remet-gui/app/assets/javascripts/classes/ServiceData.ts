import MetricData = require('./MetricData');

class ServiceData {
    name: string;
    children: MetricData[];
}

export = ServiceData;