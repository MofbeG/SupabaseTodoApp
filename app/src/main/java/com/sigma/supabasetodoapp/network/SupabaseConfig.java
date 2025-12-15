package com.sigma.supabasetodoapp.network;

public class SupabaseConfig {
    public static final String SUPABASE_URL = "https://XXXX.supabase.co";
    public static final String SUPABASE_ANON_KEY = "YOUR_ANON_KEY";

    public static final String AUTH_SIGNUP_URL =
            SUPABASE_URL + "/auth/v1/signup";
    public static final String AUTH_SIGNIN_URL =
            SUPABASE_URL + "/auth/v1/token?grant_type=password";

    public static final String TABLE_URL =
            SUPABASE_URL + "/rest/v1/personal_goals";

}
