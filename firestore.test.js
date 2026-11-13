const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const MALE_UID = "male_rohan";
const MALE_OTHER_UID = "male_aarav";
const FEMALE_UID = "female_srijana";
const FEMALE_OTHER_UID = "female_anita";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function serverTimestamp() {
  return new Date(Date.now() - 1500);
}

function futureTimestamp(days = 7) {
  return new Date(Date.now() + days * 24 * 60 * 60 * 1000);
}

function makePrivateUser(uid, gender, age = 24, activeOutgoingRequestId = "") {
  return {
    uid,
    displayName: uid,
    dateOfBirthPrivate: "2001-05-14",
    age,
    ageVerified: age >= 18,
    gender,
    language: "ne",
    theme: "himalayan_dusk",
    themeMode: "system",
    useBikramSambat: true,
    reducedMotion: false,
    discoveryRadiusKm: 50,
    notificationsEnabled: true,
    discreetNotifications: true,
    quietHoursEnabled: false,
    visibility: "visible",
    activeOutgoingRequestId,
    moderationStatus: "active",
    acceptedTermsAt: serverTimestamp(),
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
  };
}

function makePublicProfile(uid, gender, age = 24) {
  return {
    uid,
    displayName: uid,
    age,
    ageVerified: age >= 18,
    gender,
    bio: "Namaste from Kathmandu",
    lookingFor: "Meaningful connection",
    photoDataList: ["data:image/jpeg;base64,/9j/4AAQSkZJRgABAQAAAQABAAD/"],
    interests: ["Music", "Travel"],
    hobbies: ["Hiking"],
    prompts: ["Favourite momo spot?::Patan Durbar courtyard"],
    culturalBadge: "Newari heritage lover",
    province: "Bagmati",
    district: "Kathmandu",
    municipality: "Kathmandu Metro",
    publicLocationLabel: "Kathmandu, Nepal",
    visibility: "visible",
    moderationStatus: "active",
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
  };
}

function pairId(a, b) {
  return a < b ? `${a}_${b}` : `${b}_${a}`;
}

function sortedPair(a, b) {
  return a < b ? [a, b] : [b, a];
}

async function seedUser(uid, gender) {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("users").doc(uid).set(makePrivateUser(uid, gender));
    await db.collection("publicProfiles").doc(uid).set(makePublicProfile(uid, gender));
  });
}

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

test("Unauthenticated user: denied across all collections", async () => {
  const db = testEnv.unauthenticatedContext().firestore();
  await assertFails(db.collection("users").doc(MALE_UID).get());
  await assertFails(db.collection("publicProfiles").where("gender", "==", "Female").get());
  await assertFails(db.collection("interestRequests").where("fromUid", "==", MALE_UID).get());
  await assertFails(db.collection("connections").where("memberUids", "array-contains", MALE_UID).get());
  await assertFails(db.collection("conversations").where("memberUids", "array-contains", MALE_UID).get());
});

test("Private PII isolation & adult gate & shadow-update rejection", async () => {
  await seedUser(MALE_UID, "Male");
  const femaleDb = testEnv.authenticatedContext(FEMALE_UID).firestore();
  // Female cannot read Male's private /users doc containing DOB
  await assertFails(femaleDb.collection("users").doc(MALE_UID).get());

  // Underage user creation rejected
  await assertFails(
    femaleDb.collection("users").doc(FEMALE_UID).set(makePrivateUser(FEMALE_UID, "Female", 17))
  );

  // Shadow update with ghost field rejected
  const maleDb = testEnv.authenticatedContext(MALE_UID).firestore();
  await assertFails(
    maleDb.collection("users").doc(MALE_UID).update({ isAdmin: true, updatedAt: serverTimestamp() })
  );
  await assertFails(
    maleDb.collection("users").doc(MALE_UID).update({ moderationStatus: "verified", updatedAt: serverTimestamp() })
  );
});

test("Gender eligibility: Male discovers Female only; same-gender get & request blocked", async () => {
  await seedUser(MALE_UID, "Male");
  await seedUser(MALE_OTHER_UID, "Male");
  await seedUser(FEMALE_UID, "Female");

  const maleDb = testEnv.authenticatedContext(MALE_UID).firestore();

  // Male can list Female profiles
  await assertSucceeds(
    maleDb.collection("publicProfiles")
      .where("gender", "==", "Female")
      .where("visibility", "==", "visible")
      .where("ageVerified", "==", true)
      .where("moderationStatus", "==", "active")
      .get()
  );

  // Male can get Female profile
  await assertSucceeds(maleDb.collection("publicProfiles").doc(FEMALE_UID).get());

  // Male CANNOT get another Male's public profile
  await assertFails(maleDb.collection("publicProfiles").doc(MALE_OTHER_UID).get());

  // Male CANNOT send interest request to another Male
  const sameGenderBatch = maleDb.batch();
  const reqId = "req_same_gender";
  sameGenderBatch.set(maleDb.collection("interestRequests").doc(reqId), {
    fromUid: MALE_UID,
    toUid: MALE_OTHER_UID,
    fromName: "Rohan",
    fromPhoto: "",
    fromLocation: "Kathmandu",
    toName: "Aarav",
    toPhoto: "",
    toLocation: "Lalitpur",
    status: "PENDING",
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
    expiresAt: futureTimestamp(7),
  });
  sameGenderBatch.update(maleDb.collection("users").doc(MALE_UID), {
    activeOutgoingRequestId: reqId,
    updatedAt: serverTimestamp(),
  });
  await assertFails(sameGenderBatch.commit());
});

