package com.kau.jonathan.umdschedulesharer;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Picture;
import android.graphics.Typeface;
import android.graphics.drawable.PictureDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewConfiguration;
import android.webkit.WebView;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.Session.StatusCallback;
import com.facebook.SessionLoginBehavior;
import com.facebook.SessionState;
import com.facebook.UiLifecycleHelper;
import com.facebook.widget.FacebookDialog;

public class ScheduleActivity extends ActionBarActivity {
	String fbPhotoAddress = null;
	static final String HTML_SOURCE = "scheduleSource";
	ViewPager mViewPager;
	PagerAdapter mPageAdapter;
	final String[] tabNames = {"Schedule","Classes","Friends"};
	Bitmap schedule;
	String schedule_src;
	private ProgressDialog progressDialog;

	// FB upload graph object
	private static final String TAG = "ScheduleActivity";
	static final boolean UPLOAD_IMAGE = true;
	private static final List<String> PERMISSIONS = Arrays.asList("publish_actions");
	private static final String PENDING_PUBLISH_KEY = "pendingPublishReauthorization";
	private boolean pendingPublishReauthorization = false;

	private UiLifecycleHelper uiHelper;
	private Session.StatusCallback callback = new Session.StatusCallback() {
		@Override
		public void call(Session session, SessionState state, Exception exception) {
			onSessionStateChange(session, state, exception);
		}
	};

	private void onSessionStateChange(Session session, SessionState state, Exception exception) {
		if (state.isOpened()) {
			if (pendingPublishReauthorization && 
					state.equals(SessionState.OPENED_TOKEN_UPDATED)) {
				pendingPublishReauthorization = false;
			}
		} else if (state.isClosed()) {
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		uiHelper.onActivityResult(requestCode, resultCode, data, new FacebookDialog.Callback() {
			@Override
			public void onError(FacebookDialog.PendingCall pendingCall, Exception error, Bundle data) {
				Log.e("Activity", String.format("Error: %s", error.toString()));
			}

			@Override
			public void onComplete(FacebookDialog.PendingCall pendingCall, Bundle data) {
				Log.i("Activity", "Success!");
			}
		});
	}

	public void shareToFacebook(View v) {
		Session session = Session.getActiveSession();

		if (session == null) {
		    session = new Session.Builder(this).setApplicationId("580519828708344").build();

		    Session.setActiveSession(session);
		    session.addCallback(new StatusCallback() {
		        public void call(Session session, SessionState state, Exception exception) {
		            if (state == SessionState.OPENED) {
		                Session.OpenRequest openRequest = new Session.OpenRequest(ScheduleActivity.this);
		                openRequest.setLoginBehavior(SessionLoginBehavior.SSO_WITH_FALLBACK);
		                session.openForRead(openRequest);
		                session.requestNewPublishPermissions(
		                        new Session.NewPermissionsRequest(ScheduleActivity.this, PERMISSIONS));
		            }
		            else if (state == SessionState.OPENED_TOKEN_UPDATED) {
		            }
		            else if (state == SessionState.CLOSED_LOGIN_FAILED) {
		                session.closeAndClearTokenInformation();
		                // Possibly finish the activity
		            }
		            else if (state == SessionState.CLOSED) {
		                session.close();
		                // Possibly finish the activity
		            }
		        }});
		    
		    
		} else {
			List<String> permissions = session.getPermissions();
			if (!isSubsetOf(PERMISSIONS, permissions)) {
				pendingPublishReauthorization = true;
				Session.NewPermissionsRequest newPermissionsRequest = new Session 
						.NewPermissionsRequest(ScheduleActivity.this, PERMISSIONS);
				session.requestNewPublishPermissions(newPermissionsRequest);
			} else {
				publishFacebook();
			}
		}

		if (!session.isOpened()) {
		    Session.OpenRequest openRequest = new Session.OpenRequest(this);
		    openRequest.setLoginBehavior(SessionLoginBehavior.SSO_WITH_FALLBACK);
		    session.openForRead(openRequest);
		}
			
		

	}
	
	public void publishFacebook() {
		WebView content = (WebView) findViewById(R.id.schedule_browser);
        content.setDrawingCacheEnabled(true);
		Picture screenshot = content.capturePicture();
		PictureDrawable pictureDrawable = new PictureDrawable(screenshot);
		Bitmap bitmap = Bitmap.createBitmap(pictureDrawable.getIntrinsicWidth(),pictureDrawable.getIntrinsicHeight(), Config.ARGB_8888);
		Canvas canvas = new Canvas(bitmap);
		canvas.drawPicture(pictureDrawable.getPicture());

		// Crop bitmap by calling function		
		final Bitmap cropped = cropBitmap(bitmap);
		//final Bitmap cropped = bitmap;


		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(ScheduleActivity.this);


		alertDialogBuilder
		.setTitle("Share to Facebook")
		.setMessage("Would you like to post this schedule to your timeline?")
		.setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {				
			
				Session session = Session.getActiveSession();

				// Part 1: create callback to get URL of uploaded photo
				Request.Callback uploadPhotoRequestCallback = new Request.Callback() {
					@Override
					public void onCompleted(Response response) {
						// safety check
						if (isFinishing()) {
							return;
						}
						if (response.getError() != null) {  // [IF Failed Posting]
							Log.d("POTATO", "photo upload problem. Error="+response.getError() );
						}  //  [ENDIF Failed Posting]

						try {
							//String imageUrl = response.getGraphObject().getInnerJSONObject().getString("uri");
							//Toast.makeText(ScheduleActivity.this, imageUrl, Toast.LENGTH_SHORT).show();
						} catch (NullPointerException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}

					}  // [END onCompleted]
				}; 

				//Part 2: upload the photo
				Request request = Request.newUploadPhotoRequest(session, cropped, uploadPhotoRequestCallback);
				
				byte[] data = null;
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				cropped.compress(Bitmap.CompressFormat.JPEG, 100, baos);
				data = baos.toByteArray();

				Bundle postParams = request.getParameters();
				postParams.putByteArray("picture", data);
				postParams.putString("message", "Shared with UMD Social Scheduler. Download at www.umdsocialscheduler.com");

				request.setParameters(postParams);

				request.executeAsync();
				
				Toast.makeText(ScheduleActivity.this, "Schedule posted to timeline!", Toast.LENGTH_SHORT).show();
				
				
			}
		})
		.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				dialog.cancel();
			}
		});

		AlertDialog alertDialog = alertDialogBuilder.create();
		alertDialog.show();


