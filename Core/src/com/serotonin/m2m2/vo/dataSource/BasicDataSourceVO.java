package com.serotonin.m2m2.vo.dataSource;

/**
 * Used in data source editing to encapsulate basic data source info, so that if new properties are added not all
 * data source editing code needs to change.
 * 
 * @author Matthew Lohbihler
 */
public class BasicDataSourceVO {
    private String xid;
    private String name;
    private boolean enabled;
    private boolean purgeOverride;
    private int purgeType;
    private int purgePeriod;

    /**
     * @return the xid
     */
    public String getXid() {
        return xid;
    }

    /**
     * @param xid
     *            the xid to set
     */
    public void setXid(String xid) {
        this.xid = xid;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name
     *            the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the purgeOverride
     */
    public boolean isPurgeOverride() {
        return purgeOverride;
    }

    /**
     * @param purgeOverride
     *            the purgeOverride to set
     */
    public void setPurgeOverride(boolean purgeOverride) {
        this.purgeOverride = purgeOverride;
    }

    /**
     * @return the purgeType
     */
    public int getPurgeType() {
        return purgeType;
    }

    /**
     * @param purgeType
     *            the purgeType to set
     */
    public void setPurgeType(int purgeType) {
        this.purgeType = purgeType;
    }

    /**
     * @return the purgePeriod
     */
    public int getPurgePeriod() {
        return purgePeriod;
    }

    /**
     * @param purgePeriod
     *            the purgePeriod to set
     */
    public void setPurgePeriod(int purgePeriod) {
        this.purgePeriod = purgePeriod;
    }

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
    
}
