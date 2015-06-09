package com.fab;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.media.ExifInterface;
import android.os.Environment;
import android.util.Log;

public class CompressBitmap implements Constant{
	private String path;
	private String imageFileName;

	public CompressBitmap(String path, String mobile){
		this.path = path;
		this.imageFileName = mobile;
	}

	public ByteArrayOutputStream getBitmap(){
		ByteArrayOutputStream outStream = new ByteArrayOutputStream();
		getscaledImage(path).compress(Bitmap.CompressFormat.JPEG, 100, outStream);
		return outStream;
	}

	public ByteArrayOutputStream getBitmap(int quality){
		ByteArrayOutputStream outStream = new ByteArrayOutputStream();
		getscaledImage(path).compress(Bitmap.CompressFormat.JPEG, quality, outStream);  
		return outStream;
	}

	public String getComressFile(){
		FileOutputStream out = null;
		String filename = createImageFile();
		try {
			out = new FileOutputStream(filename);
			getscaledImage(path).compress(Bitmap.CompressFormat.JPEG, 70, out);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		File file = new File(filename);
		Log.e("Size of Image","Length"+file.length());
		return filename;
	}

	private Bitmap getscaledImage(String filePath){
		Bitmap scaledBitmap = null;

		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;                      
		Bitmap bmp = BitmapFactory.decodeFile(filePath,options);

		int actualHeight = options.outHeight;
		int actualWidth = options.outWidth;
		float maxHeight = 800.0f;
		float maxWidth = 600.0f;
		float imgRatio = actualWidth / actualHeight;
		float maxRatio = maxWidth / maxHeight;

		Log.v("Pictures", "Before scaling Width and height are " + actualWidth + "--" + actualHeight);

		if (actualHeight > maxHeight || actualWidth > maxWidth) {
			if (imgRatio < maxRatio) {
				imgRatio = maxHeight / actualHeight;
				actualWidth = (int) (imgRatio * actualWidth);
				actualHeight = (int) maxHeight;
			} else if (imgRatio > maxRatio) {
				imgRatio = maxWidth / actualWidth;
				actualHeight = (int) (imgRatio * actualHeight);
				actualWidth = (int) maxWidth;
			} else {
				actualHeight = (int) maxHeight;
				actualWidth = (int) maxWidth;     
			}
		}

		options.inSampleSize = calculateInSampleSize(options, actualWidth, actualHeight);
		options.inJustDecodeBounds = false;
		options.inDither = false;
		options.inPurgeable = true;
		options.inInputShareable = true;
		options.inTempStorage = new byte[16*1024];
		options.inPreferQualityOverSpeed = true;

		try{    
			bmp = BitmapFactory.decodeFile(filePath,options);
		}
		catch(OutOfMemoryError exception){
			exception.printStackTrace();

		}
		try{
			scaledBitmap = Bitmap.createBitmap(actualWidth, actualHeight, Bitmap.Config.ARGB_8888);
		}
		catch(OutOfMemoryError exception){
			exception.printStackTrace();
		}

		float ratioX = actualWidth / (float) options.outWidth;
		float ratioY = actualHeight / (float)options.outHeight;
		float middleX = actualWidth / 2.0f;
		float middleY = actualHeight / 2.0f;

		Matrix scaleMatrix = new Matrix();
		scaleMatrix.setScale(ratioX, ratioY, middleX, middleY);

		Canvas canvas = new Canvas(scaledBitmap);
		canvas.setMatrix(scaleMatrix);
		canvas.drawBitmap(bmp, middleX - bmp.getWidth()/2, middleY - bmp.getHeight() / 2, new Paint(Paint.FILTER_BITMAP_FLAG));

		Log.v("Pictures", "After scaling Width and height are " + scaledBitmap.getWidth() + "--" + scaledBitmap.getHeight());

		ExifInterface exif;
		try {
			exif = new ExifInterface(filePath);

			int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 0);
			Log.d("EXIF", "Exif: " + orientation);
			Matrix matrix = new Matrix();
			if (orientation == 6) {
				matrix.postRotate(90);
				Log.d("EXIF", "Exif: " + orientation);
			} else if (orientation == 3) {
				matrix.postRotate(180);
				Log.d("EXIF", "Exif: " + orientation);
			} else if (orientation == 8) {
				matrix.postRotate(270);
				Log.d("EXIF", "Exif: " + orientation);
			}
			scaledBitmap = Bitmap.createBitmap(scaledBitmap, 0, 0,scaledBitmap.getWidth(), scaledBitmap.getHeight(), matrix, true);
		} catch (IOException e) {
			e.printStackTrace();
		}

		return scaledBitmap;
	}

	private String createImageFile(){
		File storageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),IMAGE_DIRECTORY_NAME);
		File image;
		image = new File(storageDir.getPath() + File.separator + imageFileName + ".jpg");
		Log.d("compressed", image.getAbsolutePath());
		return image.getAbsolutePath();
		/* try {
                image = File.createTempFile(
                    imageFileName,
                    ".jpg",
                    storageDir
                );
                
                return image.getAbsolutePath();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;*/
	}

	private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
		final int height = options.outHeight;
		final int width = options.outWidth;
		int inSampleSize = 1;

		if (height > reqHeight || width > reqWidth) {
			final int heightRatio = Math.round((float) height / (float) reqHeight);
			final int widthRatio = Math.round((float) width / (float) reqWidth);
			inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
		}
		final float totalPixels = width * height;
		final float totalReqPixelsCap = reqWidth * reqHeight * 2;

		while (totalPixels / (inSampleSize * inSampleSize) > totalReqPixelsCap) {
			inSampleSize++;
		}
		return inSampleSize;
	}
}

