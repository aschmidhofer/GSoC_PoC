package at.tugraz.student.aschmidhofer.android.gsoc.poc;

import java.io.ByteArrayOutputStream;
import java.util.List;

import org.opencv.android.InstallCallbackInterface;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Size;
import org.opencv.highgui.Highgui;
import org.opencv.highgui.VideoCapture;


import android.os.Bundle;
import android.os.Vibrator;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.util.Log;
import android.view.Menu;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;

public class RecogniserActivity extends Activity implements SurfaceHolder.Callback{

	public final String TAG = "GSoC_PoC";
	public final int SEARCH_FIDELITY = 11;
	private Camera cam;
	private long[] vibratePattern = { 0, 100, 50 };
	private boolean vibrating = false;
	private ImageView view;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		//setContentView(R.layout.activity_recogniser);
		view = new ImageView(this);
		setContentView(view);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.recogniser, menu);
		return true;
	}
	
	@Override
	protected void onPause() {
		if(cam!=null){
			cam.setPreviewCallback(null);
			cam.stopPreview();
			cam.release();
			cam = null;
		}
		super.onPause();
	}

	@Override
	protected void onResume() {
		super.onResume();

		cam = Camera.open();
		if(cam!=null){
			Camera.Parameters params = cam.getParameters();
//			params.setPreviewFormat(ImageFormat.RGB_565);// does not work on my device
			cam.setParameters(params); 
			cam.startPreview();
			cam.setPreviewCallback(new Camera.PreviewCallback() {
	        	private Vibrator vibr = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
	        	private Bitmap preview;
				public void onPreviewFrame(byte[] data, Camera camera) {
						try {
						    Parameters parameters = camera.getParameters();
						    int imageFormat = parameters.getPreviewFormat();
						    if (imageFormat == ImageFormat.RGB_565 || imageFormat == ImageFormat.JPEG){
						    	preview = BitmapFactory.decodeByteArray(data, 0, data.length);
						    } else {
						    	int w = parameters.getPreviewSize().width;
						    	int h = parameters.getPreviewSize().height;
						        YuvImage img = new YuvImage(data, imageFormat,w,h, null);
						        ByteArrayOutputStream out = new ByteArrayOutputStream();
						        img.compressToJpeg(new Rect(0, 0, w, h), 50, out);
						        byte[] imageBytes = out.toByteArray();
						        preview = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
						    } 
					        boolean red = containsRedObject(preview);
					        if(vibrating&&!red){
					        	vibr.cancel();
					        	vibrating = false;
					        } else if(red&&!vibrating){
					        	vibr.vibrate(vibratePattern, 0);
					        	vibrating = true;
					        }
					        view.setImageBitmap(preview);
					        
						} catch (Exception e) {
							Log.e(TAG, "error in preview", e);
						}
					}

			});
		}
	}
	

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		
	}

	public boolean containsRedObject(Bitmap pixels){
		if(pixels==null)return false;
		for(int x=1;x<SEARCH_FIDELITY;x++){
			for(int y=1;y<SEARCH_FIDELITY;y++){
				int color = pixels.getPixel(pixels.getWidth()*x/SEARCH_FIDELITY, pixels.getHeight()*y/SEARCH_FIDELITY);
				if(Color.red(color)>128){
					if((Color.green(color)<64)&&(Color.blue(color)<64)){
						//Log.v(TAG, Color.red(color)+" "+Color.green(color)+" "+Color.blue(color)+" it's red");
						return true;
					}
				}		
			}
		}
		return false;
	}

}