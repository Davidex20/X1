package com.xpheasible.callstudio;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;
import java.io.FileOutputStream;

public class MainActivity extends Activity {
    private static final int PICK_IMAGE = 101, PICK_VIDEO = 102, TAKE_PICTURE = 103, REQ_CAMERA = 104;
    private SharedPreferences prefs;
    private TextView currentSource;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        prefs = getSharedPreferences("studio", MODE_PRIVATE);
        getWindow().setStatusBarColor(Color.rgb(9, 13, 24));
        getWindow().setNavigationBarColor(Color.rgb(9, 13, 24));
        drawHome();
    }
    private int dp(float px) { return (int) (px * getResources().getDisplayMetrics().density + .5f); }
    private GradientDrawable bg(int color, int radius) {
        GradientDrawable b = new GradientDrawable(); b.setColor(color); b.setCornerRadius(dp(radius)); return b;
    }
    private TextView label(String text, int sp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(text); t.setTextColor(color); t.setTextSize(sp);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }
    private void add(LinearLayout column, View view, int height, int bottom) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height < 0 ? height : dp(height));
        p.bottomMargin = dp(bottom); column.addView(view, p);
    }
    private Button action(String text, boolean primary, View.OnClickListener click) {
        Button b = new Button(this);
        b.setAllCaps(false); b.setText(text); b.setTextSize(15);
        b.setTextColor(Color.WHITE);
        b.setBackground(bg(primary ? Color.rgb(37, 99, 235) : Color.rgb(31, 41, 59), 13));
        b.setOnClickListener(click);
        return b;
    }
    private void drawHome() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true); scroll.setBackgroundColor(Color.rgb(9, 13, 24));
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL); body.setPadding(dp(22), dp(28), dp(22), dp(32));
        scroll.addView(body);
        TextView brand = label("XPHEASIBLE  /  CALL STUDIO", 13, Color.rgb(96, 165, 250), true); add(body,brand, -2, 15);
        TextView title = label("Your mobile\nvideo stage.", 32, Color.WHITE, true);
        add(body,title,-2,13);
        TextView subtitle = label("Show a photo, video, live camera preview or compatible web page in Android video calls — using screen sharing, no PC required.",15,Color.rgb(176, 190, 209),false);
        add(body,subtitle,-2,21);
        currentSource = label("",16,Color.WHITE,true);
        currentSource.setPadding(dp(15),dp(14),dp(15),dp(14));
        currentSource.setBackground(bg(Color.rgb(19, 28, 48), 14));
        add(body,currentSource,-2,17); refreshSource();
        add(body,label("CHOOSE YOUR SOURCE",12,Color.rgb(148,163,184),true),-2,10);
        add(body,action("🖼  Select a reference image",false,v -> pick("image/*",PICK_IMAGE)),-2,8);
        add(body,action("📸  Take a picture",false,v -> capturePhoto()),-2,8);
        add(body,action("▶  Select a video",false,v -> pick("video/*",PICK_VIDEO)),-2,8);
        add(body,action("◎  Live camera preview",false,v -> setCamera()),-2,8);
        add(body,action("🌐  Web / AI page (experimental)",false,v -> setWeb()),-2,8);
        add(body,action("🖥  DELULU DESKTOP VIEW  ·  FLOAT",true,v -> startActivity(new Intent(this,DesktopBrowserActivity.class))),dp(59),16);
        add(body,action("▶  OPEN FULLSCREEN STAGE",true,v -> openStage()),dp(59),16);
        add(body,label("USE WITH YOUR VIDEO CALL",12,Color.rgb(148,163,184),true),-2,9);
        add(body,action("Open WhatsApp",false,v -> openApp("com.whatsapp")),-2,8);
        add(body,action("Open Google Meet",false,v -> openMeet()),-2,17);
        TextView guide = label(
          "HOW TO PRESENT\n\n1. Select a source above.\n2. Start a WhatsApp or Meet video call.\n3. In the call, tap Share screen.\n4. Return here using Android Recents and tap OPEN FULLSCREEN STAGE.\n5. Everyone sees your shared stage.\n\nIMPORTANT: This is screen sharing, NOT a replacement camera that WhatsApp or Meet will list as a camera. Camera preview can be blocked if the calling app is using the physical camera.", 14, Color.rgb(195,206,224), false);
        guide.setPadding(dp(16),dp(18),dp(16),dp(18));
        guide.setBackground(bg(Color.rgb(19, 28, 48), 14));
        add(body,guide,-2,6);
        add(body,label("Delulu desktop view is experimental. Float minimizes the page using Android Picture-in-Picture, but it cannot turn Android into a desktop or virtual camera. For reliable sign-in, use Open in Chrome and enable Desktop site there.\n\nTip: Hide personal notifications when screen sharing.",12,Color.rgb(151,164,183),false),-2,8);
        setContentView(scroll);
    }
    private void refreshSource() {
        String kind = prefs.getString("source","none");
        String result = "Selected: None";
        switch(kind) {
            case "photo": result="Selected: Reference image"; break;
            case "video": result="Selected: Local video"; break;
            case "camera": result="Selected: Live camera preview"; break;
            case "web": result="Selected: Web page\n" + prefs.getString("url",""); break;
        }
        if (currentSource != null) currentSource.setText(result);
    }
    private void pick(String mime,int requestCode) {
        Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType(mime);
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(i, requestCode);
    }
    private void capturePhoto() {
        Intent i=new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        try { startActivityForResult(i,TAKE_PICTURE); }
        catch(Exception ex) {Toast.makeText(this,"No camera app is available.",Toast.LENGTH_LONG).show();}
    }
    private void setCamera() {
        prefs.edit().putString("source","camera").apply(); refreshSource();
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, REQ_CAMERA);
        }
    }
    private void setWeb() {
        EditText input=new EditText(this);
        input.setSingleLine(true);
        input.setText(prefs.getString("url","https://delulustream.com/cam"));
        input.setSelectAllOnFocus(true);
        new AlertDialog.Builder(this).setTitle("Web / AI stage")
            .setMessage("Enter an HTTPS site or compatible streaming page. DeluluStream's desktop virtual camera is NOT an Android camera. Mobile website compatibility is not guaranteed.")
            .setView(input)
            .setPositiveButton("Use page",(d,w)->{
                String u=input.getText().toString().trim();
                if (!u.contains("://")) u="https://"+u;
                Uri parsed=Uri.parse(u);
                if (!"https".equalsIgnoreCase(parsed.getScheme()) || parsed.getHost()==null) {
                    Toast.makeText(this,"Only HTTPS links are allowed.",Toast.LENGTH_LONG).show();return;
                }
                prefs.edit().putString("source","web").putString("url",u).apply();
                refreshSource();
            }).setNegativeButton("Cancel",null).show();
    }
    private void openStage() {
        String mode=prefs.getString("source","none");
        if ("none".equals(mode)) {
            Toast.makeText(this,"Select a source first.",Toast.LENGTH_SHORT).show(); return;
        }
        if ("camera".equals(mode) && checkSelfPermission(Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA},REQ_CAMERA); return;
        }
        startActivity(new Intent(this,StageActivity.class));
    }
    private void openApp(String packageName) {
        try {
            Intent i=getPackageManager().getLaunchIntentForPackage(packageName);
            if (i==null) throw new IllegalStateException("Not installed");
            startActivity(i);
        } catch (Exception e) { Toast.makeText(this,"Install the video calling app or open it yourself.",Toast.LENGTH_LONG).show(); }
    }
    private void openMeet() {
        Intent i=getPackageManager().getLaunchIntentForPackage("com.google.android.apps.tachyon");
        if(i==null) i=getPackageManager().getLaunchIntentForPackage("com.google.android.apps.meetings");
        if(i!=null) startActivity(i);
        else Toast.makeText(this,"Open or install Google Meet, then share your screen during a call.",Toast.LENGTH_LONG).show();
    }
    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data) {
        super.onActivityResult(requestCode,resultCode,data);
        if(resultCode!=RESULT_OK || data==null) return;
        if(requestCode==PICK_IMAGE || requestCode==PICK_VIDEO) {
            Uri uri=data.getData(); if(uri==null)return;
            try { getContentResolver().takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION); }
            catch(Exception ignored) {}
            prefs.edit().putString("source", requestCode==PICK_IMAGE?"photo":"video").putString("uri",uri.toString()).apply();
            refreshSource();
        } else if(requestCode==TAKE_PICTURE) {
            Object obj = data.getExtras()==null ? null : data.getExtras().get("data");
            if(!(obj instanceof Bitmap)) {
                Toast.makeText(this,"Camera didn't return a photo.",Toast.LENGTH_LONG).show();return;
            }
            try {
                File f=new File(getFilesDir(),"reference_photo.jpg");
                try(FileOutputStream os=new FileOutputStream(f)) { ((Bitmap)obj).compress(Bitmap.CompressFormat.JPEG,95,os); }
                prefs.edit().putString("source","photo").putString("uri",Uri.fromFile(f).toString()).apply();
                refreshSource();
            } catch(Exception e) {Toast.makeText(this,"Could not save photo.",Toast.LENGTH_LONG).show();}
        }
    }
    @Override public void onRequestPermissionsResult(int req,String[] permissions,int[] grants) {
        super.onRequestPermissionsResult(req,permissions,grants);
        if(req==REQ_CAMERA && (grants.length==0 || grants[0]!=PackageManager.PERMISSION_GRANTED))
            Toast.makeText(this,"Camera permission is required for live preview.",Toast.LENGTH_LONG).show();
    }
}
