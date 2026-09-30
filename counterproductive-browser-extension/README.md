# Counterproductive Flowers Browser Extension

## Files

- `manifest.json` declares the extension's name, version, toolbar popup, permissions, and page script.
- `popup.html` and `popup.css` define the toolbar interface.
- `popup.js` reads the active tab's flower state and sends start/stop messages.
- `content.js` creates the flower overlay and handles its timing and interactions.

## Manifest notes

The manifest uses Manifest V3. Its content script runs after page content loads on regular HTTP and HTTPS pages. The `activeTab` permission supports querying and messaging the active tab from the toolbar popup. Browser-internal pages are not available to content scripts.

`manifest.json` is strict JSON, which does not allow comments. This README explains its settings without making the manifest invalid.
