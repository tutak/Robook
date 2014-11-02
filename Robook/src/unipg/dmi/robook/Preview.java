package unipg.dmi.robook;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import unipg.dmi.robook.AbstractAdkActivity;
import unipg.dmi.robook.R;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.media.FaceDetector;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

class Preview extends SurfaceView implements SurfaceHolder.Callback,
		Camera.PreviewCallback {
	private static final String TAG = "Preview";
	private SurfaceHolder mHolder;
	private Camera mCamera;
	private Bitmap mWorkBitmap;
	private Bitmap mMonkeyImage;

	private static final int NUM_FACES = 32; // max is 64
	private static final boolean DEBUG = true;

	private FaceDetector mFaceDetector;
	private FaceDetector.Face[] mFaces = new FaceDetector.Face[NUM_FACES];
	private FaceDetector.Face face = null; // refactor this to the callback

	int i = 0;
	private PointF eyesMidPts[] = new PointF[NUM_FACES];
	private float eyesDistance[] = new float[NUM_FACES];

	private Paint tmpPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
	private Paint pOuterBullsEye = new Paint(Paint.ANTI_ALIAS_FLAG);
	private Paint pInnerBullsEye = new Paint(Paint.ANTI_ALIAS_FLAG);

	private int picWidth, picHeight;
	private float ratio, xRatio, yRatio;

	Preview(Context context) {
		super(context);
		Log.d(TAG, "Preview");
		mHolder = getHolder();
		mHolder.addCallback(this);
		mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		mHolder.setFormat(ImageFormat.NV21);

		pInnerBullsEye.setStyle(Paint.Style.FILL);
		pInnerBullsEye.setColor(Color.RED);

		pOuterBullsEye.setStyle(Paint.Style.STROKE);
		pOuterBullsEye.setColor(Color.RED);

		tmpPaint.setStyle(Paint.Style.STROKE);

		mMonkeyImage = BitmapFactory.decodeResource(getResources(),
				R.drawable.monkey_head);

		picWidth = mMonkeyImage.getWidth();
		picHeight = mMonkeyImage.getHeight();

	}

	public Preview(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public void obtainCamera() {
		mCamera = Camera.open();

	}

	public void surfaceCreated(SurfaceHolder holder) {
		Log.d(TAG, "surfaceCreated Surface is: "
				+ mHolder.getSurface().getClass().getName());

		if (mCamera != null) {
			try {
				mCamera.setPreviewDisplay(holder);
			} catch (IOException exception) {
				mCamera.release();
				mCamera = null;
			}
		}
		setWillNotDraw(false);
	}

	public void surfaceDestroyed(SurfaceHolder holder) {
		Log.d(TAG, "surfaceDestroyed");

	}

	public void releaseCamera() {
		if (mCamera != null) {
			mCamera.stopPreview();
			mCamera.setPreviewCallbackWithBuffer(null);
			mCamera.release();
			mCamera = null;

		}
		setWillNotDraw(true);
	}

	private Size getOptimalPreviewSize(List<Size> sizes, int w, int h) {
		final double ASPECT_TOLERANCE = 0.05;
		double targetRatio = (double) w / h;
		if (sizes == null)
			return null;

		Size optimalSize = null;
		double minDiff = Double.MAX_VALUE;

		int targetHeight = h;

		for (Size size : sizes) {
			double ratio = (double) size.width / size.height;
			if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE)
				continue;
			if (Math.abs(size.height - targetHeight) < minDiff) {
				optimalSize = size;
				minDiff = Math.abs(size.height - targetHeight);
			}
		}

		if (optimalSize == null) {
			minDiff = Double.MAX_VALUE;
			for (Size size : sizes) {
				if (Math.abs(size.height - targetHeight) < minDiff) {
					optimalSize = size;
					minDiff = Math.abs(size.height - targetHeight);
				}
			}
		}
		return optimalSize;
	}

	public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
		Log.d(TAG, String.format("surfaceChanged: format=%d, w=%d, h=%d",
				format, w, h));

		if (mCamera != null) {

			Camera.Parameters parameters = mCamera.getParameters();

			List<Size> sizes = parameters.getSupportedPreviewSizes();
			Size optimalSize = getOptimalPreviewSize(sizes, w, h);
			parameters.setPreviewSize(optimalSize.width, optimalSize.height);

			mCamera.setParameters(parameters);
			mCamera.startPreview();

			mWorkBitmap = Bitmap.createBitmap(optimalSize.width,
					optimalSize.height, Bitmap.Config.RGB_565);
			mFaceDetector = new FaceDetector(optimalSize.width,
					optimalSize.height, NUM_FACES);

			int bufSize = optimalSize.width
					* optimalSize.height
					* ImageFormat
							.getBitsPerPixel(parameters.getPreviewFormat()) / 8;
			byte[] cbBuffer = new byte[bufSize];
			mCamera.setPreviewCallbackWithBuffer(this);
			mCamera.addCallbackBuffer(cbBuffer);
		}
	}

	public void onPreviewFrame(byte[] data, Camera camera) {
		Log.d(TAG, "onPreviewFrame");

		YuvImage yuv = new YuvImage(data, ImageFormat.NV21,
				mWorkBitmap.getWidth(), mWorkBitmap.getHeight(), null);
		Rect rect = new Rect(0, 0, mWorkBitmap.getWidth(),
				mWorkBitmap.getHeight()); // TODO: make rect a member and use it
											// for width and height values above

		ByteArrayOutputStream baout = new ByteArrayOutputStream();
		if (!yuv.compressToJpeg(rect, 100, baout)) {
			Log.e(TAG, "compressToJpeg failed");
		}
		BitmapFactory.Options bfo = new BitmapFactory.Options();
		bfo.inPreferredConfig = Bitmap.Config.RGB_565;
		mWorkBitmap = BitmapFactory.decodeStream(
				new ByteArrayInputStream(baout.toByteArray()), null, bfo);

		Arrays.fill(mFaces, null);
		Arrays.fill(eyesMidPts, null);
		mFaceDetector.findFaces(mWorkBitmap, mFaces);

		for (int i = 0; i < mFaces.length; i++) {
			face = mFaces[i];
			try {
				PointF eyesMP = new PointF();
				face.getMidPoint(eyesMP);
				eyesDistance[i] = face.eyesDistance();
				eyesMidPts[i] = eyesMP;

				if (DEBUG) {
					Log.i("Face",
							i + " " + face.confidence() + " "
									+ face.eyesDistance() + " " + "Pose: ("
									+ face.pose(FaceDetector.Face.EULER_X)
									+ ","
									+ face.pose(FaceDetector.Face.EULER_Y)
									+ ","
									+ face.pose(FaceDetector.Face.EULER_Z)
									+ ")" + "Eyes Midpoint: ("
									+ eyesMidPts[i].x + "," + eyesMidPts[i].y
									+ ")");
				}
			} catch (Exception e) {

			}
		}

		invalidate();

		mCamera.addCallbackBuffer(data);
	}

	@Override
	protected void onDraw(Canvas canvas) {

		Log.d(TAG, "onDraw");
		super.onDraw(canvas);
		if (mWorkBitmap != null) {
			xRatio = getWidth() * 1.0f / mWorkBitmap.getWidth();
			yRatio = getHeight() * 1.0f / mWorkBitmap.getHeight();

			for (int i = 0; i < eyesMidPts.length; i++) {
				if (eyesMidPts[i] != null) {
					ratio = eyesDistance[i] * 4.0f / picWidth;
					RectF scaledRect = new RectF((eyesMidPts[i].x - picWidth
							* ratio / 2.0f)
							* xRatio, (eyesMidPts[i].y - picHeight * ratio
							/ 2.0f)
							* yRatio, (eyesMidPts[i].x + picWidth * ratio
							/ 2.0f)
							* xRatio, (eyesMidPts[i].y + picHeight * ratio
							/ 2.0f)
							* yRatio);

					canvas.drawBitmap(mMonkeyImage, null, scaledRect, tmpPaint);
					Log.d(TAG,
							"A face was detected ********************************************");

					MainActivity.arduino(this.getContext());

					Log.d(TAG,
							"Arduino was called+++++++++++++++++++++++++++++++++++++++++++++++++");

				} else {

				}
			}
		}
	}

}