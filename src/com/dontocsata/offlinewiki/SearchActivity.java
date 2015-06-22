package com.dontocsata.offlinewiki;

import java.util.Locale;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.webkit.WebSettings.TextSize;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

public class SearchActivity extends Activity {

	private DataHelper helper;
	private ProgressDialog pd;
	private ListView listView;

	private float textSize = 24f;

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (helper != null) {
			helper.close();
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		try {
			helper = new DataHelper();
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
			String textSize = prefs.getString("pref_textsize", "normal").toUpperCase(Locale.getDefault());
			TextSize ts = TextSize.valueOf(textSize);
			switch (ts) {
				case SMALLEST:
					this.textSize = 12f;
					break;
				case SMALLER:
					this.textSize = 18f;
					break;
				case NORMAL:
					this.textSize = 24f;
					break;
				case LARGER:
					this.textSize = 30f;
					break;
				case LARGEST:
					this.textSize = 36f;
					break;
			}
			setContentView(R.layout.search_layout);
			listView = (ListView) findViewById(R.id.searchList);

			final EditText edit = (EditText) findViewById(R.id.searchField);
			// edit.requestFocus();
			// InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
			// imm.showSoftInput(edit, InputMethodManager.HIDE_IMPLICIT_ONLY);
			edit.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
			edit.setOnEditorActionListener(new OnEditorActionListener() {

				@Override
				public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
					if (actionId == EditorInfo.IME_ACTION_SEARCH) {
						String term = v.getText().toString();
						search(term);
						return true;
					}
					return false;
				}
			});

			Button searchButton = (Button) findViewById(R.id.searchButton);
			searchButton.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					String term = edit.getText().toString();
					search(term);
				}
			});

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void search(final String term) {
		AsyncTask<Void, Void, Cursor> task = new AsyncTask<Void, Void, Cursor>() {

			@Override
			protected void onPreExecute() {
				pd = new ProgressDialog(SearchActivity.this);
				pd.setTitle("Searching");
				pd.setMessage("Retrieving records...");
				pd.setCancelable(false);
				pd.setIndeterminate(true);
				pd.show();
			}

			@Override
			protected Cursor doInBackground(Void... arg0) {
				try {
					Cursor cursor = helper.search(term);
					cursor.getCount();
					return cursor;
				} catch (Exception e) {
					e.printStackTrace();
				}
				return null;
			}

			@Override
			protected void onPostExecute(Cursor result) {
				ListAdapter la = listView.getAdapter();
				if (la == null || !(la instanceof SearchCursorAdapter)) {
					SearchCursorAdapter adapter = new SearchCursorAdapter(SearchActivity.this, result);
					listView.setAdapter(adapter);
				} else {
					((SearchCursorAdapter) la).changeCursor(result);

				}
				pd.dismiss();
			}

		};
		task.execute((Void[]) null);
	}

	public class SearchCursorAdapter extends CursorAdapter {

		public SearchCursorAdapter(Context context, Cursor cursor) {
			super(context, cursor);
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			return LayoutInflater.from(context).inflate(R.layout.search_item_layout, parent, false);
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			// Find fields to populate in inflated template
			TextView text = (TextView) view.findViewById(R.id.itemText);
			text.setTextSize(textSize);
			final String title = cursor.getString(cursor.getColumnIndex("title"));
			final int id = cursor.getInt(cursor.getColumnIndex("_id"));
			text.setText(title);
			text.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					Intent result = new Intent("com.dontocsata.search");
					result.putExtra("id", id);
					result.putExtra("title", title);
					setResult(Activity.RESULT_OK, result);
					finish();
				}
			});
		}
	}

}
