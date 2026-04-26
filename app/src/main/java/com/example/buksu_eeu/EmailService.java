package com.example.buksu_eeu;

import android.util.Log;
import androidx.annotation.NonNull;
import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;

public class EmailService {
    private static final String TAG = "EmailService";
    private static final String BREVO_API_URL = "https://api.brevo.com/v3/smtp/email";
    
    // Read from local.properties (securely injected via build.gradle.kts)
    private static final String API_KEY = BuildConfig.BREVO_API_KEY;
    
    // Replace this with the email you verified as a "Sender" in Brevo
    private static final String SENDER_EMAIL = "milckyjhonesfrancisco@gmail.com"; 
    private static final String SENDER_NAME = "BUKSU EEU Admin";

    private final OkHttpClient client;

    public EmailService() {
        this.client = new OkHttpClient();
    }

    /**
     * Sends an email to the ADMIN when a new order is placed.
     */
    public void sendNewOrderAlertToAdmin(String orderId, String customerName, double total, String itemsSummary) {
        String subject = "New Order Received! #" + orderId;
        String content = "<html><body>" +
                "<h2>New Order Alert</h2>" +
                "<p><strong>Order ID:</strong> " + orderId + "</p>" +
                "<p><strong>Customer:</strong> " + customerName + "</p>" +
                "<p><strong>Items:</strong> " + itemsSummary + "</p>" +
                "<p><strong>Total Amount:</strong> ₱" + total + "</p>" +
                "<p>Please check the Admin Dashboard for details.</p>" +
                "</body></html>";

        // Admin email (you can change this to your actual admin email)
        sendEmail("2301105770@student.buksu.edu.ph", "EEU Admin", subject, content);
    }

    /**
     * Sends an email to the STUDENT when their order is ready.
     */
    public void sendReadyToPickupEmail(String studentEmail, String studentName, String orderId, String itemsSummary) {
        String subject = "Your Order is Ready for Pickup! #" + orderId;
        String content = "<html><body>" +
                "<h2>Hello " + studentName + "!</h2>" +
                "<p>Good news! Your order <strong>#" + orderId + "</strong> is now ready for pickup at the EEU office.</p>" +
                "<p><strong>Items:</strong> " + itemsSummary + "</p>" +
                "<p>Please bring your ID when claiming your items.</p>" +
                "<p>Thank you for choosing BukSu EEU!</p>" +
                "</body></html>";

        sendEmail(studentEmail, studentName, subject, content);
    }

    private void sendEmail(String toEmail, String toName, String subject, String htmlContent) {
        try {
            JSONObject root = new JSONObject();
            
            // Sender info
            JSONObject sender = new JSONObject();
            sender.put("name", SENDER_NAME);
            sender.put("email", SENDER_EMAIL);
            root.put("sender", sender);

            // Recipient info
            JSONArray to = new JSONArray();
            JSONObject recipient = new JSONObject();
            recipient.put("email", toEmail);
            recipient.put("name", toName);
            to.put(recipient);
            root.put("to", to);

            root.put("subject", subject);
            root.put("htmlContent", htmlContent);

            RequestBody body = RequestBody.create(
                    root.toString(),
                    MediaType.get("application/json; charset=utf-8")
            );

            Request request = new Request.Builder()
                    .url(BREVO_API_URL)
                    .addHeader("api-key", API_KEY)
                    .post(body)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    Log.e(TAG, "Failed to send email: " + e.getMessage());
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    if (response.isSuccessful()) {
                        Log.d(TAG, "Email sent successfully to " + toEmail);
                    } else {
                        Log.e(TAG, "Email sending failed. Code: " + response.code() + ", Body: " + response.body().string());
                    }
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Error building JSON for email: " + e.getMessage());
        }
    }
}
