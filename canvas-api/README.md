# Nusa Canvas API (experimental)

An independently deployable serverless backend used by the optional Canvas layer in NusaMusic. The Android app continues to show local album artwork if this API is not configured, the track cannot be confidently matched, Spotify has no Canvas for it, or a network request fails.

## Deployment (Vercel)

1. Import the `yohandeku32/nusamusic` GitHub repository as a **new Vercel project**.
2. Set **Root Directory** to `canvas-api`.
3. Keep the project on branch `fix/immersive-artwork-offset-20261009` while testing.
4. Add the environment variables below in Vercel Project Settings → Environment Variables.
5. Deploy. The endpoint will be `https://YOUR-CANVAS-PROJECT.vercel.app/api/canvas`.

Required environment variables:

- `CANVAS_API_KEY`: a long random string created for this backend.
- `SPOTIFY_CLIENT_ID`: the client ID from the Spotify Developer Dashboard, used only to search the public catalog.
- `SPOTIFY_CLIENT_SECRET`: the corresponding app secret, kept only on the backend.
- `SP_DC`: the `sp_dc` session cookie from a signed-in Spotify web session, required by the unofficial Canvas endpoint.

Create the Spotify app at [Spotify for Developers](https://developer.spotify.com/dashboard). Spotify's current Development Mode rules require the app owner to have Premium and limit the app to a small allowlist; check the dashboard and Spotify's [current Development Mode docs](https://developer.spotify.com/documentation/web-api/concepts/quota-modes) before setup.

**Security and availability:** `SP_DC` is a sensitive login-session credential. Never commit it to GitHub, include it in the APK, or send it to anyone. Configure it only in Vercel's encrypted environment variables. If it expires, Canvas lookups will fail until you update it. This backend uses undocumented Spotify endpoints, which may change and may be restricted by Spotify. Spotify Client ID/Secret are used only for official catalog search.

## Configure the Android build

Open the root `local.properties` file (the file is git-ignored) and add:

```properties
CANVAS_API_BASE_URL=https://YOUR-CANVAS-PROJECT.vercel.app
CANVAS_API_KEY=the-same-random-string-as-the-backend
```

Re-sync Gradle and rebuild. If either setting is blank, Nusa silently keeps using album artwork.

The API key embedded in an Android APK can be extracted by someone who has the APK, so this is a basic access gate—not strong authentication for a public service. Keep this backend personal/private and monitor its usage.

## Endpoint

`GET /api/canvas?title=...&artist=...`

Header: `X-API-Key: <CANVAS_API_KEY>`

Returns `{ "canvasUrl": "https://...mp4" }` when Canvas is found, or `{ "canvasUrl": null }` when no safe match or Canvas is available.

## Attribution

The protobuf schema and Canvas request/authentication implementation in this folder are adapted from [Paxsenix0/Spotify-Canvas-API](https://github.com/Paxsenix0/Spotify-Canvas-API). See `LICENSE` for its GNU GPL v3 license. This is an unofficial, experimental integration and is not endorsed by Spotify.
