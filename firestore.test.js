const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";
const MONTH_KEY = "2026-11";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user cannot read or write playbackState or monthlyStats", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const past = new Date(Date.now() - 1000);

  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("playbackState").doc("current").get()
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("playbackState").doc("current").set({
      userId: ALICE_UID,
      trackKey: "track_1",
      trackTitle: "Midnight Vinyl",
      artistName: "Lo-Fi Collective",
      albumName: "Nocturne",
      trackUri: "content://media/external/audio/media/1",
      albumId: 10,
      positionMs: 12000,
      durationMs: 180000,
      totalPlaybackSeconds: 360,
      timeOfDaySlot: "NIGHT",
      createdAt: past,
      updatedAt: past,
    })
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("monthlyStats").get()
  );
});

test("Authenticated owner: valid PlaybackProgress create and partial update succeed", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 5000);
  const docRef = aliceDb.collection("users").doc(ALICE_UID).collection("playbackState").doc("current");

  await assertSucceeds(
    docRef.set({
      userId: ALICE_UID,
      trackKey: "track_101",
      trackTitle: "Amanecer Acústico",
      artistName: "Solana",
      albumName: "Primeros Rayos",
      trackUri: "content://media/external/audio/media/101",
      albumId: 42,
      positionMs: 45000,
      durationMs: 210000,
      totalPlaybackSeconds: 1200,
      timeOfDaySlot: "MORNING",
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(
    docRef.update({
      positionMs: 92000,
      totalPlaybackSeconds: 1247,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject ghost field & future timestamp & immutable createdAt overwrite
  await assertFails(
    docRef.update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertFails(
    docRef.update({
      ghostField: "hacked",
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertFails(
    docRef.update({
      positionMs: 95000,
      updatedAt: new Date(Date.now() + 60000),
    })
  );
  // Reject wrong stateId (only 'current' allowed)
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("playbackState").doc("other").set({
      userId: ALICE_UID,
      trackKey: "track_101",
      trackTitle: "Amanecer Acústico",
      artistName: "Solana",
      albumName: "Primeros Rayos",
      trackUri: "content://media/external/audio/media/101",
      albumId: 42,
      positionMs: 0,
      durationMs: 210000,
      totalPlaybackSeconds: 0,
      timeOfDaySlot: "MORNING",
      createdAt: created,
      updatedAt: created,
    })
  );
});

test("Authenticated owner: valid MonthlyStat and MonthlyTrackStat create, update, and list queries succeed", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 5000);
  const monthRef = aliceDb.collection("users").doc(ALICE_UID).collection("monthlyStats").doc(MONTH_KEY);
  const trackStatRef = monthRef.collection("trackStats").doc("trk_abc123");

  await assertSucceeds(
    monthRef.set({
      userId: ALICE_UID,
      monthKey: MONTH_KEY,
      totalSeconds: 180,
      totalPlays: 1,
      morningSeconds: 180,
      afternoonSeconds: 0,
      nightSeconds: 0,
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(
    monthRef.update({
      totalSeconds: 360,
      totalPlays: 2,
      afternoonSeconds: 180,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  await assertSucceeds(
    trackStatRef.set({
      userId: ALICE_UID,
      monthKey: MONTH_KEY,
      trackKey: "track_abc123",
      trackTitle: "Brisa de Tarde",
      artistName: "Los Andes",
      albumName: "Horizonte",
      albumId: 7,
      playCount: 1,
      totalSeconds: 180,
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(
    trackStatRef.update({
      playCount: 2,
      totalSeconds: 360,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("monthlyStats").get());
  await assertSucceeds(monthRef.collection("trackStats").get());

  // Reject invalid monthKey format and negative counters
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("monthlyStats").doc("2026-invalid").set({
      userId: ALICE_UID,
      monthKey: "2026-invalid",
      totalSeconds: 10,
      totalPlays: 1,
      morningSeconds: 10,
      afternoonSeconds: 0,
      nightSeconds: 0,
      createdAt: created,
      updatedAt: created,
    })
  );
  await assertFails(
    trackStatRef.update({
      totalSeconds: -5,
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("Cross-user isolation: Alice cannot read or write Bob's playbackState or monthlyStats", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const bobDb = context.firestore();
    const created = new Date(Date.now() - 10000);
    await bobDb.collection("users").doc(BOB_UID).collection("playbackState").doc("current").set({
      userId: BOB_UID,
      trackKey: "bob_track",
      trackTitle: "Bob Song",
      artistName: "Bob Artist",
      albumName: "Bob Album",
      trackUri: "content://media/external/audio/media/9",
      albumId: 1,
      positionMs: 5000,
      durationMs: 120000,
      totalPlaybackSeconds: 600,
      timeOfDaySlot: "AFTERNOON",
      createdAt: created,
      updatedAt: created,
    });
    await bobDb.collection("users").doc(BOB_UID).collection("monthlyStats").doc(MONTH_KEY).set({
      userId: BOB_UID,
      monthKey: MONTH_KEY,
      totalSeconds: 600,
      totalPlays: 3,
      morningSeconds: 0,
      afternoonSeconds: 600,
      nightSeconds: 0,
      createdAt: created,
      updatedAt: created,
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).collection("playbackState").doc("current").get()
  );
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).collection("playbackState").doc("current").update({
      positionMs: 0,
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).collection("monthlyStats").get()
  );
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).collection("monthlyStats").doc(MONTH_KEY).collection("trackStats").get()
  );
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).collection("favorites").get()
  );
});

test("Authenticated owner: valid FavoriteTrack create, list, and delete succeed; cross-user and invalid fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = new Date(Date.now() - 2000);
  const favRef = aliceDb.collection("users").doc(ALICE_UID).collection("favorites").doc("trk_fav1");

  await assertSucceeds(
    favRef.set({
      userId: ALICE_UID,
      trackKey: "track_fav_1",
      trackTitle: "Melodía Dorada",
      artistName: "Solana",
      albumName: "Atardecer",
      trackUri: "content://media/external/audio/media/55",
      albumId: 12,
      durationMs: 195000,
      createdAt: created,
    })
  );

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("favorites").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("favorites").get());
  await assertFails(favRef.update({ trackTitle: "Edited" }));
  await assertSucceeds(favRef.delete());
});

test("Regional global chat: signed-in users can read/post valid messages, upload chunks up to 6MB, and auto-clean expired >78h messages", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = new Date(Date.now() - 2000);
  const validExpires = new Date(Date.now() + 78 * 3600 * 1000);
  const tooLongExpires = new Date(Date.now() + 90 * 3600 * 1000);

  await assertFails(unauthDb.collection("regionalChats").doc("LATAM").collection("messages").get());

  const msgRef = aliceDb.collection("regionalChats").doc("LATAM").collection("messages").doc("msg_1");
  await assertSucceeds(
    msgRef.set({
      senderId: ALICE_UID,
      senderName: "DJ Zander",
      senderAvatarBase64: "AQIDBAUGBwg=",
      regionId: "LATAM",
      messageText: "Recomiendo este clásico para la tarde",
      recommendedTrackTitle: "Brisa de Tarde",
      recommendedArtist: "Solana",
      audioFileName: "",
      audioMimeType: "",
      audioClipBase64: "",
      audioChunkCount: 0,
      audioSizeBytes: 0,
      expiresAt: validExpires,
      createdAt: created,
    })
  );

  // Oversized avatar thumbnail (>35000 chars) rejected
  await assertFails(
    aliceDb.collection("regionalChats").doc("LATAM").collection("messages").doc("msg_bad_avatar").set({
      senderId: ALICE_UID,
      senderName: "DJ Zander",
      senderAvatarBase64: "X".repeat(36000),
      regionId: "LATAM",
      messageText: "Avatar gigante",
      recommendedTrackTitle: "",
      recommendedArtist: "",
      audioFileName: "",
      audioMimeType: "",
      audioClipBase64: "",
      audioChunkCount: 0,
      audioSizeBytes: 0,
      expiresAt: validExpires,
      createdAt: created,
    })
  );

  // Bob can list messages in LATAM
  await assertSucceeds(bobDb.collection("regionalChats").doc("LATAM").collection("messages").get());

  // Bob cannot spoof Alice's senderId or delete Alice's active (non-expired) message
  await assertFails(
    bobDb.collection("regionalChats").doc("LATAM").collection("messages").doc("msg_spoof").set({
      senderId: ALICE_UID,
      senderName: "Bob",
      senderAvatarBase64: "",
      regionId: "LATAM",
      messageText: "Spoof",
      recommendedTrackTitle: "",
      recommendedArtist: "",
      audioFileName: "",
      audioMimeType: "",
      audioClipBase64: "",
      audioChunkCount: 0,
      audioSizeBytes: 0,
      expiresAt: validExpires,
      createdAt: created,
    })
  );
  await assertFails(
    bobDb.collection("regionalChats").doc("LATAM").collection("messages").doc("msg_1").delete()
  );

  // Reject expiration beyond 78-79h window
  await assertFails(
    aliceDb.collection("regionalChats").doc("LATAM").collection("messages").doc("msg_bad_ttl").set({
      senderId: ALICE_UID,
      senderName: "DJ Zander",
      senderAvatarBase64: "",
      regionId: "LATAM",
      messageText: "Permanent attempt",
      recommendedTrackTitle: "",
      recommendedArtist: "",
      audioFileName: "",
      audioMimeType: "",
      audioClipBase64: "",
      audioChunkCount: 0,
      audioSizeBytes: 0,
      expiresAt: tooLongExpires,
      createdAt: created,
    })
  );

  // Chunked audio message (~5.2 MB, 8 chunks) succeeds
  const chunkedMsgId = "msg_chunked_5mb";
  await assertSucceeds(
    aliceDb.collection("regionalChats").doc("LATAM").collection("messages").doc(chunkedMsgId).collection("audioChunks").doc("chunk_0").set({
      senderId: ALICE_UID,
      regionId: "LATAM",
      messageId: chunkedMsgId,
      chunkIndex: 0,
      dataBase64: "A".repeat(680000),
      expiresAt: validExpires,
      createdAt: created,
    })
  );
  await assertSucceeds(
    aliceDb.collection("regionalChats").doc("LATAM").collection("messages").doc(chunkedMsgId).set({
      senderId: ALICE_UID,
      senderName: "DJ Zander",
      senderAvatarBase64: "",
      regionId: "LATAM",
      messageText: "Canción completa 5 min",
      recommendedTrackTitle: "Brisa",
      recommendedArtist: "Solana",
      audioFileName: "brisa_5m.m4a",
      audioMimeType: "audio/mp4",
      audioClipBase64: "",
      audioChunkCount: 8,
      audioSizeBytes: 5400000,
      expiresAt: validExpires,
      createdAt: created,
    })
  );
  await assertSucceeds(
    bobDb.collection("regionalChats").doc("LATAM").collection("messages").doc(chunkedMsgId).collection("audioChunks").get()
  );

  // Oversized audio (> 6 MB) rejected
  await assertFails(
    aliceDb.collection("regionalChats").doc("LATAM").collection("messages").doc("msg_too_big").set({
      senderId: ALICE_UID,
      senderName: "DJ Zander",
      senderAvatarBase64: "",
      regionId: "LATAM",
      messageText: "Too big",
      recommendedTrackTitle: "",
      recommendedArtist: "",
      audioFileName: "big.m4a",
      audioMimeType: "audio/mp4",
      audioClipBase64: "",
      audioChunkCount: 12,
      audioSizeBytes: 7500000,
      expiresAt: validExpires,
      createdAt: created,
    })
  );

  // Expired (>78h old) message seeded in DB can be cleaned up by any signed-in user
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const adminDb = ctx.firestore();
    const expiredAt = new Date(Date.now() - 3600 * 1000);
    const oldCreated = new Date(Date.now() - 79 * 3600 * 1000);
    await adminDb.collection("regionalChats").doc("LATAM").collection("messages").doc("msg_expired_78h").set({
      senderId: ALICE_UID,
      senderName: "DJ Zander",
      senderAvatarBase64: "",
      regionId: "LATAM",
      messageText: "Mensaje viejo",
      recommendedTrackTitle: "",
      recommendedArtist: "",
      audioFileName: "",
      audioMimeType: "",
      audioClipBase64: "",
      audioChunkCount: 0,
      audioSizeBytes: 0,
      expiresAt: expiredAt,
      createdAt: oldCreated,
    });
  });
  await assertSucceeds(
    bobDb.collection("regionalChats").doc("LATAM").collection("messages").doc("msg_expired_78h").delete()
  );

  // Owner can delete own message
  await assertSucceeds(msgRef.delete());
});

test("Authenticated owner: valid FCM token create, partial update, list, and delete succeed; cross-user and unauth fail", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = new Date(Date.now() - 2000);
  const aliceTokenRef = aliceDb.collection("users").doc(ALICE_UID).collection("fcmTokens").doc("fcm_abc123def456");

  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("fcmTokens").doc("fcm_abc123def456").get()
  );

  await assertSucceeds(
    aliceTokenRef.set({
      userId: ALICE_UID,
      token: "fcm_sample_token_value_123",
      notificationsEnabled: true,
      onlyActiveRegion: true,
      regionId: "LATAM",
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(
    aliceTokenRef.update({
      token: "fcm_sample_token_value_123",
      notificationsEnabled: false,
      onlyActiveRegion: false,
      regionId: "ESPANA",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("fcmTokens").get());

  // Reject cross-user access
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("fcmTokens").get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("fcmTokens").doc("fcm_abc123def456").update({
      notificationsEnabled: true,
      updatedAt: new Date(Date.now() - 200),
    })
  );

  // Reject invalid regionId and immutable createdAt overwrite
  await assertFails(
    aliceTokenRef.update({
      regionId: "INVALID_REGION",
      updatedAt: new Date(Date.now() - 200),
    })
  );
  await assertFails(
    aliceTokenRef.update({
      createdAt: new Date(Date.now() - 100),
      updatedAt: new Date(Date.now() - 100),
    })
  );

  await assertSucceeds(aliceTokenRef.delete());
});

