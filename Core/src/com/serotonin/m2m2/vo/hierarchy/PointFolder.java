/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.vo.hierarchy;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.serotonin.json.JsonException;
import com.serotonin.json.JsonReader;
import com.serotonin.json.ObjectWriter;
import com.serotonin.json.spi.JsonProperty;
import com.serotonin.json.spi.JsonSerializable;
import com.serotonin.json.type.JsonArray;
import com.serotonin.json.type.JsonObject;
import com.serotonin.json.type.JsonValue;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.i18n.TranslatableJsonException;
import com.serotonin.m2m2.vo.DataPointSummary;
import com.serotonin.m2m2.vo.DataPointVO;

/**
 * @author Matthew Lohbihler
 */
public class PointFolder implements JsonSerializable {
    private int id = Common.NEW_ID;
    @JsonProperty
    private String name;

    @JsonProperty
    private List<PointFolder> subfolders = new ArrayList<PointFolder>();

    private List<DataPointSummary> points = new ArrayList<DataPointSummary>();

    public PointFolder() {
        // no op
    }

    public PointFolder(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public void addSubfolder(PointFolder subfolder) {
        subfolders.add(subfolder);
    }

    public void addDataPoint(DataPointSummary point) {
        points.add(point);
    }

    public void removeDataPoint(int dataPointId) {
        for (int i = 0; i < points.size(); i++) {
            if (points.get(i).getId() == dataPointId) {
                points.remove(i);
                return;
            }
        }
    }

    public boolean isEmpty() {
        if (!points.isEmpty())
            return false;
        for (PointFolder sub : subfolders) {
            if (!sub.isEmpty())
                return false;
        }
        return true;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<DataPointSummary> getPoints() {
        return points;
    }

    public void setPoints(List<DataPointSummary> points) {
        this.points = points;
    }

    public List<PointFolder> getSubfolders() {
        return subfolders;
    }

    public void setSubfolders(List<PointFolder> subfolders) {
        this.subfolders = subfolders;
    }

    /**
     * Merge the Existing points with the new ones using the following rules:
     * 
     * Points with same ID will be replaced
     * All new points are added
     * Any existing points not in the new list are kept
     * 
     * @param points
     */
    public void mergePoints(List<DataPointSummary> points){
    	

    	int existingPointIndex;
    	List<DataPointSummary> newPoints = new ArrayList<DataPointSummary>();
    	for(DataPointSummary newPoint : points){
    		existingPointIndex = -1;
    		//Does this point already exist?
    		for(int i=0; i<this.points.size(); i++){
    			if(this.points.get(i).getId() == newPoint.getId()){
    				existingPointIndex = i;
    				break;
    			}
    		}
    		
    		if(existingPointIndex >= 0){
    			//Remove from existing points
    			this.points.remove(existingPointIndex);
    		}
    		
    		//Always add the new points
    		newPoints.add(newPoint);
    	}
    	
    	//Add any remaining existing points
    	for(DataPointSummary existingPoint : this.points){
    		newPoints.add(existingPoint);
    	}
    	    	
    	this.points = newPoints; //Replace with the new list
    	
    }
    
    /**
     * Merge the subfolders of this point folder with new folders 
     * use the following rules:
     * 
     * If a local folder exists with the same id as the new subfolder then merge it
     * If no local folder exists for the new subfolder, then add it
     * If a local folders exists that is not in the new subfolders then keep it
     * 
     * 
     * 
     * @param subfolders
     */
    public void mergeSubfolders(PointFolder root, List<PointFolder> subfolders){
 
    	List<PointFolder> newSubfolders = new ArrayList<PointFolder>();
    	int pointFolderIndex;
    	
    	//Recursively walk through each sub-folder and merge the points in each.
    	for(PointFolder newFolder : subfolders){
   			//Remove all of the new points from the hierarchy if they exist in it
			//Remove the new points from anywhere in the hierarchy
			for(DataPointSummary dps : newFolder.getPoints()){
				root.removePoint(dps.getId());
			}
    		pointFolderIndex = -1;
    		for(int i=0; i< this.subfolders.size(); i++){
    			if(this.subfolders.get(i).getName().equals(newFolder.getName())){
    				pointFolderIndex = i;
    				break;
    			}
    		}
    		
    		if(pointFolderIndex >= 0){
    			//If a local folder exists for the new sub-folder then merge it
    			PointFolder existing = this.subfolders.get(pointFolderIndex);
 
    			//Merge points by moving the existing points into this folder.
    			existing.mergePoints(newFolder.getPoints());
    			existing.mergeSubfolders(root, newFolder.getSubfolders());
    			newSubfolders.add(existing);
    			//Remove the existing sub-folder so that when we are done we have the remaining folders to add
    			this.subfolders.remove(pointFolderIndex);
    		}else{
    			//If no local folder exists for the new sub-folder, then add it
    			newSubfolders.add(newFolder);
    		}
    	}

    	//If a local folders exists that is not in the new sub-folders then we will add it here
		for(PointFolder remainingFolder : this.subfolders){
			newSubfolders.add(remainingFolder);
		}
    	
    	this.subfolders = newSubfolders;
    }
    

    
    /**
     * Recursivly check my subfolders and remove all instances of a point
     * @param dataPointId
     */
    public void removePoint(int dataPointId){
    	
    	//First check my points
    	for(int i=0; i<this.points.size(); i++){
    		if(this.points.get(i).getId() == dataPointId){
    			this.points.remove(i);
    			i--;
    		}
    	}
    	//Check my Subfolders next
    	for(PointFolder folder: this.subfolders){
			folder.removePoint(dataPointId); //Remove the point recursively from this folder path
    	}
    	
    }

	boolean findPoint(List<PointFolder> path, int pointId) {
        boolean found = false;
        for (DataPointSummary point : points) {
            if (point.getId() == pointId) {
                found = true;
                break;
            }
        }

        if (!found) {
            for (PointFolder subfolder : subfolders) {
                found = subfolder.findPoint(path, pointId);
                if (found)
                    break;
            }
        }

        if (found)
            path.add(this);

        return found;
    }

    void copyFoldersFrom(PointFolder that) {
        for (PointFolder thatSub : that.subfolders) {
            PointFolder thisSub = new PointFolder(thatSub.getId(), thatSub.getName());
            thisSub.copyFoldersFrom(thatSub);
            subfolders.add(thisSub);
        }
    }

    public PointFolder getSubfolder(String name) {
        for (PointFolder subfolder : subfolders) {
            if (subfolder.name.equals(name))
                return subfolder;
        }
        return null;
    }

    public DataPointSummary getPointByName(String name) {
        for (DataPointSummary point : points) {
            if (point.getName().equals(name))
                return point;
        }
        return null;
    }

    public DataPointSummary getPointByExtendedName(String extendedName) {
        for (DataPointSummary point : points) {
            if (point.getExtendedName().equals(extendedName))
                return point;
        }
        return null;
    }

    //
    //
    // Serialization
    //
    @Override
    public void jsonWrite(ObjectWriter writer) throws IOException, JsonException {
        DataPointDao dataPointDao = new DataPointDao();
        List<String> pointList = new ArrayList<String>();
        for (DataPointSummary p : points) {
            DataPointVO dp = dataPointDao.getDataPoint(p.getId());
            if (dp != null)
                pointList.add(dp.getXid());
        }
        writer.writeEntry("points", pointList);
    }

    @Override
    public void jsonRead(JsonReader reader, JsonObject jsonObject) throws JsonException {
        JsonArray jsonPoints = jsonObject.getJsonArray("points");
        if (jsonPoints != null) {
            points.clear();
            DataPointDao dataPointDao = new DataPointDao();

            for (JsonValue jv : jsonPoints) {
                String xid = jv.toString();

                DataPointVO dp = dataPointDao.getDataPoint(xid);
                if (dp == null)
                    throw new TranslatableJsonException("emport.error.missingPoint", xid);

                points.add(new DataPointSummary(dp));
            }
        }
    }
}
