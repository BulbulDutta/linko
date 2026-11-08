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

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
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

test("Unauthenticated user: cannot read or write user_presence", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("user_presence").get());
  await assertFails(
    unauthDb.collection("user_presence").doc("alice@test.com").set({
      email: "alice@test.com",
      online: true,
      isOnline: true,
      lastSeen: Date.now(),
    })
  );
});

test("Authenticated user: can write and list user_presence with isOnline and online", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("user_presence").doc("alice@test.com").set({
      email: "alice@test.com",
      userId: ALICE_UID,
      online: true,
      isOnline: true,
      lastSeen: Date.now(),
    })
  );
  await assertSucceeds(aliceDb.collection("user_presence").get());
});

test("Authenticated user: can create and update 3-stage chat_messages ('sent' -> 'delivered' -> 'read')", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("chat_messages").doc("msg_1").set({
      id: "msg_1",
      chatId: "chat_ab",
      senderId: ALICE_UID,
      senderName: "Alice",
      senderEmail: "alice@test.com",
      text: "Hello Bob",
      status: "sent",
      isEncrypted: true,
    })
  );

  await assertSucceeds(
    bobDb.collection("chat_messages").doc("msg_1").update({
      status: "delivered",
    })
  );

  await assertSucceeds(
    bobDb.collection("chat_messages").doc("msg_1").update({
      status: "read",
    })
  );
});

test("Authenticated user: cannot overwrite another user's profile in /users/{userId}", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).set({
      uid: BOB_UID,
      email: "bob@test.com",
    })
  );
});
