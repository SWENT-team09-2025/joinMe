import * as functions from "firebase-functions/v1";
import * as admin from "firebase-admin";

// Initialize Firebase Admin
admin.initializeApp();

const db = admin.firestore();

/**
 * Helper function to send FCM notification to a user
 * @param {string} userId - The ID of the user to send notification to
 * @param {string} title - Notification title
 * @param {string} body - Notification body
 * @param {string} eventId - Event ID for deep linking
 */
async function sendNotificationToUser(
  userId: string,
  title: string,
  body: string,
  eventId: string
): Promise<void> {
  try {
    // Get user's FCM token from their profile
    const userDoc = await db.collection("profiles").doc(userId).get();

    if (!userDoc.exists) {
      console.log(`User ${userId} not found`);
      return;
    }

    const userData = userDoc.data();
    const fcmToken = userData?.fcmToken;

    if (!fcmToken) {
      console.log(`User ${userId} does not have an FCM token`);
      return;
    }

    // Send the notification
    const message: admin.messaging.Message = {
      token: fcmToken,
      notification: {
        title: title,
        body: body,
      },
      data: {
        eventId: eventId,
      },
      android: {
        priority: "high",
        notification: {
          channelId: "joinme_notifications",
          priority: "high",
        },
      },
    };

    await admin.messaging().send(message);
    console.log(`Notification sent to user ${userId}`);
  } catch (error) {
    console.error(`Error sending notification to user ${userId}:`, error);
  }
}

/**
 * Helper function to get username from user ID
 * @param {string} userId - The user ID
 * @return {Promise<string>} The username
 */
async function getUsername(userId: string): Promise<string> {
  try {
    const userDoc = await db.collection("profiles").doc(userId).get();
    if (userDoc.exists) {
      return userDoc.data()?.username || "Someone";
    }
    return "Someone";
  } catch (error) {
    console.error(`Error getting username for ${userId}:`, error);
    return "Someone";
  }
}

/**
 * Triggered when an event document is updated.
 * Detects when a new participant joins and notifies the event owner.
 */
export const onEventParticipantAdded = functions.firestore
  .document("events/{eventId}")
  .onUpdate(async (change, context) => {
    const eventId = context.params.eventId;
    const beforeData = change.before.data();
    const afterData = change.after.data();

    // Get the participants lists before and after the update
    const beforeParticipants: string[] = beforeData.participants || [];
    const afterParticipants: string[] = afterData.participants || [];

    // Find new participants (users who joined)
    const newParticipants = afterParticipants.filter(
      (userId) => !beforeParticipants.includes(userId)
    );

    if (newParticipants.length === 0) {
      // No new participants, nothing to do
      return;
    }

    const ownerId = afterData.ownerId;
    const eventTitle = afterData.title || "your event";

    // Don't notify the owner if they are the one who joined (shouldn't happen)
    const participantsToNotifyAbout = newParticipants.filter(
      (userId) => userId !== ownerId
    );

    if (participantsToNotifyAbout.length === 0) {
      return;
    }

    // Notify the owner about each new participant
    for (const newParticipantId of participantsToNotifyAbout) {
      const username = await getUsername(newParticipantId);
      const title = "New participant joined!";
      const body = `${username} joined "${eventTitle}"`;

      await sendNotificationToUser(ownerId, title, body, eventId);
    }
  });

/**
 * Triggered when an event document is updated.
 * Detects when event details change and notifies all participants.
 */
export const onEventUpdated = functions.firestore
  .document("events/{eventId}")
  .onUpdate(async (change, context) => {
    const eventId = context.params.eventId;
    const beforeData = change.before.data();
    const afterData = change.after.data();

    // Check if any meaningful event details changed
    const titleChanged = beforeData.title !== afterData.title;
    const descriptionChanged = beforeData.description !== afterData.description;
    const dateChanged = beforeData.date !== afterData.date;
    const locationChanged =
      JSON.stringify(beforeData.location) !==
      JSON.stringify(afterData.location);
    const durationChanged = beforeData.duration !== afterData.duration;

    // If only participants list changed, don't send event update notification
    // (that's handled by onEventParticipantAdded)
    if (!titleChanged && !descriptionChanged &&
        !dateChanged && !locationChanged && !durationChanged) {
      return;
    }

    const ownerId = afterData.ownerId;
    const participants: string[] = afterData.participants || [];
    const eventTitle = afterData.title || "An event";

    // Notify all participants (except the owner) about the event update
    const participantsToNotify = participants.filter(
      (userId) => userId !== ownerId
    );

    if (participantsToNotify.length === 0) {
      return;
    }

    const title = "Event updated";
    const body = `"${eventTitle}" has been updated by the organizer`;

    // Send notifications to all participants
    const notificationPromises = participantsToNotify.map((participantId) =>
      sendNotificationToUser(participantId, title, body, eventId)
    );

    await Promise.all(notificationPromises);
  });

/**
 * Triggered when an event document is deleted.
 * Notifies all participants that the event was cancelled.
 */
export const onEventDeleted = functions.firestore
  .document("events/{eventId}")
  .onDelete(async (snap, context) => {
    const eventId = context.params.eventId;
    const eventData = snap.data();

    const ownerId = eventData.ownerId;
    const participants: string[] = eventData.participants || [];
    const eventTitle = eventData.title || "An event";

    // Notify all participants (except the owner) about the event cancellation
    const participantsToNotify = participants.filter(
      (userId) => userId !== ownerId
    );

    if (participantsToNotify.length === 0) {
      return;
    }

    const title = "Event cancelled";
    const body = `"${eventTitle}" has been cancelled by the organizer`;

    // Send notifications to all participants
    const notificationPromises = participantsToNotify.map((participantId) =>
      sendNotificationToUser(participantId, title, body, eventId)
    );

    await Promise.all(notificationPromises);
  });
