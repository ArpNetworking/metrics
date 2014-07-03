import ServiceData = require('./ServiceData');

class MetricsListData {
    metrics: ServiceData[];

    constructor(data: any) {
        this.metrics = data.metrics;
    }
}

export = MetricsListData;