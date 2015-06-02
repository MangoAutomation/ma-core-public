define([
    'dojo/_base/array',
    'dojo/_base/declare',
    'dstore/Request'
], function (arrayUtil, declare, Request) {

    var push = [].push;

    function encodeString(component) {
        return encodeURIComponent(component).replace('(', '%28').replace(')', '%29');
    }

    return declare(Request, {
        _renderFilterParams: function (filter) {
            // summary:
            //      Constructs filter-related params to be inserted into the query string
            // returns: String
            //      Filter-related params to be inserted in the query string
            var type = filter.type;
            var args = filter.args;
            if (!type) {
                return [''];
            }
            if (type === 'string') {
                return [args[0]];
            }
            if (type === 'and' || type === 'or') {
                return [arrayUtil.map(filter.args, function (arg) {
                    // render each of the arguments to and or or, then combine by the right operator
                    var renderedArg = this._renderFilterParams(arg);
                    return ((arg.type === 'and' || arg.type === 'or') && arg.type !== type) ?
                        // need to observe precedence in the case of changing combination operators
                        '(' + renderedArg + ')' : renderedArg;
                }, this).join(type === 'and' ? '&' : '|')];
            }
            var target = args[1];
            if (target) {
                if(target._renderUrl) {
                    // detected nested query, and render the url inside as an argument
                    target = '(' + target._renderUrl() + ')';
                } else if (target instanceof Array) {
                    target = '(' + target + ')';
                }
            }
            return [encodeString(args[0]) + '=' + (type === 'eq' ? '' : type + '=') + encodeString(target)];
        },
        
        _renderQueryParams: function () {
            var queryParams = [];
            var filterEntries = [];

            arrayUtil.forEach(this.queryLog, function (entry) {
                var type = entry.type,
                    renderMethod = '_render' + type[0].toUpperCase() + type.substr(1) + 'Params';

                if (type === 'filter') {
                    filterEntries.push(entry.normalizedArguments[0]);
                } else if (this[renderMethod]) {
                    push.apply(queryParams, this[renderMethod].apply(this, entry.normalizedArguments));
                } else {
                    console.warn('Unable to render query params for "' + type + '" query', entry);
                }
            }, this);

            if (filterEntries.length) {
                var filter = new this.Filter();
                var andFilter = filter.and.apply(filter, filterEntries);
                queryParams.unshift.apply(queryParams, this._renderFilterParams(andFilter));
            }

            return queryParams;
        }
    });

});