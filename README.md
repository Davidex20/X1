# Xpheasible Call Studio (Android)

A no-PC, rootless Android studio for sharing a selected photo, video file, phone camera preview, or compatible HTTPS web page during Google Meet or WhatsApp video calls **using the call app's built-in screen sharing**.

**Not a virtual-camera replacement.** Stock Android does not allow ordinary apps to replace the physical camera chosen by an unrelated app such as WhatsApp or Google Meet. Screen sharing is the supported workaround. Do not confuse screen sharing with a camera input.

## Experimental desktop browser & floating minimize (v0.2)

Tap **DELULU DESKTOP VIEW · FLOAT**. The built-in browser requests a desktop user-agent,
shows desktop layouts with pinch zoom, permits user-selected photo uploads, and has a
**Float** button to shrink into Android Picture-in-Picture. Tap the PiP window to expand it.
The window may pause the site or lose camera access while other applications are active.

For Delulu sign-in, choose **Chrome**, then Chrome menu → **Desktop site**. This uses
Delulu's official web app, and generally works better with single-sign-on providers
than embedded Android WebViews. **A desktop layout/UA does not guarantee Delulu
will function on a phone**; Delulu may require desktop-only software/hardware and
can reject mobile devices. If the webpage still says "Desktop Required", no setting
in this app can guarantee it will work. The feature does not integrate Delulu
as a phone-wide virtual camera. Use the existing screen-share stage for supported content.

## How it works
1. Install the debug APK from the latest successful GitHub Actions **Build Android APK** run (Artifacts -> Xpheasible-Call-Studio-APK).
2. Open Xpheasible Call Studio and select Photo, Video, Camera, or an HTTPS webpage. Photos/videos come from your device picker; no cloud upload.
3. Open WhatsApp or Google Meet from the shortcut in Call Studio and start a video call.
4. In the call, use the app's **Share screen** feature. Switch back to Call Studio from Recents and tap **Fullscreen stage** to present the source.
5. Use gesture navigation to return to the call; stop screen sharing when finished.

Video playback audio may or may not be included depending on the calling app's support for internal-audio screen sharing. Camera preview might be unavailable while the calling app owns the physical camera. DeluluStream is a separate desktop/web product; this app does not integrate an unverified DeluluStream API nor claim its PC virtual camera works on Android.

## Technical
- Java, Android SDK, no external libraries or trackers
- Minimum Android 8.0 (API 26), target SDK 35
- Camera permission required for camera preview only
- Android Storage Access Framework for images and videos; HTTPS for websites
- GitHub Actions assembles a debug APK

## Build locally
With Android SDK + Gradle 8.9 + JDK 17 installed: `gradle :app:assembleDebug`.

## Security / Privacy
Local media remains on your device; URLs in web mode load from their hosts. Screen sharing exposes the selected screen to the participants in your call, so avoid opening sensitive information while sharing.
