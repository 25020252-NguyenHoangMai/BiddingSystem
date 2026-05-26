package com.auction.client.util;

import java.util.LinkedHashMap;
import java.util.Map;

public class ImageCache {
    private static final int MAX_SIZE = 100;

    private static final Map<String, byte[]> cache = new LinkedHashMap<>(MAX_SIZE, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, byte[]> eldest) {
            return size() > MAX_SIZE;
        }
    };

    public static synchronized byte[] get(String imagePath) {
        return cache.get(imagePath);
    }

    public static synchronized void put(String imagePath, byte[] bytes) {
        if (imagePath != null && bytes != null && bytes.length > 0) {
            cache.put(imagePath, bytes);
        }
    }

    public static synchronized boolean contains(String imagePath) {
        return cache.containsKey(imagePath);
    }
}