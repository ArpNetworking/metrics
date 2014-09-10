///<reference path="../libs/knockout/knockout.d.ts"/>
interface BrowseNode {
    expandMe(): void;
    display: KnockoutComputed<string>;
    children: KnockoutObservableArray<BrowseNode>;
    expanded: KnockoutObservable<boolean>;
    name: KnockoutObservable<string>;
}
