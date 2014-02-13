package com.kau.jonathan.umdschedulesharer;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.Signature;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Picture;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebView.PictureListener;
import android.webkit.WebViewClient;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.UiLifecycleHelper;
import com.facebook.model.GraphUser;
import com.facebook.widget.LoginButton;
import com.facebook.widget.ProfilePictureView;

public class SignInActivity extends Activity {
	private static final int REAUTH_ACTIVITY_CODE = 100;
	public static final String headerString = "<center>";
	ProgressDialog loggingIn;
	ProgressDialog umdLoginDialog;
	ProfilePictureView profilePictureView;
	TextView username;
	TextView title;
	String fb_id;
	EditText umd_username;
	EditText umd_password;
	Bitmap defaultFacebookPic;

	private UiLifecycleHelper uiHelper;
	private Session.StatusCallback callback = new Session.StatusCallback() {
		@Override
		public void call(final Session session, final SessionState state, final Exception exception) {
			onSessionStateChange(session, state, exception);
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_sign_in);
		

		// Facebook Session
		uiHelper = new UiLifecycleHelper(this, callback);
		uiHelper.onCreate(savedInstanceState);
		
		try {
			PackageInfo info = getPackageManager().getPackageInfo("com.kau.jonathan.umdschedulesharer", PackageManager.GET_SIGNATURES);
			for (Signature signature : info.signatures) {
			    MessageDigest md = MessageDigest.getInstance("SHA");
			    md.update(signature.toByteArray());
			    Log.e("MY KEY HASH:", Base64.encodeToString(md.digest(), Base64.DEFAULT));
			}
			} catch (NameNotFoundException e) {

			} catch (NoSuchAlgorithmException e) {

			}

		// Check to see if user has gotten schedule already
		SharedPreferences prefs = this.getSharedPreferences("com.kau.jonathan.umdschedulesharer", Context.MODE_PRIVATE);
		int obtained_schedule = prefs.getInt("com.kau.jonathan.umdschedulesharer.obtained_schedule", 0);

		if(obtained_schedule == 1) {
			// Take source and send with intent to Schedule Activity
			Intent intent = new Intent(SignInActivity.this, ScheduleActivity.class);

			// Attach source code
			intent.putExtra("SOURCE_CODE", prefs.getString("com.kau.jonathan.umdschedulesharer.schedule_code", ""));	
			intent.putExtra("SCHEDULE_DATA", prefs.getString("com.kau.jonathan.umdschedulesharer.schedule_data", ""));


			// Start activity
			startActivity(intent);
		}

		// Set default prof pic
		defaultFacebookPic = BitmapFactory.decodeResource(getResources(),
				R.drawable.fb_default);
		((ImageView)findViewById(R.id.test_prof_pic)).setImageBitmap(makeCircular(defaultFacebookPic));


		// Generate typefaces		
		final Typeface face=Typeface.createFromAsset(this.getAssets(),
				"fonts/Lato-Reg.ttf");
		final Typeface boldface=Typeface.createFromAsset(this.getAssets(),
				"fonts/Lato-Bol.ttf");
		
		// Facebook Login Button
		LoginButton login = (LoginButton)findViewById(R.id.fb_login);
		login.setTypeface(face);


		// Populate Spinner choices
		Spinner spinner = (Spinner) findViewById(R.id.semester_spinner);
		// Create an ArrayAdapter using the string array and a default spinner layout
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.spinner_item){

			public View getView(int position, View convertView, ViewGroup parent) {
				View v = super.getView(position, convertView, parent);				
				((TextView) v).setTypeface(face);
				return v;
			}


			public View getDropDownView(int position,  View convertView,  ViewGroup parent) {
				View v =super.getDropDownView(position, convertView, parent);
				((TextView) v).setTypeface(face);
				return v;
			}
		};

		String[] semesters = getResources().getStringArray(R.array.semesters_array);

		for(String s: semesters) {
			adapter.add(s);
		}

