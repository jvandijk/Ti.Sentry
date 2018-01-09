/**
 * This file was auto-generated by the Titanium Module SDK helper for Android
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2010 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 *
 */
package ti.sentry;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.KrollExceptionHandler;
import org.appcelerator.kroll.KrollModule;
import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.kroll.common.AsyncResult;
import org.appcelerator.kroll.common.TiMessenger;
import org.appcelerator.titanium.TiApplication;
import org.appcelerator.titanium.TiProperties;
import org.json.JSONObject;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Handler;
import android.os.Message;

import com.joshdholtz.sentry.Sentry;
import com.joshdholtz.sentry.Sentry.SentryEventBuilder;

@Kroll.module(name = "Sentry", id = "ti.sentry")
public class SentryModule extends KrollModule implements Handler.Callback,
		KrollExceptionHandler {
	final int MSG_OPEN_ERROR_DIALOG = 1000;
	private static String sentryDSN;
	private static Handler mainHandler;

	public SentryModule() {
		super();
		mainHandler = new Handler(TiMessenger.getMainMessenger().getLooper(),
				this);
	}

	public boolean handleMessage(Message msg) {
		switch (msg.what) {
		case MSG_OPEN_ERROR_DIALOG:
			AsyncResult asyncResult = (AsyncResult) msg.obj;
			ExceptionMessage errorMessage = (ExceptionMessage) asyncResult
					.getArg();
			handleKrollError(errorMessage);
			asyncResult.setResult(null);
			return true;
		default:
			break;
		}

		return false;
	}

	@Kroll.onAppCreate
	public static void onAppCreate(TiApplication app) {
		// TiApplication app = TiApplication.getInstance();
		String TYPE = (app.getDeployType()
				.equals(TiApplication.DEPLOY_TYPE_PRODUCTION)) ? "PRODUCTION"
				: "DEVELOPMENT";

		TiProperties appProperties = app.getAppProperties();
		sentryDSN = appProperties.getString("SENTRY_DSN_" + TYPE, "");
		if (sentryDSN != null)
			Sentry.init(app, sentryDSN);
	}

	@Kroll.method
	public void captureMessage(String msg) {
		Sentry.captureMessage(msg);
	}

	@Kroll.method
	public void addHttpBreadcrumb(String url, String method, int result) {
		Sentry.addHttpBreadcrumb(url, method, result);
	}

	@Kroll.method
	public void addNavigationBreadcrumb(String event, String foo, String bar) {
		Sentry.addNavigationBreadcrumb(event, foo, bar);
	}

	@Kroll.method
	public void addBreadcrumb(String event, String payload) {
		Sentry.addBreadcrumb(event, payload);
	}

	@Kroll.method
	public void captureEvent(KrollDict kd) {
		Sentry.SentryEventBuilder builder = new Sentry.SentryEventBuilder();
		if (kd.containsKeyAndNotNull("message"))
			builder.setMessage(kd.getString("message"));
		if (kd.containsKeyAndNotNull("culprit"))
			builder.setCulprit(kd.getString("culprit"));
		if (kd.containsKeyAndNotNull("release"))
			builder.setRelease(kd.getString("release"));
		builder.setTimestamp(System.currentTimeMillis());
		Sentry.captureEvent(builder);
	}

	protected void handleKrollError(ExceptionMessage error) {
		SentryEventBuilder builder = new SentryEventBuilder();
		builder.addExtra("title", error.title);
		builder.addExtra("message", error.message);
		builder.addExtra("sourceName", error.sourceName);
		builder.addExtra("line", "" + error.line);
		builder.addExtra("lineSource", error.lineSource);
		builder.addExtra("lineOffset", "" + error.lineOffset);
		builder.setTimestamp(System.currentTimeMillis());
		builder.setUser(new JSONObject());
		builder.setEnvironment(android.os.Build.VERSION.CODENAME);
		try {
			PackageInfo info = TiApplication
					.getInstance()
					.getApplicationContext()
					.getPackageManager()
					.getPackageInfo(
							TiApplication.getInstance().getPackageName(), 0);
			builder.addTag("version", info.versionName);
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}

		Sentry.captureEvent(builder);
	}

	@Override
	public void handleException(ExceptionMessage error) {
		if (sentryDSN == null)
			return;
		if (TiApplication.isUIThread()) {
			handleKrollError(error);
		} else {
			TiMessenger.sendBlockingMainMessage(
					mainHandler.obtainMessage(MSG_OPEN_ERROR_DIALOG), error);
		}

	}

}
