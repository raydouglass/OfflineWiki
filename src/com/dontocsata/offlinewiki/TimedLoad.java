package com.dontocsata.offlinewiki;

import java.text.SimpleDateFormat;
import java.util.Date;

public class TimedLoad {

	private String title;
	private Date start;
	private Date queryDone;
	private Date readDone;
	private Date compressionDone;
	private Date end;

	public TimedLoad(String title) {
		this.title = title;
		start = new Date();
	}

	public TimedLoad finished() {
		end = new Date();
		return this;
	}

	public void doneQuery() {
		queryDone = new Date();
	}

	public void doneRead() {
		readDone = new Date();
	}

	public void doneCompression() {
		compressionDone = new Date();
	}

	public String getTitle() {
		return title;
	}

	public Date getStart() {
		return start;
	}

	public Date getEnd() {
		return end;
	}

	@Override
	public String toString() {
		return title + "<table>" + date("Start", start) + date("Query", queryDone) + date("Read", readDone)
				+ date("Compression", compressionDone) + date("End", end) + "</table>";
	}

	public String format(Date date) {
		SimpleDateFormat df = new SimpleDateFormat("hh:mm:ss.SSS a");
		return df.format(date);
	}

	public String date(String name, Date date) {
		return "<tr><td>" + name + "</td><td>" + format(date) + "</td></tr>";
	}

}
