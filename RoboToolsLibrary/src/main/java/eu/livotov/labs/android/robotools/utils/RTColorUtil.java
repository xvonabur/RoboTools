package eu.livotov.labs.android.robotools.utils;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.widget.ImageView;

/**
 * Created by dlivotov on 22/10/2016.
 */

public class RTColorUtil {
    public static ColorStateList createColorStateList(Context ctx, @ColorRes int color) {
        int[][] states = new int[][]{new int[]{android.R.attr.state_enabled}, // enabled
                new int[]{-android.R.attr.state_enabled}, // disabled
                new int[]{-android.R.attr.state_checked}, // unchecked
                new int[]{android.R.attr.state_pressed}  // pressed
        };

        int[] colors = new int[]{ctx.getResources().getColor(color), ctx.getResources().getColor(color), ctx.getResources().getColor(color), ctx.getResources().getColor(color)};

        return new ColorStateList(states, colors);
    }

    public static void tintIcon(ImageView icon, @ColorRes int colorRes) {
        if (Build.VERSION.SDK_INT >= 21) {
            tintIcon21(icon, colorRes);
        } else {
            tintIconCompat(icon, colorRes);
        }
    }

    @TargetApi(21)
    private static void tintIcon21(ImageView icon, int colorRes) {
        icon.setImageTintList(ColorStateList.valueOf(icon.getContext().getResources().getColor(colorRes)));
    }

    private static void tintIconCompat(ImageView icon, @ColorRes int colorRes) {
        icon.setColorFilter(icon.getContext().getResources().getColor(colorRes));
    }

    public static Drawable tintDrawable(@DrawableRes int drawableRes, int color, Resources resources) {
        Drawable original = resources.getDrawable(drawableRes);
        if (original != null)
            original.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
        return original;
    }

    public static int addColorTransparency(int color) {
        return (color & 0x00FFFFFF) | 0x40000000;
    }
}