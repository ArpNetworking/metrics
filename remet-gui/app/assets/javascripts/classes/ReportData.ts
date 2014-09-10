class ReportData {
    data: number;
    metric: string;
    server: string;
    service: string;
    statistic: string;
    timestamp: number;

    constructor(data: any) {
        this.data = data.data;
        this.metric = data.metric;
        this.server = data.server;
        this.service = data.service;
        this.statistic = data.statistic;
        this.timestamp = data.timestamp;
    }
}

export = ReportData;