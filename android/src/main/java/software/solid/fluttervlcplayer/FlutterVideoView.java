package software.solid.fluttervlcplayer;

import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import androidx.annotation.RequiresApi;

import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;
import org.videolan.libvlc.interfaces.IVLCVout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.platform.PlatformView;

class FlutterVideoView implements PlatformView, MethodChannel.MethodCallHandler, MediaPlayer.EventListener {
    private static final String TAG = FlutterVideoView.class.getSimpleName();

    private final MethodChannel channel;
    private final Context context;

    private MediaPlayer mediaPlayer;
    private SurfaceView surfaceView;
    private SurfaceHolder holder;
    private String url;
    private IVLCVout vout;
    private MethodChannel.Result result;
    private boolean replyAlreadySubmitted = false;

    private final Handler mTasksHandler;
    private final Handler mMainHandler;

    @RequiresApi(api = Build.VERSION_CODES.O)
    public FlutterVideoView(Context context, BinaryMessenger messenger, int id) {
        this.context = context;

        HandlerThread tasksHandlerThread = new HandlerThread("BackgroundTasks");
        tasksHandlerThread.setPriority(Thread.NORM_PRIORITY);
        tasksHandlerThread.start();
        mTasksHandler = new Handler(tasksHandlerThread.getLooper());
        mMainHandler = new Handler(Looper.getMainLooper());

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
        stopStream();
    }

    @Override
    public void onMethodCall(MethodCall methodCall, MethodChannel.Result result) {
        switch (methodCall.method) {
            case "playVideo":
                this.result = result;
                url = methodCall.argument("url");
                startStream();
                break;
            case "dispose":
                stopStream();
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

                if (result != null && !replyAlreadySubmitted) {
                    result.success(resultMap);
                    replyAlreadySubmitted = true;
                }
                break;
        }
    }

    private void startStream() {
        if (url != null) {
            mTasksHandler.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (surfaceView == null) {
                            surfaceView = new SurfaceView(context);
                            holder = surfaceView.getHolder();
                        }

                        surfaceView.setFitsSystemWindows(true);
                        holder.setKeepScreenOn(true);

                        ArrayList<String> options = new ArrayList<String>();
                        options.add("--avcodec-codec=h264");
                        LibVLC libVLC = new LibVLC(context, options);
                        Media media = new Media(libVLC, Uri.parse(Uri.decode(url)));
                        mediaPlayer = new MediaPlayer(media);
                        mediaPlayer.setVideoTrackEnabled(true);
                        vout = mediaPlayer.getVLCVout();

                        mMainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    surfaceView.forceLayout();
                                    vout.setVideoView(surfaceView);
                                    vout.attachViews();
                                    mediaPlayer.setEventListener(FlutterVideoView.this);
                                    mediaPlayer.play();
                                } catch (Exception e) {
                                }
                            }
                        });
                    } catch (Exception e) {
                    }
                }
            });
        }
    }

    private void stopStream() {
        mTasksHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    result = null;
                    replyAlreadySubmitted = false;
                    mediaPlayer.stop();
                } catch (Exception e) {
                }
            }
        });

        mMainHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    vout.detachViews();
                } catch (Exception e) {
                }
            }
        });
    }
}
