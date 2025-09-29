package com.drone;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class GetJson {

    public static void main(String[] args) {
        String urlString = "http://192.168.40.20";
        String fileName = System.currentTimeMillis() + ".json";
        if (args.length > 0) {
            urlString = args[0];
        }
        if (args.length > 1) {
            urlString = args[1];
        }


        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/json");

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                connection.disconnect();

                String jsonString = response.toString();
                File file = new File(fileName);
                FileWriter writer = new FileWriter(file);
                writer.write(jsonString);
                writer.close();

            } else {
                System.out.println("GET request failed with response code: " + responseCode);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
