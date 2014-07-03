interface RickshawSeriesSettings {
    name: string;
    color: any;
    data: any[];
}

enum RendererType {
    area,
    stack,
    bar,
    line,
    scatterplot,
    multi
}

enum InterpolationType {
    linear,
    step-after,
    cardinal,
    basis
}

interface RickshawGraphSettings {
    element: any;
    series: RickshawSeriesSettings[];
    renderer: string;
    width: number;
    height: number;
    min: number;
    max: number;
    padding: number[];
    interpolation: string;
}

interface RickshawGraph {
    render(): void;
    configure(settings: RickshawGraphSettings);
    onUpdate(f: any);
}

interface RickshawStatic {
    Graph(settings: RickshawGraphSettings) : RickshawGraph;
}
declare var Rickshaw : RickshawStatic