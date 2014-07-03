import ServiceNodeVM = require('./ServiceNodeVM');
import ko = require('knockout');

class FolderNodeVM implements BrowseNode {
    name: KnockoutObservable<string>;
    id: KnockoutObservable<string>;
    subFolders: KnockoutObservableArray<FolderNodeVM>;
    children: KnockoutObservableArray<ServiceNodeVM>;
    isFolder: boolean;
    expanded: KnockoutObservable<boolean>;
    visible: KnockoutObservable<boolean>;
    display: KnockoutComputed<string>

    constructor(name: string, id: string, isFolder: boolean) {
        this.name = ko.observable(name);
        this.id = ko.observable(id);
        this.isFolder = isFolder;
        this.subFolders = ko.observableArray<FolderNodeVM>();
        this.children = ko.observableArray<any>();
        this.expanded = ko.observable(false);
        this.visible = ko.observable(false);
        this.display = ko.computed<string>(() => { return this.name(); });
    }

    expandMe() {
        this.expanded(this.expanded() == false);
        var expanded = this.expanded();
        this.subFolders().forEach((item) => item.visible(this.expanded()));
        if (this.children() !== undefined) {
            this.children().forEach((item) => {
                item.expanded(expanded);
                if (item.children() !== undefined) {
                    item.children().forEach((item) => {
                        item.expanded(expanded);
                    });
                }
            });
        }
    }
}

export = FolderNodeVM