package es.munix.multicast;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;

import es.munix.multidisplaycast.CastInstance;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate( Bundle savedInstanceState ) {

        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_main );
        Log.v( "ACT", "create" );

        CastInstance.getInstance().setDiscoveryManager();
    }

    @Override
    public boolean onCreateOptionsMenu( Menu menu ) {
        getMenuInflater().inflate( R.menu.cast_menu, menu );
        CastInstance.getInstance().registerForActivity( this, menu, R.id.castMenu );
        return super.onCreateOptionsMenu( menu );
    }

    @Override
    protected void onDestroy() {
        CastInstance.getInstance().onDestroy();
        super.onDestroy();
    }
}
