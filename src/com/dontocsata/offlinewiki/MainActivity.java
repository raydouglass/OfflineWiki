package com.dontocsata.offlinewiki;

import java.util.Locale;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.WebSettings.TextSize;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;

public class MainActivity extends Activity {

	public static final int REQUEST_CODE = 123;

	private WebView webView;
	private DataHelper helper;
	private ProgressDialog pd;

	private void setTextSize() {
		if (webView != null) {
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
			String pref = prefs.getString("pref_textsize", "normal").toUpperCase(Locale.getDefault());
			TextSize ts = TextSize.valueOf(pref);
			int textSize = 24;
			switch (ts) {
				case SMALLEST:
					textSize = 12;
					break;
				case SMALLER:
					textSize = 18;
					break;
				case NORMAL:
					textSize = 24;
					break;
				case LARGER:
					textSize = 30;
					break;
				case LARGEST:
					textSize = 36;
					break;
			}
			webView.getSettings().setDefaultFontSize(textSize);
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		try {
			helper = new DataHelper();
			setContentView(R.layout.main_layout);
			webView = (WebView) findViewById(R.id.webview);
			setTextSize();
			webView.setWebViewClient(new WebViewClient() {

				@Override
				public boolean shouldOverrideUrlLoading(WebView view, String url) {
					String key = url;
					if (key.startsWith("/")) {
						key = key.substring(1);
					}
					display(view, null, key);
					return true;
				}

				@Override
				public void onPageFinished(WebView view, String url) {
					super.onPageFinished(view, url);
					if (pd != null) {
						pd.dismiss();
						pd = null;
					}
				}

				@Override
				public void onLoadResource(WebView view, String url) {
					super.onLoadResource(view, url);
					if (pd != null) {
						pd.setMessage("Rendering page...");
					}
				}

			});

			((Button) findViewById(R.id.mainSearchButton)).setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					Intent intent = new Intent(MainActivity.this, SearchActivity.class);
					startActivityForResult(intent, REQUEST_CODE);
				}
			});

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		setTextSize();
	}

	public void display(final WebView view, final Integer id, final String title) {
		AsyncTask<Void, Void, String> task = new AsyncTask<Void, Void, String>() {

			private TimedLoad timedLoad;

			@Override
			protected void onPreExecute() {
				pd = new ProgressDialog(MainActivity.this);
				pd.setTitle("Processing");
				pd.setMessage("Retrieving data...");
				pd.setCancelable(false);
				pd.setIndeterminate(true);
				pd.show();
				timedLoad = new TimedLoad(title);
				OfflineWikiApplication.TIMED_LOADS.add(timedLoad);
			}

			@Override
			protected String doInBackground(Void... arg0) {
				try {
					int newId = 0;
					if (id == null) {
						newId = helper.getId(title);
					} else {
						newId = id;
					}
					String html = helper.get(newId);
					if (html == null || html.length() == 0) {
						html = "No content.";
					}
					return html.replaceAll("%", "&#37;");
				} catch (Exception e) {
					e.printStackTrace();
				}
				return null;
			}

			@Override
			protected void onPostExecute(String result) {
				// view.loadUrl("about:blank");
				// view.loadData(result, "text/html", null);
				view.loadDataWithBaseURL("data://", result, "text/html", null, "/" + title);
				timedLoad.finished();
			}

		};
		task.execute((Void[]) null);
	}

	@Override
	protected void onDestroy() {
		helper.close();
		super.onDestroy();
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// Check if the key event was the Back button and if there's history
		if (keyCode == KeyEvent.KEYCODE_BACK && webView != null && webView.canGoBack()) {
			webView.goBack();
			return true;
		} else if (keyCode == 92 || keyCode == 93) {
			webView.pageUp(false);
			return true;
		} else if (keyCode == 94 || keyCode == 95) {
			webView.pageDown(false);
			return true;
		}
		// If it wasn't the Back key or there's no web page history, bubble up to the default
		// system behavior (probably exit the activity)
		return super.onKeyDown(keyCode, event);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQUEST_CODE && resultCode == Activity.RESULT_OK) {
			try {
				int id = data.getExtras().getInt("id");
				String title = data.getExtras().getString("title");
				display(webView, id, title);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		if (webView != null) {
			webView.restoreState(savedInstanceState);
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if (webView != null) {
			webView.saveState(outState);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
			case R.id.settings:
				Intent intent = new Intent(this, WikiPreferences.class);
				startActivity(intent);
				return true;
			case R.id.times:
				StringBuilder sb = new StringBuilder();
				for (TimedLoad tl : OfflineWikiApplication.TIMED_LOADS) {
					sb.append(tl.toString()).append("<br>");
				}
				webView.loadDataWithBaseURL("data", sb.toString(), "text/html", null, "/Special:Times");
				return true;
			case R.id.random_article:
				String data = helper.getRandomArticles(10);
				webView.loadDataWithBaseURL("data://", data, "text/html", null, "/Special:Random");
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

}