test("One-active-outgoing-request rule & recipient-only approval & mutual-consent chat", async () => {
  await seedUser(MALE_UID, "Male");
  await seedUser(FEMALE_UID, "Female");
  await seedUser(FEMALE_OTHER_UID, "Female");

  const maleDb = testEnv.authenticatedContext(MALE_UID).firestore();
  const femaleDb = testEnv.authenticatedContext(FEMALE_UID).firestore();

  // 1. Male sends interest request #1 to Female atomically
  const req1 = "req_1";
  const b1 = maleDb.batch();
  b1.set(maleDb.collection("interestRequests").doc(req1), {
    fromUid: MALE_UID,
    toUid: FEMALE_UID,
    fromName: "Rohan",
    fromPhoto: "",
    fromLocation: "Kathmandu",
    toName: "Srijana",
    toPhoto: "",
    toLocation: "Kathmandu",
    status: "PENDING",
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
    expiresAt: futureTimestamp(7),
  });
  b1.update(maleDb.collection("users").doc(MALE_UID), {
    activeOutgoingRequestId: req1,
    updatedAt: serverTimestamp(),
  });
  await assertSucceeds(b1.commit());

  // 2. Male CANNOT clear activeOutgoingRequestId while req1 is still PENDING
  await assertFails(
    maleDb.collection("users").doc(MALE_UID).update({
      activeOutgoingRequestId: "",
      updatedAt: serverTimestamp(),
    })
  );

  // 3. Male CANNOT send a second request while req1 is PENDING
  const req2 = "req_2";
  const b2 = maleDb.batch();
  b2.set(maleDb.collection("interestRequests").doc(req2), {
    fromUid: MALE_UID,
    toUid: FEMALE_OTHER_UID,
    fromName: "Rohan",
    fromPhoto: "",
    fromLocation: "Kathmandu",
    toName: "Anita",
    toPhoto: "",
    toLocation: "Pokhara",
    status: "PENDING",
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
    expiresAt: futureTimestamp(7),
  });
  b2.update(maleDb.collection("users").doc(MALE_UID), {
    activeOutgoingRequestId: req2,
    updatedAt: serverTimestamp(),
  });
  await assertFails(b2.commit());

  // 4. Male CANNOT self-approve req1 or forge a connection/chat before approval
  await assertFails(
    maleDb.collection("interestRequests").doc(req1).update({
      status: "APPROVED",
      updatedAt: serverTimestamp(),
      decisionAt: serverTimestamp(),
    })
  );
  const pid = pairId(MALE_UID, FEMALE_UID);
  const members = sortedPair(MALE_UID, FEMALE_UID);
  await assertFails(
    maleDb.collection("conversations").doc(pid).collection("messages").doc("msg_1").set({
      senderUid: MALE_UID,
      memberUids: members,
      type: "TEXT",
      text: "Hello before approval!",
      mediaData: "",
      audioDurationSec: 0,
      waveform: [],
      moderationStatus: "active",
      createdAt: serverTimestamp(),
    })
  );

  // 5. Recipient (Female) approves req1 atomically, creating connection + conversation
  const approveBatch = femaleDb.batch();
  approveBatch.update(femaleDb.collection("interestRequests").doc(req1), {
    status: "APPROVED",
    updatedAt: serverTimestamp(),
    decisionAt: serverTimestamp(),
  });
  approveBatch.set(femaleDb.collection("connections").doc(pid), {
    memberUids: members,
    sourceRequestId: req1,
    conversationId: pid,
    status: "ACTIVE",
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
  });
  approveBatch.set(femaleDb.collection("conversations").doc(pid), {
    memberUids: members,
    userAId: members[0],
    userAName: "Srijana",
    userAPhoto: "",
    userBId: members[1],
    userBName: "Rohan",
    userBPhoto: "",
    connectionId: pid,
    status: "ACTIVE",
    lastMessageText: "",
    lastMessageAt: serverTimestamp(),
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
  });
  await assertSucceeds(approveBatch.commit());

  // 6. Now Male can clear activeOutgoingRequestId since req1 is APPROVED
  await assertSucceeds(
    maleDb.collection("users").doc(MALE_UID).update({
      activeOutgoingRequestId: "",
      updatedAt: serverTimestamp(),
    })
  );

  // 7. Both members can send messages; third party CANNOT
  await assertSucceeds(
    maleDb.collection("conversations").doc(pid).collection("messages").doc("msg_1").set({
      senderUid: MALE_UID,
      memberUids: members,
      type: "TEXT",
      text: "Namaste Srijana!",
      mediaData: "",
      audioDurationSec: 0,
      waveform: [],
      moderationStatus: "active",
      createdAt: serverTimestamp(),
    })
  );
  const outsiderDb = testEnv.authenticatedContext(FEMALE_OTHER_UID).firestore();
  await assertFails(
    outsiderDb.collection("conversations").doc(pid).collection("messages").doc("msg_2").set({
      senderUid: FEMALE_OTHER_UID,
      memberUids: members,
      type: "TEXT",
      text: "Intruder!",
      mediaData: "",
      audioDurationSec: 0,
      waveform: [],
      moderationStatus: "active",
      createdAt: serverTimestamp(),
    })
  );

  // 8. Blocking immediately prevents further messages
  await assertSucceeds(
    femaleDb.collection("blocks").doc(`${FEMALE_UID}_${MALE_UID}`).set({
      blockerUid: FEMALE_UID,
      blockedUid: MALE_UID,
      createdAt: serverTimestamp(),
    })
  );
  await assertFails(
    maleDb.collection("conversations").doc(pid).collection("messages").doc("msg_3").set({
      senderUid: MALE_UID,
      memberUids: members,
      type: "TEXT",
      text: "After block",
      mediaData: "",
      audioDurationSec: 0,
      waveform: [],
      moderationStatus: "active",
      createdAt: serverTimestamp(),
    })
  );
  await assertFails(maleDb.collection("publicProfiles").doc(FEMALE_UID).get());
});

