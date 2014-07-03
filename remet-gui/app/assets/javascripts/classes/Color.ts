class Color {
    r: number;
    g: number;
    b: number;
    a: number = 1;

    constructor(r: number, g: number, b: number, a: number = 1) {
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
    }

    rgb(): number[] {
        return [this.r, this.g, this.b];
    }
}

export = Color;