package com.meowdoki.app;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.widget.RemoteViews;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * The four MEOWDOKI home-screen widgets (design: "MEOWDOKI Android Widgets" canvas).
 * Each widget is drawn into one picture at its design size, so it looks exactly like the design;
 * tapping it opens the game.
 */
public final class Widgets {
    private Widgets() { }

    public static class Streak extends AppWidgetProvider {
        @Override public void onUpdate(Context c, AppWidgetManager m, int[] ids) { update(c, m, ids, 0); }
    }

    public static class Play extends AppWidgetProvider {
        @Override public void onUpdate(Context c, AppWidgetManager m, int[] ids) { update(c, m, ids, 1); }
    }

    public static class Daily extends AppWidgetProvider {
        @Override public void onUpdate(Context c, AppWidgetManager m, int[] ids) { update(c, m, ids, 2); }
    }

    public static class Sleep extends AppWidgetProvider {
        @Override public void onUpdate(Context c, AppWidgetManager m, int[] ids) { update(c, m, ids, 3); }
    }

    private static final Class<?>[] PROVIDERS = {Streak.class, Play.class, Daily.class, Sleep.class};

    /** Redraws every placed MEOWDOKI widget (called when the game goes to the background). */
    static void updateAll(Context c) {
        AppWidgetManager m = AppWidgetManager.getInstance(c);
        for (int k = 0; k < PROVIDERS.length; k++) {
            try {
                int[] ids = m.getAppWidgetIds(new ComponentName(c, PROVIDERS[k]));
                if (ids != null && ids.length > 0) update(c, m, ids, k);
            } catch (Throwable ignored) { }
        }
    }

