/// <reference path="../jqueryui/jqueryui.d.ts"/>

interface JQuery {
    rangeSlider(): JQuery;
    rangeSlider(methodName:string): JQuery;
    rangeSlider(methodName:'destroy'): void;
    rangeSlider(methodName:'disable'): void;
    rangeSlider(methodName:'enable'): void;
    rangeSlider(methodName:'refresh'): void;
    rangeSlider(methodName:'value'): number;
    rangeSlider(methodName:'value', value:number): void;
    rangeSlider(methodName:'values'): Array<number>;
    rangeSlider(methodName:'values', index:number): number;
    rangeSlider(methodName:string, index:number, value:number): void;
    rangeSlider(methodName:'values', index:number, value:number): void;
    rangeSlider(methodName:string, values:Array<number>): void;
    rangeSlider(methodName:'values', values:Array<number>): void;
    rangeSlider(methodName:'widget'): JQuery;
    rangeSlider(options:JQueryUI.SliderOptions): JQuery;
    rangeSlider(optionLiteral:string, optionName:string): any;
    rangeSlider(optionLiteral:string, options:JQueryUI.SliderOptions): any;
    rangeSlider(optionLiteral:string, optionName:string, optionValue:any): JQuery;
}
