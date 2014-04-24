package edu.utexas.quietplaces;

import android.app.Application;
import edu.utexas.quietplaces.utils.PlatformSpecificImplementationFactory;
import edu.utexas.quietplaces.utils.base.IStrictMode;

/**
 * Base class for the whole application.
 */
public class QuietPlacesApplication extends Application {

    @Override
    public final void onCreate() {
        super.onCreate();

        if (PlacesConstants.DEVELOPER_MODE) {
            IStrictMode strictMode = PlatformSpecificImplementationFactory.getStrictMode();
            if (strictMode != null)
                strictMode.enableStrictMode();
        }
    }
}
