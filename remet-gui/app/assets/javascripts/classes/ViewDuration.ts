class ViewDuration {
    start: number = 570000;
    end: number = 600000;

    constructor(start?: number, end?: number) {
        if (start === undefined) {
            this.start = 570000;
        } else {
            this.start = start;
        }

        if (end === undefined) {
            this.end = 600000;
        } else {
            this.end = end;
        }
    }
}

export = ViewDuration;