# Galaxy A55 test checklist

Things that can only be properly checked on real Samsung hardware. Run the whole list on the A55
(latest Android / One UI) before V1 rollout. Each item says what to do and what "pass" looks like.
Items marked *(not built yet)* are known needs for planned steps; the feature must exist first.

Emulator notes that explain some of these items are in the "Why" lines.

## How to install the APK

For testers. AuraDock is not on the Play Store yet, so it is installed from a file (`AuraDock-0.1.2.apk`). Menu names
vary a little between One UI versions; the idea is the same.

1. **Get the file onto the phone.** Download it from the link you were sent (Chrome saves it to Downloads), or copy it
   over USB. Open the **My Files** app and tap the APK, or tap "Open" on Chrome's download notification.
2. **Allow installs from that app (first time only).** Samsung shows "For your security, your phone is not allowed to
   install unknown apps from this source". Tap **Settings**, switch on **Allow from this source** (for Chrome or My Files,
   whichever you opened the file with), press back, and tap **Install**. The same switch is under
   Settings > Apps > (the app) > Install unknown apps, or Settings > Security and privacy > More security settings >
   Install unknown apps.
3. **If the install is blocked by Auto Blocker.** Newer One UI turns on Auto Blocker, which stops installs from
   outside the stores. Go to Settings > Security and privacy > **Auto Blocker** and switch it off (or switch off only
   "Block installation of apps from unauthorized sources" if you see that option), then try again. You can switch it
   back on afterwards.
4. **If Google Play Protect asks.** A "Scan app?" or "App blocked" box can appear. Choose **More details > Install
   anyway** (or **Scan app** and then install). The app is signed by us but is not on Play, so Play Protect does not
   recognise it.
5. **Open AuraDock** and follow the setup. Every permission can be skipped and allowed later from the dashboard's
   **Setup** button.
6. **Keep it running.** Settings > Battery > Background usage limits: make sure AuraDock is not in "Sleeping apps" or
   "Deep sleeping apps" (add it to **Never sleeping apps**). Otherwise task reminders can arrive late or not at all.
7. **Updating.** Install the new APK over the old one: your tasks and settings stay. It only works because every build
   is signed with the same key; if the install says "App not installed", uninstall first (that deletes your tasks) and
   report it.
8. **Report** the version shown by Settings > Apps > AuraDock (it starts with 0.1.2) with anything you find.

## Screensaver (DockDreamService)

- [ ] **Onboarding "Dock it, and AuraDock appears" step.** The 3-frame animation loops; "Choose AuraDock as
  screensaver" opens the screensaver page and "Set screen timeout" opens Display settings. After choosing AuraDock
  and coming back, the first button turns into "✓ AuraDock is your screensaver" (if Samsung keeps the choice
  elsewhere the check just does not show; note it). The same explanation is on the Setup page.
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

## Notification apps and listener (Focus settings, step 10a)
- [ ] Focus page lists launchable apps with icons and names (Samsung apps, Secure Folder/duplicates appear once each, no AuraDock itself).
- [ ] Pick 3 apps: the 4th and later rows grey out; un-pick one and the others become tappable again. Choice survives closing and reopening the app.
- [ ] "Allow notification access" opens the Samsung page; after allowing, returning to AuraDock hides the amber note.
- [ ] Listener survives Samsung battery management: leave the phone idle and docked for an hour, then post a notification from a picked app; it still arrives in the cache. If not, check Settings > Battery > Background usage limits and set AuraDock to "Never sleeping".
- [ ] After a reboot, or after toggling notification access off and on, the listener reconnects without opening the app.
- [ ] Swiping a notification away in the shade removes it from AuraDock too.

