import StatisticData = require('./StatisticData');

class MetricData {
    name: string;
    children: StatisticData[];
}

export = MetricData;