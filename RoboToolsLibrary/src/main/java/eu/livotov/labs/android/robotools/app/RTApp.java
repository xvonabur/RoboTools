package eu.livotov.labs.android.robotools.app;

import android.app.Application;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.widget.Toast;

import eu.livotov.labs.android.robotools.app.injector.RTInjector;

/**
 * Created by dlivotov on 30/08/2015.
 */
public class RTApp extends Application
{
    private static RTApp instance;
    private RTInjector.ApplicationInjector injector = new RTInjector.ApplicationInjector(this);
    private Boolean debuggableStatus;

    @Override
    public void onCreate()
    {
        super.onCreate();
        injector.onCreate();
        instance = this;
    }

    @Override
    public void onTerminate()
    {
        instance = null;
        super.onTerminate();
    }

    public static RTApp getInstance()
    {
        return instance;
    }

    public static void showToast(@NonNull CharSequence text, final boolean longToast)
    {
        if (instance!=null)
        {
            Toast.makeText(instance, text, longToast ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT).show();
        }
    }

    public static void showToast(final int textId, final boolean longToast)
    {
        if (instance!=null)
        {
            Toast.makeText(instance, textId, longToast ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT).show();
        }
    }

    public static synchronized boolean isDebuggable()
    {
        if (instance.debuggableStatus==null)
        {
            try
            {
                PackageManager pm = instance.getPackageManager();
                ApplicationInfo ai = pm.getApplicationInfo(instance.getPackageName(), 128);
                instance.debuggableStatus = (ai == null) || ((ai.flags &= ApplicationInfo.FLAG_DEBUGGABLE) != 0);
            }
            catch (Throwable err)
            {
                instance.debuggableStatus = false;
            }
        }

        return instance.debuggableStatus;
    }
}

