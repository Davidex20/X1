package com.xpheasible.callstudio;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.VideoView;
import java.io.IOException;
import java.util.Arrays;

@SuppressWarnings("deprecation")
public class StageActivity extends Activity {
    private static final int WEB_CAM_REQUEST = 110;
    private FrameLayout frame;
    private LinearLayout toolbar;
    private SharedPreferences prefs;
    private ImageView photo;
    private VideoView video;
    private WebView web;
    private MediaPlayer player;
    private TextureView cameraTexture;
    private Camera camera;
    private PermissionRequest pendingWebRequest;
    private String mode;
    private boolean fit = false, audio = false, front = true;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable hide = () -> { if(toolbar != null) toolbar.setVisibility(View.GONE); };

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        prefs=getSharedPreferences("studio",MODE_PRIVATE);
        mode=prefs.getString("source","none");
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setStatusBarColor(Color.BLACK);
        getWindow().setNavigationBarColor(Color.BLACK);
        frame=new FrameLayout(this);
        frame.setBackgroundColor(Color.BLACK);
        frame.setOnClickListener(v->showControls());
        setContentView(frame);
        switch (mode) {
            case "photo": showPhoto(); break;
            case "video": showVideo(); break;
            case "camera": showCamera(); break;
            case "web": showWeb(); break;
            default: showMessage("Choose a source in Call Studio first."); break;
        }
        addToolbar();
        immersive();
    }
    private int dp(float n) {return (int)(n*getResources().getDisplayMetrics().density+.5f);}
    private void immersive() {
        getWindow().getDecorView().setSystemUiVisibility(
           View.SYSTEM_UI_FLAG_FULLSCREEN |
           View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
           View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
           View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
           View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
           View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
    }
    private void showMessage(String message) {
        TextView t=new TextView(this);t.setText(message);t.setTextSize(16);t.setTextColor(Color.WHITE);
        t.setGravity(Gravity.CENTER); t.setPadding(dp(24),dp(24),dp(24),dp(24));
        frame.addView(t,new FrameLayout.LayoutParams(-1,-1));
    }
    private void showPhoto() {
        photo=new ImageView(this);
        photo.setBackgroundColor(Color.BLACK);
        photo.setScaleType(ImageView.ScaleType.CENTER_CROP);
        try {
            String u=prefs.getString("uri","");
            if (u.isEmpty()) throw new IllegalArgumentException();
            photo.setImageURI(Uri.parse(u));
            frame.addView(photo,new FrameLayout.LayoutParams(-1,-1));
        } catch(Exception e) {showMessage("Could not open the image. Choose it again in Call Studio.");}
    }
    private void showVideo() {
        video=new VideoView(this);
        try {
            Uri uri=Uri.parse(prefs.getString("uri",""));
            video.setVideoURI(uri);
            video.setOnPreparedListener(mp -> {
                player=mp; player.setLooping(true); player.setVolume(0f,0f);
                video.start();
            });
            video.setOnErrorListener((mp,what,extra)->{
                showMessage("This video's format couldn't be played. Try MP4 (H.264).");
                return true;
            });
            frame.addView(video,new FrameLayout.LayoutParams(-1,-1,Gravity.CENTER));
            video.requestFocus();video.start();
        } catch(Exception e) {showMessage("Could not open the video. Choose it again in Call Studio.");}
    }
    private void showCamera() {
        if(checkSelfPermission(Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED) {
            showMessage("Grant camera permission from the main screen, then retry."); return;
        }
        cameraTexture=new TextureView(this);
        cameraTexture.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            public void onSurfaceTextureAvailable(SurfaceTexture surface,int width,int height) {startCamera(surface);}
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface,int width,int height) {}
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {stopCamera();return true;}
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {}
        });
        frame.addView(cameraTexture,new FrameLayout.LayoutParams(-1,-1));
    }
    private void startCamera(SurfaceTexture texture) {
        stopCamera();
        if(checkSelfPermission(Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED) return;
        try {
            int choice=-1;
            Camera.CameraInfo info=new Camera.CameraInfo();
            for(int i=0;i<Camera.getNumberOfCameras();i++){
                Camera.getCameraInfo(i,info);
                if(info.facing==(front ? Camera.CameraInfo.CAMERA_FACING_FRONT:Camera.CameraInfo.CAMERA_FACING_BACK)) {choice=i;break;}
            }
            if(choice<0) throw new IllegalStateException("No suitable camera");
            camera=Camera.open(choice);
            Camera.getCameraInfo(choice,info);
            int rotation=getWindowManager().getDefaultDisplay().getRotation();
            int degrees=(rotation==Surface.ROTATION_90?90:rotation==Surface.ROTATION_180?180:rotation==Surface.ROTATION_270?270:0);
            int result;
            if(info.facing==Camera.CameraInfo.CAMERA_FACING_FRONT) {
                result=(info.orientation+degrees)%360;result=(360-result)%360;
            } else { result=(info.orientation-degrees+360)%360; }
            camera.setDisplayOrientation(result);
            camera.setPreviewTexture(texture);
            camera.startPreview();
        } catch(Exception e) {
            stopCamera();
            showMessage("Camera is unavailable. Another app may be using it. Switch to an image or video source for screen sharing.");
        }
    }
    private void stopCamera() {
        if(camera!=null) {try {camera.stopPreview();} catch(Exception ignored){}
            try {camera.release();} catch(Exception ignored){}camera=null;}
    }
    private void showWeb() {
        String url=prefs.getString("url","");
        if(!url.startsWith("https://")) {showMessage("Only HTTPS links can be displayed."); return;}
        web=new WebView(this);
        web.setBackgroundColor(Color.BLACK);
        web.getSettings().setJavaScriptEnabled(true);
        web.getSettings().setDomStorageEnabled(true);
        web.getSettings().setMediaPlaybackRequiresUserGesture(false);
        web.setWebViewClient(new WebViewClient());
        web.setWebChromeClient(new WebChromeClient() {
            @Override public void onPermissionRequest(PermissionRequest request) {
                runOnUiThread(() -> allowWebCamera(request));
            }
        });
        frame.addView(web,new FrameLayout.LayoutParams(-1,-1));
        web.loadUrl(url);
    }
    private void allowWebCamera(PermissionRequest request) {
        if(!request.getOrigin().toString().startsWith("https://") ||
           !Arrays.asList(request.getResources()).contains(PermissionRequest.RESOURCE_VIDEO_CAPTURE)) {
            request.deny(); return;
        }
        if(checkSelfPermission(Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED) {
            pendingWebRequest=request;
            requestPermissions(new String[]{Manifest.permission.CAMERA},WEB_CAM_REQUEST);
            return;
        }
        new AlertDialog.Builder(this).setTitle("Website camera access")
            .setMessage(request.getOrigin()+" requests your camera. Allow only if you trust this site.")
            .setPositiveButton("Allow camera",(d,w)->request.grant(new String[]{PermissionRequest.RESOURCE_VIDEO_CAPTURE}))
            .setNegativeButton("Deny",(d,w)->request.deny())
            .setOnCancelListener(d->request.deny()).show();
    }
    @Override public void onRequestPermissionsResult(int requestCode,String[] permissions,int[] grantResults) {
        super.onRequestPermissionsResult(requestCode,permissions,grantResults);
        if(requestCode==WEB_CAM_REQUEST && pendingWebRequest!=null) {
            PermissionRequest request=pendingWebRequest; pendingWebRequest=null;
            if(grantResults.length>0 && grantResults[0]==PackageManager.PERMISSION_GRANTED) allowWebCamera(request);
            else request.deny();
        }
    }
    private Button control(String name,View.OnClickListener onClick) {
        Button b=new Button(this);b.setText(name);b.setAllCaps(false);
        b.setTextColor(Color.WHITE);b.setTextSize(13);
        b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(25,38,64)));
        b.setOnClickListener(onClick);return b;
    }
    private void addToolbar() {
        toolbar=new LinearLayout(this);
        toolbar.setOrientation(LinearLayout.HORIZONTAL);
        toolbar.setPadding(dp(6),dp(7),dp(6),dp(7));
        toolbar.setGravity(Gravity.CENTER_VERTICAL);
        toolbar.setBackgroundColor(Color.argb(214,9,13,24));
        Button back=control("← Back",v->finish());
        toolbar.addView(back,new LinearLayout.LayoutParams(0,dp(45),1f));
        if("photo".equals(mode)) {
            Button scale=control("Fit / Fill",v->{
                fit=!fit;
                photo.setScaleType(fit?ImageView.ScaleType.FIT_CENTER:ImageView.ScaleType.CENTER_CROP);
                showControls();
            });
            toolbar.addView(scale,new LinearLayout.LayoutParams(0,dp(45),1f));
        }
        if("camera".equals(mode)) {
            Button flip=control("Flip",v->{front=!front;if(cameraTexture!=null && cameraTexture.isAvailable()) startCamera(cameraTexture.getSurfaceTexture());showControls();});
            toolbar.addView(flip,new LinearLayout.LayoutParams(0,dp(45),1f));
        }
        if("video".equals(mode)) {
            Button sound=control("Audio off",v->{
                audio=!audio;
                if(player!=null)player.setVolume(audio?1f:0f,audio?1f:0f);
                ((Button)v).setText(audio?"Audio on":"Audio off");
                showControls();
            });
            toolbar.addView(sound,new LinearLayout.LayoutParams(0,dp(45),1f));
        }
        Button hideButton=control("Hide",v->toolbar.setVisibility(View.GONE));
        toolbar.addView(hideButton,new LinearLayout.LayoutParams(0,dp(45),1f));
        FrameLayout.LayoutParams p=new FrameLayout.LayoutParams(-1,-2,Gravity.TOP);
        frame.addView(toolbar,p);
        hideLater();
    }
    private void hideLater(){handler.removeCallbacks(hide);handler.postDelayed(hide,3500);}
    private void showControls(){toolbar.setVisibility(View.VISIBLE);hideLater();}
    @Override protected void onResume(){super.onResume();
        immersive();
        if(cameraTexture!=null && cameraTexture.isAvailable() && camera==null) startCamera(cameraTexture.getSurfaceTexture());
        if(video!=null && !video.isPlaying())video.start();
        if(web!=null)web.onResume();
    }
    @Override protected void onPause(){super.onPause();stopCamera();
        if(video!=null && video.isPlaying()) video.pause();
        if(web!=null)web.onPause();
    }
    @Override protected void onDestroy(){super.onDestroy();
        handler.removeCallbacks(hide);stopCamera();
        if(video!=null)video.stopPlayback();
        if(web!=null){web.destroy();web=null;}
    }
}
