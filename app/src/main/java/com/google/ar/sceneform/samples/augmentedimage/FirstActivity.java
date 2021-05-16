package com.google.ar.sceneform.samples.augmentedimage;

import android.app.AlertDialog;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Intent;

import com.googlecode.tesseract.android.TessBaseAPI;
import android.content.Context;


import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.core.Rect;
import org.opencv.imgproc.Imgproc;
import android.util.Log;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class FirstActivity extends AppCompatActivity {

    public static Context context_first; // context 변수 선언
    public static final int sub = 1001;

    static {
        if (!OpenCVLoader.initDebug()) {
            Log.e("TAG", "OPENCV initialization error");
        } else {
            Log.i("TAG", "OPENCV initialization success");
        }
    }

    static {
        System.loadLibrary("native-lib");
    }

    TessBaseAPI tessBaseAPI;

    Button button;
    ImageView imageView;
    CameraSurfaceView surfaceView;
    TextView textView;
//    GlobalVariable global = (GlobalVariable) getApplication();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_first);
        context_first = this;//context class 지정(BookName 변수 위함)

        imageView = findViewById(R.id.imageView);
        surfaceView = findViewById(R.id.surfaceView);

        button = findViewById(R.id.button);
        button.setOnClickListener(view -> capture());

        tessBaseAPI = new TessBaseAPI();
        String dir = getFilesDir() + "/tesseract";
        if (checkLanguageFile(dir + "/tessdata"))
            tessBaseAPI.init(dir, "eng");
    }

    boolean checkLanguageFile(String dir) {
        File file = new File(dir);
        if (!file.exists() && file.mkdirs())
            createFiles(dir);
        else if (file.exists()) {
            String filePath = dir + "/eng.traineddata";
            File langDataFile = new File(filePath);
            if (!langDataFile.exists())
                createFiles(dir);
        }
        return true;
    }

    private void createFiles(String dir) {
        AssetManager assetMgr = this.getAssets();

        InputStream inputStream = null;
        OutputStream outputStream = null;

        try {
            inputStream = assetMgr.open("eng.traineddata");
            String destFile = dir + "/eng.traineddata";
            outputStream = new FileOutputStream(destFile);
            byte[] buffer = new byte[1024];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }
            inputStream.close();
            outputStream.flush();
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void capture() {
        surfaceView.capture((bytes, camera) -> {

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 8;

            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);//bitmap allocation

            bitmap = GetRotatedBitmap(bitmap, 90);//90도 회전
            bitmap = ROIcrop(bitmap);//Region of interest crop
            //bitmap = Thresholding(bitmap,128);//이진화 by threshold(빠른 속도를 원할때 사용)

            bitmap = GetBinaryBitmap(bitmap); //이진화 by color distance
            imageView.setImageBitmap(bitmap); //프리뷰토 전처리 거친 bitmap 생성
            new AsyncTess().execute(bitmap); //전처리 거친 bitmap input으로 텍스트 변환및 결과 알람

            camera.startPreview();//프리뷰에 송출
        });
    }

    public synchronized static Bitmap GetRotatedBitmap(Bitmap bitmap, int degrees) {
        if (degrees != 0 && bitmap != null) {
            Matrix m = new Matrix();
            m.setRotate(degrees, (float) bitmap.getWidth() / 2, (float) bitmap.getHeight() / 2);
            try {
                Bitmap b2 = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), m, true);
                if (bitmap != b2) {
                    bitmap = b2;
                }
            } catch (OutOfMemoryError ex) {
                ex.printStackTrace();
            }
        }
        return bitmap;
    }
    public String BookName;

    private class AsyncTess extends AsyncTask<Bitmap, Integer, String> {
        @Override
        protected String doInBackground(Bitmap... mRelativeParams) {
            tessBaseAPI.setImage(mRelativeParams[0]);
            return tessBaseAPI.getUTF8Text();
        }

        protected void onPostExecute(String result) {
            button.setEnabled(true);
            button.setText("텍스트 인식");

            BookName=result;//텍스트 인식 완료 후 이미지 이름으로 넣어줄 변수 지정
            alertshow();//알람 송출
        }
    }

    void alertshow() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("책 제목 확인");
        builder.setMessage("이 책이 맞습니까? : "+BookName);

        builder.setPositiveButton("예",
                (dialog, which) -> {
                    Intent intent = new Intent(getApplicationContext(), AugmentedImageActivity.class);
                    startActivityForResult(intent, sub);//예 누를 시 AugmentedImageActivity로 화면 전환
                });
        builder.setNegativeButton("아니오",
                (dialog, which) -> Toast.makeText(getApplicationContext(), "다시 인식시켜주세요.", Toast.LENGTH_LONG).show());

        builder.show();
    }

    private Bitmap ROIcrop(Bitmap bitmap) {
        Mat img = new Mat(bitmap.getHeight(), bitmap.getWidth(), CvType.CV_8U, new Scalar(4));
        Bitmap bitmapTemp = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        Utils.bitmapToMat(bitmapTemp, img);//Bitmap을 Mat으로 변환

        Mat roiMat;
        Rect rect = new Rect(10, 10, img.width() - 20, img.height() / 4);//ROI영역 지정

        roiMat = new Mat(img, rect); // public Mat(Mat m, Range rowRange, Range colRange
        Bitmap resultBitmap = Bitmap.createBitmap(roiMat.cols(), roiMat.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(roiMat, resultBitmap);
        return resultBitmap;
    }

    private Bitmap GetBinaryBitmap(Bitmap bitmap_src) {
        Bitmap bitmap_new=bitmap_src.copy(bitmap_src.getConfig(), true);
        for(int x=0; x<bitmap_new.getWidth(); x++) {
            for(int y=0; y<bitmap_new.getHeight(); y++) {
                int color=bitmap_new.getPixel(x, y);
                color=GetNewColor(color);
                bitmap_new.setPixel(x, y, color);
            }
        }
        return bitmap_new;
    }

    private int GetNewColor(int c) {
        double dwhite=GetColorDistance(c,Color.WHITE);
        double dblack=GetColorDistance(c,Color.BLACK);
        if(dwhite<=dblack)
            return Color.WHITE;
        else
            return Color.BLACK;
    }

    private double GetColorDistance(int c1, int c2) {
        int db= Color.blue(c1)-Color.blue(c2);
        int dg= Color.green(c1)-Color.green(c2);
        int dr= Color.red(c1)-Color.red(c2);
        double d=Math.sqrt(Math.pow(db, 2)+Math.pow(dg, 2)+Math.pow(dr, 2));
        return d;
    }

    private Bitmap Thresholding(Bitmap bitmap_, int threshold) {
        Mat imageMat = new Mat(bitmap_.getHeight(), bitmap_.getWidth(), CvType.CV_8U, new Scalar(4));
        Bitmap bitmapTemp = bitmap_.copy(Bitmap.Config.ARGB_8888, true);
        Utils.bitmapToMat(bitmapTemp, imageMat);

        Mat BinaryMat = new Mat(bitmap_.getHeight(), bitmap_.getWidth(), CvType.CV_8U, new Scalar(1));
        Imgproc.cvtColor(imageMat, BinaryMat, Imgproc.COLOR_RGB2GRAY);
        Imgproc.threshold(BinaryMat, BinaryMat, 120, 255, Imgproc.THRESH_BINARY);

        Bitmap resultBitmap = Bitmap.createBitmap(bitmap_.getWidth(), bitmap_.getHeight(), Bitmap.Config.ARGB_8888);
        BinaryMat.convertTo(BinaryMat, CvType.CV_8UC1);
        Utils.matToBitmap(BinaryMat, resultBitmap);
        return resultBitmap;
    }
}