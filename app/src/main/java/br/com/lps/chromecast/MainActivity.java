package br.com.lps.chromecast;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.AnyRes;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.SeekBar;
import android.widget.Toast;

import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.cast.framework.CastButtonFactory;
import com.google.android.gms.cast.framework.CastContext;
import com.google.android.gms.cast.framework.CastSession;
import com.google.android.gms.cast.framework.Session;
import com.google.android.gms.cast.framework.SessionManager;
import com.google.android.gms.cast.framework.SessionManagerListener;
import com.google.android.gms.cast.framework.media.MediaIntentReceiver;
import com.google.android.gms.cast.framework.media.RemoteMediaClient;
import com.google.android.gms.common.images.WebImage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

public class MainActivity extends AppCompatActivity {

    private MenuItem mediaRouteMenuItem;

    private CastContext mCastContext;
    private CastSession mCastSession;
    private SessionManager mSessionManager;
    private final SessionManagerListener mSessionManagerListener = new SessionManagerListenerImpl();

    private String ipdevice;
    private boolean clicked;
    //private RemoteMediaClient remoteMediaClient;

    private SeekBar seekProgresso;
    private CastMediaClientListener mRemoteMediaClientListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        seekProgresso = (SeekBar) findViewById(R.id.seekProgresso);

        mRemoteMediaClientListener = new CastMediaClientListener();

        mCastContext = CastContext.getSharedInstance(this);
        mSessionManager = mCastContext.getSessionManager();

        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        int ipAddress = wifiInfo.getIpAddress();
        ipdevice = String.format("http://%d.%d.%d.%d:8080",(ipAddress & 0xff),(ipAddress >> 8 & 0xff),(ipAddress >> 16 & 0xff),(ipAddress >> 24 & 0xff));

        /*
        Toast.makeText(this, ipdevice, Toast.LENGTH_SHORT).show();

        Log.i("IP Nano", "IP: " + ipdevice);

        // start the webserver
        webserver mediaserver = new webserver();
        try {
            mediaserver.start();
        } catch(IOException ioe) {
            Log.d("Httpd", "The server could not start.");
        }
        */

        seekProgresso.incrementProgressBy(1);

