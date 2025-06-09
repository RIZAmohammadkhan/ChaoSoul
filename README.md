# ChaoSoul - Your Digital Soul as a Live Wallpaper

![ChaoSoul Banner](https://via.placeholder.com/1200x400.png?text=ChaoSoul+Live+Wallpaper)

**ChaoSoul is a conceptual Android application that transforms your daily digital behavior into a unique, evolving piece of abstract art, displayed as your phone's live wallpaper.**

Every day, the app analyzes your interactions—your communication, your work, your creativity—and uses them as forces in a dynamic system. The result is a "soul" that is a true reflection of you, continuously changing its shape and color as your digital life unfolds.

---

## Features

-   **Dynamic Live Wallpaper:** A beautiful, ever-changing abstract wallpaper generated from your own data.
-   **Continuous Evolution:** The wallpaper's state from one day carries over to the next, creating a single, continuous artistic journey.
-   **Privacy-First Data Analysis:** The app only tracks the *duration* of interactions, not the *content*. It monitors keyboard visibility, not what you type.
-   **Configurable App Categories:** Easily define which apps count as "work," "social," or "entertainment" via a simple XML file.
-   **On-Demand Generation:** Manually trigger an analysis at any time to see how your day has impacted your "soul" so far.
-   **Modern Android Tech:** Built with Kotlin, Coroutines, WorkManager, and Room, following modern architectural best practices.

---

## How It Works

The app's architecture is designed for efficiency and separation of concerns.

1.  **Data Collection:**
    -   A `UsageStatsManager` tracks time spent in categorized apps (work, social, etc.).
    -   An `AccessibilityService` (`KeyboardMonitorService`) tracks the *duration* of keyboard visibility to represent "creation."
    -   A `WallpaperService` tracks screen orientation changes to represent "dynamism."

2.  **Daily Processing:**
    -   A `DailyProcessorWorker`, managed by **WorkManager**, runs once every 24 hours (or when triggered manually).
    -   It fetches the last 24 hours of data from a `DataRepository`.
    -   It calculates a set of forces (`Fx_driving`, `Fy_driving`) and system parameters (`alpha`, `beta`) based on your activity.

3.  **Image Generation:**
    -   The calculated parameters are fed into the `ChaosEngine`.
    -   The engine runs a simulation based on a dynamic system algorithm, plotting tens of thousands of points to create a unique bitmap.
    -   The final `(x, y)` coordinates of the simulation are saved to be used as the starting point for the next day.

4.  **Display:**
    -   The generated bitmap is saved to the app's internal storage.
    -   The worker sends a broadcast to the `ChaosWallpaperService`.
    -   The wallpaper service loads the new bitmap and immediately displays it, providing a seamless update.

---

## Setup & Installation

To build and run this project yourself:

1.  **Clone the repository:**
    ```bash
    git clone https://github.com/your-username/chaosoul.git
    ```
2.  **Open in Android Studio:** Open the cloned folder in the latest version of Android Studio.
3.  **Build the project:** Let Gradle sync and build the app.
4.  **Run the app on a device or emulator.**

5.  **Grant Permissions:** The app requires two very sensitive permissions to function. You must grant them manually from the app's main screen.
    -   **Usage Access:** Tap "Grant" and enable access for ChaoSoul in the system settings. This allows the app to see which apps you use.
    -   **Keyboard Monitor:** Tap "Enable" and enable the "ChaoSoul Keyboard Monitor" accessibility service. This allows the app to know *when* the keyboard is on screen.

6.  **Set the Wallpaper:** From the app's main screen, tap "Set" on the Live Wallpaper step and choose "ChaoSoul" as your live wallpaper.

7.  **Generate Your First Soul:** Tap the **"Analyze & Generate Now"** button to create your first wallpaper immediately.

---

## Screenshots

| Main Dashboard | Wallpaper Preview |
| :---: | :---: |
| ![Main UI](https://via.placeholder.com/400x800.png?text=Main+Dashboard+UI) | ![Wallpaper](https://via.placeholder.com/400x800.png?text=Example+Wallpaper) |


---

## Technology Stack

-   **Language:** [Kotlin](https://kotlinlang.org/)
-   **Architecture:** MVVM-like (View, Repository, Worker)
-   **Asynchronous Programming:** [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html)
-   **Background Processing:** [WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager)
-   **Database:** [Room](https://developer.android.com/topic/libraries/architecture/room)
-   **UI:** Android Views with ViewBinding, Material Components
-   **Core Components:** `LiveWallpaperService`, `AccessibilityService`

---