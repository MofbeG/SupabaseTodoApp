package com.sigma.supabasetodoapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.sigma.supabasetodoapp.network.SupabaseConfig;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LoginActivity extends AppCompatActivity {

    private ExecutorService networkExecutor;
    private Handler mainHandler;

    private TextInputEditText etEmail, etPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etEmail = findViewById(R.id.emailEditText);
        etPassword = findViewById(R.id.passwordEditText);

        networkExecutor = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());

        findViewById(R.id.signUpButton).setOnClickListener(v -> signUp());
        findViewById(R.id.signInButton).setOnClickListener(v -> signIn());
    }

    private void signUp() {
        final String email = etEmail.getText().toString().trim();
        final String pass = etPassword.getText().toString().trim();

        networkExecutor.execute(() -> {
            HttpURLConnection c = null;
            try {
                URL url = new URL(SupabaseConfig.AUTH_SIGNUP_URL);
                c = (HttpURLConnection) url.openConnection();
                c.setRequestMethod("POST");
                c.setRequestProperty("apikey", SupabaseConfig.SUPABASE_ANON_KEY);
                c.setRequestProperty("Content-Type", "application/json");
                c.setDoOutput(true);

                String body = new JSONObject()
                        .put("email", email)
                        .put("password", pass)
                        .toString();

                try (OutputStream os = c.getOutputStream()) {
                    os.write(body.getBytes(StandardCharsets.UTF_8));
                }

                int code = c.getResponseCode();
                String resp = readStream(c, code);

                int finalCode = code;
                mainHandler.post(() -> Toast.makeText(
                        this,
                        finalCode == 200 || finalCode == 201 ? "Регистрация успешна" : "Ошибка: " + finalCode,
                        Toast.LENGTH_LONG
                ).show());

            } catch (Exception e) {
                mainHandler.post(() -> Toast.makeText(this, "Сетевая ошибка", Toast.LENGTH_LONG).show());
            } finally {
                if (c != null) c.disconnect();
            }
        });
    }

    private void signIn() {
        final String email = etEmail.getText().toString().trim();
        final String pass = etPassword.getText().toString().trim();

        networkExecutor.execute(() -> {
            HttpURLConnection c = null;
            try {
                URL url = new URL(SupabaseConfig.AUTH_SIGNIN_URL);
                c = (HttpURLConnection) url.openConnection();
                c.setRequestMethod("POST");
                c.setRequestProperty("apikey", SupabaseConfig.SUPABASE_ANON_KEY);
                c.setRequestProperty("Content-Type", "application/json");
                c.setDoOutput(true);

                String body = new JSONObject()
                        .put("email", email)
                        .put("password", pass)
                        .toString();

                try (OutputStream os = c.getOutputStream()) {
                    os.write(body.getBytes(StandardCharsets.UTF_8));
                }

                int code = c.getResponseCode();
                String resp = readStream(c, code);

                if (code == 200) {
                    JSONObject obj = new JSONObject(resp);
                    String accessToken = obj.getString("access_token");
                    String userId = obj.getJSONObject("user").getString("id");

                    SharedPreferences prefs = getSharedPreferences("session", MODE_PRIVATE);
                    prefs.edit()
                            .putString("access_token", accessToken)
                            .putString("user_id", userId)
                            .apply();

                    mainHandler.post(() -> {
                        startActivity(new Intent(this, MainActivity.class));
                        finish();
                    });
                } else {
                    mainHandler.post(() ->
                            Toast.makeText(this, "Ошибка входа: " + code, Toast.LENGTH_LONG).show()
                    );
                }

            } catch (Exception e) {
                mainHandler.post(() -> Toast.makeText(this, "Сетевая ошибка", Toast.LENGTH_LONG).show());
            } finally {
                if (c != null) c.disconnect();
            }
        });
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
