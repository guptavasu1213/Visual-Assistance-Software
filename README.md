# Visual Assistance Software for the Visually Impaired

### Overview
![](https://github.com/guptavasu1213/visual-assistance-software/blob/master/IMG_2989.jpg)
This is an app created for EPSON Moverio BT-300 smart glasses to aid the visually impaired avoid collisions. The glasses are attached to a controller running on Android™ 5.1. The device camera is used perform real-time object detection on the eyeglasses. The app uses **[TensorFlow Lite](https://github.com/tensorflow/examples/tree/master/lite)**, an open source deep learning framework for on device inference. 

![](https://github.com/guptavasu1213/visual-assistance-software/blob/master/screenshot_1.png)

The red box in the middle of the screen indicates the line of sight of the visually impaired. 

The controller gives 3 types of vibrations to notify the user about the people in the surroundings. 
- Vibration A is a 500ms long buzz in the controller which indicates the presence of a person the line of sight of the user. 
- Vibration B conveys the absence of people in the frame(scene) by giving a 100ms vibration. 
- Vibration C, a 250ms buzz indicates the presence of a person in the frame (scene), but not in the line of sight to alert the user.

To create a positive user experience, every vibration is only given once after the detection. This is because if the controller keeps buzzing 24x7, it would prove to be annoying for the user.

[]!(https://github.com/guptavasu1213/visual-assistance-software/blob/master/test_screenshots/Screenshot_2019-09-04-20-43-08.png)
## Build the demo using Android Studio

### Prerequisites

* If you don't have already, install **[Android Studio](https://developer.android.com/studio/index.html)**, following the instructions on the website.

* You need an Android device and Android development environment with minimum API 21.
* Android Studio 3.2 or later.

### Building
* Open Android Studio, and from the Welcome screen, select Open an existing Android Studio project.

* You may also need to install various platforms and tools, if you get errors like "Failed to find target with hash string 'android-21'" and similar.
Click the Run button (the green arrow) or select Run > Run 'android' from the top menu. You may need to rebuild the project using Build > Rebuild Project.

* If it asks you to use Instant Run, click Proceed Without Instant Run.

### References
- **[Tensorflow Lite Object Detection](https://github.com/tensorflow/examples/tree/master/lite/examples/object_detection/android)** 
