const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ADMIN_UID = "admin_farman";
const ADMIN_EMAIL = "alishehrozkhan786@gmail.com";
const CUSTOMER_UID = "cust_alice";
const OTHER_CUSTOMER_UID = "cust_bob";
const DRIVER_UID = "drv_kareem";
const DRIVER_EMAIL = "driver.kareem@farman.ae";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function tsPast(msAgo = 2000) {
  return new Date(Date.now() - msAgo);
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

function adminCtx() {
  return testEnv
    .authenticatedContext(ADMIN_UID, {
      email: ADMIN_EMAIL,
      email_verified: true,
    })
    .firestore();
}

function spoofedAdminCtx() {
  return testEnv
    .authenticatedContext("attacker_1", {
      email: ADMIN_EMAIL,
      email_verified: false,
    })
    .firestore();
}

function customerCtx(uid = CUSTOMER_UID) {
  return testEnv
    .authenticatedContext(uid, {
      email: `${uid}@example.com`,
      email_verified: true,
    })
    .firestore();
}

function driverCtx() {
  return testEnv
    .authenticatedContext(DRIVER_UID, {
      email: DRIVER_EMAIL,
      email_verified: true,
    })
    .firestore();
}

test("Unauthenticated users cannot read or write any collection", async () => {
  const unauth = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauth.collection("users").doc(CUSTOMER_UID).get());
  await assertFails(unauth.collection("bookings").get());
  await assertFails(unauth.collection("trucks").get());
  await assertFails(unauth.collection("drivers").get());
});

test("Email spoofing with unverified admin email is rejected", async () => {
  const spoofed = spoofedAdminCtx();
  await assertFails(
    spoofed.collection("pricing_config").doc("dubai_16ft").set({
      baseFeeAed: 150,
      perKmAed: 7,
      accidentSurchargeAed: 100,
      noPapersSurchargeAed: 120,
      luxuryMultiplier: 1.5,
      updatedAt: tsPast(),
    })
  );
  await assertFails(spoofed.collection("drivers").get());
});

