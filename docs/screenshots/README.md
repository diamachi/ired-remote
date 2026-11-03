# App screenshots

Add authentic screenshots from the app here, then embed them in the root README. Keep the Android status/navigation bars visible in at least one image and crop out unrelated personal notifications or device identifiers.

Suggested files:

- `lg-webos.png`
- `google-tv-streamer.png`
- `panasonic-ac.png`
- `cherry-lights.png`
- `custom-remote.png`
- `add-remote.png`

Capture each page on a connected phone after installing the build you plan to release. With USB debugging enabled, open the screen, then capture it with:

```sh
adb exec-out screencap -p > docs/screenshots/lg-webos.png
```

Replace the filename for each page. Review each image before committing; pairing screens can reveal local IP addresses, and status bars can expose notifications.
