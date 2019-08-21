/* Copyright 2019 The TensorFlow Authors. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
==============================================================================*/

package org.tensorflow.lite.examples.detection.tracking;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Cap;
import android.graphics.Paint.Join;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.os.Vibrator;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.util.TypedValue;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import org.tensorflow.lite.examples.detection.env.BorderedText;
import org.tensorflow.lite.examples.detection.env.ImageUtils;
import org.tensorflow.lite.examples.detection.env.Logger;
import org.tensorflow.lite.examples.detection.tflite.Classifier.Recognition;

import static android.content.ContentValues.TAG;
import static android.content.Context.VIBRATOR_SERVICE;

/** A tracker that handles non-max suppression and matches existing objects to new detections. */
public class MultiBoxTracker {

  private static final float TEXT_SIZE_DIP = 18;
  private static final float MIN_SIZE = 16.0f;
  private static final int[] COLORS = {
    Color.BLUE,
    Color.RED,
    Color.GREEN,
    Color.YELLOW,
    Color.CYAN,
    Color.MAGENTA,
    Color.WHITE,
    Color.parseColor("#55FF55"),
    Color.parseColor("#FFA500"),
    Color.parseColor("#FF8888"),
    Color.parseColor("#AAAAFF"),
    Color.parseColor("#FFFFAA"),
    Color.parseColor("#55AAAA"),
    Color.parseColor("#AA33AA"),
    Color.parseColor("#0D0068")
  };
  final List<Pair<Float, RectF>> screenRects = new LinkedList<Pair<Float, RectF>>();
  private final Logger logger = new Logger();
  private final Queue<Integer> availableColors = new LinkedList<Integer>();
  private final List<TrackedRecognition> trackedObjects = new LinkedList<TrackedRecognition>();
  private final Paint boxPaint = new Paint();
  private final float textSizePx;
  private final BorderedText borderedText;
  private Matrix frameToCanvasMatrix;
  private int frameWidth;
  private int frameHeight;
  private int sensorOrientation;
  private Integer detected_realtime = new Integer(0); // 0 for no person // 1 for person
  Vibrator vibrator;

  public MultiBoxTracker(final Context context) {
//    Vibrator vibrator;
//    vibrator = (Vibrator) context.getSystemService(VIBRATOR_SERVICE);
//    vibrator.vibrate(500);

    for (final int color : COLORS) {
      availableColors.add(color);
    }
    boxPaint.setColor(Color.RED);
    boxPaint.setStyle(Style.STROKE);
    boxPaint.setStrokeWidth(10.0f);
    boxPaint.setStrokeCap(Cap.ROUND);
    boxPaint.setStrokeJoin(Join.ROUND);
    boxPaint.setStrokeMiter(100);

    textSizePx =
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, TEXT_SIZE_DIP, context.getResources().getDisplayMetrics());
    borderedText = new BorderedText(textSizePx);
  }

  public synchronized void setFrameConfiguration(
      final int width, final int height, final int sensorOrientation) {
    frameWidth = width;
    frameHeight = height;
    this.sensorOrientation = sensorOrientation;
  }

  public synchronized void drawDebug(final Canvas canvas) {
    final Paint textPaint = new Paint();
    textPaint.setColor(Color.WHITE);
    textPaint.setTextSize(60.0f);

    final Paint boxPaint = new Paint();
    boxPaint.setColor(Color.RED);
    boxPaint.setAlpha(200);
    boxPaint.setStyle(Style.STROKE);

    for (final Pair<Float, RectF> detection : screenRects) {
      final RectF rect = detection.second;
      canvas.drawRect(rect, boxPaint);
      canvas.drawText("" + detection.first, rect.left, rect.top, textPaint);
      borderedText.drawText(canvas, rect.centerX(), rect.centerY(), "" + detection.first);
    }
  }

  public synchronized void trackResults(final List<Recognition> results, final long timestamp) {
    logger.i("Processing %d results from %d", results.size(), timestamp);
    processResults(results);
  }

  private Matrix getFrameToCanvasMatrix() {
    return frameToCanvasMatrix;
  }

