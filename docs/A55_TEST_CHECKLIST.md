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
- [ ] **Bumping or picking up while docked does not close the dock.**
  With the dock showing, nudge the phone, tilt it toward portrait, or lay it flat. Pass: the dock stays until you
  touch the screen (only the touch closes it).
- [ ] **Flipping the stand while docked.**
  While the dock shows, turn the phone from left-landscape to right-landscape. Pass: content turns upright
  again without closing.

## Templates and display

- [ ] **Minimal is visibly dim on the AMOLED.**
  Pick Minimal, dock the phone in a dark room, in the screensaver and in the app. Pass: the screen is very dim
  (window brightness is set to 5%; the emulator only logs it), the digits are soft gray, and nothing looks
  pure white. Also check the time is still readable in a dim room.
- [ ] **Samsung honors the screensaver's brightness request.**
  Same check, screensaver only. Pass: Minimal is dimmer than Classic. If Samsung ignores window brightness
  inside a screensaver, record it (the fix would be a different dimming method).
- [ ] **Classic looks the same in the screensaver and in the app.**
  Dock via the screensaver, then via the app (charging + landscape). Pass: same layout, same clock, same card.
- [ ] **Burn-in drift is invisible but real.**
  Leave the dock for 10+ minutes. Pass: you do not see it move, but a marker on the screen edge shows the
  content has shifted a few millimeters after several minutes.
- [ ] **Chosen template survives a reboot.**
  Pick Minimal, reboot, dock. Pass: Minimal is still selected in the Templates screen and used by the dock.
- [ ] **Weather shows a dash, not fake data.**
  Pass: Classic shows "—" under the date (until step 8 adds real weather).

## Dock now and the in-app dock

- [ ] **"Dock now" forces landscape.**
  Hold the phone upright (portrait, auto-rotate off or on), open AuraDock, tap "Dock now". Pass: the screen turns
  to landscape and shows the dock; turning the phone upside down flips it with the sensor (either landscape).
- [ ] **One-time "Viewing full screen" notice.**
  The first time the dock hides the system bars, Android shows a "Viewing full screen / Got it" dialog. Pass:
  it appears once, "Got it" dismisses it, and it does not block the dock afterwards. (It appeared on the
  emulator; Samsung may word it differently or not show it.)
- [ ] **System bars and gestures.**
  In the dock, the status bar and navigation bar are hidden. Pass: swiping from the screen edge shows them for a
  moment and they hide again; the dock is not left with a bar stuck on screen.
- [ ] **Touch and Back close the dock.**
  Pass: a tap anywhere closes it; the Back gesture closes it; either way the dashboard comes back in portrait
  (not stuck in landscape) and the screen can sleep normally again.
- [ ] **Automatic dock does not re-open after a touch.**
  Charge the phone in landscape (on the stand) with the app open; the dock shows. Tap to close. Pass: the
  dashboard stays (no instant re-open); lift the phone to portrait and put it back in landscape: the dock shows
  again.
- [ ] **Dock now when not charging.**
  Unplug and tap "Dock now". Pass: dock shows and the screen stays on while it is visible; it stops staying on
  after you close it.

## Weather and location

- [ ] **Location permission prompt on Samsung.**
  Tap "Allow location for weather" on the dashboard. Pass: Samsung shows the prompt for *approximate* location only
  (no "Precise" switch), "While using the app" works, and the weather line appears within ~10 seconds.
- [ ] **Deny, then the quiet fallback.**
  Deny the prompt. Pass: the dashboard and the Classic dock show "—" (no error text, no fake numbers) and the link
  "Allow location for weather" stays. Deny twice: the link changes to "Allow location in Settings for weather" and opens
  AuraDock's app settings.
- [ ] **Location services switched off.**
  Turn the phone's Location toggle off (quick settings) with permission granted. Pass: "—", no crash. Turn it back on and
  reopen the app: weather returns (allow the 1-minute retry pause).
- [ ] **Real location, real numbers.**
  Compare with a weather app at your real location. Pass: city name and temperature within a degree or two; condition
  matches (clear / cloudy / rain).
- [ ] **City name from Samsung's Geocoder.**
  Pass: the city shows next to the condition. If the phone has no geocoder the city is simply left out (note which).
- [ ] **Units.**
  Pass: °C on an Indian/UK/EU region setting. Change Settings > General management > Language > Region to the United
  States: after reopening, the same weather shows in °F.
- [ ] **Offline.**
  Turn Wi-Fi and mobile data off with no saved weather (clear the app's storage): "—". With recent weather saved (under
  30 minutes old) it keeps showing until it is 3 hours old, then shows "—".
- [ ] **Screensaver shows weather without opening the app.**
  Let the screensaver start from cold (force-stop the app first). Pass: weather appears in the Classic dock within a few
  seconds (it may show "—" for a moment first). Records whether Samsung lets the screensaver use the network.
