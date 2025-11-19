import * as functions from "firebase-functions/v1";
import * as admin from "firebase-admin";

// Initialize Firebase Admin
admin.initializeApp();

const db = admin.firestore();

const S_TO_MS = 1000;
const MIN_TO_S = 60;
const HOUR_TO_MIN = 60;
const DAY_TO_HOUR = 24;
const NUMBER_OF_DAYS = 30;
const RECURRENCE = "0 0 * * *";
const TIME_ZONE = "UTC";

const SERIES = "series";
const EVENTS = "events";

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
 * Detects when a new participant joins or leaves and notifies the event owner.
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

    // Find removed participants (users who left/quit)
    const removedParticipants = beforeParticipants.filter(
      (userId) => !afterParticipants.includes(userId)
    );

    const ownerId = afterData.ownerId;
    const eventTitle = afterData.title || "your event";

    // Notify the owner about new participants
    if (newParticipants.length > 0) {
      // Don't notify the owner if they are the one who joined (shouldn't happen)
      const participantsToNotifyAbout = newParticipants.filter(
        (userId) => userId !== ownerId
      );

      for (const newParticipantId of participantsToNotifyAbout) {
        const username = await getUsername(newParticipantId);
        const title = "New participant joined!";
        const body = `${username} joined "${eventTitle}"`;

        await sendNotificationToUser(ownerId, title, body, eventId);
      }
    }

    // Notify the owner about removed participants
    if (removedParticipants.length > 0) {
      // Don't notify the owner if they are the one who left (shouldn't happen)
      const participantsToNotifyAbout = removedParticipants.filter(
        (userId) => userId !== ownerId
      );

      for (const removedParticipantId of participantsToNotifyAbout) {
        const username = await getUsername(removedParticipantId);
        const title = "Participant left";
        const body = `${username} left "${eventTitle}"`;

        await sendNotificationToUser(ownerId, title, body, eventId);
      }
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
    // Compare Firestore Timestamps properly using seconds and nanoseconds
    const dateChanged =
      beforeData.date?.seconds !== afterData.date?.seconds ||
      beforeData.date?.nanoseconds !== afterData.date?.nanoseconds;
    const locationChanged =
      JSON.stringify(beforeData.location) !==
      JSON.stringify(afterData.location);
    const durationChanged = beforeData.duration !== afterData.duration;
    const typeChanged = beforeData.type !== afterData.type;
    const visibilityChanged = beforeData.visibility !== afterData.visibility;
    const maxParticipantsChanged =
      beforeData.maxParticipants !== afterData.maxParticipants;

    // Determine if any meaningful fields changed (excluding participants)
    const meaningfulChangeOccurred =
      titleChanged ||
      descriptionChanged ||
      dateChanged ||
      locationChanged ||
      durationChanged ||
      typeChanged ||
      visibilityChanged ||
      maxParticipantsChanged;

    // If only participants list changed (or nothing changed), don't send
    // event update notification (participants changes are handled by
    // onEventParticipantAdded)
    if (!meaningfulChangeOccurred) {
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
 * Skips notifications if the event is already expired (automatic cleanup).
 */
export const onEventDeleted = functions.firestore
  .document("events/{eventId}")
  .onDelete(async (snap, context) => {
    const eventId = context.params.eventId;
    const eventData = snap.data();

    // Check if event is already expired (indicates automatic cleanup)
    const eventDate = eventData.date;
    const durationMinutes = eventData.duration || 0;

    if (eventDate && eventDate.seconds) {
      const eventStartMs = eventDate.seconds * S_TO_MS;
      const durationMs = durationMinutes * MIN_TO_S * S_TO_MS;
      const eventEndMs = eventStartMs + durationMs;
      const now = Date.now();

      // If event already ended, don't send notifications (automatic cleanup)
      if (eventEndMs < now) {
        return;
      }
    }

    // Event is still upcoming or active, send cancellation notifications
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

/**
 * Scheduled function that runs daily at midnight (UTC).
 * Cleans up old events and series that are expired for more than 30 days.
 *
 * Logic:
 * 1. Fetch all series
 * 2. If lastEventEndTime < now - 30 days, mark series and its events for deletion
 * 3. Delete all marked series
 * 4. Delete all events from those series
 * 5. Fetch remaining events and check if they are expired (date + duration < now - 30 days)
 * 6. Delete expired standalone events
 */
export const cleanupOldEventsAndSeries = functions.pubsub
  .schedule(RECURRENCE)
  .timeZone(TIME_ZONE)
  .onRun(async (context) => {
    const now = Date.now(); // Current time in milliseconds
    const thirtyDaysInMs = NUMBER_OF_DAYS * DAY_TO_HOUR * HOUR_TO_MIN * MIN_TO_S * S_TO_MS; // 30 days
    const cutoffTime = now - thirtyDaysInMs;

    try {
      const seriesEventsToDelete: string[] = [];
      const seriesToDelete: string[] = [];

      // Step 1: Fetch all series
      const seriesSnapshot = await db.collection(SERIES).get();

      // Step 2: Check each series
      for (const serieDoc of seriesSnapshot.docs) {
        const serieData = serieDoc.data();
        const lastEventEndTime = serieData.lastEventEndTime;

        if (lastEventEndTime && lastEventEndTime.seconds) {
          const lastEventEndMs = lastEventEndTime.seconds * S_TO_MS;

          // If series is expired, mark it and its events for deletion
          if (lastEventEndMs < cutoffTime) {
            seriesToDelete.push(serieDoc.id);
            const eventIds: string[] = serieData.eventIds || [];
            seriesEventsToDelete.push(...eventIds);
          }
        }
      }

      // Step 3: Delete all marked series
      for (const serieId of seriesToDelete) {
        try {
          await db.collection(SERIES).doc(serieId).delete();
        } catch (err) {
          console.error(`Failed to delete series ${serieId}:`, err);
        }
      }

      // Step 4: Delete all events from expired series
      for (const eventId of seriesEventsToDelete) {
        try {
          await db.collection(EVENTS).doc(eventId).delete();
        } catch (err) {
          console.error(`Failed to delete event ${eventId}:`, err);
        }
      }

      // Step 5: Fetch remaining events and check if they are expired
      const eventsSnapshot = await db.collection(EVENTS).get();
      const standaloneEventsToDelete: string[] = [];

      for (const eventDoc of eventsSnapshot.docs) {
        const eventData = eventDoc.data();
        const eventId = eventDoc.id;

        // Calculate event end time (start date + duration)
        const eventDate = eventData.date;
        const durationMinutes = eventData.duration || 0;

        if (eventDate && eventDate.seconds) {
          const eventStartMs = eventDate.seconds * S_TO_MS;
          const durationMs = durationMinutes * MIN_TO_S * S_TO_MS;
          const eventEndMs = eventStartMs + durationMs;

          // If event is expired, mark it for deletion
          if (eventEndMs < cutoffTime) {
            standaloneEventsToDelete.push(eventId);
          }
        }
      }

      // Step 6: Delete expired standalone events
      for (const eventId of standaloneEventsToDelete) {
        try {
          await db.collection(EVENTS).doc(eventId).delete();
        } catch (err) {
          console.error(`Failed to delete event ${eventId}:`, err);
        }
      }

      return null;
    } catch (error) {
      throw error;
    }
  });
