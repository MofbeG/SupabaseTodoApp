package com.sigma.supabasetodoapp.network;

public class SupabaseConfig {
    public static final String SUPABASE_URL = "https://rsvwgktqxuruuzmylzxe.supabase.co";
    public static final String SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InJzdndna3RxeHVydXV6bXlsenhlIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjU3ODM4NTcsImV4cCI6MjA4MTM1OTg1N30.UmQTMes2GfGrSExeyOhNlK8Kt3wVA16w659mXx_mM-0";

    public static final String AUTH_SIGNUP_URL =
            SUPABASE_URL + "/auth/v1/signup";
    public static final String AUTH_SIGNIN_URL =
            SUPABASE_URL + "/auth/v1/token?grant_type=password";

    public static final String TABLE_URL =
            SUPABASE_URL + "/rest/v1/personal_goals";

}
