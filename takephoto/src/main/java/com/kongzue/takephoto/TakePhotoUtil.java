package com.kongzue.takephoto;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.kongzue.takephoto.util.FileUtils;
import com.kongzue.takephoto.util.imagechooser.api.ChooserType;
import com.kongzue.takephoto.util.imagechooser.api.ChosenImage;
import com.kongzue.takephoto.util.imagechooser.api.ChosenImages;
import com.kongzue.takephoto.util.imagechooser.api.ImageChooserListener;
import com.kongzue.takephoto.util.imagechooser.api.ImageChooserManager;
import com.zxy.tiny.Tiny;
import com.zxy.tiny.callback.FileCallback;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static android.app.Activity.RESULT_OK;

public class TakePhotoUtil {

    public static final int DEFAULT_SIZE = 800;         //图片最大体积（KB）
    public static final int DEFAULT_QUALITY = 80;       //图片质量

    private String[] permissions;
    private AppCompatActivity context;
    private int REQUEST_CODE_PERMISSION = 0x00099;

    private int chooserType;
    private static final int CODE_TAKE_PICTURE = 99;
    private ImageChooserManager imageChooserManager;
    private String filePath;
    private String originalFilePath;

    private static TakePhotoUtil takePhotoUtil;

    private TakePhotoUtil() {
    }

    public static TakePhotoUtil getInstance(AppCompatActivity appCompatActivity) {
        synchronized (TakePhotoUtil.class) {
            if (takePhotoUtil == null) takePhotoUtil = new TakePhotoUtil();
            takePhotoUtil.context = appCompatActivity;
            takePhotoUtil.checkPermissions();
            return takePhotoUtil;
        }
    }

    private void checkPermissions() {
        takePhotoUtil.permissions = new String[]{
                Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
        };
    }

