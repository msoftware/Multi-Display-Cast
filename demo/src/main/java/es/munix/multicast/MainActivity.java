package es.munix.multicast;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;

import es.munix.multidisplaycast.CastManager;
import es.munix.multidisplaycast.interfaces.CastListener;
import es.munix.multidisplaycast.interfaces.PlayStatusListener;

public class MainActivity extends AppCompatActivity implements CastListener, PlayStatusListener {

    Button videoButton;
    ProgressBar loader;

    @Override
    protected void onCreate( Bundle savedInstanceState ) {

        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_main );
        CastManager.getInstance().setDiscoveryManager();
        CastManager.getInstance().setPlayStatusListener( getClass().getSimpleName(), this );
        CastManager.getInstance().setCastListener( getClass().getSimpleName(), this );

        videoButton = (Button) findViewById( R.id.videoButton );
        loader = (ProgressBar) findViewById( R.id.loader );
        loader.setVisibility( View.GONE );
    }

    @Override
    public boolean onCreateOptionsMenu( Menu menu ) {
        getMenuInflater().inflate( R.menu.cast_menu, menu );
        CastManager.getInstance().registerForActivity( this, menu, R.id.castMenu );
        return super.onCreateOptionsMenu( menu );
    }

    @Override
    protected void onDestroy() {
        Log.v( "Activity", "ondestroy" );
        CastManager.getInstance().unsetCastListener( getClass().getSimpleName() );
        CastManager.getInstance().unsetPlayStatusListener( getClass().getSimpleName() );
        CastManager.getInstance().onDestroy();
        super.onDestroy();
    }

    @Override
    public void isConnected() {
        videoButton.setText( "Reproducir video" );
        videoButton.setEnabled( true );
    }

    @Override
    public void isDisconnected() {
        videoButton.setText( "Desconectado" );
        videoButton.setEnabled( false );
    }

    public void playVideo( View v ) {
        if ( videoButton.getText().toString().equals( "Detener video" ) ) {
            CastManager.getInstance().stop();
        } else {
            videoButton.setVisibility( View.GONE );
            loader.setVisibility( View.VISIBLE );
            CastManager.getInstance()
                    .playMedia( "http://download.blender.org/peach/bigbuckbunny_movies/BigBuckBunny_320x180.mp4", "video/mp4", "Big Buck Bunny", "MP4", "http://camendesign.com/code/video_for_everybody/poster.jpg" );
        }
    }

    @Override
    public void onPlayStatusChanged( int playStatus ) {
        videoButton.setVisibility( View.VISIBLE );
        loader.setVisibility( View.GONE );

        switch( playStatus ) {
            case STATUS_START_PLAYING:
                videoButton.setText( "Detener video" );
                CastManager.getInstance().startControlsActivity();
                break;

            case STATUS_FINISHED:
            case STATUS_STOPPED:
                videoButton.setText( "Reproducir video" );
                break;

            case STATUS_PAUSED:
                videoButton.setText( "Reanudar video" );
                break;

            case STATUS_NOT_SUPPORT_LISTENER:
                break;
        }
    }

    @Override
    public void onPositionChanged( long currentPosition ) {
    }

    @Override
    public void onTotalDurationObtained( long totalDuration ) {
    }

    @Override
    public void onSuccessSeek() {

    }
}