        seekProgresso.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clicked = true;
            }
        });

        seekProgresso.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                if(clicked){
                    long clip = mSessionManager.getCurrentCastSession().getRemoteMediaClient().getStreamDuration() * progress / 100;
                    mSessionManager.getCurrentCastSession().getRemoteMediaClient().seek(clip);
                    clicked = false;
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        WebView webView = findViewById(R.id.webView);

        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setPluginState(WebSettings.PluginState.ON);
        webView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
        webView.getSettings().setSupportMultipleWindows(true);
        webView.getSettings().setSupportZoom(true);
        webView.getSettings().setBuiltInZoomControls(true);
        webView.getSettings().setAllowFileAccess(true);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                setProgressBarIndeterminateVisibility(false);
                super.onPageFinished(view, url);
            }
        });
        webView.loadUrl("https://r2---sn-jucj-0qpl.googlevideo.com/videoplayback?id=6385060d4e227fb7&itag=18&source=blogger&mm=31&mn=sn-jucj-0qpl&ms=au&mv=m&pl=20&ei=l_XxXM3aKdW3qwXaiLOQAQ&susc=bl&mime=video/mp4&dur=2579.667&lmt=1532538630284772&mt=1559360789&ip=189.41.213.191&ipbits=0&expire=1559389719&sparams=ip,ipbits,expire,id,itag,source,mm,mn,ms,mv,pl,ei,susc,mime,dur,lmt&signature=BD4EF0652E666751FB5EE9306D48ED11C9198BA172532C18423F26A8E623BFE0.1E0B8C7DCDBD1ABA1433D30D7C241D7D002B18A7DDE5521C81B429D80C4BA9FF&key=us0&cpn=HDVMzGoURc3PTror&c=WEB_EMBEDDED_PLAYER&cver=20190530");
        webView.setVisibility(View.VISIBLE);

    }

    @Override
    protected void onResume() {
        mCastSession = mSessionManager.getCurrentCastSession();
        mSessionManager.addSessionManagerListener(mSessionManagerListener);
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSessionManager.removeSessionManagerListener(mSessionManagerListener);
        mCastSession = null;
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.main_menu, menu);
        mediaRouteMenuItem = CastButtonFactory.setUpMediaRouteButton(getApplicationContext(), menu, R.id.media_route_menu_item);
        return true;
    }

    public void OnClickEnviar(View view) {

        MediaMetadata movieMetadata = new MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE);
        movieMetadata.putString(MediaMetadata.KEY_TITLE, "Title");
        movieMetadata.putString(MediaMetadata.KEY_SUBTITLE, "Subtitle");

        String path = "https://content.jwplatform.com/videos/oH1tk38R-xd6ee0y3.mp4?exp=1559359680&sig=c4a637871d2f2232f5f5d168d683df51";

        Log.i("TesteChromecast", path);

        MediaInfo mediaInfo = new MediaInfo.Builder(path)
                .setContentType("videos/*")
                .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                .setMetadata(movieMetadata)
                .build();

        mSessionManager.getCurrentCastSession().getRemoteMediaClient().load(mediaInfo);
    }

    public void OnClickPlay(View view) {
        mSessionManager.getCurrentCastSession().getRemoteMediaClient().play();
    }

    public void OnClickStop(View view) {
        mSessionManager.getCurrentCastSession().getRemoteMediaClient().pause();
    }

    private class SessionManagerListenerImpl implements SessionManagerListener {
        @Override
        public void onSessionStarting(Session session) {
            Log.i("TesteChromecast", "onSessionStarting");
        }

        @Override
        public void onSessionStarted(Session session, String sessionId) {
            invalidateOptionsMenu();
            Log.i("TesteChromecast", "onSessionStarted");

            mSessionManager.getCurrentCastSession().getRemoteMediaClient().removeListener(mRemoteMediaClientListener);
            mSessionManager.getCurrentCastSession().getRemoteMediaClient().addListener(mRemoteMediaClientListener);
        }

        @Override
        public void onSessionStartFailed(Session session, int i) {
            Log.i("TesteChromecast", "onSessionStartFailed");
        }

        @Override
        public void onSessionEnding(Session session) {
            Log.i("TesteChromecast", "onSessionEnding");
        }

        @Override
        public void onSessionResumed(Session session, boolean wasSuspended) {
            invalidateOptionsMenu();
            Log.i("TesteChromecast", "onSessionResumed");
            mSessionManager.getCurrentCastSession().getRemoteMediaClient().removeListener(mRemoteMediaClientListener);
            mSessionManager.getCurrentCastSession().getRemoteMediaClient().addListener(mRemoteMediaClientListener);

        }

        @Override
        public void onSessionResumeFailed(Session session, int i) {
            Log.i("TesteChromecast", "onSessionResumeFailed");
        }

        @Override
        public void onSessionSuspended(Session session, int i) {
            Log.i("TesteChromecast", "onSessionSuspended");
        }

        @Override
        public void onSessionEnded(Session session, int error) {
            //finish();
            Log.i("TesteChromecast", "onSessionEnded");
        }

        @Override
        public void onSessionResuming(Session session, String s) {
            Log.i("TesteChromecast", "onSessionResuming");
        }
    }

    public class MyMediaIntentReceiver extends MediaIntentReceiver {
        @Override
        protected void onReceiveActionTogglePlayback(Session currentSession) {
        }

        @Override
        protected void onReceiveActionMediaButton(Session session, Intent intent) {
            super.onReceiveActionMediaButton(session, intent);
        }

        @Override
        protected void onReceiveOtherAction(String action, Intent intent) {
            Log.i("TesteChromecast", action);
        }
    }

    private class CastMediaClientListener implements RemoteMediaClient.Listener {

        @Override
        public void onMetadataUpdated() {
            Log.i("TesteChromecast", "onMetadataUpdated");
        }

        @Override
        public void onStatusUpdated() {

            Log.i("TesteChromecast", "onStatusUpdated");

            final long duracao = mSessionManager.getCurrentCastSession().getRemoteMediaClient().getStreamDuration();
            final boolean isPlaying = mSessionManager.getCurrentCastSession().getRemoteMediaClient().isPlaying();

            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    while(isPlaying){
                        //Log.i("TesteChromecast", "onStatusUpdated: " + mSessionManager.getCurrentCastSession().getSessionRemainingTimeMs());

                        runOnUiThread(new Runnable() {

                            int seek = 0;
                            @Override
                            public void run() {
                                seek = (int) ((duracao - mSessionManager.getCurrentCastSession().getSessionRemainingTimeMs()) * 100 / duracao);
                                Log.i("TesteChromecast", "Seek: " + seek);
                                seekProgresso.setProgress(seek);
                            }
                        });
                    }
                }
            });

            thread.start();


        }

        @Override
        public void onSendingRemoteMediaRequest() {
            Log.i("TesteChromecast", "onSendingRemoteMediaRequest");
        }

        @Override
        public void onAdBreakStatusUpdated() {
            Log.i("TesteChromecast", "onAdBreakStatusUpdated");
        }

        @Override
        public void onQueueStatusUpdated() {
            Log.i("TesteChromecast", "onQueueStatusUpdated");
        }

        @Override
        public void onPreloadStatusUpdated() {
            Log.i("TesteChromecast", "onPreloadStatusUpdated");
        }
    }

    public class webserver extends NanoHTTPD {
        FileInputStream fileInputStream;

        public webserver() {
            super(8080);
        }

        @Override
        public Response serve(IHTTPSession session) {
            String msg = "<html><body><h1>Hello server</h1>\n";
            Map<String, String> parms = session.getParms();
            if (parms.get("username") == null) {
                msg += "<form action='?' method='get'>\n";
                msg += "<p>Your name: <input type='text' name='username'></p>\n";
                msg += "</form>\n";
            } else {
                msg += "<p>Hello, " + parms.get("username") + "!</p>";
            }

            return  newFixedLengthResponse(Response.Status.OK, "image/jpeg", "");

            //return newFixedLengthResponse(msg + "</body></html>\n");
        }


    }
}
