# Netcat for Android - Enhanced Version

This document outlines the architectural improvements, bug fixes, and new features implemented in this version of the Netcat for Android wrapper.

## 🛠 How It Works (Technical Architecture)

The application acts as a front-end wrapper for the powerful `ncat` binary (from the Nmap project), which is cross-compiled for Android architectures and bundled as a shared library (`libncat.so`).

1.  **Entry Point (`AndroidNetcatHome`):** The user enters a standard netcat command (e.g., `nc -l 4444`).
2.  **Command Injection:** The app identifies the `nc` or `ncat` prefix and dynamically injects the absolute path to the internal `libncat.so` executable located in the app's native library directory.
3.  **Process Execution:** The command is wrapped in a shell (`/system/bin/sh -c`) and executed via a `ProcessBuilder` within a dedicated background thread (`NetcatWorker`).
4.  **Bidirectional Communication:**
    *   **Input:** Text entered in the UI is added to a thread-safe `LinkedList` (Send Queue) and written to the process's `STDIN`.
    *   **Output:** The `NetcatWorker` continuously monitors the process's `STDOUT/STDERR` and updates the UI via a `Handler` on the Main Looper.

---

## 🐛 Bug Fixes

### 1. Battery & CPU Optimization (Critical)
- **Problem:** The original `NetcatWorker` used a "tight loop" while waiting for data. Even when the connection was idle, the thread would cycle millions of times per second, causing 100% CPU usage on one core and rapid battery drain.
- **Fix:** Implemented an idle-state detection. If no data is being sent or received in a loop cycle, the thread now sleeps for **50ms**. This reduces CPU usage from ~100% to <1% during idle periods without sacrificing responsiveness.

### 2. Shell Command Integrity
- **Problem:** Commands were previously split by spaces, which broke complex commands containing quotes or escaped characters (e.g., `nc -e "/system/bin/sh"`).
- **Fix:** Switched to string-based path injection. The app now preserves the user's exact string formatting, ensuring that the shell correctly interprets quoted arguments and complex flags.

### 3. Lifecycle Safety
- **Problem:** The app would attempt to launch the Netcat process even if the command validation failed, leading to crashes or "ghost" processes.
- **Fix:** Added strict return-pathing in `onCreate`. The activity now safely halts if an invalid command is detected.

---

## ✨ New Features

### 🚀 Terminal Auto-Scroll
The terminal output view now automatically follows the latest data. As new logs or responses arrive, the view smoothly scrolls to the bottom, allowing for real-time monitoring of active connections without manual scrolling.

### 🧹 Clear Terminal Action
A new **"Clear"** button has been added to the top Action Bar. Users can now instantly wipe the terminal buffer during a session to focus on new incoming data.

### 🛡 Improved Robustness
- Added `.trim()` to user inputs to prevent accidental leading/trailing space errors.
- Added a fallback mechanism for the executable path injection to ensure the app remains functional even with non-standard command prefixes.

---

## 📝 Usage Note
To use the app, simply type a standard netcat command in the home screen.
*   **Example (Listener):** `nc -l -p 8080`
*   **Example (Client):** `nc 192.168.1.5 8080`
*   **Complex:** `nc -v --ssl google.com 443`