//		// Save bitmap to sdcard			        
//		String root = Environment.getExternalStorageDirectory().toString();
//		File myDir = new File(root + "/saved_images");    
//		myDir.mkdirs();
//		Random generator = new Random();
//		int n = 10000;
//		n = generator.nextInt(n);
//		String fname = "Image-"+ n +".jpg";
//		File file = new File (myDir, fname);
//		if (file.exists ()) file.delete (); 
//		try {
//			FileOutputStream out = new FileOutputStream(file);
//			cropped.compress(Bitmap.CompressFormat.JPEG, 90, out);
//			out.flush();
//			out.close();
//
//		} catch (Exception e) {
//			e.printStackTrace();
//		}

		// Share bitmap			        
		//publishStory(cropped);

		//		FacebookDialog shareDialog = new FacebookDialog.ShareDialogBuilder(this)
		//		.setApplicationName("UMD Social Scheduler - Android")
		//		.setDescription("Shared with UMD Social Scheduler")
		//		.setPicture(imageUrl)
		//		.build();
		//uiHelper.trackPendingDialogCall(shareDialog.present());
	}

	// Begin Activity code

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_schedule);

		if (savedInstanceState != null) {
			schedule_src = savedInstanceState.getString(HTML_SOURCE);

			pendingPublishReauthorization = 
					savedInstanceState.getBoolean(PENDING_PUBLISH_KEY, false);
		} else {
			// Process incoming intent data
			schedule_src = getIntent().getStringExtra("SOURCE_CODE");

			// Save data for later use
			SharedPreferences prefs = this.getSharedPreferences("com.kau.jonathan.umdschedulesharer", Context.MODE_PRIVATE);
			prefs.edit().putInt("com.kau.jonathan.umdschedulesharer.obtained_schedule", 1).commit();
			prefs.edit().putString("com.kau.jonathan.umdschedulesharer.schedule_code", schedule_src).commit();
		}
		
		// Setup Facebook lifecycle handler
		uiHelper = new UiLifecycleHelper(this, callback);
		uiHelper.onCreate(savedInstanceState);

		// Access layout elements
		mViewPager = (ViewPager) findViewById(R.id.pager);

		mViewPager.setOffscreenPageLimit(2);

		mViewPager.setOnPageChangeListener(
				new ViewPager.SimpleOnPageChangeListener() {
					@Override
					public void onPageSelected(int position) {
						// When swiping between pages, select the
						// corresponding tab.
						getSupportActionBar().setSelectedNavigationItem(position);
					}
				});

		mPageAdapter = new PagerAdapter(getSupportFragmentManager());
		mViewPager.setAdapter(mPageAdapter);

		// Make magic
		try {
			ViewConfiguration config = ViewConfiguration.get(this);
			Field menuKeyField = ViewConfiguration.class.getDeclaredField("sHasPermanentMenuKey");
			if(menuKeyField != null) {
				menuKeyField.setAccessible(true);
				menuKeyField.setBoolean(config, false);
			}
		} catch (Exception ex) {
			// Ignore
		}

		// Instantiate taps
		final ActionBar actionBar = getSupportActionBar();

		actionBar.setHomeButtonEnabled(false);


		actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_SHOW_CUSTOM);
		View homeIcon = findViewById(android.R.id.home);
		if(homeIcon != null) {
			((View) homeIcon.getParent()).setClickable(false);
		}

		actionBar.setCustomView(R.layout.actionbar);

		// Specify that tabs should be displayed in the action bar.
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

		// Create a tab listener that is called when the user changes tabs.
		ActionBar.TabListener tabListener = new ActionBar.TabListener() {
			public void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft) {
				mViewPager.setCurrentItem(tab.getPosition());
			}

			public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction ft) {
				// hide the given tab
			}

			public void onTabReselected(ActionBar.Tab tab, FragmentTransaction ft) {
				// probably ignore this event
			}
		};

		// Set typeface of text elements in page
		final Typeface face=Typeface.createFromAsset(this.getAssets(),
				"fonts/Lato-Reg.ttf");
		final Typeface lightface=Typeface.createFromAsset(this.getAssets(),
				"fonts/Lato-Lig.ttf");
		// Set typefaces

		TextView title = (TextView)findViewById(R.id.main_title);
		title.setTypeface(face);

		// Add 3 tabs, specifying the tab's text and TabListener


		for(int i = 0; i< 3; i++) {
			LayoutInflater inflater = LayoutInflater.from(this);
			View customView = inflater.inflate(R.layout.tab_title, null);

			SpannableString formattedString = new SpannableString(tabNames[i]);
			formattedString.setSpan(face, 0, tabNames[i].length(), 0);	
			formattedString.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.main_red)), 0, tabNames[i].length(), 0);

			TextView tab_title = (TextView) customView.findViewById(R.id.action_custom_title);
			tab_title.setText(tabNames[i]);
			tab_title.setTypeface(lightface);

			// This OnClickListener resolves the problem in which the custom TextView tab
			// does not allow the user to click on the actual tab title.
			tab_title.setOnClickListener(new OnClickListener(){

				@Override
				public void onClick(View v) {
					TextView tv = (TextView) v;
					String s = tv.getText().toString();
					int i;

					for(i = 0; i < 3; i++) {
						if(s.equals(tabNames[i])){
							break;
						}
					}

					mViewPager.setCurrentItem(i);

				}});

			actionBar.addTab(
					actionBar.newTab()
					.setCustomView(customView)
					.setTabListener(tabListener));
		}
		
		
