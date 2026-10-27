# GeoGuess India

An Android map-guessing game set in India. Each round shows a photo of a landmark. You have 30 seconds to drop a pin where you think it is. The closer you are, the more points you get.

Built for the course **Advance Android Programming** (Symbiosis, T3374).

## How to play

1. Sign up or log in with email and password.
2. Pick a difficulty and a region, then tap **Start**.
3. Look at the landmark photo and tap the map to drop a pin before the timer runs out.
4. See the real location, the distance and your points, plus a fun fact with a **Learn more** link.
5. After 5 rounds, see your result table, share your score, and check the leaderboard.

## Scoring

```
distance = Location.distanceBetween(guess, answer)   // metres, converted to km
points   = max(0, 5000 - km * 2.5)
```

- A perfect guess gives 5,000 points.
- 2,000 km or more away gives 0 points.
- 5 rounds, so the maximum is 25,000.

## Features

- Email and password accounts with Firebase Auth, including forgot password
- Online leaderboard with Cloud Firestore, and a local "This device" leaderboard with SQLite
- Interactive map with osmdroid and Stadia Maps tiles
- 30-second countdown timer with sound effects
- Landmark list loaded from a JSON file over HTTP, cached in SQLite for offline play
- Photos downloaded and saved to app storage
- Result screen built with a TableLayout
- Share score (implicit intent) and open the location in a maps app (`geo:` intent)
- Learn more page in a WebView
- Settings kept in SharedPreferences (name, last difficulty, sound, best score)
- Light and dark mode

## Tech stack

| Area | Choice |
| --- | --- |
| Language | Kotlin |
| Min / target SDK | 24 / 34 |
| Maps | osmdroid 6.1.20 with Stadia Maps tiles |
| Auth and online data | Firebase Auth, Cloud Firestore (Firebase BoM 34.9.0) |
| Local data | SQLite (`SQLiteOpenHelper`), SharedPreferences |
| Networking | `HttpURLConnection`, `org.json`, `AsyncTask` |
| UI | Material 3, XML layouts |

## Project structure

```
app/src/main/java/com/example/geoguessindia/
  firebase/      AuthHelper, LeaderboardRepository
  data/          DbHelper, PrefsManager, models
  game/          GamePicker, ScoreCalculator, SoundPlayer
  network/       PlacesLoaderTask, ImageLoaderTask
  map/           MapConfig, StadiaTileSource
  ui screens     Login, Signup, Main, Game, Result, Leaderboard, WebView
app/src/main/res/  layouts, drawables, colours, strings, themes (light and dark)
```

## Setup

You need your own keys. They are not stored in this repo.

### 1. Clone and open

```
git clone https://github.com/reaper22/geoguess-india-app.git
```

Open the folder in Android Studio and let Gradle sync.

### 2. Stadia Maps key

Create a free key at [stadiamaps.com](https://stadiamaps.com), then add this line to `local.properties` in the project root (this file is ignored by Git):

```
STADIA_API_KEY=your_key_here
```

### 3. Firebase

1. Create a project in the [Firebase console](https://console.firebase.google.com).
2. Add an Android app with the package name `com.example.geoguessindia`.
3. Download `google-services.json` and place it in the `app/` folder.
4. Turn on **Authentication > Email/Password**.
5. Create a **Cloud Firestore** database.
6. Paste these rules and publish them:

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /scores/{id} {
      allow read: if request.auth != null;
      allow create: if request.auth != null
        && request.resource.data.uid == request.auth.uid
        && request.resource.data.score is int
        && request.resource.data.score >= 0
        && request.resource.data.score <= 25000;
      allow update: if false;
      allow delete: if request.auth != null && resource.data.uid == request.auth.uid;
    }
    match /users/{uid} {
      allow read, write: if request.auth != null && request.auth.uid == uid;
    }
  }
}
```

### 4. Landmark data

The landmark list (`places.json`, 50 landmarks) and the photos are hosted on GitHub Pages. If you host your own copy, change `PLACES_URL` in `AppConfig.kt`.

### 5. Run

Pick an emulator or a phone and press **Run**. The app needs internet for sign-in, the map and the first load of places.

## Known limitations

- Scores are calculated on the phone, so a determined user could cheat. A real release would validate scores on a server.
- The Stadia key is inside the APK. Restrict it by app or domain in the Stadia dashboard before publishing.
- `AsyncTask` is deprecated. It is used here because the course covers it. `ExecutorService` with a `Handler` is the modern replacement.
- `MapTestActivity` and `DataTestActivity` are leftover developer screens.
- The package name `com.example.*` must be changed before publishing to Google Play.

## Author

Rishi Chaurasia, Symbiosis.
Aishwarya Gilhotra, Symbiosis.
