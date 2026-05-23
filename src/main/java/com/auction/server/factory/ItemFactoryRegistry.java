package com.auction.server.factory;

import java.util.HashMap;
import java.util.Map;

public class ItemFactoryRegistry {
    private static final Map<String, ItemFactory> factories = new HashMap<>();

    static {
        initializeDefaultFactories();
    }

    public static void registerFactory(String itemType, ItemFactory factory) {
        factories.put(itemType.toUpperCase(), factory);
    }

    public static ItemFactory getFactory(String itemType) {
        return factories.get(itemType.toUpperCase());
    }

    public static void initializeDefaultFactories() {
        registerFactory("VEHICLE", new VehicleFactory());
        registerFactory("ELECTRONICS", new ElectronicsFactory());
        registerFactory("ART", new ArtFactory());
        registerFactory("OTHER", new OtherItemFactory());
    }
}