## Dock notification strip (step 10b)
- [ ] With 1 to 3 apps picked, the latest notification from each shows as a small grey line at the bottom of Classic, Focus, Minimal and Productivity, in both the screensaver and "Dock now".
- [ ] Default shows only "App · sender"; message text appears only after turning on "Show message text on dock", and disappears again when turned off.
- [ ] The strip is clearly secondary to the clock: readable from about 1 m on the stand in a lit room, not glaring in a dark bedroom (Minimal is dimmer still).
- [ ] The strip vanishes when nothing is left: dismiss the notification on the phone (or in another room) and check the dock within a few seconds; the clock moves back to centre.
- [ ] Long names and messages cut off with "…" on one line and never run under the punch-hole or rounded corners in either landscape direction.
- [ ] Strip keeps drifting with the clock (no fixed bright pixels); check for burn-in after a night of docking.
- [ ] Privacy: with the toggle off, nothing of a message body is visible from across the room; Samsung "Hide content on lock screen" is not what controls this, AuraDock's own toggle is.
- [ ] Still shows after the phone has been docked overnight with Samsung battery management on (listener not put to sleep).

## Dock from anywhere (optional background watcher, 0.1.2)
Setup: Focus settings > "Dock from anywhere" on, allow "Display over other apps". Set AuraDock to "Never sleeping".
Watch decisions with `adb logcat -s DockWatcher` (lines like `pill: Countdown(secondsLeft=4)`, `opening the dock`).
- [ ] **Turning it on.** The switch sends you to "Display over other apps"; after allowing it and coming back the card says
  "On." and the notification "AuraDock is ready to dock" appears (quiet, no sound). Off again: the notification goes.
- [ ] **Onboarding step.** After the screensaver step there is an optional "Dock from anywhere" step; Next skips it.
- [ ] **Charging, landscape, still.** Put the phone sideways on the charger in any app (try Chrome and the home screen).
  Pass: after about 3 s a black pill "Docking in 5…" slides up; after 5 s the dock opens, upright.
- [ ] **Tap the pill.** Docks at once. **Cancel** does not dock.
- [ ] **Not charging.** Same, unplugged: "Switch to dock?" appears, tap docks, it hides itself after about 6 s.
- [ ] **Quiet after Cancel.** After Cancel (or the offer timing out), leave the phone on the stand and read something for a
  few minutes. Pass: the pill does not come back. Pick the phone up and put it back, or turn it to portrait and back:
  the pill comes again.
- [ ] **Hand-held.** Hold the phone sideways in your hand (landscape, typing, scrolling) for a minute. Pass: no pill.
  If it appears, note how still you were (the stillness limit may need loosening).
- [ ] **A real stand that wobbles.** Tap the screen on the stand (scroll a page, type). Pass: tapping does not make it
  think the phone was picked up and re-show the pill after a Cancel. If it does, note it (the pickup limit needs raising).
- [ ] **Portrait and flat.** Upright on the stand, and lying flat on a desk: no pill, ever.
- [ ] **YouTube in landscape on the charger.** Play a video full screen, sideways, on the charger. Pass: no pill while it
  plays. Pause it: the pill may appear (that is expected). Also try a muted video and a Spotify track: record what happens
  (a muted video may still show the pill; Cancel then keeps it quiet).
- [ ] **Phone call.** Be on a call with the phone sideways and still. Pass: no pill.
- [ ] **Already docked.** With the dock or the screensaver showing, no pill. Close the dock by touch while on the stand:
  no pill comes straight back.
- [ ] **Screen off.** Turn the screen off: logcat shows `not watching (screen off)`; on again: `watching`.
- [ ] **Reboot.** With the feature on, restart the phone and unlock. Pass: the notification returns without opening the app.
  (If it does not, note it: Samsung may block the start.)
- [ ] **Samsung background launch.** From another app, the dock opens when the countdown ends or the pill is tapped. If
  Samsung blocks it (the pill goes but nothing opens), note it.
- [ ] **Battery, one hour.** Fully charge, unplug, set the screen to stay on (or use the phone normally) for one hour with
  the feature ON; note the battery %. Repeat for an hour with it OFF (same use). Check Settings > Battery > AuraDock for its
  share. Pass: the difference is small (a point or two); record both numbers.
- [ ] **Turning it off** stops the service (notification gone) and the pill never appears again.

