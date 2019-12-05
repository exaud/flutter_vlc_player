package software.solid.fluttervlcplayer;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import androidx.annotation.RequiresApi;
import android.util.Base64;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;

import org.videolan.libvlc.IVLCVout;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.platform.PlatformView;

class FlutterVideoView implements PlatformView, MethodChannel.MethodCallHandler, MediaPlayer.EventListener {
    private final MethodChannel channel;
    private final Context context;

    private MediaPlayer mediaPlayer;
    private SurfaceView surfaceView;
    private SurfaceHolder holder;
    private String url;
    private IVLCVout vout;
    private MethodChannel.Result result;
    private boolean replyAlreadySubmitted = false;

    @RequiresApi(api = Build.VERSION_CODES.O)
    public FlutterVideoView(Context context, BinaryMessenger messenger, int id) {
        this.context = context;
        surfaceView = new SurfaceView(context);
        holder = surfaceView.getHolder();

        channel = new MethodChannel(messenger, "flutter_video_plugin/getVideoView_" + id);
        channel.setMethodCallHandler(this);
    }

    @Override
    public View getView() {
        return surfaceView;
    }

    @Override
    public void dispose() {
        mediaPlayer.stop();
        vout.detachViews();
    }


    @Override
    public void onMethodCall(MethodCall methodCall, MethodChannel.Result result) {
        switch (methodCall.method) {
            case "playVideo":
                this.result = result;
                if (surfaceView == null) {
                    surfaceView = new SurfaceView(context);
                    holder = surfaceView.getHolder();
                }
                url = methodCall.argument("url");

                ArrayList<String> options = new ArrayList<String>();
                options.add("--avcodec-codec=h264");

                LibVLC libVLC = new LibVLC(context, options);
                holder.setKeepScreenOn(true);

                Media media = new Media(libVLC, Uri.parse(Uri.decode(url)));
                mediaPlayer = new MediaPlayer(media);
                mediaPlayer.setVideoTrackEnabled(true);
                vout = mediaPlayer.getVLCVout();
                surfaceView.forceLayout();
                surfaceView.setFitsSystemWindows(true);
                vout.setVideoView(surfaceView);
                vout.attachViews();

                mediaPlayer.setEventListener(this);
                mediaPlayer.play();
                break;
            case "dispose":
                mediaPlayer.stop();
                vout.detachViews();
                break;
            case "onTap":
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.pause();
                } else {
                    mediaPlayer.play();
                }
                break;
        }
    }

    @Override
    public void onEvent(MediaPlayer.Event event) {
        Map<String, String> resultMap = new HashMap<>();

        switch (event.type) {
            case MediaPlayer.Event.Vout:
                String aspectRatio;
                int height = 0;
                int width = 0;
                Media.VideoTrack currentVideoTrack = mediaPlayer.getCurrentVideoTrack();
                if (currentVideoTrack != null) {
                    height = currentVideoTrack.height;
                    width = currentVideoTrack.width;
                }

                if (height != 0) {
                    aspectRatio = String.valueOf(width / height);
                    resultMap.put("aspectRatio", aspectRatio);
                }

                vout.setWindowSize(surfaceView.getWidth(), surfaceView.getHeight());
                if (!replyAlreadySubmitted) {
                    result.success(resultMap);
                    replyAlreadySubmitted = true;
                }
                break;
        }
    }
}