//		// Set up facebook permissions
//		
//		Session session = Session.getActiveSession();
//		if (session != null) {
//			// Check for publish permissions    
//			List<String> permissions = session.getPermissions();
//			if (!isSubsetOf(PERMISSIONS, permissions)) {
//				pendingPublishReauthorization = true;
//				Session.NewPermissionsRequest newPermissionsRequest = new Session 
//						.NewPermissionsRequest(ScheduleActivity.this, PERMISSIONS);
//				session.requestNewPublishPermissions(newPermissionsRequest);
//				Toast.makeText(ScheduleActivity.this, "Permissions requested", Toast.LENGTH_SHORT).show();
//			} else {
//				Toast.makeText(ScheduleActivity.this, "Permissions already obtained", Toast.LENGTH_SHORT).show();
//			}
//		}
		
	}

	// This function takes the WebView screenshot and crops away a large portion of the white space,
	// returning the cropped Bitmap file.
	public Bitmap cropBitmap(Bitmap original) {
		int x = 0, y = 0, width = 0, height = 0;

		// Calculate lower margin
		int tempX = original.getWidth() / 2;
		int tempY = original.getHeight() - 1;

		while(tempY > 0 && original.getPixel(tempX, tempY) == Color.WHITE) {
			tempY--;
		}

		int lowerBound = tempY;

		// Calculate upper margin
		tempX = original.getWidth() / 2;
		tempY = 0;

		while(tempY < original.getHeight() && original.getPixel(tempX, tempY) == Color.WHITE) {
			tempY++;
		}

		int upperBound = tempY;
		height = lowerBound - upperBound;

		// Calculate side margin
		tempX = 0;
		tempY = original.getHeight() / 2;

		while(tempX < original.getWidth() && original.getPixel(tempX, tempY) == Color.WHITE) {
			tempX++;
		}

		x = tempX - 10;
		width = original.getWidth() - 2 * tempX + 20;

		// Crop bitmap
		return Bitmap.createBitmap(original, x, upperBound - 10, width, height + 20, null, false);
	}


	// Since this isn't an object collection, use a FragmentPagerAdapter
	public class PagerAdapter extends FragmentPagerAdapter {
		public PagerAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int i) {
			Fragment fragment;

			if(i == 0) {
				fragment = new ScheduleFragment();
			} else {
				fragment = new DummySectionFragment();
			}
			return fragment;
		}

		@Override
		public int getCount() {
			return 3;
		}

		@Override
		public CharSequence getPageTitle(int position) {
			return "OBJECT " + (position + 1);
		}
	}


	/**
	 * Set up the {@link android.app.ActionBar}, if the API is available.
	 */
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void setupActionBar() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			getActionBar().setDisplayHomeAsUpEnabled(false);
		}
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Bring up submenu items
		SubMenu subMenu = menu.addSubMenu("Navigation Menu");
		subMenu.add(0, 1, Menu.NONE, "Update Schedule").setIcon(R.drawable.ic_action_refresh);
		subMenu.add(0, 2, Menu.NONE, "Settings").setIcon(R.drawable.ic_action_settings);

		MenuItem overflow = subMenu.getItem();
		overflow.setIcon(R.drawable.ic_action_overflow);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			overflow.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		}

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.schedule, menu);
		return true;
	}

	@Override
	public void onBackPressed() {
		//		Intent back = new Intent(this, MainActivity.class);
		//
		//		// Save data for later use
		//		SharedPreferences prefs = this.getSharedPreferences("com.kau.jonathan.umdschedulesharer", Context.MODE_PRIVATE);
		//		prefs.edit().putInt("com.kau.jonathan.umdschedulesharer.obtained_schedule", 0).commit();
		//		prefs.edit().putString("com.kau.jonathan.umdschedulesharer.schedule_code", schedule_src).commit();
		//
		//		startActivity(back);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			// This ID represents the Home or Up button. In the case of this
			// activity, the Up button is shown. Use NavUtils to allow users
			// to navigate up one level in the application structure. For
			// more details, see the Navigation pattern on Android Design:
			//
			// http://developer.android.com/design/patterns/navigation.html#up-vs-back
			//
			return true;
		case 1: // Update schedule
			Intent back = new Intent(this, MainActivity.class);

			// Save data for later use
			SharedPreferences prefs = this.getSharedPreferences("com.kau.jonathan.umdschedulesharer", Context.MODE_PRIVATE);
			prefs.edit().putInt("com.kau.jonathan.umdschedulesharer.obtained_schedule", 0).commit();
			prefs.edit().putString("com.kau.jonathan.umdschedulesharer.schedule_code", schedule_src).commit();

			startActivity(back);
			return true;
		case 2: // Settings
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
		// Save the user's current game state
		savedInstanceState.putString(HTML_SOURCE, schedule_src);
		savedInstanceState.putBoolean(PENDING_PUBLISH_KEY, pendingPublishReauthorization);

		// Always call the superclass so it can save the view hierarchy state
		super.onSaveInstanceState(savedInstanceState);
		uiHelper.onSaveInstanceState(savedInstanceState);
	}

	@Override
	protected void onResume() {
		super.onResume();

		Session session = Session.getActiveSession();
		if (session != null &&
				(session.isOpened() || session.isClosed()) ) {
			onSessionStateChange(session, session.getState(), null);
		}

		uiHelper.onResume();
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

	/*
	 * Helper method to check a collection for a string.
	 */
	private boolean isSubsetOf(Collection<String> subset, Collection<String> superset) {
		for (String string : subset) {
			if (!superset.contains(string)) {
				return false;
			}
		}
		return true;
	}

}
