package com.example.buksu_eeu;

import android.app.Application;
import com.cloudinary.android.MediaManager;
import java.util.HashMap;
import java.util.Map;

public class BukSuApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        Map<String, String> config = new HashMap<>();
        config.put("cloud_name", "dv2hhwzre");
        config.put("api_key", "355231292744679");
        config.put("api_secret", "-LjyAYPpTG7w3o9WV3g2PQfEVvQ");
        MediaManager.init(this, config);
    }
}
