## Installation

### 1. Clone the Repository

```bash
git clone https://github.com/fluidpotata/CSE489_Map_Pointer.git
cd CSE489_Map_Pointer
```

### 2. Open in Android Studio

- Launch Android Studio.
- Select **Open an Existing Project** and choose the `CSE489_Map_Pointer` directory.

### 3. Configure Google Maps API Key

1. [Obtain a Google Maps API key](https://developers.google.com/maps/documentation/android-sdk/get-api-key).
2. In Android Studio, open `local.properties` and add:
   ```
   MAPS_API_KEY=YOUR_API_KEY_HERE
   ```
   - Alternatively, edit `app/src/main/AndroidManifest.xml` and replace `YOUR_API_KEY_HERE` with your key in the `<meta-data android:name="com.google.android.geo.API_KEY" ... />` tag.

### 4. Build the Project

- Click **Build > Make Project** or press `Ctrl+F9`.

### 5. Run the App

- Connect an Android device or start an emulator.
- Click **Run > Run 'app'** or press the green play button.

---