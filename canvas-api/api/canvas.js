import axios from "axios";
import crypto from "node:crypto";
import { getCanvases } from "../services/spotifyCanvasService.js";

const resultCache = new Map();
let searchAccessToken = "";
let searchTokenExpiresAt = 0;

function respond(res, status, body) {
  res.setHeader("Content-Type", "application/json; charset=utf-8");
  res.setHeader("Cache-Control", "no-store");
  res.setHeader("Access-Control-Allow-Origin", "*");
  res.setHeader("Access-Control-Allow-Methods", "GET, OPTIONS");
  res.setHeader("Access-Control-Allow-Headers", "Content-Type, X-API-Key");
  return res.status(status).json(body);
}

function safeKeyMatches(expectedValue, providedValue) {
  if (!expectedValue || !providedValue) return false;
  const expected = Buffer.from(expectedValue, "utf8");
  const provided = Buffer.from(providedValue, "utf8");
  return expected.length === provided.length && crypto.timingSafeEqual(expected, provided);
}

function normalizeText(value) {
  return String(value ?? "")
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .toLowerCase()
    .replace(/&/g, " and ")
    .replace(/\b(feat\.?|featuring|ft\.?)\b.*$/g, "")
    .replace(/[\[\(][^\]\)]*(?:remaster(?:ed)?|radio edit|single version|album version|explicit|clean version)[^\]\)]*[\]\)]/g, " ")
    .replace(/[^\p{L}\p{N}]+/gu, " ")
    .trim()
    .replace(/\s+/g, " ");
}

async function getSpotifySearchToken() {
  if (searchAccessToken && Date.now() < searchTokenExpiresAt - 60_000) {
    return searchAccessToken;
  }

  const clientId = process.env.SPOTIFY_CLIENT_ID;
  const clientSecret = process.env.SPOTIFY_CLIENT_SECRET;
  if (!clientId || !clientSecret) {
    throw new Error("Missing SPOTIFY_CLIENT_ID or SPOTIFY_CLIENT_SECRET");
  }

  const basic = Buffer.from(clientId + ":" + clientSecret).toString("base64");
  const tokenResponse = await axios.post(
    "https://accounts.spotify.com/api/token",
    new URLSearchParams({ grant_type: "client_credentials" }).toString(),
    {
      timeout: 8000,
      headers: {
        Authorization: "Basic " + basic,
        "Content-Type": "application/x-www-form-urlencoded"
      }
    }
  );

  searchAccessToken = tokenResponse.data?.access_token ?? "";
  const expiresIn = Number(tokenResponse.data?.expires_in ?? 3600);
  searchTokenExpiresAt = Date.now() + Math.max(60, expiresIn) * 1000;
  if (!searchAccessToken) throw new Error("Spotify search token was empty");
  return searchAccessToken;
}

async function findMatchingTrack(title, artist) {
  const token = await getSpotifySearchToken();
  const q = `track:"${title}" artist:"${artist}"`;
  const response = await axios.get("https://api.spotify.com/v1/search", {
    timeout: 8000,
    params: { q, type: "track", limit: 10, market: "from_token" },
    headers: { Authorization: "Bearer " + token }
  });

  const tracks = response.data?.tracks?.items ?? [];
  const wantedTitle = normalizeText(title);
  const wantedArtist = normalizeText(artist);
  if (!wantedTitle || !wantedArtist) return null;

  const ranked = tracks.map(track => {
    const candidateTitle = normalizeText(track.name);
    const exactTitle = candidateTitle === wantedTitle;
    const nearTitle =
      !exactTitle &&
      (candidateTitle.includes(wantedTitle) || wantedTitle.includes(candidateTitle)) &&
      Math.min(candidateTitle.length, wantedTitle.length) /
        Math.max(candidateTitle.length, wantedTitle.length, 1) >= 0.78;

    const artistNames = (track.artists ?? []).map(entry => normalizeText(entry.name));
    const exactArtist = artistNames.some(name => name === wantedArtist);
    const partialArtist = artistNames.some(name =>
      name && (wantedArtist.includes(name) || name.includes(wantedArtist))
    );

    const score =
      (exactTitle ? 6 : nearTitle ? 4 : 0) +
      (exactArtist ? 5 : partialArtist ? 3 : 0);

    return { track, score };
  }).sort((a, b) => b.score - a.score ||
      Number(b.track.popularity ?? 0) - Number(a.track.popularity ?? 0));

  const best = ranked[0];
  // Avoid showing a Canvas for a different song with a similar title.
  if (!best || best.score < 8 || !best.track?.id) return null;
  return best.track;
}

