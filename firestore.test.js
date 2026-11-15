const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const DATABASE_ID = "ai-studio-android-smartcla-df4e4040-e7f5-46d2-91f4-106cd27ed480";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validProfilePayload(uid, role = "student", createdAt = new Date(Date.now() - 2000)) {
  return {
    userId: uid,
    displayName: "Alice Scholar",
    email: "alice@school.edu",
    role,
    emailVerified: true,
    institution: "Computer Science",
    createdAt,
    updatedAt: createdAt,
  };
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

test("Unauthenticated user: cannot read or list users", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore(DATABASE_ID);
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").get());
});

test("Unauthenticated user: cannot create user profile", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore(DATABASE_ID);
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).set(validProfilePayload(ALICE_UID))
  );
});

test("Authenticated user: can create and read own student profile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore(DATABASE_ID);
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set(validProfilePayload(ALICE_UID, "student"))
  );
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());
});

test("Authenticated user: can create own teacher profile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore(DATABASE_ID);
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set(validProfilePayload(ALICE_UID, "teacher"))
  );
});

test("Authenticated user: cannot list users collection (PII isolation)", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore(DATABASE_ID);
  await assertFails(aliceDb.collection("users").get());
});

test("Authenticated user: cannot read another user's profile", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore(DATABASE_ID).collection("users").doc(BOB_UID).set(
      validProfilePayload(BOB_UID, "teacher")
    );
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore(DATABASE_ID);
  await assertFails(aliceDb.collection("users").doc(BOB_UID).get());
});

test("Authenticated user: cannot spoof userId or create profile for another user", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore(DATABASE_ID);
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).set(validProfilePayload(BOB_UID))
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set(validProfilePayload(BOB_UID))
  );
});

test("Shadow Update / Invalid Role: rejects ghost fields and admin role", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore(DATABASE_ID);
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      ...validProfilePayload(ALICE_UID),
      isAdmin: true,
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set(validProfilePayload(ALICE_UID, "admin"))
  );
});

test("Validation Bounds: rejects too-short displayName or future timestamp", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore(DATABASE_ID);
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      ...validProfilePayload(ALICE_UID),
      displayName: "A",
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set(
      validProfilePayload(ALICE_UID, "student", new Date(Date.now() + 60000))
    )
  );
});

test("Update: allows editing displayName and institution while blocking role escalation", async () => {
  const createdAt = new Date(Date.now() - 5000);
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore(DATABASE_ID);
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set(validProfilePayload(ALICE_UID, "student", createdAt))
  );

  // Valid update
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      displayName: "Alice Updated",
      institution: "Physics Dept",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Forbidden role escalation
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      role: "teacher",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Forbidden createdAt mutation
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("Delete: owner can delete own profile, other user cannot", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore(DATABASE_ID).collection("users").doc(ALICE_UID).set(
      validProfilePayload(ALICE_UID, "student")
    );
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore(DATABASE_ID);
  await assertFails(bobDb.collection("users").doc(ALICE_UID).delete());

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore(DATABASE_ID);
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).delete());
});
