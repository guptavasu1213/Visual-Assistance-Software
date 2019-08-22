# Visual Assistance Software for the Visually Impaired

### Overview
This is an app created for EPSON Moverio BT-300 smart glasses to aid the visually impaired avoid collisions. The glasses are attached to a controller running on Android™ 5.1. The device camera is used perform real-time object detection on the eyeglasses. The app uses **[TensorFlow Lite](https://github.com/tensorflow/examples/tree/master/lite)**, an open source deep learning framework for on device inference. 

The red box in the middle of the screen indicates the line of sight of the visually impaired. The controller gives 3 types of vibrations to notify the user about the people in the surroundings. 

## Build the demo using Android Studio

### Prerequisites

* If you don't have already, install **[Android Studio](https://developer.android.com/studio/index.html)**, following the instructions on the website.

* You need an Android device and Android development environment with minimum API 21.
* Android Studio 3.2 or later.

### Building
* Open Android Studio, and from the Welcome screen, select Open an existing Android Studio project.

* If it asks you to do a Gradle Sync, click OK.

* You may also need to install various platforms and tools, if you get errors like "Failed to find target with hash string 'android-21'" and similar.
Click the Run button (the green arrow) or select Run > Run 'android' from the top menu. You may need to rebuild the project using Build > Rebuild Project.

* If it asks you to use Instant Run, click Proceed Without Instant Run.

### References
