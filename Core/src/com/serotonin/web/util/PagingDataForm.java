/*
 * Created on 3-Aug-2006
 */
package com.serotonin.web.util;

import java.sql.Date;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

/**
 * @deprecated use something else
 * @author x
 */
@Deprecated
public class PagingDataForm<T> {
    private List<T> data;

    // Sorting and pagination fields.
    private int page;
    private int numberOfPages;
    private int numberOfItems;
    private int offset;
    private int itemsPerPage = 25;

    private String sortField;
    private boolean sortDesc;

    // Criteria fields added here for convenience.
    private int startMonth;
    private int startDay;
    private int startYear;
    private int endMonth;
    private int endDay;
    private int endYear;

    public String getOrderByClause() {
        if (sortField == null)
            return "";
        return "order by " + sortField + (sortDesc ? " desc" : "");
    }

    public void setData(List<T> data) {
        this.data = data;
    }

    public void setNumberOfItems(int numberOfItems) {
        this.numberOfItems = numberOfItems;

        numberOfPages = (numberOfItems - 1) / itemsPerPage + 1;

        if (page >= numberOfPages)
            page = numberOfPages - 1;

        offset = page * itemsPerPage;
    }

    public int getEndIndex() {
        int end = offset + itemsPerPage - 1;
        if (end >= numberOfItems)
            end = numberOfItems - 1;
        if (end < offset)
            end = offset;
        return end;
    }

    public Date getRunTime() {
        return new Date(System.currentTimeMillis());
    }

    public Date getStartDate() {
        Date date = null;
        if (startMonth > 0) {
            GregorianCalendar gc = new GregorianCalendar(startYear, startMonth - 1, startDay);
            gc.setLenient(true);
            date = new Date(gc.getTimeInMillis());

            startYear = gc.get(Calendar.YEAR);
            startMonth = gc.get(Calendar.MONTH) + 1;
            startDay = gc.get(Calendar.DATE);
        }
        return date;
    }

    public Date getEndDate(boolean increment) {
        Date date = null;
        if (endMonth > 0) {
            GregorianCalendar gc = new GregorianCalendar(endYear, endMonth - 1, endDay);
            gc.setLenient(true);

            endYear = gc.get(Calendar.YEAR);
            endMonth = gc.get(Calendar.MONTH) + 1;
            endDay = gc.get(Calendar.DATE);

            // The date should be incremented if we are comparing with timestamps,
            // because the default time of a date is 0:00:00:000
            if (increment)
                gc.add(Calendar.DATE, 1);

            date = new Date(gc.getTimeInMillis());
        }
        return date;
    }

    /**
     * @return Returns the itemsPerPage.
     */
    public int getItemsPerPage() {
        return itemsPerPage;
    }

    /**
     * @param itemsPerPage
     *            The itemsPerPage to set.
     */
    public void setItemsPerPage(int itemsPerPage) {
        this.itemsPerPage = itemsPerPage;
    }

    /**
     * @return Returns the numberOfPages.
     */
    public int getNumberOfPages() {
        return numberOfPages;
    }

    /**
     * @return Returns the numberOfItems.
     */
    public int getNumberOfItems() {
        return numberOfItems;
    }

    /**
     * @return Returns the page.
     */
    public int getPage() {
        return page;
    }

    /**
     * @param page
     *            The page to set.
     */
    public void setPage(int page) {
        this.page = page;
    }

    /**
     * @return Returns the offset.
     */
    public int getOffset() {
        return offset;
    }

    /**
     * @return Returns the sortField.
     */
    public String getSortField() {
        return sortField;
    }

    /**
     * @param sortField
     *            The sortField to set.
     */
    public void setSortField(String sortField) {
        this.sortField = sortField;
    }

    /**
     * @return Returns the sortDesc.
     */
    public boolean getSortDesc() {
        return sortDesc;
    }

    /**
     * @param sortDesc
     *            The sortDesc to set.
     */
    public void setSortDesc(boolean sortDesc) {
        this.sortDesc = sortDesc;
    }

    /**
     * @return Returns the data.
     */
    public List<T> getData() {
        return data;
    }

    /**
     * @return Returns the endDay.
     */
    public int getEndDay() {
        return endDay;
    }

    /**
     * @param endDay
     *            The endDay to set.
     */
    public void setEndDay(int endDay) {
        this.endDay = endDay;
    }

    /**
     * @return Returns the endMonth.
     */
    public int getEndMonth() {
        return endMonth;
    }

    /**
     * @param endMonth
     *            The endMonth to set.
     */
    public void setEndMonth(int endMonth) {
        this.endMonth = endMonth;
    }

    /**
     * @return Returns the endYear.
     */
    public int getEndYear() {
        return endYear;
    }

    /**
     * @param endYear
     *            The endYear to set.
     */
    public void setEndYear(int endYear) {
        this.endYear = endYear;
    }

    /**
     * @return Returns the startDay.
     */
    public int getStartDay() {
        return startDay;
    }

    /**
     * @param startDay
     *            The startDay to set.
     */
    public void setStartDay(int startDay) {
        this.startDay = startDay;
    }

    /**
     * @return Returns the startMonth.
     */
    public int getStartMonth() {
        return startMonth;
    }

    /**
     * @param startMonth
     *            The startMonth to set.
     */
    public void setStartMonth(int startMonth) {
        this.startMonth = startMonth;
    }

    /**
     * @return Returns the startYear.
     */
    public int getStartYear() {
        return startYear;
    }

    /**
     * @param startYear
     *            The startYear to set.
     */
    public void setStartYear(int startYear) {
        this.startYear = startYear;
    }
}
