package com.kongzue.takephotodemo;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.kongzue.takephoto.TakePhotoUtil;

public class MainActivity extends AppCompatActivity {

    private Button btnTakePhoto;
    private ImageView imgPhoto;
    private TextView txtPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //initViews
        btnTakePhoto = findViewById(R.id.btnTakePhoto);
        imgPhoto = findViewById(R.id.imgPhoto);
        txtPath = findViewById(R.id.txtPath);

        //initDatas
        TakePhotoUtil.getInstance(MainActivity.this).setReturnPhoto(new TakePhotoUtil.ReturnPhoto() {
            @Override
            public void onGetPhoto(String path, Bitmap bitmap) {
                imgPhoto.setImageBitmap(bitmap);
                txtPath.setText(path);
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(MainActivity.this, "发生错误，请在Logcat查看详情", Toast.LENGTH_SHORT);
                e.printStackTrace();
            }
        });

        //setEvents
        btnTakePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showMenu();
            }
        });
    }

    private void showMenu() {
        String[] selectShunxuStr = new String[]{"使用相机拍摄", "从相册中选择", "删除"};
        new AlertDialog.Builder(this).setItems(selectShunxuStr, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0:
                        TakePhotoUtil.getInstance(MainActivity.this).doOpenCamera();
                        break;
                    case 1:
                        TakePhotoUtil.getInstance(MainActivity.this).doOpenGallery();
                        break;
                    case 2:
                        imgPhoto.setImageDrawable(null);
                        txtPath.setText("");
                        break;
                    default:
                        break;
                }
                dialog.dismiss();
            }
        }).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        TakePhotoUtil.getInstance(MainActivity.this).onActivityResult(requestCode, resultCode, data);
    }
}