## Focus sessions (0.2.0)
Setup: install 0.2.0 over 0.1.4 (see "Upgrade" below). Give AuraDock Do Not Disturb access and notification access, and pick one app
in Focus settings. Watch the saved sessions with `adb logcat -s SessionTracker` (lines like `session saved: 130 s, 1 tasks, SessionCounts(held=4, through=0)`).
- [ ] **Upgrade.** Install 0.2.0 over 0.1.4 with tasks and a chosen template in place. Pass: it opens on the dashboard (not
  onboarding), the tasks and template are still there, nothing crashes.
- [ ] **Session line, readable at 1 m.** Dock on the stand, in a lit room and in a dark one. Pass: from about 1 m you can see the grey
  line under the clock and tell roughly how full it is; it is never louder than the clock; it is not coral. Check it on Bold, Classic,
  Focus, Productivity and Minimal (Minimal is dimmer by design). It must not touch the digits, the date or the strip in either landscape direction.
- [ ] **Session line moves.** After 30 minutes the line is about half full; after an hour it starts a new lap. It drifts with the clock
  (no fixed bright pixels): look for burn-in after a night of docking.
- [ ] **Under two minutes is ignored.** Dock for about a minute and undock. Pass: no card, and logcat says `ignored (under two minutes)`.
- [ ] **Summary appears on a real undock.** Dock for at least two minutes, then touch the screen to close the dock (try it after "Dock now",
  after the screensaver and after the Dock-from-anywhere pill). Pass: for "Dock now" and the pill the card shows on the dashboard straight away,
  for three seconds, tap closes it sooner, and it settles in softly. After the screensaver the card waits and shows when you next open the
  app (within ten minutes). It shows only once.
- [ ] **Handover does not split a session.** Let the screensaver start, then open the app while it is showing (or turn the phone). Pass: one
  session in logcat, not two.
- [ ] **Counts match real notifications.** With Do Not Disturb on (Focus while docked), send yourself messages from apps you did NOT pick,
  and from the app you did. Pass: the dock caption reads "N held · M through", where N is the number of notifications from the other apps
  and M the number from the picked app (one per notification, not per message: five texts in one chat count once). A call or message from
  a starred contact counts as "through". The card at the end shows the same held number. Nothing is counted for ongoing notifications
  (music, downloads), AuraDock's own reminders, or when Do Not Disturb is off.
- [ ] **Held counts are numbers only.** From across the room the caption says nothing about who wrote or what was said.
- [ ] **Tasks done.** A task ticked before you docked is not counted: dock for two minutes after ticking one and the card says
  "0 tasks done". A task ticked while the app is open during a session counts: close the dock, tick a task within three seconds and
  dock again (the two docks merge into one session); the card at the end says "1 task done".
- [ ] **Weekly line.** After a few sessions the dashboard says "N h M m focused this week" with seven bars, today on the right in cream. With no
  sessions in seven days it says "Dock your phone to start a focus session." The total matches the sessions you actually did.
- [ ] **Midnight.** Dock across midnight: the time is split between the two bars.
- [ ] **Battery.** One night docked with 0.2.0 (the line updates once a minute): note the battery use from Settings > Battery > AuraDock;
  it should match 0.1.4.

## Founder focus, batch B (0.2.1)
Setup: install 0.2.1 over 0.2.0 (see "Upgrade" below the 0.2.0 list). Allow calendar access. The checks that need a time of day (after 4 am,
after 6 pm) can be done on the day, or by changing the phone's clock.
- [ ] **Upgrade keeps data.** Install 0.2.1 over 0.2.0 with sessions and tasks in place. Pass: dashboard opens (not onboarding), the weekly
  line and the tasks are as before, nothing crashes.
- [ ] **A killed app keeps its session.** Dock for three minutes, then force-stop AuraDock (Settings > Apps > Force stop) while docked, and open
  the app. Pass: the weekly line gains about three minutes (at most a minute short). A force-stop inside the first two minutes saves nothing.
