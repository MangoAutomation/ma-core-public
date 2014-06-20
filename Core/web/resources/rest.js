/** 
 * Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
 * @author Terry Packer
 */

//Full Mango Rest Javascript Object
var mangoRest = {
        dataPoints: {},
        pointValues: {}
};

require(['dojo/json', 'dojo/_base/xhr', "dojo/request", "dojo/store/JsonRest", "dojo/store/Memory", "dijit/form/FilteringSelect", "dojo/domReady!"], 
        function(JSON, xhr, request, JsonRest, Memory, FilteringSelect){

    //Define the JSON Stores
    mangoRest.dataPoints.store = new JsonRest({
        target: "/rest/v1/dataPoints/",
        idProperty: "xid"
    });
    
    
    
    // Point lookup
    new FilteringSelect({
           store: mangoRest.dataPoints.store,
           autoComplete: false,
           style: "width: 250px;",
           queryExpr: "*\${0}*",
           highlightMatch: "all",
           required: false,
           onChange: function(pointXid) {
               if (this.item){
                   var dataPoint = this.item;
                   mangoRest.dataPoints.setInputs(this.item);
                   
                   mangoRest.pointValues.loadLatestPointValues(this.item, 1, function(data){
                       if(data.length > 0){
                           mangoRest.pointValues.createPointValueInput("value", dataPoint, data[0].value);
                       }
                   },function(error){
                       mangoRest.showError(error)
                   },function(evt){
                       //No-op, used for updating progress
                   });
                   
                   mangoRest.dataPoints.loadChartData(this.item);
               }
           }
       }, "dataPoints"); 


    //
    // Utility Methods 
    //
    /**
     * Display an error message
     */
    mangoRest.showError = function(error){
        var errorMessage = dojo.byId("errorMessage");
        var msg = error.response.getHeader("errors")
        
        if(msg === null)
            msg = error.message;
        
        if(msg === null)
            msg = "Unknown Error";
        
        errorMessage.innerHTML = msg;
        errorDialog.show();
    }
    
    
    //
    //  Data Point Methods
    //
    
    /**
     * Load the settings into the view
     */
    mangoRest.dataPoints.setInputs = function(dataPoint){
        
        //Save locally
        this.dataPoint = dataPoint;
        
        $set("name", this.dataPoint.name);
        $set("xid", this.dataPoint.xid);
        $set("deviceName", this.dataPoint.deviceName);
        $set("enabled", this.dataPoint.enabled);
    }
    
    /**
     * Get the settings from the page
     * and place into my data point
     */
    mangoRest.dataPoints.getInputs = function(){
        this.dataPoint.name = $get("name");
        this.dataPoint.xid = $get("xid");
        this.dataPoint.deviceName = $get("deviceName");
        this.dataPoint.enabled = $get("enabled");
        
        return this.dataPoint;
    }

    
    /**
     * Get a Data Point
     **/
    mangoRest.dataPoints.loadDataPointXhr = function(xid){

        xhr.get({
           url: "/rest/v1/dataPoints/" + xid + ".json",
           handleAs: "json",
           load: function(data){
               mangoRest.dataPoints.setInputs(data);
           }
        
        });
    };
    
    /**
     * Get a Data Point
     **/
    mangoRest.dataPoints.get = function(xid){
        
        this.store.get(xid).then(function(data){
            mangoRest.dataPoints.setInputs(data);
        });
    };
    
    /**
     * Save a Data Point
     **/
    mangoRest.dataPoints.put = function(){
        
        var dataPoint = this.getInputs();
        
        mangoRest.dataPoints.store.put(dataPoint).then(function(response){
            alert('Data Point saved.');
        }, function(error){
            mangoRest.showError(error)
        }, function(event){});
        
    };
    
    /**
     * Save a Data Point
     **/
    mangoRest.dataPoints.saveDataPointXhr = function(){
        
        var dataPoint = mangoRest.dataPoints.getInputs();
        
        request.put("/rest/v1/dataPoints/" + dataPoint.xid,
                {   
            data: JSON.stringify(dataPoint),
            timeout: 2000,
            handleAs: "json",
            headers: { 'Content-Type': 'application/json' }
        }).then(function(response){
            alert('Data Point saved.');
        }, function(error){
            mangoRest.showError(error)
        }, function(event){});
    };
    
    
    /**
     * Load the data for a data point into the chart
     */
    mangoRest.dataPoints.loadChartData = function(dataPoint){
        //Do XHR Request
        mangoRest.pointValues.loadLatestPointValues(dataPoint, 500, function(data){
            
            //We need at least 2 points to plot
            if(data.length == 0){
                alert('No data for plot');
                return;
            }

            //For now just format the data
            var chartValues = [];
            for(var i=0; i<data.length; i++){
                chartValues.push({x: data[i].time, y: data[i].value});
            }
            var chartData = [];
            chartData.push({
                    values: chartValues,
                    key: dataPoint.name,
                    color: 'red'
            });
            
            if(dataPoint.pointLocator.dataType === "ALPHANUMERIC"){
                
                //Need to create .y(function(d) {})
                // in the chart that will map strings to a value
                // and pull out that value to display it
                // then change the y axis to show Strings as labels to those numbers
                
                hide("chart");
                return; 
            }

            
            mangoRest.dataPoints.createDateGraph(chartData,"chart");
        },function(error){
            mangoRest.showError(error)
        },function(evt){
            //No-op, used for updating progress
        });
        
    }
    
    /**
     * Create a date based graph
     * @param chartData
     * @param chartName
     */
     mangoRest.dataPoints.createDateGraph = function(chartData, chartName){

        nv.addGraph(function() {  
           var chart = nv.models.lineWithFocusChart().interactive(true);
        
           chart.xAxis
           .tickFormat(function(d) {
               return d3.time.format('%H:%M:%S.%L')(new Date(d))
             });
           
           chart.x2Axis
           .tickFormat(function(d) {
                return d3.time.format('%H:%M:%S.%L')(new Date(d))
             });
           
           //Scale x if we have enough data to
           if(chartData[0].values.length > 1)
               var x = d3.time.scale().domain([chartData[0].values[0].x, chartData[0].values[chartData[0].values.length-1].x]);
           
           chart.xAxis.scale(d3.time.scale());
           chart.x2Axis.scale(d3.time.scale());
           

           
        //  chart.xAxis
        //          .tickFormat(d3.format(',f'));
          chart.yAxis
              .tickFormat(d3.format(',.4f'));
          chart.y2Axis
              .tickFormat(d3.format(',.4f'));
        
              
          d3.select('#' + chartName + ' svg')
              .datum(chartData)
              .transition().duration(500)
              .call(chart);
        
          //TODO: Figure out a good way to do this automatically
          nv.utils.windowResize(chart.update);
        
          //chart.dispatch.on('stateChange', function(e) { nv.log('New State:', JSON.stringify(e)); });
        
          return chart;
        });
        
        show('chart');
    }

     //
     // Point Value methods
     //
     /**
      * Create the input for the point value of the type for the datapoint
      */
     mangoRest.pointValues.createPointValueInput = function(inputName, dataPoint, value){
         
         var input = dojo.byId(inputName);
         if(dataPoint.pointLocator.dataType === "ALPHANUMERIC"){
             input.type = "text";
             input.value = value;
         }else if(dataPoint.pointLocator.dataType === "BINARY"){
             input.type = "checkbox";
             input.value = value;             
         }if((dataPoint.pointLocator.dataType === "MULTISTATE")||(dataPoint.pointLocator.dataType === "NUMERIC")){
             input.type = "number";
             input.value = value;
         }
         
         
     }

     
     /**
      * Load latest
      */
     mangoRest.pointValues.loadLatestPointValues = function(dataPoint,limit,onComplete, onError, onProgress){
         request("/rest/v1/pointValues/" + dataPoint.xid + "/latest.json?limit=" + limit, {handleAs: "json"}).then(onComplete, onError, onProgress);
     }
     
     /**
      * Save a value
      */
     mangoRest.pointValues.put = function(){
         
         var dataPoint = mangoRest.dataPoints.getInputs();
         
         var value = $get('value');
         if((dataPoint.pointLocator.dataType === "MULTISTATE")||(dataPoint.pointLocator.dataType === "NUMERIC")){
             //Since the input is text type we must convert
             value = new Number(value);
         }
         
         var time = new Date().getTime();
         var data = {
                 annotation: 'set by Terry',
                 dataType: dataPoint.pointLocator.dataType,
                 value: value,
                 time: time};
         
         request.put("/rest/v1/pointValues/" + dataPoint.xid,
                 {   
                     data: JSON.stringify(data),
                     timeout: 2000,
                     handleAs: "json",
                     headers: { 'Content-Type': 'application/json; charset=utf-8'}
                 }).then(function(response){
                     alert('Point Value saved.');
                 }, function(error){
                     mangoRest.showError(error);
                 }, function(event){});
         
         
     }
     
     
     
     //
     //  Data source methods
     //
     /**
      * Get a Data Source
      **/
     mangoRest.dataPoints.loadDataSource = function(xid){

         xhr.get({
            url: "/rest/v1/dataSources/" + xid + ".json",
            handleAs: "json",
            load: function(data){
                $set("name", data.name);
            }
         
         });
     };
     

});