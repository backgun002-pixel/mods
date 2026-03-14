package com.example.cosmod.codex;

import com.example.cosmod.crop.CosmodCrops;
import com.example.cosmod.item.CosmodItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.List;

/** 도감 항목 정의 - 이미지 레이아웃 순서 */
public class CodexRegistry {

    public record Entry(String id, String name, Item item) {}

    // ── 농부 도감 ─────────────────────────────────────────────────
    // 이미지에 있는 것 중 바닐라 + 유저가 직접 추가한 커스텀(토마토, 고추)만 포함
    // 양파/대파/배추/무/쌀/오이/메론/레몬/포도/해독버섯/라플레시아/동충하조/파리지옥/만드라고라 = 미구현 → 제외
    public static final List<Entry> FARMER = List.of(
        // 행 1
        e("carrot",                 "당근",             Items.CARROT),
        e("potato",                 "감자",             Items.POTATO),
        e("poisonous_potato",       "독감자",            Items.POISONOUS_POTATO),
        e("beetroot",               "비트",             Items.BEETROOT),
        e("wheat",                  "밀",              Items.WHEAT),
        e("bread",                  "빵",              Items.BREAD),
        // 행 2
        e("pumpkin_pie",            "호박파이",          Items.PUMPKIN_PIE),
        e("melon_slice",            "수박 조각",         Items.MELON_SLICE),
        e("golden_carrot",          "황금당근",          Items.GOLDEN_CARROT),
        e("sugar_cane",             "사탕수수",          Items.SUGAR_CANE),
        e("apple",                  "사과",             Items.APPLE),
        e("golden_apple",           "황금사과",          Items.GOLDEN_APPLE),
        // 행 3
        e("enchanted_golden_apple", "마법부여 황금사과",  Items.ENCHANTED_GOLDEN_APPLE),
        e("glistering_melon_slice", "반짝이는 수박 조각", Items.GLISTERING_MELON_SLICE),
        e("nether_wart",            "네더 사마귀",       Items.NETHER_WART),
        e("cake",                   "케이크",            Items.CAKE),
        e("glow_berries",           "발광열매",          Items.GLOW_BERRIES),
        e("sweet_berries",          "달콤한 열매",       Items.SWEET_BERRIES),
        // 행 4 - 바닐라
        e("chorus_fruit",           "후렴과",            Items.CHORUS_FRUIT),
        // 행 4 - 커스텀 (유저 추가)
        e("pepper",                 "고추",             null),   // CosmodCrops.PEPPER
        e("tomato",                 "토마토",            null),   // CosmodCrops.TOMATO
        e("corn",                   "옥수수",            null)    // CosmodCrops.CORN
    );

    // ── 광부 도감 ─────────────────────────────────────────────────
    // 이미지에 있는 것 중 바닐라 + 유저가 직접 추가한 커스텀(루비/사파이어/레드다이아)만 포함
    // 강철/한철/돌소금/백금/마석 = 미구현 → 제외
    public static final List<Entry> MINER = List.of(
        // 행 1
        e("flint",               "부싯돌",          Items.FLINT),
        e("coal",                "석탄",            Items.COAL),
        e("charcoal",            "숯",             Items.CHARCOAL),
        e("raw_copper",          "구리 원석",        Items.RAW_COPPER),
        e("raw_iron",            "철원석",           Items.RAW_IRON),
        e("raw_gold",            "금원석",           Items.RAW_GOLD),
        // 행 2
        e("copper_ingot",        "구리주괴",         Items.COPPER_INGOT),
        e("iron_ingot",          "철주괴",           Items.IRON_INGOT),
        e("gold_ingot",          "금주괴",           Items.GOLD_INGOT),
        e("lapis_lazuli",        "청금석",           Items.LAPIS_LAZULI),
        e("redstone",            "레드스톤 가루",     Items.REDSTONE),
        e("amethyst_shard",      "자수정 조각",      Items.AMETHYST_SHARD),
        // 행 3
        e("diamond",             "다이아몬드",        Items.DIAMOND),
        e("emerald",             "에메랄드",         Items.EMERALD),
        e("quartz",              "네더석영",          Items.QUARTZ),
        e("netherite_scrap",     "네더라이트 파편",   Items.NETHERITE_SCRAP),
        e("netherite_ingot",     "네더라이트 주괴",   Items.NETHERITE_INGOT),
        e("echo_shard",          "메아리 조각",      Items.ECHO_SHARD),
        // 행 4
        e("prismarine_shard",    "프리즈머린 조각",   Items.PRISMARINE_SHARD),
        e("prismarine_crystals", "프리즈머린 수정",   Items.PRISMARINE_CRYSTALS),
        // 행 4-5 - 커스텀 (유저 추가)
        e("ruby",                "루비",            null),   // CosmodItems.RUBY
        e("sapphire",            "사파이어",         null),   // CosmodItems.SAPPHIRE
        e("red_diamond",         "레드 다이아몬드",   null)    // CosmodItems.RED_DIAMOND
    );

    /** 커스텀 아이템 초기화 - CosmodMod.register() 이후 호출 */
    public static void initCustomItems() {
        setItem(FARMER, "pepper",     CosmodCrops.PEPPER);
        setItem(FARMER, "tomato",     CosmodCrops.TOMATO);
        setItem(FARMER, "corn",       CosmodCrops.CORN);
        setItem(MINER,  "ruby",       CosmodItems.RUBY);
        setItem(MINER,  "sapphire",   CosmodItems.SAPPHIRE);
        setItem(MINER,  "red_diamond",CosmodItems.RED_DIAMOND);
    }

    /** Entry는 record라 item을 직접 교체 못하므로 리스트를 가변으로 관리 */
    private static final java.util.Map<String, Item> CUSTOM = new java.util.HashMap<>();

    public static Item getItem(Entry entry) {
        if (entry.item() != null) return entry.item();
        return CUSTOM.get(entry.id());
    }

    private static void setItem(List<Entry> list, String id, Item item) {
        CUSTOM.put(id, item);
    }

    private static Entry e(String id, String name, Item item) {
        return new Entry(id, name, item);
    }
}
