# Galaxy A55 test checklist

Things that can only be properly checked on real Samsung hardware. Run the whole list on the A55
(latest Android / One UI) before V1 rollout. Each item says what to do and what "pass" looks like.
Items marked *(not built yet)* are known needs for planned steps; the feature must exist first.

Emulator notes that explain some of these items are in the "Why" lines.

## Screensaver (DockDreamService)

- [ ] **AuraDock appears in the screensaver picker.**
  Settings > Display > Screen saver. Pass: "AuraDock" is listed and can be selected; the start option
  offered is "while charging".
  *Why: confirmed by the owner that third-party savers (Google Photos) are listed; AuraDock must too.*
- [ ] **Starts by itself while charging.**
  Select AuraDock, plug in a charger, leave the phone alone past the screen timeout. Pass: AuraDock's dock
  screen appears (not Samsung's clock or a black screen) within a few seconds of the timeout.
- [ ] **"Stay awake" does not hide the saver.**
  Check Developer options > Stay awake is off (it is on in the emulator). Pass: with it off, the saver starts;
  note in the results if it was on and the saver never started (expected: it never starts).
- [ ] **Closes on touch.**
  With the dock showing, tap the screen. Pass: dock closes immediately and the phone is awake.
- [ ] **Live clock.**
  Leave the dock showing across a minute change. Pass: the time updates at the start of the minute.
- [ ] **Dark room does not swap AuraDock for another clock.**
  The emulator replaced AuraDock with the system's "low light clock" when its light sensor read dark.
  Run the dock in a dark room. Pass: AuraDock stays; if the phone shows its own clock instead, record it.
- [ ] **Does not start when not charging.**
  Unplug and let the screen time out. Pass: normal sleep, no dock.

## Orientation gate (accelerometer)

- [ ] **Landscape on a real stand.**
  Put the phone on your actual stand, both landscape directions, and let the saver start. Pass: dock shows
  upright, not sideways or upside-down, in both directions.
- [ ] **Real stand tilt.**
  Note the tilt of your stand. Pass: the dock still shows. Fail example: the dock closes because the phone
  leans back too far (see the 53-degree limit in `OrientationGate.kt`; record the stand's tilt if it fails).
- [ ] **Auto-rotate locked to portrait.**
  Quick settings: set "Portrait" (rotation locked). Put the phone in landscape and let the saver start.
  Pass: dock still shows upright (AuraDock rotates the content itself), left and right.
- [ ] **Auto-rotate on.**
  Same with Auto rotate on. Pass: dock shows upright, content is not rotated twice.
- [ ] **Portrait and flat close the saver.**
  Start the saver with the phone upright, then lying flat on a table. Pass: each closes within about a second
  and the phone sleeps normally.
- [ ] **First reading is not slow.**
  Pass: the screen is black for no more than about a second before the dock appears.
- [ ] **Flipping the stand while docked.**
  While the dock shows, turn the phone from left-landscape to right-landscape. Pass: content turns upright
  again without closing.

## Battery, alarms, listener *(not built yet)*

- [ ] **Sleeping apps / Deep sleeping apps.**
  Settings > Battery > Background usage limits. With AuraDock in "Sleeping apps", a task due in 5 minutes
  still notifies on time; then add AuraDock to "Never sleeping apps" and repeat. Pass: the notification
  arrives within a minute of the due time at least when set to Never sleeping; record behavior for both.
- [ ] **Exact alarms after reboot.**
  Schedule a task for 10 minutes ahead, reboot the phone. Pass: the notification still fires on time.
- [ ] **Notification listener disconnects.**
  Enable notification access, then let the phone idle overnight / force-stop the app. Pass: after reopening
  AuraDock, mirrored notifications appear again without re-granting access.

## Do Not Disturb *(not built yet)*

- [ ] **DND with Modes and Routines.**
  Grant DND access, enter dock mode. Pass: Samsung shows Do not disturb on; a call from a starred contact
  rings; a normal app notification is silent.
- [ ] **State is restored.**
  Try with DND off before docking, and again with DND already on. Pass: leaving dock mode puts it back exactly
  as it was in each case, including after force-stopping AuraDock while docked.
