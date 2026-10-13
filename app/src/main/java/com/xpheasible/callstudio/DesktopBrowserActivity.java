package com.xpheasible.callstudio;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.PictureInPictureParams;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Rational;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Experimental desktop-style browser. PiP floats the actual Android Activity
 * while the user switches to another app. Neither desktop UA nor PiP makes
 * a computer-only service function on Android, or creates a virtual camera.
 */
public class DesktopBrowserActivity extends Activity {
    private static final String URL = "https://delulustream.com/";
    private static final int PICK_FILE = 301;
    private static final int MEDIA_PERMISSION = 302;
    private static final String DESKTOP_UA =
        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/132.0.0.0 Safari/537.36";

    private WebView browser;
    private LinearLayout topbar;
    private TextView status;
    private boolean desktop = true;
    private String current = URL;
    private android.webkit.ValueCallback<Uri[]> uploadCallback;
    private PermissionRequest permissionRequest;

    private int dp(float n) { return (int)(n * getResources().getDisplayMetrics().density + 0.5f); }

    private Button button(String title, View.OnClickListener action) {
        Button b = new Button(this);
        b.setText(title);
        b.setAllCaps(false);
        b.setTextColor(Color.WHITE);
        b.setTextSize(12);
        b.setPadding(dp(5), 0, dp(5), 0);
        b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(29, 48, 86)));
        b.setOnClickListener(action);
        return b;
    }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.rgb(9,13,24));
        getWindow().setNavigationBarColor(Color.rgb(9,13,24));
        FrameLayout frame = new FrameLayout(this);
        frame.setBackgroundColor(Color.rgb(10,12,22));
        LinearLayout all = new LinearLayout(this);
        all.setOrientation(LinearLayout.VERTICAL);
        frame.addView(all, new FrameLayout.LayoutParams(-1,-1));

        topbar = new LinearLayout(this);
        topbar.setPadding(dp(3),dp(2),dp(3),dp(2));
        topbar.setOrientation(LinearLayout.HORIZONTAL);
        topbar.setGravity(Gravity.CENTER_VERTICAL);
        topbar.setBackgroundColor(Color.rgb(15,22,41));
        addControl("←", v -> finish(), 0.65f);
        addControl("Desktop ✓", v -> {
            desktop = !desktop;
            setDesktop();
            Toast.makeText(this,desktop?"Desktop browser view enabled":"Mobile browser view enabled",Toast.LENGTH_SHORT).show();
        }, 1.5f);
        addControl("⟳", v -> browser.reload(),0.65f);
        addControl("Chrome", v -> openChrome(),1.0f);
        addControl("▣ Float", v -> minimize(),1.05f);
        all.addView(topbar,new LinearLayout.LayoutParams(-1,dp(54)));

        status = new TextView(this);
        status.setTextColor(Color.rgb(201,213,233));
        status.setTextSize(11);
        status.setPadding(dp(12),dp(6),dp(12),dp(6));
        status.setText("Desktop website requested · Delulu may still require a real computer.");
        all.addView(status,new LinearLayout.LayoutParams(-1,-2));

        browser = new WebView(this);
        browser.setBackgroundColor(Color.BLACK);
        WebSettings settings = browser.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(true);
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(browser,true);
        browser.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, android.webkit.WebResourceRequest request) {
                if (!request.getUrl().getScheme().equalsIgnoreCase("https")) {
                    try { startActivity(new Intent(Intent.ACTION_VIEW,request.getUrl())); } catch(Exception ignored) {}
                    return true;
                }
                return false;
            }
            @Override public void onPageFinished(WebView view,String url) {
                current = url;
                status.setText("Desktop mode: "+ (desktop?"ON":"OFF") +" · "+ Uri.parse(url).getHost()
                    +" · Use ▣ Float to minimize over other apps");
            }
        });
        browser.setWebChromeClient(new WebChromeClient() {
            @Override public boolean onShowFileChooser(WebView view,
                    android.webkit.ValueCallback<Uri[]> callback, FileChooserParams params) {
                if(uploadCallback != null) uploadCallback.onReceiveValue(null);
                uploadCallback = callback;
                try {
                    startActivityForResult(params.createIntent(), PICK_FILE);
                    return true;
                } catch (ActivityNotFoundException e) {
                    uploadCallback = null;
                    callback.onReceiveValue(null);
                    Toast.makeText(DesktopBrowserActivity.this,"No document picker installed.",Toast.LENGTH_LONG).show();
                    return true;
                }
            }
            @Override public void onPermissionRequest(PermissionRequest request) {
                runOnUiThread(() -> offerWebsitePermission(request));
            }
        });
        all.addView(browser,new LinearLayout.LayoutParams(-1,0,1f));
        setContentView(frame);
        setDesktop();
        if(state == null) browser.loadUrl(URL);
        else browser.restoreState(state);
    }

    private void addControl(String name, View.OnClickListener action,float weight) {
        Button b = button(name, action);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0,dp(46),weight);
        p.leftMargin=dp(2);
        topbar.addView(b,p);
    }

    private void setDesktop(){
        if(browser == null) return;
        browser.getSettings().setUserAgentString(desktop ? DESKTOP_UA : null);
        browser.getSettings().setUseWideViewPort(desktop);
        browser.getSettings().setLoadWithOverviewMode(desktop);
        browser.reload();
    }

    private void minimize(){
        if(Build.VERSION.SDK_INT < 26 || getPackageManager().hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)==false){
            Toast.makeText(this,"Floating window is unavailable on this device.",Toast.LENGTH_LONG).show();
            return;
        }
        try {
            PictureInPictureParams params = new PictureInPictureParams.Builder()
                .setAspectRatio(new Rational(16,9)).build();
            enterPictureInPictureMode(params);
        } catch(Exception e){
            Toast.makeText(this,"Couldn't minimize. Use Android Recents to switch apps.",Toast.LENGTH_LONG).show();
        }
    }

    private void openChrome() {
        new AlertDialog.Builder(this)
            .setTitle("Open official Delulu website")
            .setMessage("Chrome is the recommended sign-in option. In Chrome, tap ⋮ → Desktop site. "
              +"A desktop-site setting may still be rejected by Delulu or require desktop hardware. "
              +"Minimize Chrome using Android Home/Recents; this does not keep a desktop virtual camera running.")
            .setPositiveButton("Open Chrome", (dialog, which)->{
                try {
                    Intent i=new Intent(Intent.ACTION_VIEW,Uri.parse(current));
                    i.setPackage("com.android.chrome");
                    startActivity(i);
                } catch(Exception e) {
                    try {startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(URL)));}
                    catch(Exception ignored){ Toast.makeText(this,"No browser found.",Toast.LENGTH_LONG).show(); }
                }
            }).setNegativeButton("Cancel",null).show();
    }

    private void offerWebsitePermission(PermissionRequest request) {
        String origin = request.getOrigin().toString();
        Uri uri = Uri.parse(origin);
        String host = uri.getHost();
        if(!"https".equalsIgnoreCase(uri.getScheme()) ||
           host == null || !(host.equals("delulustream.com") || host.endsWith(".delulustream.com"))) {
            request.deny();
            return;
        }
        boolean wantsCamera=Arrays.asList(request.getResources()).contains(PermissionRequest.RESOURCE_VIDEO_CAPTURE);
        boolean wantsMic=Arrays.asList(request.getResources()).contains(PermissionRequest.RESOURCE_AUDIO_CAPTURE);
        if(!wantsCamera && !wantsMic){request.deny();return;}
        permissionRequest = request;
        ArrayList<String> needed = new ArrayList<>();
        if(wantsCamera && checkSelfPermission(Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED)
            needed.add(Manifest.permission.CAMERA);
        if(wantsMic && checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED)
            needed.add(Manifest.permission.RECORD_AUDIO);
        if(!needed.isEmpty()){
            requestPermissions(needed.toArray(new String[0]),MEDIA_PERMISSION);
            return;
        }
        askMediaConsent();
    }

    private void askMediaConsent(){
        PermissionRequest req=permissionRequest;
        permissionRequest=null;
        if(req==null) return;
        ArrayList<String> allowed = new ArrayList<>();
        if(Arrays.asList(req.getResources()).contains(PermissionRequest.RESOURCE_VIDEO_CAPTURE)
            && checkSelfPermission(Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED)
            allowed.add(PermissionRequest.RESOURCE_VIDEO_CAPTURE);
        if(Arrays.asList(req.getResources()).contains(PermissionRequest.RESOURCE_AUDIO_CAPTURE)
            && checkSelfPermission(Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED)
            allowed.add(PermissionRequest.RESOURCE_AUDIO_CAPTURE);
        if(allowed.isEmpty()){req.deny();return;}
        new AlertDialog.Builder(this).setTitle("Allow website media access?")
            .setMessage("Allow "+req.getOrigin()+" to use the phone's camera/microphone? "
                +"Another video-call app may be unable to use the camera at the same time.")
            .setPositiveButton("Allow", (d,w)->req.grant(allowed.toArray(new String[0])))
            .setNegativeButton("Deny",(d,w)->req.deny())
            .setOnCancelListener(d->req.deny()).show();
    }

    @Override public void onRequestPermissionsResult(int requestCode,String[] permissions,int[] grants){
        super.onRequestPermissionsResult(requestCode,permissions,grants);
        if(requestCode==MEDIA_PERMISSION) askMediaConsent();
    }
    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){
        super.onActivityResult(requestCode,resultCode,data);
        if(requestCode==PICK_FILE && uploadCallback!=null){
            Uri[] picked = WebChromeClient.FileChooserParams.parseResult(resultCode,data);
            uploadCallback.onReceiveValue(picked);
            uploadCallback=null;
        }
    }
    @Override public void onPictureInPictureModeChanged(boolean pip, Configuration config) {
        super.onPictureInPictureModeChanged(pip,config);
        if(topbar!=null) topbar.setVisibility(pip?View.GONE:View.VISIBLE);
        if(status!=null) status.setVisibility(pip?View.GONE:View.VISIBLE);
    }
    @Override protected void onSaveInstanceState(Bundle out){
        browser.saveState(out);
        super.onSaveInstanceState(out);
    }
    @Override public void onBackPressed(){
        if(browser!=null && browser.canGoBack()) browser.goBack();
        else super.onBackPressed();
    }
    @Override protected void onDestroy(){
        if(uploadCallback!=null) {uploadCallback.onReceiveValue(null);uploadCallback=null;}
        if(permissionRequest!=null){permissionRequest.deny();permissionRequest=null;}
        if(browser!=null){
            browser.stopLoading();
            browser.destroy();
            browser=null;
        }
        super.onDestroy();
    }
}
