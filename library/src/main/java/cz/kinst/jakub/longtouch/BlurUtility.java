package cz.kinst.jakub.longtouch;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Build;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.view.View;


public class BlurUtility {
	public static Bitmap blurBitmap(Bitmap source, Context context, float radius, int sampling) {
		if(Build.VERSION.SDK_INT < 17) return Bitmap.createScaledBitmap(source, 45, 45, true);

		int width = source.getWidth();
		int height = source.getHeight();
		int scaledWidth = width / sampling;
		int scaledHeight = height / sampling;

		Bitmap bitmap = Bitmap.createBitmap(scaledWidth, scaledHeight, Bitmap.Config.ARGB_8888);

		Canvas canvas = new Canvas(bitmap);
		canvas.scale(1 / (float) sampling, 1 / (float) sampling);
		Paint paint = new Paint();
		paint.setFlags(Paint.FILTER_BITMAP_FLAG);
		canvas.drawBitmap(source, 0, 0, paint);

		RenderScript rs = RenderScript.create(context);
		Allocation input = Allocation.createFromBitmap(rs, bitmap, Allocation.MipmapControl.MIPMAP_NONE,
				Allocation.USAGE_SCRIPT);
		Allocation output = Allocation.createTyped(rs, input.getType());
		ScriptIntrinsicBlur blur = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));

		blur.setInput(input);
		blur.setRadius(radius);
		blur.forEach(output);
		output.copyTo(bitmap);

		rs.destroy();

		return bitmap;
	}


	public static Bitmap getBlurredViewBitmap(View view, float radius, int sampling) {
		view.setDrawingCacheEnabled(true);
		Bitmap blurredBitmap = blurBitmap(view.getDrawingCache(), view.getContext(), radius, sampling);
		return blurredBitmap;
	}


	public static Bitmap getBlurredViewBitmap(View view, float radius) {
		return getBlurredViewBitmap(view, radius, 4);
	}
}