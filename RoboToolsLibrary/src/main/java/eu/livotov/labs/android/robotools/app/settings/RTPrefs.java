package eu.livotov.labs.android.robotools.app.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import java.util.StringTokenizer;

import eu.livotov.labs.android.robotools.text.RTBase64;

/**
 * Created by dlivotov on 30/08/2015.
 */
public class RTPrefs
{
    private static RTPrefs defaultPreferences = null;

    protected SharedPreferences preferences;
    protected Context ctx;

    public static synchronized RTPrefs getDefault(final Context ctx)
    {
        if (defaultPreferences == null)
        {
            defaultPreferences = new RTPrefs(ctx, null);
        }

        return defaultPreferences;
    }

    public RTPrefs(final Context ctx)
    {
        this(ctx, null, false);
    }

    public RTPrefs(final Context ctx, final String preferenceStorageName)
    {
        this(ctx, preferenceStorageName, false);
    }

    public RTPrefs(final Context ctx, final String preferenceStorageName, boolean privateMode)
    {
        super();
        this.ctx = ctx;
        this.preferences = TextUtils.isEmpty(preferenceStorageName) ? PreferenceManager.getDefaultSharedPreferences(ctx) : ctx.getSharedPreferences(preferenceStorageName, getPrefsMode(privateMode));
    }

    public String getString(int key, String defaultValue)
    {
        return getString(ctx.getString(key), defaultValue);
    }

    public String getString(String key, String defaultValue)
    {
        return preferences.getString(key, defaultValue);
    }

    public void setString(int key, String value)
    {
        setString(ctx.getString(key), value);
    }

    public void setString(String key, String value)
    {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(key, value);
        editor.commit();
    }

    public int getInt(int key, int defaultValue)
    {
        return getInt(ctx.getString(key), defaultValue);
    }

    public int getInt(String key, int defaultValue)
    {
        return preferences.getInt(key, defaultValue);
    }

    public void setInt(int key, int value)
    {
        setInt(ctx.getString(key), value);
    }

    public void setInt(String key, int value)
    {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(key, value);
        editor.commit();
    }

    public long getLong(int key, long defaultValue)
    {
        return getLong(ctx.getString(key), defaultValue);
    }

    public long getLong(String key, long defaultValue)
    {
        try
        {
            return preferences.getLong(key, defaultValue);
        }
        catch (Throwable err)
        {
            err.printStackTrace();
            return 0;
        }
    }

    public void setLong(int key, long value)
    {
        setLong(ctx.getString(key), value);
    }

    public void setLong(String key, long value)
    {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putLong(key, value);
        editor.commit();
    }

    public void setDouble(int key, double value)
    {
        setDouble(ctx.getString(key), value);
    }

    public void setDouble(String key, double value)
    {
        setString(key, "" + value);
    }

    public double getDouble(int key, double defaultValue)
    {
       return getDouble(ctx.getString(key), defaultValue);
    }

    public double getDouble(String key, double defaultValue)
    {
        try
        {
            return Double.parseDouble(getString(key, "" + defaultValue));
        }
        catch (Throwable err)
        {
            return defaultValue;
        }
    }

    public boolean getBoolean(int key, boolean defaultValue)
    {
        return getBoolean(ctx.getString(key), defaultValue);
    }

    public boolean getBoolean(String key, boolean defaultValue)
    {
        return preferences.getBoolean(key, defaultValue);
    }

    public void setBoolean(int key, boolean value)
    {
        setBoolean(ctx.getString(key), value);
    }

    public void setBoolean(String key, boolean value)
    {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(key, value);
        editor.commit();
    }

    public void setIntArray(Context ctx, int key, int[] array)
    {
        setString(key, arrayToString(array));
    }

    public void setIntArray(Context ctx, String key, int[] array)
    {
        setString(key, arrayToString(array));
    }

    public void setLongArray(Context ctx, int key, long[] array)
    {
        setString(key, arrayToString(array));
    }

    public void setLongArray(Context ctx, String key, long[] array)
    {
        setString(key, arrayToString(array));
    }

    public void setByteArray(Context ctx, int key, byte[] array)
    {
        setString(key, RTBase64.encodeToString(array, RTBase64.NO_WRAP));
    }

    public void setByteArray(Context ctx, String key, byte[] array)
    {
        setString(key, RTBase64.encodeToString(array, RTBase64.NO_WRAP));
    }

    public int[] getIntArray(Context ctx, int key)
    {
        return stringToIntegerArray(getString(key, ""));
    }

    public int[] getIntArray(Context ctx, String key)
    {
        return stringToIntegerArray(getString(key, ""));
    }

    public long[] getLongArray(Context ctx, int key)
    {
        return stringToLongArray(getString(key, ""));
    }

    public long[] getLongArray(Context ctx, String key)
    {
        return stringToLongArray(getString(key, ""));
    }

    public byte[] getByteArray(Context ctx, int key)
    {
        return RTBase64.decode(getString(key, ""), RTBase64.NO_WRAP);
    }

    public byte[] getByteArray(Context ctx, String key)
    {
        return RTBase64.decode(getString(key, ""), RTBase64.NO_WRAP);
    }

    public void remove(final int key)
    {
        remove(ctx.getString(key));
    }

    public void remove(final String key)
    {
        SharedPreferences.Editor editor = preferences.edit();
        editor.remove(key);
        editor.commit();
    }

    public void clear()
    {
        preferences.edit().clear().commit();
    }

    public SharedPreferences getPreferences()
    {
        return preferences;
    }

    protected static int getPrefsMode(boolean privateMode)
    {
        if (Build.VERSION.SDK_INT >= 11 && !privateMode)
        {
            return Context.MODE_MULTI_PROCESS;
        }
        else
        {
            return Context.MODE_PRIVATE;
        }
    }

    protected String arrayToString(int[] array)
    {
        StringBuffer str = new StringBuffer();

        for (int a : array)
        {
            if (str.length() > 0)
            {
                str.append("|");
            }

            str.append("" + a);
        }

        return str.toString();
    }

    protected String arrayToString(long[] array)
    {
        StringBuffer str = new StringBuffer();

        for (long a : array)
        {
            if (str.length() > 0)
            {
                str.append("|");
            }

            str.append("" + a);
        }

        return str.toString();
    }

    protected int[] stringToIntegerArray(final String str)
    {
        StringTokenizer tok = new StringTokenizer(str, "|", false);
        if (tok.countTokens() > 0)
        {
            int[] arr = new int[tok.countTokens()];
            int index = 0;
            while (tok.hasMoreTokens())
            {
                arr[index] = Integer.parseInt(tok.nextToken());
                index++;
            }

            return arr;
        }
        else
        {
            return null;
        }
    }

    protected long[] stringToLongArray(final String str)
    {
        StringTokenizer tok = new StringTokenizer(str, "|", false);
        if (tok.countTokens() > 0)
        {
            long[] arr = new long[tok.countTokens()];
            int index = 0;
            while (tok.hasMoreTokens())
            {
                arr[index] = Long.parseLong(tok.nextToken());
                index++;
            }

            return arr;
        }
        else
        {
            return null;
        }
    }
}