function getCanvasEntries(response) {
  return response?.canvasesList ??
    response?.canvases_list ??
    response?.data?.canvasesList ??
    response?.data?.canvases_list ??
    [];
}

export default async function handler(req, res) {
  if (req.method === "OPTIONS") return respond(res, 204, {});
  if (req.method !== "GET") {
    res.setHeader("Allow", "GET, OPTIONS");
    return respond(res, 405, { error: "Method not allowed" });
  }

  const expectedKey = process.env.CANVAS_API_KEY;
  const providedKey = Array.isArray(req.headers["x-api-key"])
    ? req.headers["x-api-key"][0]
    : req.headers["x-api-key"];
  if (!safeKeyMatches(expectedKey, providedKey)) {
    return respond(res, 401, { error: "Unauthorized" });
  }

  const title = String(req.query?.title ?? "").trim().slice(0, 160);
  const artist = String(req.query?.artist ?? "").trim().slice(0, 160);
  const trackId = String(req.query?.trackId ?? "").trim();

  if (!trackId && (!title || !artist)) {
    return respond(res, 400, { error: "Provide title and artist, or a Spotify trackId" });
  }

  const cacheKey = trackId
    ? "id:" + trackId
    : "query:" + normalizeText(title) + "::" + normalizeText(artist);
  const cached = resultCache.get(cacheKey);
  if (cached && cached.expiresAt > Date.now()) {
    return respond(res, 200, cached.value);
  }

  try {
    let track = null;
    let resolvedTrackId = trackId;

    if (!resolvedTrackId) {
      track = await findMatchingTrack(title, artist);
      resolvedTrackId = track?.id ?? "";
    }

    if (!resolvedTrackId) {
      const value = { canvasUrl: null, reason: "track_not_matched" };
      resultCache.set(cacheKey, { value, expiresAt: Date.now() + 30 * 60 * 1000 });
      return respond(res, 200, value);
    }

    const canvasResponse = await getCanvases("spotify:track:" + resolvedTrackId);
    const entries = getCanvasEntries(canvasResponse);
    const entry = entries.find(item =>
      typeof (item?.canvasUrl ?? item?.canvas_url) === "string" &&
      (item.canvasUrl ?? item.canvas_url).startsWith("https://")
    );

    const value = {
      canvasUrl: entry ? (entry.canvasUrl ?? entry.canvas_url) : null,
      trackId: resolvedTrackId,
      matchedTitle: track?.name ?? null,
      matchedArtist: track?.artists?.map(item => item.name).join(", ") ?? null,
      reason: entry ? "available" : "canvas_unavailable"
    };
    resultCache.set(cacheKey, { value, expiresAt: Date.now() + (entry ? 6 : 1) * 60 * 60 * 1000 });
    if (resultCache.size > 500) {
      const oldestKey = resultCache.keys().next().value;
      if (oldestKey) resultCache.delete(oldestKey);
    }
    return respond(res, 200, value);
  } catch (error) {
    console.error("Canvas lookup failed:", error?.response?.status ?? error?.message ?? error);
    return respond(res, 503, { canvasUrl: null, reason: "lookup_failed" });
  }
}
