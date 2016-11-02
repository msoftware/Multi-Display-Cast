package es.munix.multidisplaycast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

/**
 * Created by munix on 2/11/16.
 */

public class CastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive( Context context, Intent intent ) {
        Bundle extras = intent.getExtras();
        if ( extras != null ) {
            String action = extras.getString( "action" );

            if ( action.equals( "disconnect" ) ) {
                CastManager.getInstance().stop();
            }
        }
    }
}