- [ ] **Morning question.** First dock after 4 am: "What's the one thing today?" with the field, Save and Skip, on the black screen. Pass: the
  keyboard does not hide Save; Save shows the clock with the thing as a quiet line with a circle under it; Skip shows the plain clock and it is not
  asked again that day; docking again the same morning does not ask again. The screensaver never asks (it cannot take touch), and the next in-app
  dock does.
- [ ] **Intention line on every template.** Bold, Classic, Focus, Productivity, Minimal. Pass: one quiet line under the clock, never touching the
  digits, the date or the notification strip (try with a notification showing), readable at about 1 m, dimmer on Minimal.
- [ ] **Ticking it.** Tap the circle on the dock: the line strikes through from left to right, goes grey and stays. Tap again to undo. The same
  intention is the first row of the dashboard's Today card and ticking either one updates both. It is never sent anywhere.
- [ ] **Next meeting.** Make a calendar event ten minutes ahead. Pass: from ten minutes before, the dock shows its title and "in N min" in coral,
  above the intention; from "in 2 min" the whole line slowly fades in and out (no movement, no sound); it disappears at the start time. An
  all-day event, a declined event and a cancelled event never show.
- [ ] **Evening shutdown.** Dock for at least two minutes after 6 pm and touch the screen to close it. Pass: the usual card for three seconds,
  then "Tomorrow's top task?" (tap the card to move on sooner). Save with a time and without: the task is not in today's list, it is in
  tomorrow's at that time (a reminder fires then), and tomorrow morning's question starts with that text. Skip leaves nothing. A second evening
  session the same evening shows only the normal card. A session before 6 pm gets no second step.
- [ ] **Pick up.** With an unfinished intention (or a task you made or edited today), dock again after an earlier dock. Pass: "Pick up: <task>" is the
  first line for 30 seconds and then goes by itself; a screensaver handing over to the app does not bring it back; it never shows for a finished
  task, nor on the dock that asks the morning question.
- [ ] **Stacking.** Meeting soon, intention open and a re-dock all at once: Pick up, then the meeting, then the intention, with the clock still
  readable on Bold.
- [ ] **People who can always reach you (add).** Focus settings, "People who can always reach you", tap "Add a person". Pass: the Contacts
  permission is asked only now (not at install or first launch); Allow opens the contact picker; the person you choose appears as a chip and the
  count says "1 of 5". Add up to five; at five "Remove someone to add another" shows. The cross on a chip removes the person. Denying the
  permission just leaves the list as it was.
- [ ] **Ring-through (use a second phone to call).** Turn on "Let their calls ring through" and allow changing contacts. Dock, with Do Not Disturb
  access allowed. Pass: a call from the VIP rings on the A55 (and one from a number that is not a VIP or a Favourite stays silent); the VIP appears
  under Favourites in the Contacts app only while docked. After undocking (wait about 4 seconds) they are not a Favourite any more.
- [ ] **A real Favourite stays one.** Mark one VIP as a Favourite in Contacts first. Dock and undock. Pass: they are still a Favourite afterwards.
- [ ] **A killed app puts people back.** Dock, then force-stop AuraDock. The VIP is still a Favourite (expected). Open AuraDock. Pass: they are
  not a Favourite any more. If "Still on loan" is shown on the Focus page, "Put back" clears it at once.
- [ ] **Their messages count as let through.** Dock for three minutes. A WhatsApp or SMS message from a VIP, and one from someone else, arrive.
  Pass: the undock card counts the VIP's as let through and the other as held. (A message is matched by the contact link the app attaches, or by
  the sender's name matching exactly; if a messaging app does neither, the name in its title may differ from your Contacts name.)
- [ ] **Google contacts sync.** With a Google account, add a VIP, dock and undock, then look at contacts.google.com. Pass: the VIP is not left
  as a Favourite there. Note anything odd: the Favourite mark syncs, so it can show there briefly while docked.

## Batch C (0.2.2): first run, trust and focus

