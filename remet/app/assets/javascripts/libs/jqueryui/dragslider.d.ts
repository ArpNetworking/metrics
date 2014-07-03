/// <reference path="../jqueryui/jqueryui.d.ts"/>

interface JQuery {
    dragslider(): JQuery;
    dragslider(methodName:string): JQuery;
    dragslider(methodName:'destroy'): void;
    dragslider(methodName:'disable'): void;
    dragslider(methodName:'enable'): void;
    dragslider(methodName:'refresh'): void;
    dragslider(methodName:'value'): number;
    dragslider(methodName:'value', value:number): void;
    dragslider(methodName:'values'): Array<number>;
    dragslider(methodName:'values', index:number): number;
    dragslider(methodName:string, index:number, value:number): void;
    dragslider(methodName:'values', index:number, value:number): void;
    dragslider(methodName:string, values:Array<number>): void;
    dragslider(methodName:'values', values:Array<number>): void;
    dragslider(methodName:'widget'): JQuery;
    dragslider(options:JQueryUI.SliderOptions): JQuery;
    dragslider(optionLiteral:string, optionName:string): any;
    dragslider(optionLiteral:string, options:JQueryUI.SliderOptions): any;
    dragslider(optionLiteral:string, optionName:string, optionValue:any): JQuery;
}