/// <reference path="../jquery/jquery.d.ts"/>

interface JQueryStatic {
    toJSON(obj: any): string; 
    evalJSON(json: string): any;
    secureEvalJSON(json: string): any;
}