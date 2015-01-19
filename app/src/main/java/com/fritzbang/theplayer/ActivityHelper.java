package com.fritzbang.theplayer;

import android.app.Activity;
import android.content.pm.ActivityInfo;

public class ActivityHelper {
	public static void initialize(Activity activity) {
		// Do all sorts of common task for your activities here including:

		activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
	}
}
