define("mango/mobile/DataSource", ["mango/mobile/structure","dojo/on", 
	"dojo/_base/connect",
	"dijit/registry", // dijit.byId
	"dojox/mobile/common",
	"dojox/mobile/IconContainer"],
  function(structure, on, connect, registry, dm) {
	var internalNavRecords = [];
	return {
		init : function() {
			// workaround for wrong insertion point after pulling back HTML content
			// TODO: remove this work around if dojo mobile provides better solution
			connect.connect(registry.byId("urlIcon"), "iconClicked", function() {
				if (!registry.byId("icons-url")) {
					dm.currentView = registry.byId("iconsMain");
				}
			});
			connect.subscribe("/dojox/mobile/afterTransitionIn", function(toWidget, moveTo){
				if (moveTo === "icons-url") {
					structure.navRecords.push({
						from:"iconsMain",
						to:"icons-url",
						navTitle:"Back"
					});
				}
			});
			connect.subscribe("onAfterDemoViewTransitionIn", function(id) {
				if (id == "icons") {
					var navRecords = structure.navRecords;
					for (var i = 0; i < internalNavRecords.length ; ++i) {
						navRecords.push(internalNavRecords[i]);
					}
				}
			});
			on(registry.byId("icons"), "beforeTransitionOut", function() {
				var navRecords = structure.navRecords;
				internalNavRecords = [];
				for (var i = 0; i < navRecords.length ; ++ i) {
					var navRecord = navRecords[i];
					if (navRecord.from == "navigation" ||
						navRecord.to == "source")
						continue;
					internalNavRecords.push(navRecord);
				};
			});
		}
	};
});