//===================================
  public synchronized void draw(final Canvas canvas, Context context) {
    // Vibrator is defined amongst the class private members
    vibrator = (Vibrator) context.getSystemService(VIBRATOR_SERVICE);
//    vibrator.vibrate(500); // RUNS WHEN WE CLOSE THE APP FOR SOME REASON- On the phone-Works on the device

    // DRAWING THE MIDDLE RECTANGLE

//    final RectF midRect = new RectF((float)(canvas.getWidth()/2)-50,(float)0,(float)(canvas.getWidth()/2)+50,(float)canvas.getHeight());
      final RectF midRect = new RectF((float)(865.0/2.0)-100,(float)0,(float)(865.0/2.0),(float)canvas.getHeight());
      boxPaint.setColor(Color.RED);
      canvas.drawRect(midRect, boxPaint);

    // SIDE Verification
//    final RectF loc1 = new RectF((float)1062.79886,(float)256.0452,(float)1282.63,(float)471.007);
////    final RectF loc1 = new RectF((float)450.000000,(float)0,(float)650.000000,(float)canvas.getHeight());
//    boxPaint.setColor(Color.RED);
//    canvas.drawRect(loc1, boxPaint);

//    final RectF loc2 = new RectF((float)954.858349,(float)284.7672,(float)1152.36,(float)487.68227);
//    boxPaint.setColor(Color.GREEN);
//    canvas.drawRect(loc2, boxPaint);

//    if (detected == 1){
//      Log.i(TAG, "No person in the frame"); // TAG= "ContentValues"
//      detected = 0;
//    }

//    if (Integer.compare(counter,counter) ){
//     Log.i(TAG, "No person in the frame"); // TAG= "ContentValues"
//    }
//===================================

    final boolean rotated = sensorOrientation % 180 == 90;
    final float multiplier =
        Math.min(
            canvas.getHeight() / (float) (rotated ? frameWidth : frameHeight),
            canvas.getWidth() / (float) (rotated ? frameHeight : frameWidth));

    frameToCanvasMatrix =
        ImageUtils.getTransformationMatrix(
            frameWidth,
            frameHeight,
            (int) (multiplier * (rotated ? frameHeight : frameWidth)),
            (int) (multiplier * (rotated ? frameWidth : frameHeight)),
            sensorOrientation,
            false);
//==============
//    Fwidth:1280Fheight720
//    Log.i(TAG, "Fwidth:" + frameWidth+ "Fheight" +frameHeight); // TAG= "ContentValues"


//    final RectF test_rect = new RectF((float)0,(float)0,(float)640.0,(float)480);
//    boxPaint.setColor(Color.BLUE);
//    canvas.drawRect(test_rect, boxPaint);
//
//    final RectF test_rect2 = new RectF((float)0,(float)0,(float)865,(float)610);
//    boxPaint.setColor(Color.YELLOW);
//    canvas.drawRect(test_rect2, boxPaint);
//==============


    // 0 Denotes "no person detected" // 1 denotes "person detected"
    Integer detected_in_current_frame = new Integer(0);
    for (final TrackedRecognition recognition : trackedObjects) {
      RectF trackedPos = new RectF(recognition.location);
      if (!recognition.title.equals("person")) continue;
//      trackedPos.left = trackedPos.left*((float)1280/480);
//      trackedPos.right = trackedPos.right*((float)1280/480);
//      Log.i(TAG, "AFTER:" + trackedPos.left + "  " +trackedPos.right);

//      Log.i(TAG, "canvasH :" + canvas.getHeight()+ " canvasW" +canvas.getWidth()); // canvasH :647 canvasW1280

//      trackedPos.left = trackedPos.left*((float)1280/720);
//      trackedPos.right = trackedPos.right*((float)1280/480);
//      trackedPos.top = trackedPos.top*(480/300);
//      trackedPos.bottom = trackedPos.bottom*(480/300);

      float cornerSize = Math.min(trackedPos.width(), trackedPos.height()) / 10.0f;

//      float x_dist = 1150;
//      float y_dist = 647;
//
//      float cx = (float) (x_dist/2.0);
//      float cy = (float) (y_dist/2.0);

//      float left    = (float) 506.155;
//      float right   = (float) 931.8232;
//      float top     = (float) -1.334;
//      float bottom  = (float) 109.788;
//
//      float x2F = ( ((right-cx) * cy) / cx) + cy;
//      float y1F = ( ((cy-top) * cx)   / cy) + cx;
//
//      float x1F = ( ((left-cx) * cy)  / cx) + cy;
//      float y2F = ( ((cy-bottom) * cy)/ cy) + cx;
//      Log.i(TAG, "x1F: "+ x1F + "x2F: "+ x2F + "y1F: "+ y1F + "y2F: "+ y2F+ " cx-x1 "+ (cx-left));
//===================================
      // To identify if the object is a person
//      Log.i(TAG, "detectionConf" + recognition.detectionConfidence+" obj: "+recognition.title);

      if (recognition.title.equals("person")) {
        Matrix matrix = new Matrix();
        Log.i(TAG, "FOUND!");
//        Log.i(TAG, "BEFORE :" + trackedPos.left + "  " +trackedPos.top +" " +trackedPos.right+ " "+trackedPos.bottom);

//=====
        matrix.setScale((float)(480.0/640.0), (float)(640.0/480.0), (float)(640.0/2.0), (float) (480.0/2.0));
        matrix.mapRect(trackedPos);

        matrix.setRotate(90, (float)(640.0/2.0), (float) (((float)480.0/2.0)));
        matrix.mapRect(trackedPos);

        matrix.setScale((float)(865.0/640.0), (float)(647.0/480.0));
        matrix.mapRect(trackedPos);
//=====

        //=======================================================
        // WORKING CODE FOR 1280*720
//        matrix.setScale((float)(647.0/1150.0), (float)(1150.0/647.0), (float)(1150.0/2.0), (float) (((float)647.0/2.0)));
//        matrix.mapRect(trackedPos);
//
//        matrix.setRotate(90, (float)(1280.0/2.0), (float) (((float)647.0/2.0)));
//        matrix.mapRect(trackedPos);
        //=======================================================


        boxPaint.setColor(recognition.color);

//        canvas.drawRoundRect(trackedPos, cornerSize, cornerSize, boxPaint);
        canvas.drawRect(trackedPos, boxPaint); // -- To have rectangular edges


        final String labelString =
                !TextUtils.isEmpty(recognition.title)
                        ? String.format("%s %.2f", recognition.title, (100 * recognition.detectionConfidence))
                        : String.format("%.2f", (100 * recognition.detectionConfidence));

        borderedText.drawText(
                canvas, trackedPos.left + cornerSize, trackedPos.top, labelString + "%", boxPaint);

        // When the detected person is in the rectangle
        if (midRect.intersects(trackedPos.left, trackedPos.top, trackedPos.right, trackedPos.bottom)){
          detected_in_current_frame = 1; // 1 denotes that the person is found in the frame
          if (detected_realtime == 0){ // When there was no person in the mid-rectangle before
            Log.i(TAG, "Person in the frame"); // TAG= "ContentValues"
            vibrator.vibrate(500);
          }
          boxPaint.setColor(Color.GREEN); // Changing the box color to green
          canvas.drawRect(midRect, boxPaint);
          detected_realtime = 1;
        }
      }
    }
    // If the person is not found in the current frame but was present in the previous frame
    if (detected_in_current_frame == 0 && detected_realtime == 1){
        Log.i(TAG, "No person in the frame anymore"); // TAG= "ContentValues"
        vibrator.vibrate(200);
        detected_realtime = 0;
    }
  }
