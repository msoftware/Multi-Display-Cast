package es.munix.multidisplaycast.interfaces;

/**
 * Created by munix on 3/11/16.
 */

public interface PlayStatusListener {

    public static int STATUS_PLAYING = 0;
    public static int STATUS_PAUSED = 1;
    public static int STATUS_STOPPED = 2;
    public static int STATUS_FINISHED = 3;
    public static int STATUS_NOT_SUPPORT_LISTENER = 4;

    void onPlayStatusChanged( int playStatus );

    void onPositionChanged( long currentPosition );

    void onTotalDurationObtained( long totalDuration );
}
