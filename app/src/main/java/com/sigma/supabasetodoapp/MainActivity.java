package com.sigma.supabasetodoapp;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
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
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
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

        adapter.setListener(new GoalAdapter.OnGoalActionListener() {
            @Override
            public void onDelete(PersonalGoal goal) {
                confirmDelete(goal);
            }

            @Override
            public void onEdit(PersonalGoal goal) {
                showEditDialog(goal);
            }
        });

        findViewById(R.id.fabAdd).setOnClickListener(v -> showAddDialog());

        findViewById(R.id.btnLogout).setOnClickListener(v -> confirmLogout());

        executor = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());

        String token = prefs.getString("access_token", null);
        if (token == null || token.isEmpty()) {
            goToLogin();
            return;
        }

        loadGoals(token);
    }
    private void confirmDelete(PersonalGoal goal) {
        new AlertDialog.Builder(this)
                .setTitle("Удалить цель?")
                .setMessage(goal.getGoalText())
                .setPositiveButton("Удалить", (d, w) -> deleteGoal(goal))
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void deleteGoal(PersonalGoal goal) {
        String token = prefs.getString("access_token", null);
        if (token == null) { goToLogin(); return; }

        executor.execute(() -> {
            HttpURLConnection c = null;
            try {
                String urlStr = SupabaseConfig.TABLE_URL + "?id=eq." + URLEncoder.encode(goal.getId(), "UTF-8");
                URL url = new URL(urlStr);

                c = (HttpURLConnection) url.openConnection();
                c.setRequestMethod("DELETE");
                c.setConnectTimeout(7000);
                c.setReadTimeout(7000);

                c.setRequestProperty("apikey", SupabaseConfig.SUPABASE_ANON_KEY);
                c.setRequestProperty("Authorization", "Bearer " + token);

                int code = c.getResponseCode();
                readStream(c, code);

                if (code == 204 || code == 200) {
                    mainHandler.post(() -> {
                        Toast.makeText(this, "Удалено", Toast.LENGTH_SHORT).show();
                        loadGoals(token);
                    });
                } else if (code == 401) {
                    mainHandler.post(() -> {
                        Toast.makeText(this, "Сессия истекла, войдите снова", Toast.LENGTH_LONG).show();
                        prefs.edit().clear().apply();
                        goToLogin();
                    });
                } else {
                    mainHandler.post(() ->
                            Toast.makeText(this, "Ошибка удаления: " + code, Toast.LENGTH_LONG).show()
                    );
                }
            } catch (Exception e) {
                mainHandler.post(() ->
                        Toast.makeText(this, "Сетевая ошибка при удалении", Toast.LENGTH_LONG).show()
                );
            } finally {
                if (c != null) c.disconnect();
            }
        });
    }

    private void showEditDialog(PersonalGoal goal) {
        View view = getLayoutInflater().inflate(R.layout.dialog_add_goal, null);

        EditText etGoalText = view.findViewById(R.id.etGoalText);
        EditText etDesiredResult = view.findViewById(R.id.etDesiredResult);
        EditText etTargetDate = view.findViewById(R.id.etTargetDate);

        etGoalText.setText(goal.getGoalText());
        etDesiredResult.setText(String.valueOf(goal.getDesiredResult()));

        final String[] selectedIsoDate = { goal.getTargetDate() };

        if (goal.getTargetDate() != null) {
            try {
                SimpleDateFormat iso = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                SimpleDateFormat ru = new SimpleDateFormat("dd.MM.yyyy", new Locale("ru"));
                Date d = iso.parse(goal.getTargetDate());
                etTargetDate.setText(ru.format(d));
            } catch (Exception ignored) {}
        }

        etTargetDate.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            if (selectedIsoDate[0] != null) {
                try {
                    Date parsed = new SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(selectedIsoDate[0]);
                    cal.setTime(parsed);
                } catch (Exception ignored) {}
            }

            new DatePickerDialog(
                    this,
                    (dp, year, month, day) -> {
                        Calendar picked = Calendar.getInstance();
                        picked.set(year, month, day);

                        SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                        SimpleDateFormat uiFormat = new SimpleDateFormat("dd.MM.yyyy", new Locale("ru"));

                        selectedIsoDate[0] = isoFormat.format(picked.getTime());
                        etTargetDate.setText(uiFormat.format(picked.getTime()));
                    },
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)
            ).show();
        });

        new AlertDialog.Builder(this)
                .setTitle("Редактировать цель")
                .setView(view)
                .setPositiveButton("Сохранить", (d, w) -> {
                    String text = etGoalText.getText().toString().trim();
                    String desiredStr = etDesiredResult.getText().toString().trim();

                    if (text.isEmpty() || desiredStr.isEmpty()) {
                        Toast.makeText(this, "Заполни цель и результат", Toast.LENGTH_LONG).show();
                        return;
                    }

                    double desired;
                    try { desired = Double.parseDouble(desiredStr); }
                    catch (NumberFormatException e) {
                        Toast.makeText(this, "Результат должен быть числом", Toast.LENGTH_LONG).show();
                        return;
                    }

                    updateGoal(goal.getId(), text, desired, selectedIsoDate[0]);
                })
                .setNeutralButton("Сбросить дату", (d, w) -> updateGoal(goal.getId(),
                        etGoalText.getText().toString().trim(),
                        parseDoubleSafe(etDesiredResult.getText().toString().trim()),
                        null))
                .setNegativeButton("Отмена", null)
                .show();
    }

    private double parseDoubleSafe(String s) {
        try { return Double.parseDouble(s); } catch (Exception e) { return 0; }
    }

    private void updateGoal(String id, String goalText, double desiredResult, @Nullable String targetDateIso) {
        String token = prefs.getString("access_token", null);
        String userId = prefs.getString("user_id", null);
        if (token == null || userId == null) { goToLogin(); return; }

        executor.execute(() -> {
            HttpURLConnection c = null;
            try {
                String urlStr = SupabaseConfig.TABLE_URL + "?id=eq." + URLEncoder.encode(id, "UTF-8");
                URL url = new URL(urlStr);

                c = (HttpURLConnection) url.openConnection();
                c.setRequestMethod("PATCH");
                c.setConnectTimeout(7000);
                c.setReadTimeout(7000);

                c.setRequestProperty("apikey", SupabaseConfig.SUPABASE_ANON_KEY);
                c.setRequestProperty("Authorization", "Bearer " + token);
                c.setRequestProperty("Content-Type", "application/json");
                c.setRequestProperty("Prefer", "return=minimal");
                c.setDoOutput(true);

                JSONObject body = new JSONObject();
                body.put("goal_text", goalText);
                body.put("desired_result", desiredResult);

                if (targetDateIso == null) body.put("target_date", JSONObject.NULL);
                else body.put("target_date", targetDateIso);

                try (OutputStream os = c.getOutputStream()) {
                    os.write(body.toString().getBytes(StandardCharsets.UTF_8));
                }

                int code = c.getResponseCode();
                readStream(c, code);

                if (code == 204 || code == 200) {
                    mainHandler.post(() -> {
                        Toast.makeText(this, "Сохранено", Toast.LENGTH_SHORT).show();
                        loadGoals(token);
                    });
                } else if (code == 401) {
                    mainHandler.post(() -> {
                        Toast.makeText(this, "Сессия истекла, войдите снова", Toast.LENGTH_LONG).show();
                        prefs.edit().clear().apply();
                        goToLogin();
                    });
                } else {
                    mainHandler.post(() ->
                            Toast.makeText(this, "Ошибка обновления: " + code, Toast.LENGTH_LONG).show()
                    );
                }

            } catch (Exception e) {
                mainHandler.post(() ->
                        Toast.makeText(this, "Сетевая ошибка при обновлении", Toast.LENGTH_LONG).show()
                );
            } finally {
                if (c != null) c.disconnect();
            }
        });
    }

    private void showAddDialog() {
        View view = getLayoutInflater().inflate(R.layout.dialog_add_goal, null);

        EditText etGoalText = view.findViewById(R.id.etGoalText);
        EditText etDesiredResult = view.findViewById(R.id.etDesiredResult);
        EditText etTargetDate = view.findViewById(R.id.etTargetDate);

        final String[] selectedIsoDate = {null};

        etTargetDate.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();

            new DatePickerDialog(
                    this,
                    (view1, year, month, dayOfMonth) -> {
                        Calendar picked = Calendar.getInstance();
                        picked.set(year, month, dayOfMonth);

                        SimpleDateFormat isoFormat =
                                new SimpleDateFormat("yyyy-MM-dd", Locale.US);

                        SimpleDateFormat uiFormat =
                                new SimpleDateFormat("dd.MM.yyyy", new Locale("ru"));

                        selectedIsoDate[0] = isoFormat.format(picked.getTime());
                        etTargetDate.setText(uiFormat.format(picked.getTime()));
                    },
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)
            ).show();
        });

        new AlertDialog.Builder(this)
                .setTitle("Добавить цель")
                .setView(view)
                .setPositiveButton("Добавить", (d, which) -> {
                    String goalText = etGoalText.getText().toString().trim();
                    String desiredStr = etDesiredResult.getText().toString().trim();
                    String targetDate = selectedIsoDate[0];

                    if (goalText.isEmpty() || desiredStr.isEmpty()) {
                        Toast.makeText(this, "Заполни цель и результат", Toast.LENGTH_LONG).show();
                        return;
                    }

                    double desired;
                    try {
                        desired = Double.parseDouble(desiredStr);
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "Результат должен быть числом", Toast.LENGTH_LONG).show();
                        return;
                    }

                    if (targetDate == null || targetDate.isEmpty()) {
                        targetDate = null;
                    }

                    addGoal(goalText, desired, targetDate);
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void addGoal(String goalText, double desiredResult, @Nullable String targetDate) {
        String token = prefs.getString("access_token", null);
        String userId = prefs.getString("user_id", null);

        if (token == null || userId == null) {
            goToLogin();
            return;
        }

        executor.execute(() -> {
            HttpURLConnection c = null;
            try {
                URL url = new URL(SupabaseConfig.TABLE_URL);
                c = (HttpURLConnection) url.openConnection();
                c.setRequestMethod("POST");
                c.setConnectTimeout(7000);
                c.setReadTimeout(7000);

                c.setRequestProperty("apikey", SupabaseConfig.SUPABASE_ANON_KEY);
                c.setRequestProperty("Authorization", "Bearer " + token);
                c.setRequestProperty("Content-Type", "application/json");
                c.setRequestProperty("Prefer", "return=minimal");
                c.setDoOutput(true);

                JSONObject body = new JSONObject();
                body.put("user_id", userId);
                body.put("goal_text", goalText);
                body.put("desired_result", desiredResult);
                if (targetDate == null) body.put("target_date", JSONObject.NULL);
                else body.put("target_date", targetDate);

                JSONArray arr = new JSONArray();
                arr.put(body);

                try (OutputStream os = c.getOutputStream()) {
                    os.write(arr.toString().getBytes(StandardCharsets.UTF_8));
                }

                int code = c.getResponseCode();
                String resp = readStream(c, code);

                if (code == 201) {
                    mainHandler.post(() -> {
                        Toast.makeText(this, "Цель добавлена", Toast.LENGTH_SHORT).show();
                        loadGoals(token);
                    });
                } else if (code == 401) {
                    mainHandler.post(() -> {
                        Toast.makeText(this, "Сессия истекла, войдите снова", Toast.LENGTH_LONG).show();
                        prefs.edit().clear().apply();
                        goToLogin();
                    });
                } else {
                    mainHandler.post(() ->
                            Toast.makeText(this, "Ошибка добавления: " + code, Toast.LENGTH_LONG).show()
                    );
                }

            } catch (Exception e) {
                mainHandler.post(() ->
                        Toast.makeText(this, "Сетевая ошибка при добавлении", Toast.LENGTH_LONG).show()
                );
            } finally {
                if (c != null) c.disconnect();
            }
        });
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

    private void confirmLogout() {
        new AlertDialog.Builder(this)
                .setTitle("Выход из аккаунта")
                .setMessage("Вы действительно хотите выйти?")
                .setPositiveButton("Выйти", (d, w) -> {
                    prefs.edit().clear().apply();
                    startActivity(new Intent(this, LoginActivity.class));
                    finish();
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void logout() {
        prefs.edit().clear().apply();
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

}
