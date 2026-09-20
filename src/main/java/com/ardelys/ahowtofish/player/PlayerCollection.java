package com.ardelys.ahowtofish.player;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerCollection {
    public record Record(
            String fishId,
            long totalCatches,
            double largestWeight,
            double totalWeight,
            double totalValue,
            long firstCatchTimestamp
    ) {}

    private final Map<String, Record> records = new ConcurrentHashMap<>();

    public PlayerCollection() {}

    public boolean hasDiscovered(String fishId) {
        return records.containsKey(fishId.toLowerCase());
    }

    public Record getRecord(String fishId) {
        return records.get(fishId.toLowerCase());
    }

    public Map<String, Record> getAllRecords() {
        return Collections.unmodifiableMap(records);
    }

    public int getDiscoveredCount() {
        return records.size();
    }

    public void putRecord(Record record) {
        records.put(record.fishId().toLowerCase(), record);
    }

    public boolean recordCatch(String fishId, double weight, double value, long timestamp) {
        String key = fishId.toLowerCase();
        Record current = records.get(key);
        boolean isFirstCatch = (current == null);

        if (isFirstCatch) {
            records.put(key, new Record(key, 1, weight, weight, value, timestamp));
        } else {
            double newLargest = Math.max(current.largestWeight(), weight);
            records.put(key, new Record(
                    key,
                    current.totalCatches() + 1,
                    newLargest,
                    current.totalWeight() + weight,
                    current.totalValue() + value,
                    current.firstCatchTimestamp()
            ));
        }
        return isFirstCatch;
    }
}
