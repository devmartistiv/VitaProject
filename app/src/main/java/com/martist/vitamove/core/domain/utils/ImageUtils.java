package com.martist.vitamove.core.domain.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;


public class ImageUtils {

    private static final String TAG = "ImageUtils";


    public static boolean saveImageToInternalStorage(Context context, Uri imageUri, String fileName) {
        try {

            InputStream inputStream = context.getContentResolver().openInputStream(imageUri);
            if (inputStream == null) {
                Log.e(TAG, "Не удалось открыть входной поток для Uri: " + imageUri);
                return false;
            }


            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            inputStream.close();


            File imageFile = new File(context.getFilesDir(), fileName);
            FileOutputStream fos = new FileOutputStream(imageFile);


            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
            fos.close();

            Log.d(TAG, "Изображение успешно сохранено в: " + imageFile.getAbsolutePath());
            return true;

        } catch (IOException e) {
            Log.e(TAG, "Ошибка при сохранении изображения: " + e.getMessage(), e);
            return false;
        }
    }


    public static Bitmap loadImageFromInternalStorage(Context context, String fileName) {
        try {
            File imageFile = new File(context.getFilesDir(), fileName);
            if (!imageFile.exists()) {
                Log.d(TAG, "Файл изображения не существует: " + fileName);
                return null;
            }

            return BitmapFactory.decodeFile(imageFile.getAbsolutePath());
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при загрузке изображения: " + e.getMessage(), e);
            return null;
        }
    }


    public static boolean imageExists(Context context, String fileName) {
        File imageFile = new File(context.getFilesDir(), fileName);
        return imageFile.exists();
    }


    public static boolean deleteImage(Context context, String fileName) {
        File imageFile = new File(context.getFilesDir(), fileName);
        if (imageFile.exists()) {
            boolean deleted = imageFile.delete();
            if (deleted) {
                Log.d(TAG, "Файл изображения успешно удален: " + fileName);
            } else {
                Log.e(TAG, "Не удалось удалить файл изображения: " + fileName);
            }
            return deleted;
        }
        return false;
    }
} 