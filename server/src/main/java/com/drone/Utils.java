package com.drone;

import com.google.gson.FieldNamingStrategy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.nio.charset.Charset;

public class Utils {
    private static Gson gson = new GsonBuilder().setPrettyPrinting().create();
    public static final Charset utf8 = Charset.forName("UTF-8");
    private Utils(){

    }

    public static String toJson(Object object) {
        return gson.toJson(object);

    }

    public static <T> T fromJson(String json, Class<T> clazz) {
        return gson.fromJson(json, clazz);
    }
}
