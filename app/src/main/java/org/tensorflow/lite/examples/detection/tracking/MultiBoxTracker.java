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

import android.annotation.SuppressLint;
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
import static java.lang.String.*;

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
  private final List<Pair<Float, RectF>> screenRects = new LinkedList<>();
  private final Logger logger = new Logger();
  private final Queue<Integer> availableColors = new LinkedList<>();
  private final List<TrackedRecognition> trackedObjects = new LinkedList<>();
  private final Paint boxPaint = new Paint();
  private final float textSizePx;
  private final BorderedText borderedText;
  private Matrix frameToCanvasMatrix;
  private int frameWidth;
  private int frameHeight;
  private int sensorOrientation;
  private Integer person_found = 0; // 0 for no person // 1 for person
                                                       // 2 for informed the user before
//  private boolean person_in_frame = false;


  private Integer los_or_out = 1; // 0 for LOS // 1 for out
  private Integer previous_frame = 3; // 0 for no person // 1 for person


  private Vibrator vibrator;

  public MultiBoxTracker(final Context context) {

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


  public synchronized void draw(final Canvas canvas, Context context) {
    //===================================
    // Vibrator is defined amongst the class private members
    vibrator = (Vibrator) context.getSystemService(VIBRATOR_SERVICE);

    // DRAWING THE MIDDLE RECTANGLE- representing line of sight
    //    final RectF midRect = new RectF((float)(canvas.getWidth()/2)-50,(float)0,(float)(canvas.getWidth()/2)+50,(float)canvas.getHeight());

    // Hardcoding values for EPSON-BT-300 device
      final RectF midRect = new RectF((float)(865.0/2.0)-100,(float)0,(float)(865.0/2.0),(float)canvas.getHeight());
      boxPaint.setColor(Color.RED);
      canvas.drawRect(midRect, boxPaint);

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

    // Change of situation has occurred
    if (person_found == 1){
      person_found = 0;
    }

    for (final TrackedRecognition recognition2 : trackedObjects){
      if (recognition2.title.equals("person")){
        person_found = 1; // meaning there is a person detected
      }
    }
    // When there is no person found - FIRST TIME
    if (person_found == 0){
      Log.i(TAG, "No person in the frame -100"); // TAG= "ContentValues"
      vibrator.vibrate(100);
      person_found = 2;
      previous_frame = 3; // Making it changing scenarios
    }
    else if (person_found == 1){ // When there is someone in the frame

      los_or_out = 2; // Initialized at every frame

      for (final TrackedRecognition recognition : trackedObjects) {
        RectF trackedPos = new RectF(recognition.location);
        if (!recognition.title.equals("person")) continue;

        //      Log.i(TAG, "canvasH :" + canvas.getHeight()+ " canvasW" +canvas.getWidth()); // canvasH :647 canvasW1280

        float cornerSize = Math.min(trackedPos.width(), trackedPos.height()) / 10.0f;


        // Scaling the rectangle matrix according to the screen size
          Matrix matrix = new Matrix();
          Log.i(TAG, "FOUND!");

          // For display size (640 * 480) having corresponding values as (865*640)
          matrix.setScale((float) (480.0 / 640.0), (float) (640.0 / 480.0), (float) (640.0 / 2.0), (float) (480.0 / 2.0));
          matrix.mapRect(trackedPos);

          matrix.setRotate(90, (float) (640.0 / 2.0), (float) (((float) 480.0 / 2.0)));
          matrix.mapRect(trackedPos);

          matrix.setScale((float) (865.0 / 640.0), (float) (647.0 / 480.0));
          matrix.mapRect(trackedPos);

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


          @SuppressLint("DefaultLocale") final String labelString =
                  !TextUtils.isEmpty(recognition.title)
                          ? format("%s %.2f", recognition.title, (100 * recognition.detectionConfidence))
                          : format("%.2f", (100 * recognition.detectionConfidence));

          borderedText.drawText(
                  canvas, trackedPos.left + cornerSize, trackedPos.top, labelString + "%", boxPaint);

        // These if and else if statements just recognize what is the scenario
        // Priority is given to Line of sight
        // This is because if there is a person in the LOS, we need not tell the user that there is
        //      a person around as well

        // Person in LOS
        if (midRect.intersects(trackedPos.left, trackedPos.top, trackedPos.right, trackedPos.bottom)) {
          los_or_out = 0;

          boxPaint.setColor(Color.GREEN); // Changing the box color to green
          canvas.drawRect(midRect, boxPaint);
        }
        // In the frame, but not in LOS
        else{
          // As we prioritize LOS over around, so our result will be LOS if we even found one person
          // in the LOS, but if we didn't, then we give our result as around.
          if (los_or_out != 0){
            los_or_out = 1; // Not in the LOS
          }
        }
      }
      // When the result of the frame was a person in the LOS and the previous frame did not show
      // the same result
      if (los_or_out == 0 && previous_frame != 0){
        Log.i(TAG, "Person in the Line of Sight - 500"); // TAG= "ContentValues"
        vibrator.vibrate(500);
        previous_frame = 0;
      }
      // When the result of the frame was a person in the frame, but not in LOS and the previous
      // frame did not show the same result
      else if (los_or_out == 1 && previous_frame != 1){
        Log.i(TAG, "No person in the Line of sight (but in the frame) - 250"); // TAG= "ContentValues"
        vibrator.vibrate(250);
        previous_frame = 1;
      }
    }
  }

  private void processResults(final List<Recognition> results) {
    final List<Pair<Float, Recognition>> rectsToTrack = new LinkedList<>();

    screenRects.clear();
    final Matrix rgbFrameToScreen = new Matrix(getFrameToCanvasMatrix());
    for (final Recognition result : results) {
      if (result.getLocation() == null) {
        continue;
      }
      final RectF detectionFrameRect = new RectF(result.getLocation());

      final RectF detectionScreenRect = new RectF();
      rgbFrameToScreen.mapRect(detectionScreenRect, detectionFrameRect);

      logger.v(
          "Result! Frame: " + result.getLocation() + " mapped to screen:" + detectionScreenRect);

      screenRects.add(new Pair<>(result.getConfidence(), detectionScreenRect));

      if (detectionFrameRect.width() < MIN_SIZE || detectionFrameRect.height() < MIN_SIZE) {
        logger.w("Degenerate rectangle! " + detectionFrameRect);
        continue;
      }

      rectsToTrack.add(new Pair<>(result.getConfidence(), result));
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