import ConnectionVM = require('./ConnectionVM');
import ViewDuration = require('./ViewDuration');
import GraphSpec = require('./GraphSpec')

interface StatisticView {
    id: string;
    name: string;
    spec: GraphSpec;
    paused: boolean;
    start(): void;
    postData(server: string, timestamp: number, dataValue: number, cvm: ConnectionVM): void;
    shutdown(): void;
    setViewDuration(duration: ViewDuration): void;
    updateColor(cvm: ConnectionVM): void;
    disconnectConnection(cvm: ConnectionVM): void;
}

export = StatisticView;