    private static void update(Context c, AppWidgetManager m, int[] ids, int kind) {
        WidgetData d = WidgetData.load(c);
        RemoteViews v = new RemoteViews(c.getPackageName(), R.layout.widget_image);
        try {
            v.setImageViewBitmap(R.id.widget_image, draw(c, kind, d));
        } catch (Throwable t) {
            v.setImageViewResource(R.id.widget_image, R.mipmap.ic_launcher);
        }
        v.setContentDescription(R.id.widget_image, description(kind, d));
        Intent open = new Intent(c, MainActivity.class).setAction(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        PendingIntent pi = PendingIntent.getActivity(c, kind, open, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        v.setOnClickPendingIntent(R.id.widget_root, pi);
        for (int id : ids) m.updateAppWidget(id, v);
    }

    private static String description(int kind, WidgetData d) {
        boolean de = d.german;
        switch (kind) {
            case 0: return de ? "MEOWDOKI: " + d.streak + " Tage in Folge, heute " + d.today + " von 3 Rätseln gelöst"
                    : "MEOWDOKI: " + d.streak + "-day streak, today " + d.today + " of 3 puzzles solved";
            case 1: return de ? "MEOWDOKI: Level " + d.level + " spielen" : "MEOWDOKI: play level " + d.level;
            case 2: return de ? "MEOWDOKI Tagesrätsel" : "MEOWDOKI Daily Challenge";
            default: return de ? "MEOWDOKI: ruhige Rätsel vor dem Schlafen" : "MEOWDOKI: cozy puzzles before bed";
        }
    }

    // ---------- drawing (all numbers are CSS px of the design = dp) ----------

    private static final int[] DESIGN_W = {358, 170, 170, 358};
    private static final int DESIGN_H = 170;
    private static Typeface black, extraBold;

    private static Bitmap draw(Context c, int kind, WidgetData d) {
        float s = Math.min(c.getResources().getDisplayMetrics().density, 2.5f);
        int w = DESIGN_W[kind];
        Bitmap art = Bitmap.createBitmap(Math.round(w * s), Math.round(DESIGN_H * s), Bitmap.Config.ARGB_8888);
        Canvas cv = new Canvas(art);
        cv.scale(s, s);
        Pen p = new Pen(c, cv);
        if (kind == 0) streak(p, d);
        else if (kind == 1) play(p, d);
        else if (kind == 2) daily(p, d);
        else sleep(p, d);
        // round the corners (28 px), anti-aliased
        Bitmap out = Bitmap.createBitmap(art.getWidth(), art.getHeight(), Bitmap.Config.ARGB_8888);
        Paint mask = new Paint(Paint.ANTI_ALIAS_FLAG);
        mask.setShader(new BitmapShader(art, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP));
        new Canvas(out).drawRoundRect(new RectF(0, 0, out.getWidth(), out.getHeight()), 28 * s, 28 * s, mask);
        art.recycle();
        return out;
    }

    private static void streak(Pen p, WidgetData d) {
        boolean de = d.german;
        p.cream(358, 0xFFFFF8EC, 0xFFFEEFD8);
        // flame panel
        p.vgrad(14, 14, 112, 142, 22, 0xFFFFE6C9, 0xFFFFCF9C, 0x73DE9650, 3);
        p.icon(R.drawable.wg_flame, 38, 34, 64, 76, 0);
        p.textShadow(String.valueOf(d.streak), 70, 98, 28, true, Color.WHITE, 0x73AA3C0A, Paint.Align.CENTER, 60);
        p.fit(de ? (d.streak == 1 ? "Tag in Folge" : "Tage in Folge") : "day streak", 70, 131, 15, true, 0xFF8A3F12, Paint.Align.CENTER, 104);
        // today's goal
        p.text(de ? "Heutiges Ziel" : "Today's goal", 142, 35, 17, true, 0xFF2B1A14, Paint.Align.LEFT);
        boolean all = d.today >= WidgetData.GOAL;
        String goal = all ? (de ? "Geschafft!" : "Done!") : d.today + (de ? " von 3" : " of 3");
        p.text(goal, 342, 35, 14, false, 0xFFD9611A, Paint.Align.RIGHT);
        float cell = (204 - 16) / 3f;
        for (int k = 0; k < 3; k++) {
            float x = 140 + k * (cell + 8);
            if (k < d.today) {
                p.vgrad(x, 55, cell, 42, 14, 0xFFFF9A47, 0xFFF4701F, 0xFFDD5F17, 3);
                p.icon(R.drawable.wg_paw, x + cell / 2 - 12, 64, 24, 24, Color.WHITE);
            } else {
                p.round(x, 55, cell, 42, 14, 0xFFEEDFCD);
                p.round(x + 2, 57, cell - 4, 38, 12, 0xFFFBF3E9);
                p.icon(R.drawable.wg_paw, x + cell / 2 - 12, 64, 24, 24, 0xFFE6D3BF);
            }
        }
        p.button(140, 110, 204, 46, all ? (de ? "Weiterspielen" : "Play more") : (de ? "Jetzt spielen" : "Play now"), 15, 18, 9, 19);
    }

    private static void play(Pen p, WidgetData d) {
        Paint bg = new Paint(Paint.ANTI_ALIAS_FLAG);
        bg.setShader(new RadialGradient(85, 57.8f, 140.8f, new int[]{0xFFFFFBF3, 0xFFFFE9CC, 0xFFFDD6A6}, new float[]{0, 0.58f, 1}, Shader.TileMode.CLAMP));
        p.cv.drawRect(0, 0, 170, 170, bg);
        p.bottomInset(170, 0x8CE8B478);
        p.iconRotated(R.drawable.wg_paw, 14, 16, 22, -20, 0xFFF2B98A, 0.55f);
        p.iconRotated(R.drawable.wg_paw, 138, 50, 16, 18, 0xFFF2B98A, 0.5f);
        // the player's cat (face sprite: 14 faces of 240 x 210)
        Bitmap faces = p.bitmap(R.drawable.wg_faces);
        if (faces != null) {
            int fw = faces.getWidth() / WidgetData.SKIN_COUNT;
            Rect src = new Rect(d.skin * fw, 0, (d.skin + 1) * fw, faces.getHeight());
            p.shadowBlob(85, 100, 46, 7);
            p.cv.drawBitmap(faces, src, new RectF(31, 12, 139, 12 + 108f * faces.getHeight() / fw), p.bmp);
        }
        p.button(12, 112, 146, 46, d.german ? "Spielen" : "Play", 17, 20, 10, 22);
        // level badge
        String badge = "Level " + d.level;
        Paint t = p.font(13, true, 0xFFE0681A);
        float bw = t.measureText(badge) + 18;
        p.round(170 - 8 - bw, 101, bw, 24, 12, 0x40CF5E1A);
        p.round(170 - 8 - bw, 100, bw, 23, 12, 0xFFFFF8EE);
        p.text(badge, 170 - 8 - bw / 2, 116, 13, true, 0xFFE0681A, Paint.Align.CENTER);
    }

    private static final String[] MONTHS_EN = {"January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"};
    private static final String[] MONTHS_DE = {"Januar", "Februar", "März", "April", "Mai", "Juni", "Juli", "August", "September", "Oktober", "November", "Dezember"};

    private static void daily(Pen p, WidgetData d) {
        boolean de = d.german;
        p.cream(170, 0xFFFFF8EC, 0xFFFEF0DA);
        p.calendar(14, 14);
        p.text(de ? "Tages-" : "Daily", 63, 32, 16, true, 0xFF2B1A14, Paint.Align.LEFT);
        p.text(de ? "rätsel" : "Challenge", 63, 49.5f, 16, true, 0xFF2B1A14, Paint.Align.LEFT);
        Calendar now = Calendar.getInstance();
        String day = String.valueOf(now.get(Calendar.DAY_OF_MONTH));
        p.text(day, 16, 97, 30, true, 0xFFE0681A, Paint.Align.LEFT);
        float dw = p.font(30, true, 0).measureText(day);
        p.fit((de ? MONTHS_DE : MONTHS_EN)[now.get(Calendar.MONTH)], 16 + dw + 6, 97, 15, false, 0xFF6A5852, Paint.Align.LEFT, 150 - dw - 6);
        String label = d.dailyDone ? (de ? "Gelöst!" : "Solved!") : (de ? "Heute spielen" : "Play today");
        p.button(12, 114, 146, 44, label, 13, 16, 8, 18);
    }

    private static void sleep(Pen p, WidgetData d) {
        boolean de = d.german;
        p.round(0, 0, 358, 170, 0, 0xFF2E2C5E);
        Bitmap night = p.bitmap(R.drawable.wg_night);
        if (night != null) p.cv.drawBitmap(night, null, new RectF(0, 0, 358, 170), p.bmp);
        Paint shade = new Paint();
        shade.setShader(new LinearGradient(0, 0, 358, 0, new int[]{0xE01E1B48, 0x9E1E1B48, 0x001E1B48}, new float[]{0, 0.5f, 0.82f}, Shader.TileMode.CLAMP));
        p.cv.drawRect(0, 0, 358, 170, shade);
        // moon with a soft glow, two little stars
        p.glow(321, 31, 24, 0x80FFE296);
        p.icon(R.drawable.wg_moon, 306, 16, 30, 30, 0);
        p.icon(R.drawable.wg_sparkle, 262, 24, 10, 10, Color.WHITE);
        p.icon(R.drawable.wg_sparkle, 232, 46, 7, 7, 0xFFFFE29A);
        // sleeping cat on a warm glow
        Paint warm = new Paint(Paint.ANTI_ALIAS_FLAG);
        warm.setShader(new RadialGradient(0, 0, 1, new int[]{0x59FFC88C, 0x00FFC88C}, new float[]{0, 0.7f}, Shader.TileMode.CLAMP));
        p.cv.save();
        p.cv.translate(271, 155);
        p.cv.scale(75, 15);
        p.cv.drawCircle(0, 0, 1, warm);
        p.cv.restore();
        Bitmap cat = p.bitmap(R.drawable.wg_sleepcat);
        if (cat != null) p.cv.drawBitmap(cat, null, new RectF(196, 74, 348, 164), p.bmp);
        p.text("z", 214, 73, 13, true, 0xFFFFE7B8, Paint.Align.LEFT);
        p.text("z", 204, 60, 16, true, 0xFFFFE7B8, Paint.Align.LEFT);
        p.text("Z", 192, 44, 20, true, 0xFFFFE7B8, Paint.Align.LEFT);
        // texts
        Paint kick = p.font(11, true, 0xFFFFC98A);
        kick.setLetterSpacing(1.6f / 11);
        p.cv.drawText(de ? "VOR DEM SCHLAFEN" : "BEFORE BED", 18, 27, kick);
        List<String> title = p.wrap(de ? "3 Rätsel zum Abschalten" : "3 cozy puzzles to unwind", 21, 172);
        float y = 53;
        for (int i = 0; i < Math.min(2, title.size()); i++, y += 23.5f) p.fit(title.get(i), 18, y, 21, true, Color.WHITE, Paint.Align.LEFT, 172);
        p.fit(de ? "Ein ruhiger Ausklang des Tages." : "A calm way to end the day.", 18, y + 0.5f, 13, false, 0xFFDAD6F4, Paint.Align.LEFT, 172);
        String start = "Start";
        float tw = p.font(19, true, 0).measureText(start);
        p.button(18, 114, 18 + 14 + 9 + tw + 22, 42, start, 14, 17, 9, 19);
    }

    /** Small drawing toolkit on top of a Canvas, in design px. */
    private static final class Pen {
        final Context ctx;
        final Canvas cv;
        final Paint bmp = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);

        Pen(Context ctx, Canvas cv) {
            this.ctx = ctx;
            this.cv = cv;
            loadFonts(ctx);
        }

        Paint font(float size, boolean heavy, int color) {
            Paint t = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
            t.setTypeface(heavy ? black : extraBold);
            t.setTextSize(size);
            t.setColor(color);
            return t;
        }

        void text(String s, float x, float y, float size, boolean heavy, int color, Paint.Align align) {
            Paint t = font(size, heavy, color);
            t.setTextAlign(align);
            cv.drawText(s, x, y, t);
        }

        /** Text that shrinks until it fits maxW. */
        void fit(String s, float x, float y, float size, boolean heavy, int color, Paint.Align align, float maxW) {
            Paint t = font(size, heavy, color);
            t.setTextAlign(align);
            while (size > 8 && t.measureText(s) > maxW) t.setTextSize(--size);
            cv.drawText(s, x, y, t);
        }

        void textShadow(String s, float x, float y, float size, boolean heavy, int color, int shadow, Paint.Align align, float maxW) {
            fit(s, x, y + 2, size, heavy, shadow, align, maxW);
            fit(s, x, y, size, heavy, color, align, maxW);
        }

        List<String> wrap(String s, float size, float maxW) {
            Paint t = font(size, true, 0);
            List<String> lines = new ArrayList<>();
            String line = "";
            for (String word : s.split(" ")) {
                String next = line.isEmpty() ? word : line + " " + word;
                if (!line.isEmpty() && t.measureText(next) > maxW) { lines.add(line); line = word; }
                else line = next;
            }
            if (!line.isEmpty()) lines.add(line);
            return lines;
        }

        void round(float x, float y, float w, float h, float r, int color) {
            Paint f = new Paint(Paint.ANTI_ALIAS_FLAG);
            f.setColor(color);
            cv.drawRoundRect(new RectF(x, y, x + w, y + h), r, r, f);
        }

        /** Rounded box with a top-to-bottom gradient and a darker inner band at the bottom (CSS inset shadow). */
        void vgrad(float x, float y, float w, float h, float r, int top, int bottom, int inset, float insetH) {
            Paint f = new Paint(Paint.ANTI_ALIAS_FLAG);
            f.setShader(new LinearGradient(0, y, 0, y + h, top, bottom, Shader.TileMode.CLAMP));
            RectF box = new RectF(x, y, x + w, y + h);
            cv.drawRoundRect(box, r, r, f);
            cv.save();
            cv.clipRect(x, y + h - insetH, x + w, y + h);
            Paint band = new Paint(Paint.ANTI_ALIAS_FLAG);
            band.setColor(inset);
            cv.drawRoundRect(box, r, r, band);
            cv.restore();
        }

        void cream(float w, int top, int bottom) {
            Paint f = new Paint();
            f.setShader(new LinearGradient(0, 0, 0, 170, top, bottom, Shader.TileMode.CLAMP));
            cv.drawRect(0, 0, w, 170, f);
            bottomInset(w, 0x8CE8C496);
        }

        void bottomInset(float w, int color) {
            Paint f = new Paint();
            f.setColor(color);
            cv.drawRect(0, 166, w, 170, f);
        }

        /** The orange game button with a play triangle and a label, centred. */
        void button(float x, float y, float w, float h, String label, float iw, float ih, float gap, float size) {
            float r = h / 2;
            Paint glow = new Paint(Paint.ANTI_ALIAS_FLAG);
            glow.setColor(0x26CF5E1A);
            cv.drawRoundRect(new RectF(x, y + 3, x + w, y + h + 3), r, r, glow);
            Paint f = new Paint(Paint.ANTI_ALIAS_FLAG);
            f.setShader(new LinearGradient(0, y, 0, y + h, new int[]{0xFFFF9A47, 0xFFFA7E2C, 0xFFF4701F}, new float[]{0, 0.5f, 1}, Shader.TileMode.CLAMP));
            Paint dark = new Paint(Paint.ANTI_ALIAS_FLAG);
            dark.setColor(0xFFDD5F17);
            RectF box = new RectF(x, y, x + w, y + h);
            cv.drawRoundRect(box, r, r, dark);
            cv.save();
            cv.clipRect(x, y, x + w, y + h - 4);
            cv.drawRoundRect(box, r, r, f);
            cv.restore();
            Paint hi = new Paint(Paint.ANTI_ALIAS_FLAG);
            hi.setColor(0x42FFFFFF);
            cv.save();
            cv.clipRect(x, y, x + w, y + 2);
            cv.drawRoundRect(box, r, r, hi);
            cv.restore();
            Paint t = font(size, true, Color.WHITE);
            float maxText = w - 24 - iw - gap;
            while (size > 9 && t.measureText(label) > maxText) t.setTextSize(--size);
            float tw = t.measureText(label);
            float left = x + (w - (iw + gap + tw)) / 2;
            float mid = y + (h - 3) / 2;
            icon(R.drawable.wg_play, left, mid - ih / 2, iw, ih, Color.WHITE);
            Paint.FontMetrics fm = t.getFontMetrics();
            cv.drawText(label, left + iw + gap, mid - (fm.ascent + fm.descent) / 2, t);
        }

        void icon(int res, float x, float y, float w, float h, int tint) {
            Drawable dr = ctx.getResources().getDrawable(res, null);
            if (dr == null) return;
            dr = dr.mutate();
            if (tint != 0) dr.setTint(tint);
            cv.save();
            cv.translate(x, y);
            cv.scale(w / 100f, h / 100f);
            dr.setBounds(0, 0, 100, 100);
            dr.draw(cv);
            cv.restore();
        }

        void iconRotated(int res, float x, float y, float size, float deg, int tint, float alpha) {
            cv.save();
            cv.rotate(deg, x + size / 2, y + size / 2);
            int layer = cv.saveLayerAlpha(new RectF(x - 2, y - 2, x + size + 2, y + size + 2), Math.round(alpha * 255));
            icon(res, x, y, size, size, tint);
            cv.restoreToCount(layer);
            cv.restore();
        }

        void glow(float cx, float cy, float r, int color) {
            Paint g = new Paint(Paint.ANTI_ALIAS_FLAG);
            g.setShader(new RadialGradient(cx, cy, r, color, color & 0x00FFFFFF, Shader.TileMode.CLAMP));
            cv.drawCircle(cx, cy, r, g);
        }

        /** a soft oval shadow under the cat face */
        void shadowBlob(float cx, float cy, float rx, float ry) {
            Paint g = new Paint(Paint.ANTI_ALIAS_FLAG);
            g.setShader(new RadialGradient(0, 0, 1, 0x38B2601E, 0x00B2601E, Shader.TileMode.CLAMP));
            cv.save();
            cv.translate(cx, cy);
            cv.scale(rx, ry);
            cv.drawCircle(0, 0, 1, g);
            cv.restore();
        }

        Bitmap bitmap(int res) {
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inScaled = false;
            return BitmapFactory.decodeResource(ctx.getResources(), res, o);
        }

        /** the calendar icon of the design (viewBox 48 x 51, drawn at 40 x 43) */
        void calendar(float x, float y) {
            cv.save();
            cv.translate(x, y);
            cv.scale(40f / 48f, 43f / 51f);
            Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
            stroke.setStyle(Paint.Style.STROKE);
            stroke.setStrokeWidth(2.6f);
            stroke.setColor(0xFF7A4536);
            RectF page = new RectF(3, 8, 45, 48);
            round(3, 8, 42, 40, 7, 0xFFFFFDF8);
            cv.save();
            cv.clipRect(0, 0, 48, 20);
            round(3, 8, 42, 40, 7, 0xFFE8463D);
            cv.restore();
            cv.drawLine(3, 20, 45, 20, stroke);
            cv.drawRoundRect(page, 7, 7, stroke);
            round(12, 2, 5.5f, 11, 2.75f, 0xFF7A4536);
            round(30.5f, 2, 5.5f, 11, 2.75f, 0xFF7A4536);
            int red = 0xFFE8463D, amber = 0xFFF3A043;
            round(10, 25, 7.5f, 7, 1.6f, red);
            round(20.3f, 25, 7.5f, 7, 1.6f, amber);
            round(30.5f, 25, 7.5f, 7, 1.6f, red);
            round(10, 35, 7.5f, 7, 1.6f, amber);
            round(20.3f, 35, 7.5f, 7, 1.6f, red);
            cv.restore();
        }
    }

    private static synchronized void loadFonts(Context c) {
        if (black != null) return;
        black = fontFrom(c, R.raw.nunito_900, "nunito_900.ttf", Typeface.DEFAULT_BOLD);
        extraBold = fontFrom(c, R.raw.nunito_800, "nunito_800.ttf", Typeface.DEFAULT_BOLD);
    }

    private static Typeface fontFrom(Context c, int res, String name, Typeface fallback) {
        try {
            File f = new File(c.getCacheDir(), name);
            if (!f.exists() || f.length() == 0) {
                try (InputStream in = c.getResources().openRawResource(res); FileOutputStream out = new FileOutputStream(f)) {
                    byte[] b = new byte[8192];
                    int r;
                    while ((r = in.read(b)) > 0) out.write(b, 0, r);
                }
            }
            return Typeface.createFromFile(f);
        } catch (Throwable t) {
            return fallback;
        }
    }
}
