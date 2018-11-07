/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.infiniteautomation.mango.db.query.pojo.RQLToObjectListQuery;
import com.infiniteautomation.mango.util.RQLUtils;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.web.mvc.rest.v1.message.RestProcessResult;
import com.serotonin.m2m2.web.mvc.rest.v1.model.DemoModel;
import com.serotonin.m2m2.web.mvc.rest.v1.model.DemoModel.Demo;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import net.jazdw.rql.parser.ASTNode;

/**
 * @author Terry Packer
 *
 */
@Api(value="Demo Controller")
@RestController(value="DemoRestController")
@RequestMapping("/v1/demo")
public class MangoDemoRestController {

    private static int MAX_ITEMS = 100;
    private List<Demo> demoStore;

    public MangoDemoRestController(){
        createDemoStore();
    }

    @ApiOperation(
            value = "Return a path variable",
            notes = ""
            )
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Ok"),
            @ApiResponse(code = 403, message = "User does not have access")
    })
    @RequestMapping(method = RequestMethod.GET, value = "/get/{result}")
    public ResponseEntity<String> getPath(
            @ApiParam(value = "Result to return", required=true)
            @PathVariable
            String result,
            
            @AuthenticationPrincipal User user
        ){
        return ResponseEntity.ok(result);
    }
    
    @ApiOperation(
            value = "Get all demos",
            notes = "Notes for getting all demos"
            )
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Ok"),
            @ApiResponse(code = 403, message = "User does not have access")
    })
    @RequestMapping(method = RequestMethod.GET, value = "/list")
    public ResponseEntity<List<DemoModel>> getAllDemos(HttpServletRequest request,
        @ApiParam(value = "Limit the number of results", required=false)
        @RequestParam(value="limit", required=false, defaultValue="100")int limit,
        @AuthenticationPrincipal User user){
        RestProcessResult<List<DemoModel>> result = new RestProcessResult<List<DemoModel>>(HttpStatus.OK);
        ASTNode root = new ASTNode("limit", limit);
        List<DemoModel> models = queryStore(root);
        return result.createResponseEntity(models);
    }
    
    
    @ApiOperation(
            value = "Query Demos",
            notes = "Notes for getting all demos"
            )
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Ok"),
            @ApiResponse(code = 403, message = "User does not have access")
    })
    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<List<DemoModel>> queryDemos(
            HttpServletRequest request,
            @AuthenticationPrincipal User user){
        List<DemoModel> result = queryStore(RQLUtils.parseRQLtoAST(request.getQueryString()));
        return ResponseEntity.ok(result);
    }

    /**
     * Perform an RQL Query on the Store
     * @param root
     * @return
     */
    private List<DemoModel> queryStore(ASTNode root) {

        List<Demo> values = root.accept(new RQLToObjectListQuery<Demo>(), this.demoStore);
        List<DemoModel> models = new ArrayList<DemoModel>();

        for(Demo value : values){
            models.add(new DemoModel(value));
        }

        return models;
    }

    /**
     * Setup the store using MAX_ITEMS
     */
    private void createDemoStore(){
        this.demoStore = new ArrayList<Demo>();
        boolean demoBoolean = true;
        Double demoDouble = 1.0D;
        Integer demoInteger = 1;
        String demoString = "demoString";
        Double doubleInc = .3D;

        String demoXid = "DEMO_";
        String demoName = "Demo-";

        for(int i=0; i<MAX_ITEMS; i++){
            Demo demo = new Demo();
            demo.setXid(demoXid + demoInteger);
            demo.setName(demoName + demoInteger);
            demo.setDemoBoolean(demoBoolean);
            demo.setDemoDouble(demoDouble);
            demo.setDemoInteger(demoInteger);
            demo.setDemoString(demoString + demoInteger);

            demoBoolean = !demoBoolean;
            demoDouble+= doubleInc;
            demoInteger++;
            this.demoStore.add(demo);
        }
    }


}
