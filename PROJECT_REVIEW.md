# Project review — 3 November 2026

## Google TV connection fixes

The pairing encoder sent a type enum in field 3 and a payload in field 4. Android TV Remote v2 instead embeds each message at its own field number (10, 20, 30, 40, and their acknowledgements). The client also used protocol version 1 and skipped the Options exchange. These prevent the intended pairing sequence from completing. The updated client uses version 2, sends Options before Configuration, and validates status and the expected acknowledgement before saving pairing success.

The command client previously sent Configure and SetActive immediately, then displayed Connected without receiving a TV response. It now responds to TV Configure and SetActive messages, intersects supported capabilities with ping/key support, and only displays Connected on RemoteStart. TLS/initial command reads have a timeout. PINs are no longer logged, input accepts hex characters only, malformed/oversized protobuf frames are rejected, IPv6 addresses are preserved, and the ViewModel releases manager resources.

Reference schema and implementation:
- https://github.com/tronikos/androidtvremote2/blob/main/src/androidtvremote2/polo.proto
- https://github.com/tronikos/androidtvremote2/blob/main/src/androidtvremote2/pairing.py
- https://github.com/tronikos/androidtvremote2/blob/main/src/androidtvremote2/remote.py

### Verify on hardware

Install `app/build/outputs/apk/debug/app-debug.apk`. Open Google TV, scan or enter the Streamer's local address, select Pair with PIN, and enter the six hex characters shown on the television. Check that Connected appears after pairing, exercise D-pad/Home/Back, wait at least a minute to verify ping responses, and relaunch to verify saved pairing. If discovery fails, try the IP directly on the same LAN; guest-network isolation can prevent connectivity even when both devices have internet access.

The wire fixtures run with `bash tools/test-gtv-wire.sh` after `./gradlew assembleDebug`. These check message fields/options, acknowledgement validation, malformed payload rejection, and key-message encoding. They do not replace testing TLS, discovery, or control on the Streamer.

## Recommended code work, in priority order

1. **Preserve intentional remote deletions.** `AppPreferences.loadRemotes()` recreates defaults for an empty saved list and inserts Google TV whenever none exists. Make migrations run once with a schema version; distinguish first launch from a saved empty list. Currently deleting Google TV does not survive restart.
2. **Give custom remotes their own screen.** `HomeScreen` routes CUSTOM_REMOTE to `LgTvScreen`, exposing LG power, input, navigation and media codes on unrelated appliances. Render only that remote's configured commands.
3. **Validate custom NEC codes.** `AddCustomButtonDialog` silently converts invalid hex to zero and accepts values outside unsigned 32-bit range. Show inline errors and disable Add until the label and code are valid.
4. **Serialize state and command operations.** Repository read/modify/write operations run independently on Dispatchers.IO; simultaneous edits can lose changes. Google TV connection attempts and key sends also use independent coroutines. Use one connection owner with cancellable attempts and ordered queues for key/IR output; test rapid reconnect/cancel and key order.
5. **Store Google TV pairing per remote.** All Google TV cards share one manager and global host/name/paired preferences despite allowing multiple remotes. Model device identity and pairing state explicitly, reconnect by discovered identity when DHCP changes, and provide a saved-device reconnect action.
6. **Pin the TV certificate after successful PIN pairing.** The TLS trust manager currently accepts any server certificate for command connections. Store the verified server fingerprint and require re-pairing when it changes. Handle Android backup restoring preferences without the corresponding AndroidKeyStore key.
7. **Handle discovery lifecycle and failures.** NSD errors only go to logs, old resolve callbacks can arrive after stopping, and concurrent legacy resolveService requests can fail. Queue resolution on older Android versions, use newer APIs where available, suppress stale callbacks, bound scans, and show failure/empty states.
8. **Align documentation with the actual build.** README claims LG WebSocket/Wake-on-LAN, a customizable grid/designer, AC fan/mode/swing controls, Google TV IR and operating modes that the current source does not implement. Document current capabilities separately from planned features. The AC model currently holds only power and temperature.
9. **Add repeatable project checks.** Keep wire fixtures, add state-machine tests with a fake TV transport, repository migration tests and NEC/AC timing fixtures. Add CI and replace the ad hoc Gradle launcher with a standard pinned wrapper when preparing distribution.

## Recommended UI work

1. Make the connection card the primary disconnected state. Disable remote controls until ready; show distinct Searching, Connecting, Enter code, Verifying, Connected and Connection lost states with a clear next action. Separate Reconnect from Pair again.
2. Keep the D-pad and Back/Home in a stable, reachable area. Collapse setup diagnostics after connection and use a compact status chip to reclaim space.
3. Increase small 10–12sp helper text and ensure every touch target is at least 48dp. Google TV volume pills are currently 44dp high. Check large font settings and narrow screens; fixed rows and dialog button rows can crowd.
4. Use a density-adjusted swipe threshold instead of 40 physical pixels. Consider repeated navigation during longer drags and long-press volume/D-pad repeat with guaranteed release on cancellation.
5. Use saveable UI state for navigation mode and dialog input, lifecycle-aware flow collection, shared theme tokens, and smaller Google TV composables. The screen is almost 1,500 lines and repeats many custom control styles.
6. Clarify Assistant: the present action sends a voice-assist key; it does not capture or stream microphone audio. Label its actual action and validate device behavior before presenting it as voice input.
7. Distinguish intended AC state from confirmed state: infrared has no acknowledgement, but the UI currently updates even if transmission fails. Avoid implying that the appliance reported its power or temperature.

This review is based on source inspection; no physical-device or rendered UI verification was available in this session.
