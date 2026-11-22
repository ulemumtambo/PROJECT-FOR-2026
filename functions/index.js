const crypto = require("node:crypto");
const { initializeApp } = require("firebase-admin/app");
const { getAuth } = require("firebase-admin/auth");
const { FieldValue, getFirestore } = require("firebase-admin/firestore");
const { HttpsError, onCall } = require("firebase-functions/v2/https");

initializeApp();

const db = getFirestore();
const auth = getAuth();

async function requireAdministratorManager(request) {
  if (!request.auth) {
    throw new HttpsError("unauthenticated", "Sign in to manage staff accounts.");
  }

  const manager = await db.collection("users").doc(request.auth.uid).get();
  if (!manager.exists || manager.get("role") !== "ADMIN" || manager.get("isActive") === false) {
    throw new HttpsError("permission-denied", "Only the administrator manager can manage staff accounts.");
  }
}

async function findOfficerByEmail(email) {
  const result = await db.collection("users").where("email", "==", email).limit(1).get();
  const profile = result.docs[0];
  if (!profile || profile.get("role") !== "OFFICER") {
    throw new HttpsError("not-found", "Staff account not found.");
  }
  return profile;
}

function cleanStaffEmail(value) {
  const email = typeof value === "string" ? value.trim().toLowerCase() : "";
  const domain = email.split("@")[1] || "";
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email) || !domain.endsWith(".mw")) {
    throw new HttpsError("invalid-argument", "Use a valid email address whose domain ends in .mw.");
  }
  return email;
}

exports.createAdminAccount = onCall(
  { region: "us-central1", maxInstances: 5 },
  async (request) => {
    await requireAdministratorManager(request);

    const name = typeof request.data?.name === "string" ? request.data.name.trim() : "";
    const email = cleanStaffEmail(request.data?.email);
    if (name.length < 2) {
      throw new HttpsError("invalid-argument", "Enter the staff member's full name.");
    }

    let user;
    try {
      user = await auth.createUser({
        email,
        displayName: name,
        password: crypto.randomBytes(32).toString("base64url"),
        emailVerified: false,
        disabled: false,
      });
    } catch (error) {
      if (error.code === "auth/email-already-exists") {
        throw new HttpsError("already-exists", "An account with this email already exists.");
      }
      throw new HttpsError("internal", "Could not create the staff account.");
    }

    try {
      await db.collection("users").doc(user.uid).set({
        name,
        email,
        role: "OFFICER",
        isActive: true,
        createdAt: FieldValue.serverTimestamp(),
        createdBy: request.auth.uid,
      });
    } catch (error) {
      await auth.deleteUser(user.uid).catch(() => {});
      throw new HttpsError("internal", "Could not save the staff profile.");
    }

    return { uid: user.uid, email };
  }
);

exports.listAdminAccounts = onCall(
  { region: "us-central1", maxInstances: 5 },
  async (request) => {
    await requireAdministratorManager(request);

    const staff = await db.collection("users").where("role", "==", "OFFICER").get();
    return {
      admins: staff.docs.map((profile) => ({
        email: profile.get("email") || "",
        name: profile.get("name") || "",
        isActive: profile.get("isActive") !== false,
      })),
    };
  }
);

exports.setAdminAccountActive = onCall(
  { region: "us-central1", maxInstances: 5 },
  async (request) => {
    await requireAdministratorManager(request);

    const email = cleanStaffEmail(request.data?.email);
    if (typeof request.data?.isActive !== "boolean") {
      throw new HttpsError("invalid-argument", "Choose whether the staff account is enabled.");
    }

    const profile = await findOfficerByEmail(email);
    const isActive = request.data.isActive;
    await auth.updateUser(profile.id, { disabled: !isActive });
    await profile.ref.update({ isActive, updatedAt: FieldValue.serverTimestamp() });
    return { email, isActive };
  }
);

exports.removeAdminAccount = onCall(
  { region: "us-central1", maxInstances: 5 },
  async (request) => {
    await requireAdministratorManager(request);

    const email = cleanStaffEmail(request.data?.email);
    const profile = await findOfficerByEmail(email);
    await auth.deleteUser(profile.id);
    await profile.ref.delete();
    return { email, removed: true };
  }
);
