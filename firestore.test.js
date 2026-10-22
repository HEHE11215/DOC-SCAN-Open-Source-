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

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function nowTimestamp() {
  return new Date(Date.now() - 2000);
}

function validDocPayload(userId, overrides = {}) {
  const ts = nowTimestamp();
  return {
    userId,
    title: "Scanned Contract Q4",
    folderId: "folder_legal",
    category: "Contract",
    pageCount: 3,
    fileSizeBytes: 245120,
    ocrText: "Agreement between parties...",
    aiSummary: "Standard Q4 service contract.",
    tags: ["contract", "q4"],
    isFavorite: true,
    isPasswordProtected: false,
    pageSize: "A4",
    createdAt: ts,
    updatedAt: ts,
    ...overrides,
  };
}

function validFolderPayload(userId, overrides = {}) {
  const ts = nowTimestamp();
  return {
    userId,
    name: "Legal & Contracts",
    colorHex: "#2563EB",
    createdAt: ts,
    updatedAt: ts,
    ...overrides,
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

test("1. Unauthenticated user cannot read or create documents", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("documents").doc("doc_1").get()
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("documents").doc("doc_1").set(validDocPayload(ALICE_UID))
  );
});

test("2. Authenticated owner can create, read, list, update, and delete their own document", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const docRef = aliceDb.collection("users").doc(ALICE_UID).collection("documents").doc("doc_1");

  await assertSucceeds(docRef.set(validDocPayload(ALICE_UID)));
  await assertSucceeds(docRef.get());
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("documents").where("userId", "==", ALICE_UID).get()
  );
  await assertSucceeds(
    docRef.update({
      title: "Updated Contract Title",
      isFavorite: false,
      updatedAt: nowTimestamp(),
    })
  );
  await assertSucceeds(docRef.delete());
});

test("3. Cross-user isolation: Bob cannot read, list, update, or delete Alice's document", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const aliceDocOnAlice = aliceDb.collection("users").doc(ALICE_UID).collection("documents").doc("doc_1");
  await assertSucceeds(aliceDocOnAlice.set(validDocPayload(ALICE_UID)));

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const aliceDocOnBob = bobDb.collection("users").doc(ALICE_UID).collection("documents").doc("doc_1");
  await assertFails(aliceDocOnBob.get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("documents").get());
  await assertFails(
    aliceDocOnBob.update({
      title: "Hacked by Bob",
      updatedAt: nowTimestamp(),
    })
  );
  await assertFails(aliceDocOnBob.delete());
});

test("4. Identity spoofing rejected on create", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const docRef = aliceDb.collection("users").doc(ALICE_UID).collection("documents").doc("doc_spoof");
  await assertFails(docRef.set(validDocPayload(BOB_UID)));
});

test("5. Ghost/shadow field injection rejected on create and update", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const docRef = aliceDb.collection("users").doc(ALICE_UID).collection("documents").doc("doc_ghost");
  await assertFails(docRef.set(validDocPayload(ALICE_UID, { isVerifiedAdmin: true })));

  await assertSucceeds(docRef.set(validDocPayload(ALICE_UID)));
  await assertFails(
    docRef.update({
      isVerifiedAdmin: true,
      updatedAt: nowTimestamp(),
    })
  );
});

test("6. Immutable createdAt and userId cannot be mutated on update", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const docRef = aliceDb.collection("users").doc(ALICE_UID).collection("documents").doc("doc_immortal");
  await assertSucceeds(docRef.set(validDocPayload(ALICE_UID)));

  await assertFails(
    docRef.update({
      createdAt: new Date(1000),
      updatedAt: nowTimestamp(),
    })
  );
  await assertFails(
    docRef.update({
      userId: BOB_UID,
      updatedAt: nowTimestamp(),
    })
  );
});

test("7. Value and boundary poisoning rejected (invalid pageSize, pageCount, tags)", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const col = aliceDb.collection("users").doc(ALICE_UID).collection("documents");

  await assertFails(col.doc("bad_page_size").set(validDocPayload(ALICE_UID, { pageSize: "TABLOID_XL" })));
  await assertFails(col.doc("bad_page_count").set(validDocPayload(ALICE_UID, { pageCount: 0 })));
  await assertFails(
    col.doc("bad_tags").set(
      validDocPayload(ALICE_UID, {
        tags: ["1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11"],
      })
    )
  );
});

test("8. Folders: owner can CRUD folders and cross-user access is rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const folderRef = aliceDb.collection("users").doc(ALICE_UID).collection("folders").doc("folder_1");

  await assertSucceeds(folderRef.set(validFolderPayload(ALICE_UID)));
  await assertSucceeds(folderRef.get());
  await assertSucceeds(
    folderRef.update({
      name: "Tax Documents",
      updatedAt: nowTimestamp(),
    })
  );

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("folders").doc("folder_1").get()
  );
});