//===================================


  private void processResults(final List<Recognition> results) {
    final List<Pair<Float, Recognition>> rectsToTrack = new LinkedList<Pair<Float, Recognition>>();

    screenRects.clear();
    final Matrix rgbFrameToScreen = new Matrix(getFrameToCanvasMatrix());
//    rgbFrameToScreen.
    for (final Recognition result : results) {
      if (result.getLocation() == null) {
        continue;
      }
      final RectF detectionFrameRect = new RectF(result.getLocation());

      final RectF detectionScreenRect = new RectF();
      rgbFrameToScreen.mapRect(detectionScreenRect, detectionFrameRect);

      logger.v(
          "Result! Frame: " + result.getLocation() + " mapped to screen:" + detectionScreenRect);

      screenRects.add(new Pair<Float, RectF>(result.getConfidence(), detectionScreenRect));

      if (detectionFrameRect.width() < MIN_SIZE || detectionFrameRect.height() < MIN_SIZE) {
        logger.w("Degenerate rectangle! " + detectionFrameRect);
        continue;
      }

      rectsToTrack.add(new Pair<Float, Recognition>(result.getConfidence(), result));
    }

    if (rectsToTrack.isEmpty()) {
      logger.v("Nothing to track, aborting.");
      return;
    }

    trackedObjects.clear();
    for (final Pair<Float, Recognition> potential : rectsToTrack) {
      final TrackedRecognition trackedRecognition = new TrackedRecognition();
      trackedRecognition.detectionConfidence = potential.first;
      trackedRecognition.location = new RectF(potential.second.getLocation());
      trackedRecognition.title = potential.second.getTitle();
      trackedRecognition.color = COLORS[trackedObjects.size()];
      trackedObjects.add(trackedRecognition);
      Log.i(TAG, "OUT##############");

      if (trackedObjects.size() >= COLORS.length) {
        break;
      }
    }
  }

  private static class TrackedRecognition {
    RectF location;
    float detectionConfidence;
    int color;
    String title;
  }
}