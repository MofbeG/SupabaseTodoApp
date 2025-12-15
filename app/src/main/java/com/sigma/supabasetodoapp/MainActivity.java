package com.sigma.supabasetodoapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.sigma.supabasetodoapp.adapters.GoalAdapter;
import com.sigma.supabasetodoapp.models.PersonalGoal;
import com.sigma.supabasetodoapp.network.SupabaseConfig;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private RecyclerView rvGoals;
    private GoalAdapter adapter;

    private ExecutorService executor;
    private Handler mainHandler;

    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences("session", MODE_PRIVATE);

        rvGoals = findViewById(R.id.rvGoals);
        rvGoals.setLayoutManager(new LinearLayoutManager(this));
        adapter = new GoalAdapter();
        rvGoals.setAdapter(adapter);

        executor = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());

        String token = prefs.getString("access_token", null);
        if (token == null || token.isEmpty()) {
            goToLogin();
            return;
        }

        loadGoals(token);
    }

    private void goToLogin() {
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    private void loadGoals(String token) {
        executor.execute(() -> {
            HttpURLConnection c = null;
            try {
                String urlStr = SupabaseConfig.TABLE_URL + "?select=*&order=created_at.desc";
                URL url = new URL(urlStr);

                c = (HttpURLConnection) url.openConnection();
                c.setRequestMethod("GET");
                c.setConnectTimeout(7000);
                c.setReadTimeout(7000);

                c.setRequestProperty("apikey", SupabaseConfig.SUPABASE_ANON_KEY);
                c.setRequestProperty("Authorization", "Bearer " + token);
                c.setRequestProperty("Accept", "application/json");

                int code = c.getResponseCode();
                String resp = readStream(c, code);

                if (code != HttpURLConnection.HTTP_OK) {
                    // если токен протух/битый — выкидываем на логин
                    if (code == 401) {
                        mainHandler.post(() -> {
                            Toast.makeText(this, "Сессия истекла, войдите снова", Toast.LENGTH_LONG).show();
                            prefs.edit().clear().apply();
                            goToLogin();
                        });
                        return;
                    }

                    mainHandler.post(() ->
                            Toast.makeText(this, "Ошибка загрузки: " + code, Toast.LENGTH_LONG).show()
                    );
                    return;
                }

                List<PersonalGoal> goals = parseGoals(resp);

                mainHandler.post(() -> adapter.setGoals(goals));

            } catch (Exception e) {
                mainHandler.post(() ->
                        Toast.makeText(this, "Сетевая ошибка", Toast.LENGTH_LONG).show()
                );
            } finally {
                if (c != null) c.disconnect();
            }
        });
    }

    private List<PersonalGoal> parseGoals(String json) throws JSONException {
        List<PersonalGoal> list = new ArrayList<>();
        JSONArray arr = new JSONArray(json);

        for (int i = 0; i < arr.length(); i++) {
            JSONObject obj = arr.getJSONObject(i);

            PersonalGoal g = new PersonalGoal();
            g.setId(obj.optString("id", ""));
            g.setGoalText(obj.optString("goal_text", ""));
            g.setDesiredResult(obj.optDouble("desired_result", 0));
            g.setTargetDate(obj.isNull("target_date") ? null : obj.optString("target_date", null));

            list.add(g);
        }
        return list;
    }

    private String readStream(HttpURLConnection c, int code) throws IOException {
        InputStream is = (code >= 200 && code < 300) ? c.getInputStream() : c.getErrorStream();
        if (is == null) return "";
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) sb.append(line);
        br.close();
        return sb.toString();
    }
}
