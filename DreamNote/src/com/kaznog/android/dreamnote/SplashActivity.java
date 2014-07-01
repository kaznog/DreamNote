/**
 *
 */
package com.kaznog.android.dreamnote;

import com.kaznog.android.dreamnote.fragment.Notes;
import com.kaznog.android.dreamnote.settings.PreferencesUtil;
import com.kaznog.android.dreamnote.util.Constant;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.Window;
import android.widget.TextView;

/**
 * @author noguchi
 *
 */
public class SplashActivity extends Activity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        //タイトルバー非表示
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.splash);
        String strsplash = PreferencesUtil.getPreferences(getApplicationContext(), Constant.PREFS_SPLASH, "");
        if(strsplash.equals("")) {
        	TextView raindrop = (TextView)findViewById(R.id.splash_raindrop);
        	raindrop.setText(Html.fromHtml(
        			getResources().getString(R.string.splash_raindrop_thanks_msg_head) + "<BR>" +
        			getResources().getString(R.string.splash_raindrop_thanks_msg_middle) + "<BR>" +
        			getResources().getString(R.string.raindrop_deviantart_url) + "<BR>" +
        			getResources().getString(R.string.splash_raindrop_thanks_msg_foot)
        			)
        	);
        	raindrop.setMovementMethod(LinkMovementMethod.getInstance());
	        Handler hdl = new Handler();
	        hdl.postDelayed(new splashHandler(), 2000);
        } else {
    		Intent i = new Intent(getApplicationContext(), Notes.class);
    		startActivity(i);
    		finish();
        }
	}
	private class splashHandler implements Runnable {

		public void run() {
			Intent i = new Intent(getApplicationContext(), Notes.class);
			startActivity(i);
			SplashActivity.this.finish();
		}
	}
}
