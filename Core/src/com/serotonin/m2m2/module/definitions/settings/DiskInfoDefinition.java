/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.module.definitions.settings;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.serotonin.m2m2.module.SystemInfoDefinition;
import com.serotonin.m2m2.module.definitions.settings.DiskInfoDefinition.DiskInfo;

/**
 * 
 * @author Terry Packer
 */
public class DiskInfoDefinition extends SystemInfoDefinition<List<DiskInfo>>{

	public final String KEY = "diskInfo";
	
	@Override
	public String getKey() {
		return KEY;
	}

	@Override
	public List<DiskInfo> getValue() {
        FileSystem fs = FileSystems.getDefault();
        List<DiskInfo> disks = new ArrayList<DiskInfo>();
        for(Path root : fs.getRootDirectories()){
        	try {
				FileStore store = Files.getFileStore(root);
				DiskInfo disk = new DiskInfo();
				disk.setName(root.getRoot().toString());
				disk.setTotalSpace(store.getTotalSpace());
				disk.setUsableSpace(store.getUsableSpace());
				disks.add(disk);
			} catch (IOException e) { }
        }
        return disks;
	}

	public class DiskInfo{
		private String name;
		private long totalSpace;
		private long usableSpace;
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		public long getTotalSpace() {
			return totalSpace;
		}
		public void setTotalSpace(long totalSpace) {
			this.totalSpace = totalSpace;
		}
		public long getUsableSpace() {
			return usableSpace;
		}
		public void setUsableSpace(long useableSpace) {
			this.usableSpace = useableSpace;
		}
	}

    @Override
    public String getDescriptionKey() {
        return "systemInfo.diskInfoListDesc";
    }
	
}
