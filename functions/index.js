const { onRequest } = require("firebase-functions/v2/https");
const { defineSecret } = require("firebase-functions/params");
const admin = require("firebase-admin");

if (!admin.apps.length) {
  admin.initializeApp();
}

const auddApiKey = defineSecret("AUDD_API_KEY");
const MIN_WAV_BYTES = 4096;
const MAX_WAV_BYTES = 2 * 1024 * 1024; // 2 MB limit for short acoustic clips

/**
 * Authenticated HTTPS proxy for acoustic song recognition.
 * Keeps AUDD_API_KEY strictly inside Google Cloud Secret Manager on the server
 * so no third-party API key is ever bundled into the store APK/AAB.
 */
exports.recognizeSongProxy = onRequest(
  {
    secrets: [auddApiKey],
    cors: false,
    maxInstances: 10,
    timeoutSeconds: 20,
    memory: "256MiB",
  },
  async (req, res) => {
    if (req.method !== "POST") {
      res.status(405).json({ status: "error", error: "method_not_allowed" });
      return;
    }

    const authHeader = req.headers.authorization || "";
    const match = /^Bearer\s+(.+)$/i.exec(authHeader);
    if (!match || !match[1]) {
      res.status(401).json({ status: "error", error: "missing_auth_token" });
      return;
    }

    try {
      await admin.auth().verifyIdToken(match[1].trim());
    } catch (_err) {
      res.status(401).json({ status: "error", error: "invalid_auth_token" });
      return;
    }

    const audioBuffer = Buffer.isBuffer(req.rawBody) ? req.rawBody : null;
    if (
      !audioBuffer ||
      audioBuffer.length < MIN_WAV_BYTES ||
      audioBuffer.length > MAX_WAV_BYTES ||
      audioBuffer.subarray(0, 4).toString("ascii") !== "RIFF"
    ) {
      res.status(400).json({ status: "error", error: "invalid_wav_payload" });
      return;
    }

    const secretToken = auddApiKey.value();
    if (!secretToken || !secretToken.trim()) {
      res.status(503).json({ status: "error", error: "service_unconfigured" });
      return;
    }

    try {
      const form = new FormData();
      form.append("api_token", secretToken.trim());
      form.append("return", "apple_music,spotify");
      form.append(
        "file",
        new Blob([audioBuffer], { type: "audio/wav" }),
        "sample.wav"
      );

      const upstream = await fetch("https://api.audd.io/", {
        method: "POST",
        body: form,
        signal: AbortSignal.timeout(12000),
      });

      if (!upstream.ok) {
        res.status(502).json({ status: "error", error: "upstream_unavailable" });
        return;
      }

      const payload = await upstream.json();
      if (payload?.status !== "success" || !payload?.result) {
        res.status(200).json({ status: "no_match", result: null });
        return;
      }

      const r = payload.result;
      const artworkUrl =
        r?.spotify?.album?.images?.[0]?.url ||
        (typeof r?.apple_music?.artwork?.url === "string"
          ? r.apple_music.artwork.url.replace("{w}x{h}", "400x400")
          : "") ||
        "";

      res.status(200).json({
        status: "success",
        result: {
          title: typeof r.title === "string" ? r.title.trim() : "",
          artist: typeof r.artist === "string" ? r.artist.trim() : "",
          album: typeof r.album === "string" ? r.album.trim() : "",
          artworkUrl,
        },
      });
    } catch (_err) {
      res.status(502).json({ status: "error", error: "proxy_request_failed" });
    }
  }
);
