package com.dontocsata.offlinewiki;

import java.util.ArrayList;
import java.util.List;

import android.app.Application;

public class OfflineWikiApplication extends Application {

	public static final List<TimedLoad> TIMED_LOADS = new ArrayList<TimedLoad>();

	public static TimedLoad getMostRecent() {
		if (TIMED_LOADS.isEmpty()) {
			return null;
		}
		return TIMED_LOADS.get(TIMED_LOADS.size() - 1);
	}

	public static List<String> RECENT_SEARCH;

}