test("Culture posts, reactions, saved notes ownership, fontStyle & bannerData", async () => {
  await seedUser(MALE_UID, "Male");
  await seedUser(FEMALE_UID, "Female");

  const maleDb = testEnv.authenticatedContext(MALE_UID).firestore();
  const femaleDb = testEnv.authenticatedContext(FEMALE_UID).firestore();
  const unauthDb = testEnv.unauthenticatedContext().firestore();

  // Updating fontStyle and bannerData works for owner
  await assertSucceeds(
    maleDb.collection("users").doc(MALE_UID).update({
      fontStyle: "kalam",
      updatedAt: serverTimestamp(),
    })
  );
  await assertSucceeds(
    maleDb.collection("publicProfiles").doc(MALE_UID).update({
      bannerData: "preset:tihar",
      updatedAt: serverTimestamp(),
    })
  );

  // Create CulturePost as Male
  const postId = "post_1";
  await assertSucceeds(
    maleDb.collection("culturePosts").doc(postId).set({
      authorUid: MALE_UID,
      authorName: "Rohan",
      authorPhoto: "",
      authorLocation: "Kathmandu, Nepal",
      thoughtText: "Evening masala chiya near Patan Durbar Square.",
      imageData: "",
      culturalTag: "Chiya & Heritage",
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );

  // Unauthenticated denied; Female cannot impersonate Male or delete Male's post
  await assertFails(unauthDb.collection("culturePosts").doc(postId).get());
  await assertFails(
    femaleDb.collection("culturePosts").doc("post_forged").set({
      authorUid: MALE_UID,
      authorName: "Rohan",
      authorPhoto: "",
      authorLocation: "Kathmandu",
      thoughtText: "Forged post",
      imageData: "",
      culturalTag: "Heritage",
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );
  await assertFails(femaleDb.collection("culturePosts").doc(postId).delete());

  // Female can read post and toggle reaction as herself, not as Male
  await assertSucceeds(femaleDb.collection("culturePosts").doc(postId).get());
  await assertSucceeds(
    femaleDb.collection("culturePosts").doc(postId).collection("reactions").doc(FEMALE_UID).set({
      userUid: FEMALE_UID,
      createdAt: serverTimestamp(),
    })
  );
  await assertFails(
    femaleDb.collection("culturePosts").doc(postId).collection("reactions").doc(MALE_UID).set({
      userUid: MALE_UID,
      createdAt: serverTimestamp(),
    })
  );

  // Saved notes are strictly owner-only
  const noteId = "note_1";
  await assertSucceeds(
    femaleDb.collection("users").doc(FEMALE_UID).collection("savedNotes").doc(noteId).set({
      title: "Patan Chiya Spot",
      noteText: "Visit Patan courtyard this Saturday evening.",
      sourcePostId: postId,
      sourceAuthorName: "Rohan",
      sourceExcerpt: "Evening masala chiya near Patan Durbar Square.",
      imageData: "",
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );
  await assertFails(
    maleDb.collection("users").doc(FEMALE_UID).collection("savedNotes").doc(noteId).get()
  );
  await assertFails(
    maleDb.collection("users").doc(FEMALE_UID).collection("savedNotes").doc("note_2").set({
      title: "Intrude",
      noteText: "No access",
      sourcePostId: "",
      sourceAuthorName: "",
      sourceExcerpt: "",
      imageData: "",
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );
});
