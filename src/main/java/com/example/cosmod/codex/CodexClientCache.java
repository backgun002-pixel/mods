package com.example.cosmod.codex;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/** 클라이언트 측 등록 캐시 */
public class CodexClientCache {
    private static Set<String> farmer = new HashSet<>();
    private static Set<String> miner  = new HashSet<>();

    public static void update(Set<String> f, Set<String> m) {
        farmer = new HashSet<>(f);
        miner  = new HashSet<>(m);
    }

    public static boolean isFarmerRegistered(String id) { return farmer.contains(id); }
    public static boolean isMinerRegistered(String id)  { return miner.contains(id);  }

    public static int farmerCount() { return farmer.size(); }
    public static int minerCount()  { return miner.size();  }
}
