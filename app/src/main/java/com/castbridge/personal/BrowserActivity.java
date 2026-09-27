package com.castbridge.personal;

import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.mediarouter.app.MediaRouteButton;

import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaLoadRequestData;
import com.google.android.gms.cast.framework.CastButtonFactory;
import com.google.android.gms.cast.framework.CastContext;
import com.google.android.gms.cast.framework.CastSession;
import com.google.android.gms.cast.framework.SessionManagerListener;

import org.json.JSONTokener;

public class BrowserActivity extends AppCompatActivity {
    private WebView webView;
    private EditText addressBar;
    private TextView connectionText;
    private CastContext castContext;
    private CastSession castSession;

    private final SessionManagerListener<CastSession> sessionListener = new SessionManagerListener<CastSession>() {
        @Override public void onSessionStarting(CastSession session) { setConnectionText("Connecting…"); }
        @Override public void onSessionStarted(CastSession session, String sessionId) { setSession(session); }
        @Override public void onSessionStartFailed(CastSession session, int error) { setConnectionText("Cast connection failed"); }
        @Override public void onSessionEnding(CastSession session) { setConnectionText("Disconnecting…"); }
        @Override public void onSessionEnded(CastSession session, int error) { castSession = null; setConnectionText("Not connected"); }
        @Override public void onSessionResuming(CastSession session, String sessionId) { setConnectionText("Reconnecting…"); }
        @Override public void onSessionResumed(CastSession session, boolean wasSuspended) { setSession(session); }
        @Override public void onSessionResumeFailed(CastSession session, int error) { setConnectionText("Reconnect failed"); }
        @Override public void onSessionSuspended(CastSession session, int reason) { setConnectionText("Cast connection suspended"); }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_browser);

        webView = findViewById(R.id.webView);
        addressBar = findViewById(R.id.addressBar);
        connectionText = findViewById(R.id.browserConnectionText);
        MediaRouteButton castButton = findViewById(R.id.browserCastButton);
        Button goButton = findViewById(R.id.goButton);
        Button backButton = findViewById(R.id.backButton);
        Button forwardButton = findViewById(R.id.forwardButton);
        Button reloadButton = findViewById(R.id.reloadButton);
        Button castVideoButton = findViewById(R.id.castVideoButton);

        castContext = CastContext.getSharedInstance(this);
        CastButtonFactory.setUpMediaRouteButton(getApplicationContext(), castButton);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, android.webkit.WebResourceRequest request) {
                return false;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                addressBar.setText(url);
                addressBar.setSelection(addressBar.length());
            }
        });
        webView.setWebChromeClient(new WebChromeClient());

        goButton.setOnClickListener(v -> loadAddress());
        addressBar.setOnEditorActionListener((v, actionId, event) -> {
            loadAddress();
            return true;
        });
        backButton.setOnClickListener(v -> { if (webView.canGoBack()) webView.goBack(); });
        forwardButton.setOnClickListener(v -> { if (webView.canGoForward()) webView.goForward(); });
        reloadButton.setOnClickListener(v -> webView.reload());
        castVideoButton.setOnClickListener(v -> castVideoFromPage());

        String startUrl = getIntent().getStringExtra("url");
        if (TextUtils.isEmpty(startUrl)) startUrl = "https://www.google.com";
        addressBar.setText(startUrl);
        webView.loadUrl(startUrl);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (castContext != null) {
            castContext.getSessionManager().addSessionManagerListener(sessionListener, CastSession.class);
            CastSession current = castContext.getSessionManager().getCurrentCastSession();
            if (current != null) setSession(current);
        }
    }

    @Override
    protected void onStop() {
        if (castContext != null) {
            castContext.getSessionManager().removeSessionManagerListener(sessionListener, CastSession.class);
        }
        super.onStop();
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    private void loadAddress() {
        String value = addressBar.getText().toString().trim();
        if (TextUtils.isEmpty(value)) return;
        if (!value.matches("^[a-zA-Z][a-zA-Z0-9+.-]*://.*$")) {
            if (value.contains(" ")) {
                value = "https://www.google.com/search?q=" + Uri.encode(value);
            } else {
                value = "https://" + value;
            }
        }
        webView.loadUrl(value);
    }

    private void castVideoFromPage() {
        if (castSession == null || castSession.getRemoteMediaClient() == null) {
            toast("Connect to your Chromecast first");
            return;
        }

        String script = "(function(){var v=document.querySelector('video'); if(v){return v.currentSrc||v.src||((v.querySelector('source')||{}).src)||'';} var s=document.querySelector('source'); return s?s.src:'';})()";
        webView.evaluateJavascript(script, value -> {
            try {
                Object parsed = new JSONTokener(value).nextValue();
                String mediaUrl = parsed == null ? "" : parsed.toString();
                if (TextUtils.isEmpty(mediaUrl) || mediaUrl.startsWith("blob:")) {
                    toast("This page does not expose a castable direct video URL");
                    return;
                }
                playUrlOnTv(mediaUrl);
            } catch (Exception e) {
                toast("Could not detect a castable video on this page");
            }
        });
    }

    private void playUrlOnTv(String rawUrl) {
        Uri uri = Uri.parse(rawUrl);
        String lower = rawUrl.toLowerCase();
        String contentType = "video/mp4";
        if (lower.contains(".m3u8")) contentType = "application/x-mpegURL";
        else if (lower.contains(".mpd")) contentType = "application/dash+xml";
        else if (lower.contains(".webm")) contentType = "video/webm";
        else if (lower.contains(".mov")) contentType = "video/quicktime";

        MediaInfo mediaInfo = new MediaInfo.Builder(uri.toString())
                .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                .setContentType(contentType)
                .build();
        MediaLoadRequestData requestData = new MediaLoadRequestData.Builder()
                .setMediaInfo(mediaInfo)
                .setAutoplay(true)
                .build();

        castSession.getRemoteMediaClient().load(requestData).setResultCallback(result -> {
            if (result.getStatus().isSuccess()) {
                toast("Video sent to TV");
            } else {
                toast("TV could not play this video");
            }
        });
    }

    private void setSession(CastSession session) {
        castSession = session;
        setConnectionText("Connected to " + session.getCastDevice().getFriendlyName());
    }

    private void setConnectionText(String text) {
        if (connectionText != null) connectionText.setText(text);
    }

    private void toast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }
}
