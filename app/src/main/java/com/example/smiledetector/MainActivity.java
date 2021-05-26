package com.example.smiledetector;

import android.app.Person;
import android.app.slice.Slice;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.media.MediaMetadata;
import android.net.Uri;
import android.os.Bundle;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.vision.face.FaceDetectorV2Jni;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.mlkit.vision.common.InputImage;

import com.google.mlkit.vision.common.internal.VisionImageMetadataParcel;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceContour;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.google.mlkit.vision.face.FaceLandmark;
import com.otaliastudios.cameraview.CameraView;
import com.otaliastudios.cameraview.Facing;
import com.otaliastudios.cameraview.Frame;
import com.otaliastudios.cameraview.FrameProcessor;
import com.theartofdev.edmodo.cropper.CropImage;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.provider.MediaStore;
import android.view.Surface;
import android.view.View;

import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MainActivity extends AppCompatActivity implements FrameProcessor {
    private Facing cameraFacing = Facing.FRONT;
    private ImageView imageView;
    private CameraView faceDetectionCameraView;
    private RecyclerView BottomSheetRecycler;
    private BottomSheetBehavior bottomSheetBehavior;
    private ArrayList<FaceDetectionModel> faceDetectionModels;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        faceDetectionModels = new ArrayList<>();
        bottomSheetBehavior = BottomSheetBehavior.from(findViewById(R.id.bottom_sheet));
        imageView = findViewById(R.id.face_detection_image_view);
        faceDetectionCameraView = findViewById(R.id.face_detection_camera_view);
        Button toggle = findViewById(R.id.face_detection_toggle_button);
        FrameLayout bottomSheetButton = findViewById(R.id.bottom_sheet_button);
        BottomSheetRecycler = findViewById(R.id.bottom_sheet_recycler_view);
        faceDetectionCameraView.setFacing(cameraFacing);
        faceDetectionCameraView.setLifecycleOwner(MainActivity.this);
        faceDetectionCameraView.addFrameProcessor(MainActivity.this);

        toggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                cameraFacing = (cameraFacing==Facing.FRONT)? Facing.BACK : Facing.FRONT;
                faceDetectionCameraView.setFacing(cameraFacing);
            }
        });
        bottomSheetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CropImage.activity().start(MainActivity.this);
            }
        });
        BottomSheetRecycler.setLayoutManager(new LinearLayoutManager(MainActivity.this));
        BottomSheetRecycler.setAdapter(new FaceDetectionAdapter(faceDetectionModels, MainActivity.this));

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                Uri imageUri = result.getUri();
                try {
                    analyseImage(MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri));
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }
    }

    private void analyseImage(Bitmap bitmap) {
        if (bitmap == null) {
            Toast.makeText(this, "Error", Toast.LENGTH_SHORT).show();
            return;
        }
        imageView.setImageBitmap(null);
        faceDetectionModels.clear();
        BottomSheetRecycler.getAdapter().notifyDataSetChanged();
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        showProgress();
        InputImage inputImage = InputImage.fromBitmap(bitmap, 0);
        FaceDetectorOptions options =
                new FaceDetectorOptions.Builder()
                        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                        .build();
        FaceDetector faceDetector = FaceDetection.getClient(options);
        faceDetector.process(inputImage).addOnSuccessListener(new OnSuccessListener<List<Face>>() {
            @Override
            public void onSuccess(@NonNull List<Face> faces) {
                Bitmap mutableImage = bitmap.copy(Bitmap.Config.ARGB_8888, true);
                detectFaces(faces, mutableImage);
                imageView.setImageBitmap(mutableImage);
                hideProgress();
                BottomSheetRecycler.getAdapter().notifyDataSetChanged();
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(MainActivity.this, "Error", Toast.LENGTH_SHORT).show();
                hideProgress();
            }
        });


    }

    private void detectFaces(List<Face> faces, Bitmap mutableImage) {
        if (faces == null || mutableImage == null) {
            Toast.makeText(this, "Error", Toast.LENGTH_SHORT).show();
            return;
        }
        Canvas canvas = new Canvas(mutableImage);

        Paint facePaint = new Paint();
        facePaint.setColor(Color.GREEN);
        facePaint.setStyle(Paint.Style.STROKE);
        facePaint.setStrokeWidth(5f);
        Paint faceTextPaint = new Paint();
        faceTextPaint.setColor(Color.BLUE);
        faceTextPaint.setTextSize(30f);
        faceTextPaint.setTypeface(Typeface.SANS_SERIF);
        Paint landmarkPaint = new Paint();
        landmarkPaint.setColor(Color.RED);
        landmarkPaint.setStyle(Paint.Style.FILL);
        landmarkPaint.setStrokeWidth(8f);

        for (int i = 0; i < faces.size(); i++) {
            canvas.drawRect(faces.get(i).getBoundingBox(), facePaint);
            canvas.drawText("Face " + i, (faces.get(i).getBoundingBox().centerX() - (faces.get(i).getBoundingBox().width() >> 2) + 8f),
                    (faces.get(i).getBoundingBox().centerY() + faces.get(i).getBoundingBox().height() >> 2), facePaint);

            Face face = faces.get(i);
            if (face.getLandmark(FaceLandmark.LEFT_EYE) != null) {
                FaceLandmark eye = face.getLandmark(FaceLandmark.LEFT_EYE);
                canvas.drawCircle(eye.getPosition().x, eye.getPosition().y, 8f,
                        landmarkPaint);
            }
            if (face.getLandmark(FaceLandmark.RIGHT_EYE) != null) {
                FaceLandmark eye = face.getLandmark(FaceLandmark.RIGHT_EYE);
                canvas.drawCircle(eye.getPosition().x, eye.getPosition().y, 8f,
                        landmarkPaint);
            }
            if (face.getLandmark(FaceLandmark.NOSE_BASE) != null) {
                FaceLandmark nose = face.getLandmark(FaceLandmark.NOSE_BASE);
                canvas.drawCircle(nose.getPosition().x, nose.getPosition().y, 8f,
                        landmarkPaint);
            }
            if (face.getLandmark(FaceLandmark.RIGHT_EAR) != null) {
                FaceLandmark ear = face.getLandmark(FaceLandmark.RIGHT_EAR);
                canvas.drawCircle(ear.getPosition().x, ear.getPosition().y, 8f,
                        landmarkPaint);
            }
            if (face.getLandmark(FaceLandmark.LEFT_EAR) != null) {
                FaceLandmark ear = face.getLandmark(FaceLandmark.LEFT_EAR);
                canvas.drawCircle(ear.getPosition().x, ear.getPosition().y, 8f,
                        landmarkPaint);
            }
            if (face.getLandmark(FaceLandmark.MOUTH_LEFT) != null && face.getLandmark(FaceLandmark.MOUTH_BOTTOM) != null
                    && face.getLandmark(FaceLandmark.MOUTH_RIGHT) != null) {
                FaceLandmark leftMouth = face.getLandmark(FaceLandmark.MOUTH_LEFT);
                FaceLandmark rightMouth = face.getLandmark(FaceLandmark.MOUTH_RIGHT);
                FaceLandmark bottomMouth = face.getLandmark(FaceLandmark.MOUTH_BOTTOM);
                canvas.drawLine(leftMouth.getPosition().x, leftMouth.getPosition().y, bottomMouth.getPosition().x, bottomMouth.getPosition().y,
                        landmarkPaint);
                canvas.drawLine(bottomMouth.getPosition().x, bottomMouth.getPosition().y, rightMouth.getPosition().x, rightMouth.getPosition().y,
                        landmarkPaint);
            }
            faceDetectionModels.add(new FaceDetectionModel(i, "Smiling Probability" + face.getSmilingProbability()));
            faceDetectionModels.add(new FaceDetectionModel(i, "Left Eye Open Probability" + face.getLeftEyeOpenProbability()));
            faceDetectionModels.add(new FaceDetectionModel(i, "Right Eye Open Probability" + face.getRightEyeOpenProbability()));
        }

    }

    private void showProgress() {
        findViewById(R.id.bottom_sheet_image).setVisibility(View.GONE);
        findViewById(R.id.bottom_sheet_progressbar).setVisibility(View.VISIBLE);
    }

    private void hideProgress() {
        findViewById(R.id.bottom_sheet_image).setVisibility(View.VISIBLE);
        findViewById(R.id.bottom_sheet_progressbar).setVisibility(View.GONE);
    }

    @Override
    public void process(@NonNull Frame frame) {
        int width = frame.getSize().getWidth();
        int height = frame.getSize().getHeight();
        int rotation = 90;
        if(cameraFacing == Facing.FRONT)
            rotation = 90;
        else
            rotation = 270;

        InputImage inputImage = InputImage.fromByteArray(frame.getData(),width,height, rotation,InputImage.IMAGE_FORMAT_NV21);
        FaceDetectorOptions options =
                new FaceDetectorOptions.Builder()
                        .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL).
                        build();
        FaceDetector faceDetector = FaceDetection.getClient(options);
        faceDetector.process(inputImage).addOnSuccessListener(new OnSuccessListener<List<Face>>() {
            @Override
            public void onSuccess(@NonNull List<Face> faces) {
                imageView.setImageBitmap(null);
                Bitmap bitmap = Bitmap.createBitmap(height,width,Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(bitmap);
                Paint dotPaint = new Paint();
                dotPaint.setColor(Color.RED);
                dotPaint.setStyle(Paint.Style.FILL);
                dotPaint.setStrokeWidth(3f);
                Paint linePaint = new Paint();
                linePaint.setColor(Color.GREEN);
                linePaint.setStyle(Paint.Style.STROKE);
                linePaint.setStrokeWidth(2f);

                for(Face face: faces) {
                    List<PointF> faceContours = face.getContour(FaceContour.FACE).getPoints();


                    for (int i = 0; i < faceContours.size(); i++)
                    {
                        PointF faceContour = null;
                        if(i!=(faceContours.size()-1))
                        {
                            faceContour = faceContours.get(i);
                            canvas.drawLine(faceContour.x, faceContour.y, faceContours.get(i + 1).x, faceContours.get(i + 1).y, linePaint);
                        }
                        else {
                            canvas.drawLine(faceContour.x, faceContour.y, faceContours.get(0).x, faceContours.get(0).y, linePaint);
                        }
                        canvas.drawCircle(faceContour.x,faceContour.y,2f,dotPaint);
                    }

                    List<PointF> leftEyebrowTopCountours = face.getContour(
                            FaceContour.LEFT_EYEBROW_TOP).getPoints();
                    for (int i = 0; i < leftEyebrowTopCountours.size(); i++) {
                        PointF contour = leftEyebrowTopCountours.get(i);
                        if (i != (leftEyebrowTopCountours.size() - 1))
                            canvas.drawLine(contour.x, contour.y, leftEyebrowTopCountours.get(i + 1).x,leftEyebrowTopCountours.get(i + 1).y, linePaint);
                        canvas.drawCircle(contour.x, contour.y, 4f, dotPaint);

                    }

                    List<PointF> rightEyebrowTopCountours = face.getContour(
                            FaceContour. RIGHT_EYEBROW_TOP).getPoints();
                    for (int i = 0; i < rightEyebrowTopCountours.size(); i++) {
                        PointF contour = rightEyebrowTopCountours.get(i);
                        if (i != (rightEyebrowTopCountours.size() - 1))
                            canvas.drawLine(contour.x, contour.y, rightEyebrowTopCountours.get(i + 1).x,rightEyebrowTopCountours.get(i + 1).y, linePaint);
                        canvas.drawCircle(contour.x, contour.y, 4f, dotPaint);

                    }

                    List<PointF> rightEyebrowBottomCountours = face.getContour(
                            FaceContour. RIGHT_EYEBROW_BOTTOM).getPoints();
                    for (int i = 0; i < rightEyebrowBottomCountours.size(); i++) {
                        PointF contour = rightEyebrowBottomCountours.get(i);
                        if (i != (rightEyebrowBottomCountours.size() - 1))
                            canvas.drawLine(contour.x, contour.y, rightEyebrowBottomCountours.get(i + 1).x,rightEyebrowBottomCountours.get(i + 1).y, linePaint);
                        canvas.drawCircle(contour.x, contour.y, 4f, dotPaint);

                    }

                    List<PointF> leftEyeContours = face.getContour(
                            FaceContour.LEFT_EYE).getPoints();
                    for (int i = 0; i < leftEyeContours.size(); i++) {
                        PointF contour = leftEyeContours.get(i);
                        if (i != (leftEyeContours.size() - 1)){
                            canvas.drawLine(contour.x, contour.y, leftEyeContours.get(i + 1).x,leftEyeContours.get(i + 1).y, linePaint);

                        }else {
                            canvas.drawLine(contour.x, contour.y, leftEyeContours.get(0).x,
                                    leftEyeContours.get(0).y, linePaint);
                        }
                        canvas.drawCircle(contour.x, contour.y, 4f, dotPaint);


                    }

                    List<PointF> rightEyeContours = face.getContour(
                            FaceContour.RIGHT_EYE).getPoints();
                    for (int i = 0; i < rightEyeContours.size(); i++) {
                        PointF contour = rightEyeContours.get(i);
                        if (i != (rightEyeContours.size() - 1)){
                            canvas.drawLine(contour.x, contour.y, rightEyeContours.get(i + 1).x,rightEyeContours.get(i + 1).y, linePaint);

                        }else {
                            canvas.drawLine(contour.x, contour.y, rightEyeContours.get(0).x,
                                    rightEyeContours.get(0).y, linePaint);
                        }
                        canvas.drawCircle(contour.x, contour.y, 4f, dotPaint);


                    }

                    List<PointF> upperLipTopContour = face.getContour(
                            FaceContour.UPPER_LIP_TOP).getPoints();
                    for (int i = 0; i < upperLipTopContour.size(); i++) {
                        PointF contour = upperLipTopContour.get(i);
                        if (i != (upperLipTopContour.size() - 1)){
                            canvas.drawLine(contour.x, contour.y,
                                    upperLipTopContour.get(i + 1).x,
                                    upperLipTopContour.get(i + 1).y, linePaint);
                        }
                        canvas.drawCircle(contour.x, contour.y, 4f, dotPaint);

                    }

                    List<PointF> upperLipBottomContour = face.getContour(
                            FaceContour.UPPER_LIP_BOTTOM).getPoints();
                    for (int i = 0; i < upperLipBottomContour.size(); i++) {
                        PointF contour = upperLipBottomContour.get(i);
                        if (i != (upperLipBottomContour.size() - 1)){
                            canvas.drawLine(contour.x, contour.y, upperLipBottomContour.get(i + 1).x,upperLipBottomContour.get(i + 1).y, linePaint);
                        }
                        canvas.drawCircle(contour.x, contour.y, 4f, dotPaint);

                    }
                    List<PointF> lowerLipTopContour = face.getContour(
                            FaceContour.LOWER_LIP_TOP).getPoints();
                    for (int i = 0; i < lowerLipTopContour.size(); i++) {
                        PointF contour = lowerLipTopContour.get(i);
                        if (i != (lowerLipTopContour.size() - 1)){
                            canvas.drawLine(contour.x, contour.y, lowerLipTopContour.get(i + 1).x,lowerLipTopContour.get(i + 1).y, linePaint);
                        }
                        canvas.drawCircle(contour.x, contour.y, 4f, dotPaint);

                    }
                    List<PointF> lowerLipBottomContour = face.getContour(
                            FaceContour.LOWER_LIP_BOTTOM).getPoints();
                    for (int i = 0; i < lowerLipBottomContour.size(); i++) {
                        PointF contour = lowerLipBottomContour.get(i);
                        if (i != (lowerLipBottomContour.size() - 1)){
                            canvas.drawLine(contour.x, contour.y, lowerLipBottomContour.get(i + 1).x,lowerLipBottomContour.get(i + 1).y, linePaint);
                        }
                        canvas.drawCircle(contour.x, contour.y, 4f, dotPaint);

                    }

                    List<PointF> noseBridgeContours = face.getContour(
                            FaceContour.NOSE_BRIDGE).getPoints();
                    for (int i = 0; i < noseBridgeContours.size(); i++) {
                        PointF contour = noseBridgeContours.get(i);
                        if (i != (noseBridgeContours.size() - 1)) {
                            canvas.drawLine(contour.x, contour.y, noseBridgeContours.get(i + 1).x,noseBridgeContours.get(i + 1).y, linePaint);
                        }
                        canvas.drawCircle(contour.x, contour.y, 4f, dotPaint);

                    }

                    List<PointF> noseBottomContours = face.getContour(
                            FaceContour.NOSE_BOTTOM).getPoints();
                    for (int i = 0; i < noseBottomContours.size(); i++) {
                        PointF contour = noseBottomContours.get(i);
                        if (i != (noseBottomContours.size() - 1)) {
                            canvas.drawLine(contour.x, contour.y, noseBottomContours.get(i + 1).x,noseBottomContours.get(i + 1).y, linePaint);
                        }
                        canvas.drawCircle(contour.x, contour.y, 4f, dotPaint);

                    }
                    if (cameraFacing == Facing.FRONT) {
                        //Flip image!
                        Matrix matrix = new Matrix();
                        matrix.preScale(-1f, 1f);
                        Bitmap flippedBitmap = Bitmap.createBitmap(bitmap, 0, 0,
                                bitmap.getWidth(), bitmap.getHeight(),
                                matrix, true);
                        imageView.setImageBitmap(flippedBitmap);
                    }else
                        imageView.setImageBitmap(bitmap);
                }

            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                imageView.setImageBitmap(null);
            }
        });

    }
}