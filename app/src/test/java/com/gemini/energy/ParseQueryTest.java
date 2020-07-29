package com.gemini.energy;

import org.json.JSONObject;
import org.junit.Test;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ParseQueryTest {

    String parseApiRoot = "http://ec2-18-189-11-148.us-east-2.compute.amazonaws.com:80/parse/";
    String applicationId = "47f916f7005d19ddd78a6be6b4bdba3ca49615a0";
    String masterKey = "NLI214vDqkoFTJSTtIE2xLqMme6Evd0kA1BbJ20S";
    String classPath = "classes/";

    @Test
    public void plugloadQueryTest() {
        queryPlugload();
    }

    private void queryPlugload() {

        String plugloadGetUrl = parseApiRoot + classPath + "PlugLoad?where=";
        try {

            JSONObject json = new JSONObject();
            json.put("data.style_type", "Reach-in");
            json.put("data.total_volume", 17.89);

            String finalUrl = plugloadGetUrl + json;

            JSONObject response = get(finalUrl);
            System.out.println(response.toString());

        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    private JSONObject get(String url) {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", "OkHttp Headers.java")
                .addHeader("Content-Type", "application/json")
                .addHeader("X-Parse-Application-Id", applicationId)
                .addHeader("X-Parse-REST-API-Key", masterKey)
                .build();

        try {
            Response response = client.newCall(request).execute();
            JSONObject jsonResponse = new JSONObject();

            try {
                JSONObject data = new JSONObject(response.body().string());
                jsonResponse.put("status", "success");
                jsonResponse.put("response", data);
            } catch (Exception e) {
            }

            return jsonResponse;

        } catch (Exception e) {

            JSONObject exceptionResponse = new JSONObject();

            try {
                exceptionResponse.put("status", "fail");
                exceptionResponse.put("message", "Exception occurred while executing get.");
            } catch (Exception ex) {
            }

            return exceptionResponse;
        }
    }
}
