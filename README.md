# StudyOS 📚⚡
### Dedicated Offline Android Operating System for CA Intermediate Preparation
*Engineered for ICAI CA Inter Aspirants (New Scheme)*

[![Build StudyOS Debug APK](https://github.com/rahul23145/StudyOS/actions/workflows/build.yml/badge.svg)](https://github.com/rahul23145/StudyOS/actions/workflows/build.yml)

---

## 🌟 Overview

**StudyOS** is a native Android application built in Kotlin, tailored specifically for CA Intermediate preparation under the ICAI New Scheme. It is designed to be **100% offline**—it requires **no internet permissions, no cloud accounts, no login gates, and no Android Studio** for the end user.

All data is stored directly on your device in a high-performance local **Room SQLite Database**.

---

## 🎯 Pre-Configured Daily Routine & Architecture

StudyOS embeds the student's high-yield daily routine out-of-the-box, while keeping **every activity 100% editable**:

| Time Slot | Duration | Activity | Category & Description |
|---|---|---|---|
| **07:00 – 07:15 AM** | 15 min | **Wake Up & Hydrate** | Fixed wake time (7:00 AM) for circadian alignment |
| **07:15 – 08:15 AM** | 60 min | **Morning Gym** | 1 hour strength & endurance training |
| **08:15 – 08:40 AM** | 25 min | **Freshen Up & Breakfast** | High-protein nutrition at home |
| **08:40 – 09:00 AM** | 20 min | **Travel: Home ➔ Study Room** | 20-minute transition to distraction-free study environment |
| **09:00 – 11:18 AM** | **138 min (2h 18m)** | **Costing Lecture 1** | Paper 4: Cost & Management Accounting (Daily Session 1) |
| **11:18 – 11:30 AM** | 12 min | **Break** | Eye rest, hydration, light stretch |
| **11:30 – 01:48 PM** | **138 min (2h 18m)** | **Costing Lecture 2** | Paper 4: Cost & Management Accounting (Daily Session 2) |
| **01:48 – 02:08 PM** | 20 min | **Travel: Study Room ➔ Home** | 20-minute travel home for lunch |
| **02:08 – 02:40 PM** | 32 min | **Lunch at Home & Rest** | Home-cooked meal & digestive relaxation |
| **02:40 – 03:00 PM** | 20 min | **Travel: Home ➔ Study Room** | 20-minute transition back to study room |
| **03:00 – 04:30 PM** | **90 min (1h 30m)** | **Audit Lecture 1** | Paper 5: Auditing & Ethics Standards |
| **04:30 – 04:45 PM** | 15 min | **Tea Break & Active Recall** | 15-minute break with concept memory review |
| **04:45 – 06:15 PM** | **90 min / Varied** | **Audit Lecture 2 / Shift** | Mon–Sat: Audit Lecture 2 (adjusted on MWF for tuition) |
| **06:00 – 07:00 PM** | 60 min | **Tuition Class (MWF)** | Monday, Wednesday, Friday evening tuition |
| **06:30 – 07:15 PM** | 45 min | **TTSS Running** | Tuesday, Thursday, Saturday, Sunday cardio & mental recharge |
| **08:20 – 10:20 PM** | **120 min (2 hours)** | **2-Hour Study Block** | Deep work: Costing practical problem solving & Audit question bank |
| **10:20 – 10:30 PM** | 10 min | **Short Break** | 10-minute mental break |
| **10:30 – 11:30 PM** | 60 min | **Revision & Tomorrow Planning**| Active recall of daily formulas and audit standards |
| **11:30 PM – 07:00 AM**| **7.5 hours** | **Sleep** | Strict sleep time (11:30 PM) for memory consolidation |

### ☀️ Sunday Audit-Only Mode
On Sundays, **zero Costing lectures** are scheduled. The entire day is optimized for:
1. **Audit SA 200/500 Series Deep Dive** (2-hour study block)
2. **Company Audit (Sec 139–148) & CARO 2020** (2-hour study block)
3. **Audit Lecture Sessions 1 & 2** (1h 30m each)
4. **TTSS Running & Active Recovery**
5. **Weekly Audit Mastery Mock Test** (2-hour study block)

---

## 📱 Features

1. **Daily Progress Dashboard**:
   - Visual progress bar showing % of today's tasks completed.
   - Dynamic **"Current Focus"** & **"Up Next"** cards calculating your active session in real-time.
   - Quick check-off list for today's activities.
   - Lecture counters for Costing and Audit.
   - Consecutive day streak counter.

2. **Editable Weekly Timetable**:
   - Full Monday to Sunday schedule browser.
   - **Edit any activity**: Change title, start time, end time, category, day, and notes.
   - **Add custom activities**: One-tap Floating Action Button.
   - **Delete activities**: With safety confirmation.
   - **Reset to Recommended CA Inter Routine**: Easily restore defaults anytime.

3. **Costing & Audit Lecture Tracker**:
   - Track total lectures watched vs total syllabus (default 85 Costing, 65 Audit).
   - Plus (+) and Minus (-) buttons to increment lectures on the fly.
   - Calculates total hours watched automatically.

4. **Dedicated Study Timers**:
   - Preset: **2-Hour Study Block** (120 min)
   - Preset: **Costing Lecture** (2h 18m = 138 min)
   - Preset: **Audit Lecture** (1h 30m = 90 min)
   - Preset: **15-Minute Break** (15 min)
   - Preset: **Tuition** (60 min)
   - Audio chime & haptic vibration alerts when timer completes.
   - One-tap button to save completed session into the study database.

5. **Analytics & Adherence**:
   - Total study hours logged today and past 7 days.
   - Routine Integrity Checklist (Gym, Running, Tuition, Meals, Sleep).
   - Chronological study history log.

---

## 🚀 How to Download & Install the APK (No Android Studio Required!)

You do **not** need Android Studio or a developer environment to use StudyOS.

### Download from GitHub Actions:
1. Open this repository on GitHub: [`https://github.com/rahul23145/StudyOS`](https://github.com/rahul23145/StudyOS).
2. Click on the **Actions** tab at the top.
3. Select the latest workflow run: **"Build StudyOS Debug APK"**.
4. Scroll down to the **Artifacts** section at the bottom of the page.
5. Download **`StudyOS-CAInter-Debug-APK.zip`**.
6. Unzip the file on your computer or phone to obtain `app-debug.apk`.
7. Transfer or open the `.apk` file on your Android phone and tap **Install**.
8. Enjoy distraction-free, 100% offline study tracking!

---

## 🛠️ Architecture & Tech Stack

- **Language**: Kotlin 1.9.23
- **Minimum SDK**: Android 7.0 (API 24)
- **Target SDK**: Android 14 (API 34)
- **Architecture**: MVVM (Model-View-ViewModel) + Repository Pattern
- **Local Storage**: Android Jetpack Room (SQLite underneath) with Kotlin Flow & Coroutines
- **UI & Layouts**: Google Material 3 Design Components, ViewBinding
- **CI/CD**: GitHub Actions (`.github/workflows/build.yml`) on Ubuntu Runner with Java 17 and Gradle 8.7

---

## 🔒 100% Offline & Privacy Guarantee

- **Zero Network Permissions**: The app does not request `android.permission.INTERNET`.
- **Zero Cloud Accounts**: No email, no passwords, no remote databases.
- **Zero Tracking**: Your study habits and notes remain entirely on your physical phone storage.