		// Specify the layout to use when the list of choices appears
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		// Apply the adapter to the spinner
		spinner.setAdapter(adapter);


//		// Find the user's profile picture custom view
//		profilePictureView = (ProfilePictureView) findViewById(R.id.selection_profile_pic);
//		profilePictureView.setCropped(true);

		// Change typeface for all text
		title = (TextView)findViewById(R.id.main_title);
		username = (TextView)findViewById(R.id.selection_user_name);

		umd_username = (EditText) findViewById(R.id.umd_username);
		umd_password = (EditText) findViewById(R.id.umd_password);
		Button umd_login = (Button) findViewById(R.id.umd_login);

		title.setTypeface(face);
		username.setTypeface(face);
		umd_username.setTypeface(face);
		umd_password.setTypeface(face);
		umd_login.setTypeface(boldface);

		// Get user data
		Session session = Session.getActiveSession();
		if (session != null && session.isOpened()) {
			loggingIn = ProgressDialog.show(
					SignInActivity.this, "", "Logging into Facebook", true);
			makeMeRequest(session);
		}

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.sign_in, menu);
		return true;
	}

	private void makeMeRequest(final Session session) {
		// Make an API call to get user data and define a 
		// new callback to handle the response.
		Request request = Request.newMeRequest(session, 
				new Request.GraphUserCallback() {
			@Override
			public void onCompleted(GraphUser user, Response response) {
				// If the response is successful
				if(loggingIn != null) loggingIn.dismiss();
				

				// Show views
				//profilePictureView.setVisibility(View.VISIBLE);
				username.setVisibility(View.VISIBLE);
				title.setVisibility(View.VISIBLE);
				findViewById(R.id.login_box).setVisibility(View.VISIBLE);
				findViewById(R.id.picture_frame).setVisibility(View.VISIBLE);
				findViewById(R.id.semester_spinner).setVisibility(View.VISIBLE);

				if (session == Session.getActiveSession()) {
					if (user != null) {
						// Set the id for the ProfilePictureView
						// view that in turn displays the profile picture.
						//profilePictureView.setProfileId(user.getId());
						// Set the Textview's text to the user's name.
						username.setText(user.getName());

						// Get Facebook number ID
						fb_id = user.getId();


						// Playing with image stuff
						String imageUrl= "http://graph.facebook.com/" + fb_id + "/picture?type=large";
						new RetrieveImgTask().execute(imageUrl);
					}
				}
				if (response.getError() != null) {
					// Handle errors, will do so later.
				}
			}
		});
		request.executeAsync();
	} 

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		uiHelper.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	public void onBackPressed() {

	}

	// FB State Managers

	private void onSessionStateChange(final Session session, SessionState state, Exception exception) {
		if (session.isOpened()) {
			// Get the user's data.
			makeMeRequest(session);
		} else if (session.isClosed()) {
			// Reset views
			defaultFacebookPic = BitmapFactory.decodeResource(getResources(),
					R.drawable.fb_default);
			((ImageView)findViewById(R.id.test_prof_pic)).setImageBitmap(makeCircular(defaultFacebookPic));
			username.setVisibility(View.GONE);
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		// For scenarios where the main activity is launched and user
		// session is not null, the session state change notification
		// may not be triggered. Trigger it if it's open/closed.
		Session session = Session.getActiveSession();

		if (session != null &&
				(session.isOpened() || session.isClosed()) ) {
			onSessionStateChange(session, session.getState(), null);
		} else {
		}

		uiHelper.onResume();

	}

	@Override
	public void onSaveInstanceState(Bundle bundle) {
		super.onSaveInstanceState(bundle);
		uiHelper.onSaveInstanceState(bundle);
	}

	@Override
	public void onPause() {
		super.onPause();
		uiHelper.onPause();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		uiHelper.onDestroy();
	}

	// For circular profile image

	class RetrieveImgTask extends AsyncTask<String, Void, Void> {
		private Bitmap bitmap;
		private Bitmap output;
		private Exception exception;

		protected Void doInBackground(String... urls) {
			try {
				String imageUrl = urls[0];

				URL newurl = null;
				try {
					newurl = new URL(imageUrl);
				} catch (MalformedURLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} 
				bitmap = null;
				try {
					bitmap = BitmapFactory.decodeStream(newurl.openConnection().getInputStream());
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				return null;
			} catch (Exception e) {
				this.exception = e;
				return null;
			}
		}

		protected void onPostExecute(Void v) {


			if (bitmap != null) {
				Bitmap output = makeCircular(bitmap);

				((ImageView)findViewById(R.id.test_prof_pic)).setImageBitmap(output);
			}
		}
	}

	public Bitmap makeCircular(Bitmap bitmap) {
		Bitmap output;
		if(bitmap.getWidth() >= bitmap.getHeight()){
			output = Bitmap.createBitmap(bitmap.getHeight(),
					bitmap.getHeight(), Config.ARGB_8888);
		} else {
			output = Bitmap.createBitmap(bitmap.getWidth(),
					bitmap.getWidth(), Config.ARGB_8888);
		}
		Canvas canvas = new Canvas(output);

		final int color = 0xff424242;
		final Paint paint = new Paint();
		Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());

		paint.setAntiAlias(true);
		canvas.drawARGB(0, 0, 0, 0);
		paint.setColor(color);
		if(bitmap.getWidth() >= bitmap.getHeight()){
			rect = new Rect(0, 0, bitmap.getHeight(), bitmap.getHeight());
			canvas.drawCircle(bitmap.getHeight() / 2, bitmap.getHeight() / 2,
					bitmap.getHeight() / 2, paint);
		} else {

			rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getWidth());
			canvas.drawCircle(bitmap.getWidth() / 2, bitmap.getWidth() / 2,
					bitmap.getWidth() / 2, paint);
		}
		paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
		canvas.drawBitmap(bitmap, rect, rect, paint);

		return output;
	}

	// When user presses sign in	
	public void umdSignInAction(View v) {
		umd_username = (EditText) findViewById(R.id.umd_username);
		umd_password = (EditText) findViewById(R.id.umd_password);

		if(umd_username != null && umd_password != null && 
				umd_username.getText().toString().trim().length() > 0 && umd_password.getText().toString().trim().length() > 0) {

			// Hide keyboard
			InputMethodManager imm = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
			if(getCurrentFocus() != null) {
				imm.hideSoftInputFromWindow(this.getCurrentFocus().getWindowToken(),      
						InputMethodManager.HIDE_NOT_ALWAYS);
			}

			// Show dialog
			umdLoginDialog = ProgressDialog.show(
					SignInActivity.this, "", "Signing In", true);
			loginProcess();
		} else {
			Toast.makeText(SignInActivity.this, "Please fill out both login fields", Toast.LENGTH_SHORT).show();
		}
	}

	//////////////////////////////////////////////////////////////////////////////////////
	// Capturing Schedule Code


	@SuppressLint({ "JavascriptInterface", "NewApi" })
	public void loginProcess() {

		String loadUrl = "http://mobilemy.umd.edu/";

		// Initialize the WebView and edit settings
		WebView view = (WebView) findViewById(R.id.login_page);
		view.getSettings().setJavaScriptEnabled(true);
		view.getSettings().setBuiltInZoomControls(true);
		view.getSettings().setDomStorageEnabled(true);
		view.getSettings().setLoadWithOverviewMode(true);
		view.getSettings().setUseWideViewPort(true);
		view.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
		view.getSettings().setSavePassword(false);
		view.getSettings().setSaveFormData(false);
		view.clearCache(true);
		view.clearHistory();
		view.clearSslPreferences();
		view.clearFormData();

		CookieSyncManager.createInstance(this);
		CookieSyncManager.getInstance().startSync();
		CookieManager.getInstance().setAcceptCookie(true);
		CookieManager.getInstance().removeAllCookie();


		/* Register a new JavaScript interface called HTMLOUT */
		view.addJavascriptInterface(new MyJavaScriptInterface(this), "HTMLOUT");

		// Sets the webview client for loading and accessing the HTML source of the schedule
		view.setWebViewClient(new WebViewClient() {
			int count = 0;

			@Override  
			public boolean shouldOverrideUrlLoading(WebView view, String url)  
			{  
				return false; 
			}  

			@SuppressWarnings("deprecation")
			@Override
			public void onPageFinished(WebView view, String url) {
				if (count == 0) {
					count++;
					// Sets the webview client for loading and accessing the HTML source of the schedule
					view.setPictureListener(new PictureListener() {  
						int count = 0;

						public void onNewPicture(WebView view, Picture picture) {

							if (count == 0) {
								count++;

								//Toast.makeText(SignInActivity.this, "Signed into my.umd.edu!", Toast.LENGTH_SHORT).show();

								// Wait for completed login using UID       
								CookieManager manager = CookieManager.getInstance();
								//Toast.makeText(SignInActivity.this, Boolean.toString(manager.hasCookies()), Toast.LENGTH_SHORT).show();
								//Toast.makeText(SignInActivity.this, "Cookie URL: " + view.getUrl(), Toast.LENGTH_SHORT).show();

								if((manager.getCookie(view.getUrl()) != null && manager.getCookie(view.getUrl()).contains("true")) || !view.getUrl().contains("0")) {
									// Load the actual schedule page
									view.loadUrl("javascript:window.HTMLOUT.processHTML('<html>'+document.getElementsByTagName('html')[0].innerHTML+'</html>');");

								}
							}

						}
					});

					String umd_username = ((EditText) findViewById(R.id.umd_username)).getText().toString();
					String umd_password = ((EditText) findViewById(R.id.umd_password)).getText().toString();

					view.loadUrl("javascript:(function() { " +  
							"document.lform.in_tx_username.value='" + umd_username + "'; " +  
							"document.lform.in_pw_userpass.value='" + umd_password + "'; " +
							"doLogin(); " +
							"})()");
				}

				//Toast.makeText(SignInActivity.this, url, Toast.LENGTH_LONG).show();

				// Wait for completed login using UID       
				CookieManager manager = CookieManager.getInstance();
				//Toast.makeText(SignInActivity.this, Boolean.toString(manager.hasCookies()), Toast.LENGTH_SHORT).show();
				//Toast.makeText(SignInActivity.this, "Cookie URL: " + view.getUrl(), Toast.LENGTH_SHORT).show();

				if((manager.getCookie(view.getUrl()) != null && manager.getCookie(view.getUrl()).contains("true")) || !view.getUrl().contains("0")) {
					if(!view.getUrl().contains("0")) { // Incorrect login
						Toast.makeText(SignInActivity.this, "Incorrect login", Toast.LENGTH_SHORT).show();
						umdLoginDialog.dismiss();
					} else { // Correct login

						// Load the actual schedule page
						view.loadUrl("javascript:window.HTMLOUT.processHTML('<html>'+document.getElementsByTagName('html')[0].innerHTML+'</html>');");
					}
				}

			}
		});

		// Load the actual schedule page
		//view.loadUrl("https://mobilemy.umd.edu/portal/server.pt/gateway/PTARGS_0_340574_368_211_0_43/https%3B/www.sis.umd.edu/testudo/studentSched?term=201401");
		view.loadUrl("https://mobilemy.umd.edu/portal/server.pt/gateway/PTARGS_0_340574_368_211_0_43/https%3B/www.sis.umd.edu/testudo/studentSched?term=201401");
	}


	// Captures the HTML source of the schedule webpage
	class MyJavaScriptInterface
	{
		int count = 0;
		Context mContext;

		MyJavaScriptInterface(Context c) {
			mContext = c;
		}

		@SuppressLint("NewApi")
		@SuppressWarnings("deprecation")
		@JavascriptInterface
		public void processHTML(String html)
		{
			//Toast.makeText(SignInActivity.this, "Herro anybody" + html, Toast.LENGTH_SHORT).show();
			//WebView scheduleBrowser = (WebView) findViewById(R.id.screenshot_page);
			//scheduleBrowser.removeJavascriptInterface("HTMLOUT");

			if(count == 0) {
				count++;

				//Toast.makeText(SignInActivity.this, "Processing HTML!" + html, Toast.LENGTH_SHORT).show();

				// Determines the beginning and end of just the schedule
				int beginIndex = html.indexOf(headerString);
				int endIndex = html.indexOf("</table>", beginIndex) + 7;
				if(beginIndex == -1) beginIndex = 0;

				// Crops the substring of HTML source
				final String scheduleTable = html.substring(beginIndex, endIndex + 1);

				// Find beginning and end of comment for schedule data
				int beginComment = html.indexOf("<!-- data size:");
				int endComment = html.indexOf("-->", beginComment);

				final String scheduleData = html.substring(beginComment, endComment + 1);

				if(html.indexOf("An Error occurred while running this application.") != -1) { // Encountered error
					Toast.makeText(SignInActivity.this, "An Error occurred while running this application. " +
							"Please try again. If the problem persists, contact the Office of the Registrar " +
							"at registrar-help@umd.edu or (301)314-8240.", Toast.LENGTH_LONG).show();
					umdLoginDialog.dismiss();
				} else {

					float densityDPI = getResources().getDisplayMetrics().density; 
					double zoom = 1;
					double height = 100;
					//if (densityDPI > 1.5) {
						zoom = 1.35;
						height = 110;
					//}

					final String header = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>" +
							"<head><style type='text/css'>html, body {	zoom:" + zoom + "; height: 100%;	padding: 0;	margin: 0;} " +
							"#table {display: table; 	height: 100%;	width: 100%;} " +
							"#cell {	display: table-cell; 	vertical-align: middle;}</style></head>";
					final String body = "<body><div id='table'><div id='cell'>";
					final String footer = "<br><font size=+2><b>www.umdsocialscheduler.com</b></font></div></div></body></html>";

					//<meta name='viewport' content='target-densityDpi=device-dpi, initial-scale = 1.2, minimum-scale=1.2'/>
					//<style type=\"text/css\">table {position: absolute; top: 25%;} html {height:120%;}</style>

					umdLoginDialog.dismiss();

					// Take source and send with intent to Schedule Activity
					Intent intent = new Intent(SignInActivity.this, ScheduleActivity.class);

					// Attach source code + data
					intent.putExtra("SOURCE_CODE", header + body + scheduleTable + footer);	
					intent.putExtra("SCHEDULE_DATA", scheduleData);	


					// Start activity
					startActivity(intent);
				}
			}    
		}
	}



	// This function takes the WebView screenshot and crops away a large portion of the white space,
	// returning the cropped Bitmap file.
	public Bitmap cropBitmap(Bitmap original) {
		int x = 0, y = 0, width = 0, height = 0;

		// Calculate lower margin
		int tempX = original.getWidth() / 2;
		int tempY = original.getHeight() - 1;

		while(original.getPixel(tempX, tempY) == Color.WHITE) {
			tempY--;
		}

		height = tempY + 10;

		// Calculate side margin
		tempX = 0;
		tempY = original.getHeight() / 5;

		while(original.getPixel(tempX, tempY) == Color.WHITE) {
			tempX++;
		}

		x = tempX - 10;
		width = original.getWidth() - 2 * tempX + 20;

		// Crop bitmap
		return Bitmap.createBitmap(original, x, y, width, height, null, false);
	}


	//
	//////////////////////////////////////////////////////////////////////////////////////


}
