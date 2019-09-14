package karoldrwila.human_detection;

import android.Manifest;
import android.app.Activity;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import android.media.MediaMetadataRetriever;
import android.os.Bundle;

import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

import karoldrwila.human_detection.models.Classification;
import karoldrwila.human_detection.models.Classifier;
import karoldrwila.human_detection.models.TensorFlowClassifier;

public class MainActivity extends Activity implements View.OnClickListener
{
    private TextView resText;
    private Classifier classifier;

    final int MY_PERMISSION_READ_EXTERNAL_STORAGE = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.btn_1).setOnClickListener(this);
        findViewById(R.id.btn_2).setOnClickListener(this);
        findViewById(R.id.btn_3).setOnClickListener(this);
        findViewById(R.id.btn_4).setOnClickListener(this);
        findViewById(R.id.btn_5).setOnClickListener(this);

        resText = (TextView) findViewById(R.id.tfRes);

        new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                classifier = new TensorFlowClassifier(getAssets(), "TEST_MODEL", "model.pb", "conv2d_1_input", "dense_2/Sigmoid");
            }
        }).start();
    }

    @Override
    public void onClick(View view)
    {
        InputStream ims = null;
        Bitmap bitmap = null;
        Bitmap ivBitmap = null;
        ImageView imageView = (ImageView) findViewById(R.id.imageView);

        switch (view.getId())
        {
            case R.id.btn_1:
                try
                {
                    ims = getAssets().open("1.png");
                } catch (IOException e)
                {
                    e.printStackTrace();
                }

                ivBitmap = BitmapFactory.decodeStream(ims);
                bitmap = Bitmap.createBitmap(ivBitmap, 29, 26, 24, 48);
                break;

            case R.id.btn_2:
                try
                {
                    ims = getAssets().open("2.png");
                } catch (IOException e)
                {
                    e.printStackTrace();
                }

                ivBitmap = BitmapFactory.decodeStream(ims);
                bitmap = Bitmap.createBitmap(ivBitmap, 0, 0, 64, 128);
                break;

            case R.id.btn_3:
                try
                {
                    ims = getAssets().open("3.png");
                } catch (IOException e)
                {
                    e.printStackTrace();
                }

                ivBitmap = BitmapFactory.decodeStream(ims);
                bitmap = Bitmap.createBitmap(ivBitmap, 32, 64, 64, 128);
                break;

            case R.id.btn_4:
                analyzeBitmap(imageView);
                return;

            case R.id.btn_5:
                analyzeVideo();
                return;
        }

        analyzeFrame(imageView, ivBitmap, bitmap);
    }

    private Bitmap adjustedContrast(Bitmap src, double value)
    {
        int width = src.getWidth();
        int height = src.getHeight();

        Bitmap bmOut = Bitmap.createBitmap(width, height, src.getConfig());

        int A, R, G, B;
        int pixel;
        double contrast = Math.pow((100 + value) / 100, 2);

        for (int x = 0; x < width; ++x)
        {
            for (int y = 0; y < height; ++y)
            {
                pixel = src.getPixel(x, y);
                A = Color.alpha(pixel);
                R = range(bumpColorContrast(Color.red(pixel), contrast), 0, 255);
                G = range(bumpColorContrast(Color.green(pixel), contrast), 0, 255);
                B = range(bumpColorContrast(Color.blue(pixel), contrast), 0, 255);

                bmOut.setPixel(x, y, Color.argb(A, R, G, B));
            }
        }
        return bmOut;
    }

    public Bitmap drawRectToBitmap(Bitmap source,
                                   float x, float y, float width, float height, int color)
    {
        Canvas canvas = new Canvas(source);

        Paint p = new Paint();
        p.setStyle(Paint.Style.STROKE);
        p.setColor(color);

        canvas.drawRect(x, y, x + width, y + height, p);

        return source;
    }

    private Bitmap processBitmap(Bitmap source, int width, int height, int destWidth, int destHeight, int stepX, int stepY)
    {
        android.graphics.Bitmap.Config bitmapConfig = source.getConfig();
        Bitmap output = source.copy(bitmapConfig, true);

        for (int x = 0; x < source.getWidth() - width; x += stepX)
        {
            for (int y = 0; y < source.getHeight() - height; y += stepY)
            {
                Bitmap buffer;
                if (width != destWidth || height != destHeight)
                {
                    buffer = Bitmap.createBitmap(output, x, y, width, height);
                    buffer = Bitmap.createScaledBitmap(
                            buffer, destWidth, destHeight, false);
                } else
                    buffer = Bitmap.createBitmap(output, x, y, width, height);

                final Classification res = classifyFrame(buffer);

                if (res.getLabel() == null)
                    continue;

                if (res.getLabel().equals("HUMAN") && res.getConf() > 0.95)
                {
                    Log.d("MAINACTIVITY", "found " + x + " " + y);
                    output = drawRectToBitmap(output, x, y, width, height, Color.GREEN);
                }
            }
        }

        return output;
    }

    private void analyzeVideo()
    {
        File sdcard = Environment.getExternalStorageDirectory();
        File file = new File(sdcard, "test_ir_video.avi");

        if (!ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.READ_EXTERNAL_STORAGE))
        {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    MY_PERMISSION_READ_EXTERNAL_STORAGE);
        }

        MediaMetadataRetriever retriever = new MediaMetadataRetriever();

        class VideoAnalyzer implements Runnable
        {
            private MediaMetadataRetriever retriever;
            private File file;

            private VideoAnalyzer(File file, MediaMetadataRetriever retriever)
            {
                this.file = file;
                this.retriever = retriever;
            }

            @Override
            public void run()
            {
                retriever.setDataSource(file.getAbsolutePath());
                final ImageView imageView = (ImageView) findViewById(R.id.imageView);

                for (int i = 0; i < 160; i += 1)
                {
                    Bitmap bitmap = retriever.getFrameAtTime(i * 1000000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
                    final Bitmap bitmap_ = processBitmap(bitmap, 100, 200, 100, 100, 50, 100);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            imageView.setImageBitmap(bitmap_);
                        }
                    });
                }
            }
        }

        new Thread(new VideoAnalyzer(file, retriever)).start();
    }

    private void analyzeBitmap(ImageView imageView)
    {
        Bitmap ivBitmap;
        InputStream ims = null;

        try
        {
            ims = getAssets().open("4.jpg");
        } catch (IOException e)
        {
            e.printStackTrace();
        }

        ivBitmap = BitmapFactory.decodeStream(ims);
        ivBitmap = processBitmap(ivBitmap, 100, 200, 100, 100, 50, 100);
        imageView.setImageBitmap(ivBitmap);
    }

    private void analyzeFrame(ImageView imageView, Bitmap ivBitmap, Bitmap bitmap)
    {
        if (bitmap != null)
        {
            bitmap = adjustedContrast(bitmap, 25.0);
        }

        if (bitmap != null)
        {
            bitmap = Bitmap.createScaledBitmap(
                    bitmap, 100, 100, false);
        }

        imageView.setImageBitmap(ivBitmap);

        String text = "";

        final Classification res = classifyFrame(bitmap);
        if (res.getLabel() != null)
        {
            text = String.format(Locale.ENGLISH, "%s: %s, %f\n", classifier.name(), res.getLabel(),
                    res.getConf());
        }

        resText.setText(text);
    }

    private Classification classifyFrame(Bitmap bitmap)
    {
        int width = 0;
        int height = 0;
        if (bitmap != null)
        {
            width = bitmap.getWidth();
            height = bitmap.getHeight();
        }

        int[] pixels = new int[width * height];
        if (bitmap != null)
        {
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
        }

        float[] retPixels = new float[pixels.length * 3];
        int x = 0;
        for (int pixel : pixels)
        {
            retPixels[x++] = (((pixel >> 16) & 0xff) / 255.0f);
            retPixels[x++] = (((pixel >> 8) & 0xff) / 255.0f);
            retPixels[x++] = (((pixel) & 0xff) / 255.0f);
        }

        return classifier.recognize(retPixels);
    }

    private int range(int value, int min, int max)
    {
        return Math.min(Math.max(value, min), max);
    }

    private int bumpColorContrast(int value, double contrast)
    {
        return (int) (((((value / 255.0) - 0.5) * contrast) + 0.5) * 255.0);
    }
}