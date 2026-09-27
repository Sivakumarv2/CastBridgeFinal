package com.castbridge.personal;

import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
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

public class MainActivity extends AppCompatActivity {
    private CastContext castContext;
    private CastSession castSession;
    private TextView statusText;
    private EditText urlInput;

    private final SessionManagerListener<CastSession> sessionListener = new SessionManagerListener<CastSession>() {
        @Override public void onSessionStarting(CastSession session) { updateStatus("Connecting…"); }
        @Override public void onSessionStarted(CastSession session, String sessionId) { castSession = session; updateStatus("Connected to " + session.getCastDevice().getFriendlyName()); }
        @Override public void onSessionStartFailed(CastSession session, int error) { updateStatus("Connection failed"); }
        @Override public void onSessionEnding(CastSession session) { updateStatus("Disconnecting…"); }
        @Override public void onSessionEnded(CastSession session, int error) { castSession = null; updateStatus("Not connected"); }
        @Override public void onSessionResuming(CastSession session, String sessionId) { updateStatus("Reconnecting…"); }
        @Override public void onSessionResumed(CastSession session, boolean wasSuspended) { castSession = session; updateStatus("Connected to " + session.getCastDevice().getFriendlyName()); }
        @Override public void onSessionResumeFailed(CastSession session, int error) { updateStatus("Reconnect failed"); }
        @Override public void onSessionSuspended(CastSession session, int reason) { updateStatus("Connection temporarily suspended"); }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        statusText = findViewById(R.id.statusText);
        urlInput = findViewById(R.id.urlInput);
        MediaRouteButton castButton = findViewById(R.id.castButton);
        Button playButton = findViewById(R.id.playButton);
        Button stopButton = findViewById(R.id.stopButton);
        Button browserButton = findViewById(R.id.browserButton);

        castContext = CastContext.getSharedInstance(this);
        CastButtonFactory.setUpMediaRouteButton(getApplicationContext(), castButton);

        playButton.setOnClickListener(v -> playUrlOnTv());
        stopButton.setOnClickListener(v -> stopCasting());
        browserButton.setOnClickListener(v -> startActivity(new android.content.Intent(this, BrowserActivity.class)));
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (castContext != null) {
            castContext.getSessionManager().addSessionManagerListener(castSessionListener(), CastSession.class);
            CastSession current = castContext.getSessionManager().getCurrentCastSession();
            if (current != null) {
                castSession = current;
                updateStatus("Connected to " + current.getCastDevice().getFriendlyName());
            }
        }
    }

    @Override
    protected void onStop() {
        if (castContext != null) {
            castContext.getSessionManager().removeSessionManagerListener(castSessionListener(), CastSession.class);
        }
        super.onStop();
    }

    private SessionManagerListener<CastSession> castSessionListener() {
        return sessionListener;
    }

    private void playUrlOnTv() {
        String rawUrl = urlInput.getText().toString().trim();
        if (TextUtils.isEmpty(rawUrl)) {
            toast("Paste a direct media URL first");
            return;
        }
        if (castSession == null || castSession.getRemoteMediaClient() == null) {
            toast("Connect to your Chromecast first");
            return;
        }

        Uri uri = Uri.parse(rawUrl);
        String contentType = guessContentType(rawUrl);
        MediaInfo mediaInfo = new MediaInfo.Builder(uri.toString())
                .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                .setContentType(contentType)
                .build();

        MediaLoadRequestData requestData = new MediaLoadRequestData.Builder()
                .setMediaInfo(mediaInfo)
                .setAutoplay(true)
                .build();

        castSession.getRemoteMediaClient().load(requestData)
                .setResultCallback(result -> {
                    if (!result.getStatus().isSuccess()) {
                        toast("TV could not start this media URL");
                    }
                });
    }

    private void stopCasting() {
        if (castSession != null && castSession.getRemoteMediaClient() != null) {
            castSession.getRemoteMediaClient().stop();
        }
    }

    private String guessContentType(String url) {
        String lower = url.toLowerCase();
        if (lower.contains(".m3u8")) return "application/x-mpegURL";
        if (lower.contains(".mpd")) return "application/dash+xml";
        if (lower.contains(".mp3")) return "audio/mpeg";
        if (lower.contains(".m4a")) return "audio/mp4";
        if (lower.contains(".webm")) return "video/webm";
        if (lower.contains(".mov")) return "video/quicktime";
        if (lower.contains(".mkv")) return "video/x-matroska";
        return "video/mp4";
    }

    private void updateStatus(String text) {
        if (statusText != null) statusText.setText(text);
    }

    private void toast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }
}