Use a fresh install of 0.2.2 for the first-run checks (Settings > Apps > AuraDock > Storage > Clear data, or uninstall and install), and a second
phone to call from where noted.

- [ ] **Upgrade keeps data.** Install 0.2.2 over 0.2.1 with sessions, tasks and a VIP in place. Pass: the dashboard opens (not onboarding), the weekly
  line, tasks and VIPs are as before, nothing crashes. (Database 6 to 7 only adds the event log.)
- [ ] **VIP confirmation, once.** Focus settings, switch on "Let their calls ring through" for the first time. Pass: a sheet says AuraDock marks these
  people as Favourites in Contacts so their calls ring, that Favourites can sync to other devices, and that everything is put back when you undock;
  "Turn on" (cream) goes on to the contacts permission if needed, "Cancel" leaves the switch off and shows the sheet again next time. After "Turn on"
  the sheet never shows again, also after switching off and on.
- [ ] **Dock first, timed.** On a fresh install, start a stopwatch as you open the app. Pass: Welcome, then "Get started": the Bold dock fills the screen
  in landscape within 30 seconds of opening (it should be about 10) with "This is AuraDock" at the top left, and moves to "Pick your dock" by itself after
  about 5 seconds (or on a touch). Then the template, the screensaver test and "Start it from anywhere" (both skippable), then the dashboard. No
  permission is asked before the dashboard.
- [ ] **Permissions asked when used.** Each permission card shows as a sheet, once, the first time its feature is used: Allow on the Calendar card
  (calendar), "Allow location for weather" (location), the first app you pick or Allow in Focus (notification access), the first "Dock now" with Focus on
  (Do Not Disturb), the first task with a time (notifications, then alarms), "Add a person" (contacts). Pass: each sheet has the card, "Nothing leaves this
  phone." and Skip/Allow; Skip closes it and it does not come back by itself; tapping an Allow later shows it again. Setup lists all seven and its Allow
  goes straight to Android.
- [ ] **Screensaver test (the real one).** Setup, "Screensaver test" (or the walkthrough step). Choose AuraDock as screensaver, set a screen timeout of a
  minute or two (steps 1 and 2 tick by themselves when you come back), then charge the phone sideways on the stand and wait. Pass: it starts by itself;
  opening the app afterwards shows "✓ AuraDock appeared on its own" on the dashboard (and the Setup row says so); "Done" puts the note away.
- [ ] **Screensaver hint.** On a phone where it has not appeared a day after install, the dashboard shows "The screensaver hasn't started yet." with
  "Try again", which opens the test and hides the hint for a day.
- [ ] **Undo focus.** Dock (Focus on, Do Not Disturb access allowed). Touch the screen once. Pass: a small "Undo focus" pill fades in at the top for 4 seconds
  and goes by itself; tapping it turns Do Not Disturb back to how it was (check the quick settings) and any VIP Favourite marks are put back, while the dock
  stays open; a touch while the pill shows closes the dock. With Focus off, a touch closes the dock at once.
- [ ] **Trust caption.** Every permission card (the sheets, Setup) says "Nothing leaves this phone."
- [ ] **Hidden debug page.** Setup, scroll to the bottom, tap the version label seven times. Pass: "Debug" shows "Activated within 24 h" (Yes once the screensaver was
  set and the first dock happened within a day of install) and "Docked on 5+ of the last 7 days" with the day count, and the event counts. Nothing on it
  names a task, a person or a message.
- [ ] **Empty-day dashboard.** With no tasks, no events and the calendar allowed, the dashboard looks like the handoff frame (see
  `docs/design/handoff/compare/Dashboard-Empty.png`): the greeting, then Today, Calendar, the dock card with only the preview and Dock now, and the one-line
  Templates and Focus tiles. Add a task: the fuller layout returns, and its Focus tile reads "On while docked" (or "Off").
- [ ] **Motion.** Tick a task: a light tick under the finger, the strike and fade where the row is, and after a moment the row glides down below the others.
  Watch the dock clock over a minute change: the new time fades in as the old one fades out.
