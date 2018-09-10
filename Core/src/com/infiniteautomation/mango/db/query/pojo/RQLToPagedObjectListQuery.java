/**
 * Copyright (C) 2018 Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.db.query.pojo;

import java.util.List;

/**
 * 
 * @author Terry Packer
 */
public class RQLToPagedObjectListQuery <T> extends RQLToObjectListQuery<T> {

    protected int unlimitedSize;
    
    public RQLToPagedObjectListQuery(int originalSize) {
        this.unlimitedSize = originalSize;
    }
    
    public int getUnlimitedSize() {
        return unlimitedSize;
    }
    
    @Override
    protected List<T> applyLimit(List<Object> arguments, List<T> data) {
        
        if(arguments.size() > 0){
            this.unlimitedSize = data.size();
            if(arguments.size() == 1){
                int limit = (int)arguments.get(0);
                if(data.size() > limit)
                    return data.subList(0, limit);
                else
                    return data;
            }else{
                //Do limit and offset
                int limit = (int)arguments.get(0);
                int offset = (int)arguments.get(1);
                int end = offset + limit;
                
                //Compute end location
                if(end>data.size())
                    end = data.size();
            
                return data.subList(offset, end);
            }
        }else{
            this.unlimitedSize = data.size();
            return data;
        }
                
    }
}