- [ ] **Refresh timing.**
  Leave the phone docked for an hour. Pass: the temperature updates about every 30 minutes only when you wake or dock it
  (there is deliberately no background refresh).

## Focus template

- [ ] **Readable at a glance.**
  Dock with Focus selected. From about 1.5 m: Pass: the clock is readable, and the NEXT / TASK lines are legible but
  clearly secondary (no glare, mostly black).
- [ ] **Next event is right.**
  With real calendar events: Pass: it shows the next *timed* event that has not started, skips all-day events and events
  already under way, and shows nothing for events once none are left today.
- [ ] **Top task is right.**
  With a few tasks: Pass: an overdue task wins (earliest overdue first); otherwise the earliest due time; otherwise the
  oldest task without a time. Tick the shown task off in the app: the next one takes its place within a minute.
- [ ] **Nothing to show.**
  Finish every task and clear today's events: Pass: just the clock, no empty labels.

## Productivity template and readability

- [ ] **Readable from across a desk.**
  Dock the phone on your stand with Productivity selected and stand 1 m, then 2 m away. Pass: the time is readable at 2 m;
  task titles and event titles are readable at 1 m. Note the distance at which task titles stop being readable. If they fail
  at 1 m, the font sizes in `DockContent.kt` (tasks 26sp, events 24sp) need to go up, and the number of rows down.
- [ ] **Everything fits the A55 screen.**
  With 4+ open tasks (one overdue), "+N more", and 3 upcoming events: Pass: nothing is clipped at the bottom, with the
  slow burn-in drift moving it a few millimetres. Try long titles (30+ characters): they end in "…" and never wrap.
- [ ] **Camera cutout.**
  In landscape the punch-hole camera sits on one short edge. Pass: no text runs under it, in either landscape direction.
- [ ] **Burn-in safety.**
  Leave Productivity docked for a few hours at night. Pass: the screen is only thin text on black (no bright blocks), and
  afterwards a black screenshot/wallpaper test shows no ghosting of the divider line or text.
- [ ] **Colours at night.**
  In a dark room. Pass: white clock digits are not glaring; task and event text is soft gray-white; the overdue amber is
  visible but not harsh.
- [ ] **All four templates.**
  Templates screen shows Classic, Focus, Minimal, Productivity with correct previews of *your* real tasks/events/weather;
  choosing each and docking looks the same as its preview. Pass: yes for all four, in the app and in the screensaver.

## Tasks (dashboard)

- [ ] **Swipe to delete does not fight the Back gesture.**
  Samsung's gesture navigation treats a swipe from the screen edge as Back. Swipe a task left, starting well inside
  the row, then starting near the right edge. Pass: the first deletes the task; the second may trigger Back, but
  the app never closes unexpectedly and a task is never deleted by mistake.
- [ ] **Typing a task on Samsung Keyboard.**
  Add a task with a title and a time. Pass: the keyboard does not cover the "Add task" button (the sheet scrolls
  or resizes), the hour/minute boxes accept typing, and Done on the keyboard saves the task. (On the emulator
  fast scripted typing scrambled the time boxes; this check is about real fingers.)
- [ ] **Overdue highlight appears by itself.**
  Add a task due 2 minutes from now and leave the dashboard open. Pass: within a minute of the due time the row
  turns amber without touching anything.
- [ ] **Midnight.**
  Tick off a task late in the evening, then look again after midnight. Pass: the finished task is gone; an
  unfinished one from the day before is still there.

## Task reminders (alarms and notifications)

- [ ] **Notification permission prompt.**
  Fresh install. Add the first task that has a time. Pass: Android asks "Allow AuraDock to send you notifications?"
  once. Allow: reminders appear. Deny, then add another timed task: no second prompt, the task is still saved, no
  notification appears (and nothing crashes).
- [ ] **Exact-alarm permission flow.**
  New installs on Android 14+ start with exact alarms *denied*. Add a task with a time: the sheet shows
  "Reminders may arrive a few minutes late" with an "Allow exact alarms" button. Tap it. Pass: Android's "Alarms &
  reminders" page for AuraDock opens; switch it on and come back: the hint is gone. Then add a task due in 2
  minutes. Pass: the notification arrives at the minute (not 5+ minutes later).
- [ ] **Inexact fallback is usable.**
  With exact alarms off, add a task due in 5 minutes. Pass: the notification still arrives (may be a few minutes
  late). Note how late it was.
- [ ] **Reminder on time with the screen off.**
  Add a task due in 2 minutes, lock the phone, put it down. Pass: the screen wakes/the notification sounds at the
  due minute, and tapping it opens the AuraDock dashboard.
- [ ] **Tapping the notification.**
  Pass: opens AuraDock on the dashboard; the notification disappears.
- [ ] **Checking off or deleting cancels the reminder.**
  Add a task due in 3 minutes, tick it off (or swipe-delete) after 1 minute. Pass: no notification at the due time.
