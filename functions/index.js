const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const { setGlobalOptions } = require("firebase-functions/v2");
const admin = require("firebase-admin");

admin.initializeApp();
setGlobalOptions({ maxInstances: 10 });

exports.sendPushNotification = onDocumentCreated("notifications/{notificationId}", async (event) => {
    const snapshot = event.data;
    if (!snapshot) {
        console.log("No data associated with the event");
        return;
    }

    const notification = snapshot.data();
    const recipientId = notification.recipientId;
    const title = notification.title || "New Notification";
    const message = notification.message || "";

    if (!recipientId) {
        console.log("No recipientId found for notification:", event.params.notificationId);
        return;
    }

    try {
        let tokens = [];

        if (recipientId === "admin") {
            // Fetch all admin tokens
            const adminsSnapshot = await admin.firestore()
                .collection("users")
                .where("role", "==", "admin")
                .get();

            adminsSnapshot.forEach(doc => {
                const token = doc.data().fcmToken;
                if (token) {
                    tokens.push(token);
                }
            });
        } else {
            // Fetch specific user's token
            const userDoc = await admin.firestore().collection("users").doc(recipientId).get();
            if (userDoc.exists) {
                const token = userDoc.data().fcmToken;
                if (token) {
                    tokens.push(token);
                }
            }
        }

        if (tokens.length === 0) {
            console.log(`No valid FCM tokens found for recipientId: ${recipientId}`);
            return;
        }

        // Create the payload
        const payload = {
            notification: {
                title: title,
                body: message,
            },
            // We can also send data payload if we want the app to handle it differently
            data: {
                click_action: "FLUTTER_NOTIFICATION_CLICK" // Just in case, standard payload
            }
        };

        // Send notifications
        const response = await admin.messaging().sendEachForMulticast({
            tokens: tokens,
            notification: payload.notification,
            data: payload.data
        });

        console.log(`Successfully sent ${response.successCount} messages. Failed: ${response.failureCount}`);
        
        // Log failures if any
        if (response.failureCount > 0) {
            response.responses.forEach((resp, idx) => {
                if (!resp.success) {
                    console.error(`Failed to send to token at index ${idx}:`, resp.error);
                }
            });
        }

    } catch (error) {
        console.error("Error sending push notification:", error);
    }
});
