package com.fritzbang.theplayer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class ThePlayerStartServiceReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		Intent service = new Intent(context, ThePlayerMediaService.class);
		context.startService(service);
	}
}