test("Customer profile creation prevents self-assigned role escalation and shadow fields", async () => {
  const cust = customerCtx();
  const now = tsPast(5000);

  // Valid customer profile
  await assertSucceeds(
    cust.collection("users").doc(CUSTOMER_UID).set({
      userId: CUSTOMER_UID,
      name: "Alice Dubai",
      email: "cust_alice@example.com",
      phone: "+971501112233",
      role: "CUSTOMER",
      blocked: false,
      preferredLang: "en",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Escalation to ADMIN rejected
  await assertFails(
    cust.collection("users").doc(CUSTOMER_UID).update({
      role: "ADMIN",
      updatedAt: tsPast(1000),
    })
  );

  // Self-assigned DRIVER on create rejected
  await assertFails(
    customerCtx("sneaky_user")
      .collection("users")
      .doc("sneaky_user")
      .set({
        userId: "sneaky_user",
        name: "Sneaky",
        email: "sneaky@example.com",
        phone: "",
        role: "DRIVER",
        blocked: false,
        preferredLang: "en",
        createdAt: now,
        updatedAt: now,
      })
  );

  // Ghost field rejected
  await assertFails(
    customerCtx("ghost_user")
      .collection("users")
      .doc("ghost_user")
      .set({
        userId: "ghost_user",
        name: "Ghost",
        email: "ghost@example.com",
        phone: "",
        role: "CUSTOMER",
        blocked: false,
        preferredLang: "en",
        isVerified: true,
        createdAt: now,
        updatedAt: now,
      })
  );

  // Cross-user PII read rejected
  await assertFails(
    customerCtx(OTHER_CUSTOMER_UID).collection("users").doc(CUSTOMER_UID).get()
  );
});

test("Admin can provision fleet truck, driver, and pricing; customer cannot", async () => {
  const admin = adminCtx();
  const cust = customerCtx();
  const now = tsPast(5000);

  await assertFails(
    cust.collection("trucks").doc("truck_01").set({
      truckNumber: "DXB-16FT-01",
      modelName: "Isuzu 16ft Flatbed",
      pinColorHex: "#D32F2F",
      driverId: "",
      driverName: "",
      status: "FREE",
      lat: 25.2048,
      lng: 55.2708,
      speedKmh: 0,
      headingDeg: 0,
      locationName: "Downtown Dubai",
      activeBookingId: "",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    admin.collection("trucks").doc("truck_01").set({
      truckNumber: "DXB-16FT-01",
      modelName: "Isuzu 16ft Flatbed",
      pinColorHex: "#D32F2F",
      driverId: DRIVER_UID,
      driverName: "Kareem Farman",
      status: "FREE",
      lat: 25.2048,
      lng: 55.2708,
      speedKmh: 0,
      headingDeg: 0,
      locationName: "Downtown Dubai",
      activeBookingId: "",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    admin.collection("drivers").doc("drv_01").set({
      userId: "",
      driverCode: "DRV-101",
      passcode: "16FT-8899",
      name: "Kareem Farman",
      email: DRIVER_EMAIL,
      phone: "+971509998877",
      truckId: "truck_01",
      truckNumber: "DXB-16FT-01",
      approved: true,
      status: "FREE",
      ratingAvg: 4.9,
      totalTrips: 42,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Customer cannot read driver credentials/PII
  await assertFails(cust.collection("drivers").doc("drv_01").get());
  await assertFails(cust.collection("drivers").get());
});

test("Full Customer -> Driver -> Rating lifecycle enforces strict RBAC and terminal locking", async () => {
  const admin = adminCtx();
  const cust = customerCtx();
  const otherCust = customerCtx(OTHER_CUSTOMER_UID);
  const drv = driverCtx();
  const now = tsPast(10000);

  // Admin sets up truck & driver
  await assertSucceeds(
    admin.collection("trucks").doc("truck_01").set({
      truckNumber: "DXB-16FT-01",
      modelName: "Isuzu 16ft Flatbed",
      pinColorHex: "#D32F2F",
      driverId: DRIVER_UID,
      driverName: "Kareem Farman",
      status: "FREE",
      lat: 25.2048,
      lng: 55.2708,
      speedKmh: 0,
      headingDeg: 0,
      locationName: "Downtown Dubai",
      activeBookingId: "",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    admin.collection("drivers").doc("drv_01").set({
      userId: "",
      driverCode: "DRV-101",
      passcode: "16FT-8899",
      name: "Kareem Farman",
      email: DRIVER_EMAIL,
      phone: "+971509998877",
      truckId: "truck_01",
      truckNumber: "DXB-16FT-01",
      approved: true,
      status: "FREE",
      ratingAvg: 5.0,
      totalTrips: 10,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Driver verifies email & creates driver_sessions/{uid}, customer cannot
  await assertFails(
    cust.collection("driver_sessions").doc(CUSTOMER_UID).set({
      userId: CUSTOMER_UID,
      driverDocId: "drv_01",
      truckId: "truck_01",
      createdAt: tsPast(5000),
    })
  );

  await assertSucceeds(
    drv.collection("driver_sessions").doc(DRIVER_UID).set({
      userId: DRIVER_UID,
      driverDocId: "drv_01",
      truckId: "truck_01",
      createdAt: tsPast(5000),
    })
  );

  // Customer creates booking + job_dispatch atomically in batch
  const bookingRef = cust.collection("bookings").doc("bk_100");
  const dispatchRef = cust.collection("job_dispatch").doc("bk_100");
  const batch = cust.batch();
  batch.set(bookingRef, {
    customerId: CUSTOMER_UID,
    customerName: "Alice Dubai",
    customerPhone: "+971501112233",
    pickupArea: "Dubai Marina",
    pickupAddress: "Marina Walk Gate 4",
    pickupLat: 25.0805,
    pickupLng: 55.1403,
    dropArea: "Al Quoz",
    dropAddress: "Al Quoz Industrial 3 Garage",
    dropLat: 25.1375,
    dropLng: 55.2255,
    carType: "LUXURY",
    carCondition: "BREAKDOWN",
    issueNotes: "Alternator failure",
    distanceKm: 16.5,
    priceAed: 380.0,
    paymentMethod: "CARD",
    paymentStatus: "PAID",
    status: "REQUESTED",
    driverId: "",
    driverName: "",
    driverPhone: "",
    truckId: "",
    truckNumber: "",
    truckLat: 25.2048,
    truckLng: 55.2708,
    truckSpeedKmh: 0,
    etaMinutes: 18,
    distanceToTargetKm: 16.5,
    carPhotoBase64: "",
    rating: 0,
    reviewText: "",
    createdAt: tsPast(3000),
    updatedAt: tsPast(3000),
  });
  batch.set(dispatchRef, {
    bookingId: "bk_100",
    pickupArea: "Dubai Marina",
    pickupAddress: "Marina Walk Gate 4",
    pickupLat: 25.0805,
    pickupLng: 55.1403,
    dropArea: "Al Quoz",
    dropAddress: "Al Quoz Industrial 3 Garage",
    dropLat: 25.1375,
    dropLng: 55.2255,
    carType: "LUXURY",
    carCondition: "BREAKDOWN",
    issueNotes: "Alternator failure",
    distanceKm: 16.5,
    status: "OPEN",
    assignedDriverId: "",
    createdAt: tsPast(3000),
    updatedAt: tsPast(3000),
  });
  await assertSucceeds(batch.commit());

  // Unfiltered bookings query fails; customer-filtered query succeeds
  await assertFails(cust.collection("bookings").get());
  await assertSucceeds(
    cust.collection("bookings").where("customerId", "==", CUSTOMER_UID).get()
  );

  // Other customer cannot read booking
  await assertFails(otherCust.collection("bookings").doc("bk_100").get());

  // Driver sees OPEN dispatch, claims job & updates booking
  await assertSucceeds(
    drv.collection("job_dispatch").where("status", "==", "OPEN").get()
  );
  await assertSucceeds(
    drv.collection("job_dispatch").doc("bk_100").update({
      status: "ACCEPTED",
      assignedDriverId: DRIVER_UID,
      updatedAt: tsPast(2000),
    })
  );
  await assertSucceeds(
    drv.collection("bookings").doc("bk_100").update({
      driverId: DRIVER_UID,
      driverName: "Kareem Farman",
      driverPhone: "+971509998877",
      truckId: "truck_01",
      truckNumber: "DXB-16FT-01",
      truckLat: 25.15,
      truckLng: 55.21,
      truckSpeedKmh: 62.0,
      etaMinutes: 11,
      distanceToTargetKm: 9.4,
      status: "ASSIGNED",
      updatedAt: tsPast(2000),
    })
  );

  // Driver cannot alter priceAed
  await assertFails(
    drv.collection("bookings").doc("bk_100").update({
      priceAed: 999.0,
      updatedAt: tsPast(1500),
    })
  );

  // Driver progresses to DELIVERED
  await assertSucceeds(
    drv.collection("bookings").doc("bk_100").update({
      status: "DELIVERED",
      truckLat: 25.1375,
      truckLng: 55.2255,
      truckSpeedKmh: 0.0,
      etaMinutes: 0,
      distanceToTargetKm: 0.0,
      updatedAt: tsPast(1000),
    })
  );

  // Terminal state locking: driver cannot revert status after DELIVERED
  await assertFails(
    drv.collection("bookings").doc("bk_100").update({
      status: "CAR_LOADED",
      updatedAt: tsPast(500),
    })
  );

  // Customer rates DELIVERED booking once
  await assertSucceeds(
    cust.collection("bookings").doc("bk_100").update({
      rating: 5,
      reviewText: "Fast 16ft flatbed service!",
      updatedAt: tsPast(400),
    })
  );

  // Cannot overwrite rating a second time
  await assertFails(
    cust.collection("bookings").doc("bk_100").update({
      rating: 2,
      reviewText: "Changed",
      updatedAt: tsPast(300),
    })
  );
});
