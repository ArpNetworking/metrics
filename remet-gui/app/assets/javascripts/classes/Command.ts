class Command<T> {
    command: string;
    data: T;

    constructor(command: string, data: T) {
        this.data = data;
        this.command = command;
    }
}

export = Command;