/*
 * Copyright 2014 Groupon.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

///<reference path="BrowseNode.ts" />
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
    display: KnockoutComputed<string>;

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
