declare module "log4js" {
    function configure(appenders:any, options:any);
    function getLogger(name:string):Logger;

    export interface Logger{
        info(message:string);
    }

}

