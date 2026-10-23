package com.gathin.tvhome;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

/** Geometry-only mask. Keeps the original icon's colours and allocates no bitmap. */
final class ThemedIcon extends Drawable {
    private final Drawable source;
    private final String shape;
    private final Path mask = new Path();

    ThemedIcon(Drawable source, String shape) {
        this.source = source;
        this.shape = shape;
    }

    @Override protected void onBoundsChange(Rect bounds) {
        source.setBounds(bounds);
        mask.reset();
        RectF rect = new RectF(bounds);
        if ("circle".equals(shape)) {
            float radius = Math.min(rect.width(), rect.height()) / 2f;
            mask.addCircle(rect.centerX(), rect.centerY(), radius, Path.Direction.CW);
        } else if ("squircle".equals(shape)) {
            float x = rect.left, y = rect.top, w = rect.width(), h = rect.height();
            mask.moveTo(x + w / 2, y);
            mask.cubicTo(x + w, y, x + w, y, x + w, y + h / 2);
            mask.cubicTo(x + w, y + h, x + w, y + h, x + w / 2, y + h);
            mask.cubicTo(x, y + h, x, y + h, x, y + h / 2);
            mask.cubicTo(x, y, x, y, x + w / 2, y);
            mask.close();
        } else {
            float radius = "square".equals(shape) ? 0 : Math.min(rect.width(), rect.height()) * 0.18f;
            mask.addRoundRect(rect, radius, radius, Path.Direction.CW);
        }
    }

    @Override public void draw(Canvas canvas) {
        int saved = canvas.save();
        canvas.clipPath(mask);
        source.draw(canvas);
        canvas.restoreToCount(saved);
    }
    @Override public void setAlpha(int alpha) { source.setAlpha(alpha); }
    @Override public void setColorFilter(ColorFilter filter) { /* Third-party logos are never tinted. */ }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
    @Override public int getIntrinsicWidth() { return source.getIntrinsicWidth(); }
    @Override public int getIntrinsicHeight() { return source.getIntrinsicHeight(); }

    static Drawable wrap(Drawable icon, ThemeStyle style) {
        return icon == null || icon instanceof ThemedIcon ? icon : new ThemedIcon(icon, style.palette.iconShape);
    }
}
