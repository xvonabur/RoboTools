package eu.livotov.labs.android.robotools.geo;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.VisibleRegion;

/**
 * (c) Livotov Labs Ltd. 2012
 * Date: 16.04.13
 */
public class RTGoogleMaps
{

    public static boolean intiMap(GoogleMap map)
    {
        return intiMap(map, null);
    }

    public static boolean intiMap(GoogleMap map, Activity activity)
    {
        if (activity != null && !isGoogleMapsAvailable(activity))
        {
            return false;
        }

        map.setMyLocationEnabled(true);
        map.getUiSettings().setMyLocationButtonEnabled(true);
        map.getUiSettings().setRotateGesturesEnabled(true);
        map.getUiSettings().setAllGesturesEnabled(true);
        map.getUiSettings().setTiltGesturesEnabled(true);
        map.getUiSettings().setZoomControlsEnabled(false);
        map.getUiSettings().setZoomGesturesEnabled(true);
        map.getUiSettings().setCompassEnabled(true);

        if (activity != null)
        {
            MapsInitializer.initialize(activity);
        }

        return true;
    }

    public static boolean intiMapForInlinePreview(GoogleMap map)
    {
        return intiMapForInlinePreview(map, null);
    }

    public static boolean intiMapForInlinePreview(GoogleMap map, Activity activity)
    {
        if (activity != null && !isGoogleMapsAvailable(activity))
        {
            return false;
        }

        map.setMyLocationEnabled(true);
        map.getUiSettings().setMyLocationButtonEnabled(false);
        map.getUiSettings().setRotateGesturesEnabled(false);
        map.getUiSettings().setAllGesturesEnabled(false);
        map.getUiSettings().setTiltGesturesEnabled(false);
        map.getUiSettings().setZoomControlsEnabled(false);
        map.getUiSettings().setZoomGesturesEnabled(false);
        map.getUiSettings().setCompassEnabled(false);
        map.getUiSettings().setScrollGesturesEnabled(false);

        if (activity != null)
        {
            MapsInitializer.initialize(activity);
        }

        return true;
    }

    public static void toggleSatelliteMode(GoogleMap map, boolean satellite)
    {
        if (map == null)
        {
            return;
        }

        map.setMapType(satellite ? GoogleMap.MAP_TYPE_SATELLITE : GoogleMap.MAP_TYPE_NORMAL);
    }

    public static boolean isSatelliteMode(GoogleMap map)
    {
        if (map == null)
        {
            return false;
        }

        return map.getMapType() == GoogleMap.MAP_TYPE_SATELLITE;
    }

    public static void moveMapTo(GoogleMap map, double lat, double lon, float zoom, boolean animate)
    {
        if (map == null)
        {
            return;
        }

        CameraUpdate position = CameraUpdateFactory.newLatLngZoom(new LatLng(lat, lon), zoom == 0.0 ? 14.0f : zoom);
        if (animate)
        {
            map.animateCamera(position);
        } else
        {
            map.moveCamera(position);
        }
    }

    public static boolean isGoogleMapsAvailable(Context ctx)
    {
        return GooglePlayServicesUtil.isGooglePlayServicesAvailable(ctx) == ConnectionResult.SUCCESS && checkGmsServiceInstalled(ctx);
    }

    public static String getStaticMapPictureUrl(final double lat, final double lon)
    {
        return getStaticMapPictureUrl(lat, lon, 350, 110);
    }

    public static String getStaticMapPictureUrl(final double lat, final double lon, final int width, final int height)
    {
        return getStaticMapPictureUrl(lat, lon, width, height, 16);
    }

    public static String getStaticMapPictureUrl(final double lat, final double lon, final int width, final int height, final int zoom)
    {
        final String coordPair = String.format("%s,%s", lat, lon);
        final String sizePair = String.format("%sx%s", width, height);
        return "http://maps.googleapis.com/maps/api/staticmap?"
                       + "&zoom=" + zoom
                       + "&size=" + sizePair
                       + "&maptype=roadmap&sensor=true"
                       + "&center=" + coordPair
                       + "&markers=color:black|" + coordPair;
    }

    private static boolean checkGmsServiceInstalled(final Context ctx)
    {
        try
        {
            ApplicationInfo info = ctx.getPackageManager().getApplicationInfo("com.google.android.gms", 0);
            return info != null;
        } catch (PackageManager.NameNotFoundException e)
        {
        }

        return false;
    }

    public static Uri getNavigationRouteUrl(double lat1, double lon1, double lat2, double lon2)
    {
        return getNavigationRouteUrl(lat1, lon1, lat2, lon2, 12);
    }

    public static Uri getNavigationRouteUrl(double lat1, double lon1, double lat2, double lon2, double zoom)
    {
        return Uri.parse(String.format("http://maps.google.com/maps/?f=d&saddr=(%s,%s)&daddr=(%s,%s)&hl=en&z=%s", lat1, lon1, lat2, lon2, 12));
    }

    public static double getMapVisibleRadius(GoogleMap map)
    {
        VisibleRegion region = map.getProjection().getVisibleRegion();
        return RTGeo.findDistance(region.nearLeft.latitude, region.nearLeft.longitude, region.farRight.latitude, region.farRight.longitude);
    }

    public static void installGoogleMapsIfRequired(final Activity activity)
    {
        int code = GooglePlayServicesUtil.isGooglePlayServicesAvailable(activity);

        if (code != ConnectionResult.SUCCESS)
        {
            Dialog d = GooglePlayServicesUtil.getErrorDialog(code, activity, 987);
            if (d != null)
            {
                d.show();
            }
        } else if (!checkGmsServiceInstalled(activity))
        {
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(activity);

            alertDialogBuilder
                    .setTitle("Google Play Services Not Installed")
                    .setMessage("In order to use Google Maps in this application, you must install Google Maps and Google Play Services applications to your device.\n\nWould you like to go to Goole Play (tm) to install them now ?")
                    .setCancelable(true)

                    .setPositiveButton("Yes", new DialogInterface.OnClickListener()
                    {
                        public void onClick(DialogInterface dialog, int id)
                        {
                            dialog.dismiss();
                            try
                            {
                                // Try the new HTTP scheme first (I assume that is the official way now given that google uses it).
                                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/details?id=com.google.android.gms"));
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                                intent.setPackage("com.android.vending");
                                activity.startActivity(intent);
                            } catch (ActivityNotFoundException e)
                            {
                                // Ok that didn't work, try the market method.
                                try
                                {
                                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.google.android.gms"));
                                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                                    intent.setPackage("com.android.vending");
                                    activity.startActivity(intent);
                                } catch (ActivityNotFoundException f)
                                {
                                    // Ok, weird. Maybe they don't have any market app. Just show the website.
                                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/details?id=com.google.android.gms"));
                                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                                    activity.startActivity(intent);
                                }
                            }
                        }
                    })

                    .setNegativeButton("No", new DialogInterface.OnClickListener()
                    {
                        public void onClick(DialogInterface dialog, int id)
                        {
                            dialog.cancel();
                        }
                    })

                    .create().show();
        }
    }
}