- [ ] **Editing the time reschedules.**
  Add a task due in 10 minutes, edit it to 3 minutes. Pass: it fires at the new time only, once.
- [ ] **Alarms survive a reboot.**
  Add a task due in 10 minutes, restart the phone, do not open AuraDock. Pass: the notification still arrives at the
  due time. (This is the BOOT_COMPLETED receiver; on the emulator it could only be checked that the receiver is
  registered, since a script cannot send that protected broadcast.)
- [ ] **Alarms survive an app update.**
  Install a new build over the old one with a task pending. Pass: it still fires (MY_PACKAGE_REPLACED).
- [ ] **Samsung "Sleeping apps" / "Deep sleeping apps".**
  Settings > Battery > Background usage limits. Put AuraDock in "Sleeping apps", add a task due in 5 minutes, lock
  the phone, wait. Record whether it fires and how late. Then move AuraDock to "Never sleeping apps" and repeat.
  Pass: fires on time at least when set to "Never sleeping apps". If it fails when sleeping, the onboarding screen
  must tell testers to set "Never sleeping".
- [ ] **Force-stop clears alarms (expected).**
  Settings > Apps > AuraDock > Force stop: pending reminders are lost until the app is opened again. Pass: after
  opening AuraDock once, pending timed tasks fire again (the app resets them on start-up).
- [ ] **Do Not Disturb.**
  With DND on (manually), a task reminder is silenced like any other notification. Record the behaviour. (Dock mode
  DND handling arrives in step 9.)
- [ ] **Notification channel settings.**
  Long-press a reminder > settings. Pass: a "Task reminders" channel exists; turning its sound off works.

## Notification listener *(not built yet)*

- [ ] **Notification listener disconnects.**
  Enable notification access, then let the phone idle overnight / force-stop the app. Pass: after reopening
  AuraDock, mirrored notifications appear again without re-granting access.

## Do Not Disturb while docked (Focus)

- [ ] **Granting access.**
  Dashboard > Focus > "Allow Do Not Disturb access". Pass: Samsung's access list opens (it may be called "Do not disturb
  access" or "Modes access"), AuraDock is listed, switching it on and pressing Back returns to the Focus page showing
  "Ready: Do Not Disturb turns on when you dock".
- [ ] **DND turns on when docked, in the app and in the screensaver.**
  With Do Not Disturb off, tap "Dock now" (then separately let the screensaver start). Pass: the Do Not Disturb icon shows
  in the status bar / the quick-settings tile turns on while the dock is showing, within a second of docking.
- [ ] **Starred contacts really ring, others do not.**
  Star a contact (Contacts > favourites). Dock the phone and call it from another phone: Pass: the call from the starred
  contact rings; a call from a non-starred number is silent; an alarm (set a 2-minute one) still rings; a WhatsApp/SMS
  message makes no sound.
- [ ] **Your own settings come back exactly.**
  First set Do Not Disturb up your way (for example allow "Calls from favourites" and "Alarms", and some "Hide
  notifications" options), leave it OFF, dock, undock. Pass: open the DND settings: every choice is as before. (Android may
  show two internal flag bits differently; the visible options must be identical.)
- [ ] **Never overwrites your own DND.**
  Turn Do Not Disturb on yourself (and, separately, a Samsung mode such as Sleep or Driving). Dock, undock. Pass: it is on
  the whole time and is still on afterwards, with your settings untouched.
- [ ] **You change it while docked.**
  Dock, then switch Do Not Disturb off yourself from the shade. Undock. Pass: it stays off (AuraDock does not switch it back
  on or "restore" anything over your choice).
- [ ] **Modes and Routines.**
  With a Samsung routine that turns a mode on/off at a time (for example Bedtime at 22:00), dock across that time.
  Pass: the routine is not broken, and after undocking the phone is in the state the routine left it, not an old one. Also
  note whether a mode named "AuraDock" appears in Modes while docked (newer Android) and disappears after.
- [ ] **Force-stop while docked.**
  Dock (DND on), then Settings > Apps > AuraDock > Force stop. Pass: DND is still on right after (nothing can restore it
  yet); open AuraDock: DND is off again within a second.
- [ ] **Reboot while docked.**
  Dock via the screensaver, restart the phone. Pass: after the phone is up (without opening AuraDock) DND is off.
- [ ] **"Focus while docked" switch.**
  Turn it off in Focus settings: docking leaves DND alone; turn it off while docked: DND is restored at once.
- [ ] **Ringer mode is untouched.**
  Phone on vibrate or silent before docking: Pass: same after undocking.
- [ ] **Task reminders while docked.**
  A task reminder due while docked is silent (Do Not Disturb blocks it); it appears when you undock. This is expected;
  record whether it is acceptable.
- [ ] **Quick re-dock does not flicker.**
  Dock, undock, and dock again within 3 seconds. Pass: DND stays on throughout (the restore waits 3 seconds).
