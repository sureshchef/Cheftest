package controllers;

import com.google.gson.annotations.Expose;

/**
 * Holder for set of results to be sent to as a result of an Ajax query
 * from DataTables jQuery plugin.
 * 
 * @see <a href="http://datatables.net">DataTables</a>
 */
public class PaginatedResult {
	@Expose
	public int sEcho;
	@Expose
	public long iTotalRecords;
	@Expose
	public long iTotalDisplayRecords;
	@Expose
	public Object aaData;
	
	PaginatedResult(int sEcho, long iTotalRecords, long iTotalDisplayRecords, Object aaData) {
		this.sEcho = sEcho;
		this.iTotalRecords = iTotalRecords;
		this.iTotalDisplayRecords= iTotalDisplayRecords;
		this.aaData = aaData;
	}
}
