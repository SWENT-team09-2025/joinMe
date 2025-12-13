import * as functions from "firebase-functions/v1";
import * as admin from "firebase-admin";

// Implemented with the help of Claud AI

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
const INVITATIONS = "invitations";


// Notification type constants
const NOTIFICATION_TYPE_EVENT_CHAT_MESSAGE = "event_chat_message";
const NOTIFICATION_TYPE_GROUP_CHAT_MESSAGE = "group_chat_message";

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

/**
 * Triggered when a new follow relationship is created.
 * Sends a push notification to the user being followed.
 */
export const onUserFollowed = functions.firestore
  .document("follows/{followId}")
  .onCreate(async (snap, context) => {
    try {
      const followData = snap.data();
      const followerId = followData.followerId;
      const followedId = followData.followedId;

      console.log(`User ${followerId} followed user ${followedId}`);

      // Get the follower's username
      const followerUsername = await getUsername(followerId);

      // Get user's FCM token from their profile
      const userDoc = await db.collection("profiles").doc(followedId).get();

      if (!userDoc.exists) {
        console.log(`User ${followedId} not found`);
        return null;
      }

      const userData = userDoc.data();
      const fcmToken = userData?.fcmToken;

      if (!fcmToken) {
        console.log(`User ${followedId} does not have an FCM token`);
        return null;
      }

      // Send notification with follower info
      const message: admin.messaging.Message = {
        token: fcmToken,
        notification: {
          title: "New follower",
          body: `${followerUsername} started following you`,
        },
        data: {
          type: "new_follower",
          followerId: followerId,
          followerUsername: followerUsername,
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
      console.log(`Follow notification sent to user ${followedId}`);
      return null;
    } catch (error) {
      console.error("Error in onUserFollowed:", error);
      return null;
    }
  });

/**
 * Triggered when a new message is created in a group or event chat.
 * Sends push notifications to all members/participants except the sender.
 */
export const onChatMessageCreated = functions.database
  .ref("conversations/{conversationId}/messages/{messageId}")
  .onCreate(async (snapshot, context) => {
    try {
      const conversationId = context.params.conversationId;
      const messageId = context.params.messageId;
      const messageData = snapshot.val();

      console.log(`New message in conversation ${conversationId}`);

      // Get message details
      const senderId = messageData.senderId;
      const senderName = messageData.senderName || "Someone";
      const messageContent = messageData.content || "New message";
      const messageType = messageData.type || "TEXT";

      // Skip system messages (like "User joined")
      if (messageType === "SYSTEM") {
        console.log("Skipping notification for system message");
        return null;
      }

      // STEP 1: Try to fetch as a group first
      const groupDoc = await db.collection("groups").doc(conversationId).get();

      let memberIds: string[] = [];
      let chatName = "Chat";
      let isEventChat = false;

      if (groupDoc.exists) {
        // It's a group chat
        const groupData = groupDoc.data();
        if (!groupData) {
          return null;
        }
        memberIds = groupData.memberIds || [];
        chatName = groupData.name || "Group Chat";
        console.log(`Group "${chatName}" has ${memberIds.length} members`);
      } else {
        // STEP 2: Try to fetch as an event
        const eventDoc = await db.collection("events").doc(conversationId).get();

        if (!eventDoc.exists) {
          console.log(`Neither group nor event found: ${conversationId}`);
          return null;
        }

        const eventData = eventDoc.data();
        if (!eventData) {
          return null;
        }

        isEventChat = true;
        memberIds = eventData.participants || [];
        chatName = eventData.title || "Event Chat";
        console.log(`Event "${chatName}" has ${memberIds.length} participants`);
      }

      // STEP 2: Filter out the sender (don't notify yourself)
      const recipientIds = memberIds.filter((userId) => userId !== senderId);

      if (recipientIds.length === 0) {
        console.log("No recipients to notify");
        return null;
      }

      // STEP 3: Truncate long messages for notification
      const truncatedContent =
        messageContent.length > 100
          ? messageContent.substring(0, 100) + "..."
          : messageContent;

      // STEP 4: Send notification to each recipient
      const notificationPromises = recipientIds.map(async (userId) => {
        try {
          // Get user's FCM token
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

          // Build notification payload
          const notificationData: {[key: string]: string} = {
            type: isEventChat ? NOTIFICATION_TYPE_EVENT_CHAT_MESSAGE : NOTIFICATION_TYPE_GROUP_CHAT_MESSAGE,
            conversationId: conversationId,
            messageId: messageId,
            senderId: senderId,
            senderName: senderName,
            chatName: chatName,
          };

          // Add eventId or groupId based on chat type
          if (isEventChat) {
            notificationData.eventId = conversationId;
          } else {
            notificationData.groupId = conversationId;
          }

          const message: admin.messaging.Message = {
            token: fcmToken,
            notification: {
              title: `${chatName}: ${senderName}`,
              body: truncatedContent,
            },
            data: notificationData,
            android: {
              priority: "high",
              notification: {
                channelId: "joinme_notifications",
                priority: "high",
                sound: "default",
              },
            },
          };

          // Send the notification
          await admin.messaging().send(message);
          console.log(`Notification sent to user ${userId}`);
        } catch (error) {
          console.error(`Error sending notification to user ${userId}:`, error);
        }
      });

      // Wait for all notifications to be sent
      await Promise.all(notificationPromises);

      console.log(
        `Successfully sent notifications to ${recipientIds.length} recipients`
      );
      return null;
    } catch (error) {
      console.error("Error in onChatMessageCreated:", error);
      return null;
    }
  });

/**
 * Scheduled function that runs daily at midnight (UTC).
 * Cleans up expired invitations from the database.
 *
 * Logic:
 * 1. Fetch all invitations
 * 2. Check each invitation's expiresAt timestamp
 * 3. Delete invitations where expiresAt < now
 */
export const cleanupExpiredInvitations = functions.pubsub
  .schedule(RECURRENCE)
  .timeZone(TIME_ZONE)
  .onRun(async (context) => {
    const now = Date.now();

    try {
      const expiredInvitations: string[] = [];

      // Step 1: Fetch all invitations
      const invitationsSnapshot = await db.collection(INVITATIONS).get();

      // Step 2: Check each invitation
      for (const invitationDoc of invitationsSnapshot.docs) {
        const invitationData = invitationDoc.data();
        const expiresAt = invitationData.expiresAt;

        if (expiresAt && expiresAt.seconds) {
          const expiresAtMs = expiresAt.seconds * S_TO_MS;

          if (expiresAtMs < now) {
            expiredInvitations.push(invitationDoc.id);
          }
        }
      }

      // Step 3: Delete all expired invitations
      for (const token of expiredInvitations) {
        try {
          await db.collection(INVITATIONS).doc(token).delete();
        } catch (err) {
          console.error(`Failed to delete invitation ${token}:`, err);
        }
      }
      return null;
    } catch (error) {
      throw error;
    }
  });
