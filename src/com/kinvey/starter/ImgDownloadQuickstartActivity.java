package com.kinvey.starter;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.kinvey.KCSClient;
import com.kinvey.exception.KinveyException;
import com.kinvey.persistence.EntityDict;
import com.kinvey.util.KinveyCallback;
import com.kinvey.util.ListCallback;

public class ImgDownloadQuickstartActivity extends Activity {

	protected static final String TAG = "KinveyQuickstartActivity";
	private TextView textView;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		textView = (TextView) findViewById(R.id.maintext);


		mKinveyClient = ((ImgDownloadQuickstartApp) getApplication()).getKinveyService();

		mKinveyClient.pingService(new KinveyCallback<Boolean>() {

			public void onFailure(Throwable t) {
				appendMsgTextView("Connection FAILED");
			}

			public void onSuccess(Boolean b) {
				appendMsgTextView("Connection OK");
			}

		});
		
		
		mKinveyClient.collection("_blob").all(new ListCallback<EntityDict>() {
			
			@Override
			public void onSuccess(List<EntityDict> blobs) {
				String[] blobNames = new String[blobs.size()];
				
				if (blobNames.length == 0) 
					appendMsgTextView("No blobs to download");

				int i = 0;
				for (EntityDict b : blobs) {
					appendMsgTextView(b.getProperty("objectname").toString());
					blobNames[i++] = b.getProperty("objectname").toString();
				}
				
				new DownloadFilesTask().execute(blobNames);
			}

		});
		

	}

	private void appendMsgTextView(String msg) {
		textView.setText(textView.getText() + "\n" + msg);
	}

	private void setProgressPercent(Integer integer) {
		appendMsgTextView("Download " + integer + "% complete");
	}

	
	 private class DownloadFilesTask extends AsyncTask<String, Integer, Long> {
		private static final int MAX_W = 512;
		private static final int MAX_H = 512;


		protected Long doInBackground(String... blobnames) {
			
			URL[] urls  = fetchAllBlobUrls(blobnames);
			
			int count = urls.length;
			long totalsize = 0;

			Bitmap thumbnail = null;
			BitmapFactory.Options opts = new BitmapFactory.Options();
			
			for (int i = 0; i < count; i++) {
				try {
					publishProgress((int) ((i / (float) count) * 100));

					// down sampling 
					int scaleFactor = 0;
					opts.inJustDecodeBounds = true;
					do {
						opts.inSampleSize = (int) Math.pow(2, scaleFactor++);
						BitmapFactory.decodeStream(
								(InputStream) urls[i].getContent(), null, opts);
						android.util.Log.d(TAG, "opts.inSampleSize: "
								+ opts.inSampleSize + ", opts.outWidth: "
								+ opts.outWidth);
					} while (opts.outWidth > MAX_W || opts.outHeight > MAX_H);

					// download the file
					opts.inJustDecodeBounds = false;
					thumbnail = BitmapFactory.decodeStream(
							(InputStream) urls[i].getContent(), null, opts);
					
					totalsize += thumbnail.getRowBytes();
				} catch (MalformedURLException e) {
					Log.e(TAG, "bad url", e);
				} catch (IOException e) {
					Log.e(TAG, "decoding bitmap", e);
				}

				// Escape early if cancel() is called
				if (isCancelled())
					break;
			}

			return totalsize;
		}
		
		private URL[] fetchAllBlobUrls(String[] blobnames) {
			URL[] urls = new URL[blobnames.length];
			int count = blobnames.length;
			for (int i = 0; i < count; i++) {
				try {
					urls[i] = new URL( mKinveyClient.resource(blobnames[i]).getUriForResource());
				} catch (MalformedURLException e) {
					appendMsgTextView("failed to get URI" + e.getMessage());
				} catch (KinveyException e) {
					appendMsgTextView("failed to get URI" + e.getMessage());
				}
			}
			return urls;
		}

		@Override
		protected void onProgressUpdate(Integer... progress) {
			setProgressPercent(progress[0]);
		}


		protected void onPostExecute(Long result) {
			appendMsgTextView("Download completed "+ result + " bytes");
		}

	 }

	 
	private AlertDialog alertDialog;
	private static KCSClient mKinveyClient;
	
	@Override
	protected void onPause() {
		// avoid window leaks
		if (alertDialog != null && alertDialog.isShowing())
			alertDialog.dismiss();
		
		super.onPause();
	}
	
	/**
	 * Display a simple alert dialog with the given text and title.
	 * 
	 * @param context
	 *            Android context in which the dialog should be displayed
	 * @param title
	 *            Alert dialog title
	 * @param text
	 *            Alert dialog message
	 */
	protected void showAlert(Context context, String title, String text) {
		Builder alertBuilder = new Builder(context);
		alertBuilder.setTitle(title);
		alertBuilder.setMessage(text);
		alertDialog = alertBuilder.create();
		alertDialog.show();
	}

}