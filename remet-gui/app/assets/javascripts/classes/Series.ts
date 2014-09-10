class Series {
    //This is really an array of elements of [timestamp, data value]
    data: number[][] = [];
    label: string = "";
    points: any = { show: true };
    lines: any = { show: true };
    color: string = "black";

    constructor(label: string, color: string) {
        this.color = color;
        this.label = label;
    }
}

export = Series;