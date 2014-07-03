declare class Gauge {
    constructor(id: string, options: any);
    render(): void;
    redraw(value: number, duration?: number): void;
}