    private ImageChooserListener imageChooserListener = new ImageChooserListener() {
        @Override
        public void onImageChosen(final ChosenImage image) {
            context.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    chooseImage(image);
                }
            });
        }

        @Override
        public void onError(String reason) {
            context.runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    log("获取图片失败");
                }
            });
        }

        @Override
        public void onImagesChosen(final ChosenImages images) {
            context.runOnUiThread(new Runnable() {
                @Override
                public void run() {

                }
            });
        }
    };

    private void chooseImage(final ChosenImage image) {
        context.runOnUiThread(new Runnable() {

            @Override
            public void run() {
                originalFilePath = image.getFilePathOriginal();
                if (image != null) {
                    Log.i("选择图像", "Chosen Image:" + originalFilePath);

                    Tiny.FileCompressOptions options = new Tiny.FileCompressOptions();
                    options.quality = DEFAULT_QUALITY;
                    options.size = DEFAULT_SIZE;
                    Tiny.getInstance().source(originalFilePath).asFile().withOptions(options).compress(new FileCallback() {
                        @Override
                        public void callback(boolean isSuccess, String outfile, Throwable t) {
                            if (isSuccess) {
                                log("outfile:" + outfile);
                                if (returnPhoto != null)
                                    returnPhoto.onGetPhoto(outfile, getBitmapFromUri(outfile));

                            } else {
                                if (returnPhoto != null) returnPhoto.onError(new Exception(t));
                            }
                        }
                    });
                } else {
                    Log.i("未选择图像", "Chosen Image: Is null");
                }
            }
        });
    }

    private void log(final Object obj) {
        try {
            context.runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    if (BuildConfig.DEBUG) {
                        Log.i(">>>", obj.toString());
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Bitmap getBitmapFromUri(String outfile) {
        try {
            outfile = outfile.replace("/storage/emulated/0/", "file:///sdcard/");
            Uri uri = Uri.parse(outfile);
            // 读取uri所在的图片
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(context.getContentResolver(), uri);
            return bitmap;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    //拍照
    private File mTmpFile;

    public void doOpenCamera() {
        if (checkPermissions(permissions)) {
            try {
                mTmpFile = FileUtils.createTmpFile(context);
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (mTmpFile != null && mTmpFile.exists()) {
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if (android.os.Build.VERSION.SDK_INT < 24) {
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(mTmpFile));
                    context.startActivityForResult(intent, CODE_TAKE_PICTURE);
                } else {
                    ContentValues contentValues = new ContentValues(1);
                    contentValues.put(MediaStore.Images.Media.DATA, mTmpFile.getAbsolutePath());
                    Uri uri = context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
                    context.startActivityForResult(intent, CODE_TAKE_PICTURE);
                }
            } else {
                log("doOpenCamera：无法创建照片文件，请检查权限设置");
            }
        } else {
            requestPermission(permissions, 0x0001);
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case CODE_TAKE_PICTURE:
                log("gfsgs");
                log("resultCode:" + resultCode);
                if (resultCode == RESULT_OK) {
//                    log(">>>拍照：" + imageUri);
                    try {
                        Tiny.FileCompressOptions options = new Tiny.FileCompressOptions();
                        options.quality = DEFAULT_QUALITY;
                        options.size = DEFAULT_SIZE;
                        Tiny.getInstance().source(mTmpFile).asFile().withOptions(options).compress(new FileCallback() {
                            @Override
                            public void callback(boolean isSuccess, String outfile, Throwable t) {
                                //return the compressed file path
                                if (isSuccess) {
                                    log("outfile:" + outfile);
                                    if (returnPhoto != null)
                                        returnPhoto.onGetPhoto(outfile, getBitmapFromUri(outfile));
                                } else {
                                    if (returnPhoto != null) returnPhoto.onError(new Exception(t));
                                }
                            }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                break;
            case ChooserType.REQUEST_PICK_PICTURE:
                if (imageChooserManager == null) {
                    reinitializeImageChooser();
                }
                imageChooserManager.submit(requestCode, data);
                break;
            case ChooserType.REQUEST_CAPTURE_PICTURE:
                if (imageChooserManager == null) {
                    reinitializeImageChooser();
                }
                imageChooserManager.submit(requestCode, data);
                break;
        }
    }

    private void reinitializeImageChooser() {
        imageChooserManager = new ImageChooserManager(context, chooserType, true);
        Bundle bundle = new Bundle();
        bundle.putBoolean(Intent.EXTRA_ALLOW_MULTIPLE, true);
        imageChooserManager.setExtras(bundle);
        imageChooserManager.setImageChooserListener(imageChooserListener);
        imageChooserManager.reinitialize(filePath);
    }

    //相册
    public void doOpenGallery() {
        if (checkPermissions(permissions)) {
            chooserType = ChooserType.REQUEST_PICK_PICTURE;
            imageChooserManager = new ImageChooserManager(context,
                    ChooserType.REQUEST_PICK_PICTURE, true);
            Bundle bundle = new Bundle();
            bundle.putBoolean(Intent.EXTRA_ALLOW_MULTIPLE, false);
            imageChooserManager.setExtras(bundle);
            imageChooserManager.setImageChooserListener(imageChooserListener);
            imageChooserManager.clearOldFiles();
            try {
                filePath = imageChooserManager.choose();
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            requestPermission(permissions, 0x0001);
        }
    }

    //权限处理
    public boolean checkPermissions(String[] permissions) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }

        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                log("未获取权限：" + permission );
                return false;
            }
        }
        return true;
    }

    public void requestPermission(String[] permissions, int requestCode) {
        this.REQUEST_CODE_PERMISSION = requestCode;
        if (checkPermissions(permissions)) {
            permissionSuccess(REQUEST_CODE_PERMISSION);
        } else {
            List<String> needPermissions = getDeniedPermissions(permissions);
            ActivityCompat.requestPermissions(context, needPermissions.toArray(new String[needPermissions.size()]), REQUEST_CODE_PERMISSION);
        }
    }

    public void permissionSuccess(int requestCode) {
        Log.d(">>>", "获取权限成功=" + requestCode);
    }

    private List<String> getDeniedPermissions(String[] permissions) {
        List<String> needRequestPermissionList = new ArrayList<>();
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(context, permission) !=
                    PackageManager.PERMISSION_GRANTED ||
                    ActivityCompat.shouldShowRequestPermissionRationale(context, permission)) {
                needRequestPermissionList.add(permission);
            }
        }
        return needRequestPermissionList;
    }

    //接口
    private ReturnPhoto returnPhoto;

    public ReturnPhoto getReturnPhoto() {
        return returnPhoto;
    }

    public TakePhotoUtil setReturnPhoto(ReturnPhoto returnPhoto) {
        this.returnPhoto = returnPhoto;
        return this;
    }

    public interface ReturnPhoto {
        void onGetPhoto(String path, Bitmap bitmap);

        void onError(Exception e);
    }

}
