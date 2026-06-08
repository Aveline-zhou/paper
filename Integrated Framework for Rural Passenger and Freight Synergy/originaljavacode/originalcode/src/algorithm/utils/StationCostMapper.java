package algorithm.utils;

import java.util.*;

/**
 * 鲁棒实验成本映射工具（物理点 + 风险等级版）
 */
public class StationCostMapper {
    private static final Map<String, Double> STATION_COST_MAP = new HashMap<>();

    // 1. 48个物理站点的名义成本池
    private static final double[] PHYSICAL_NOMINAL_COSTS = {
            5.01738653, 5.018233324, 5.047328258, 5.07129067, 5.072883573, 5.085742503,
            5.137198677, 5.156690632, 5.174729998, 5.180325682, 5.194610362, 5.20855524,
            5.216721452, 5.232419036, 5.254221025, 5.297142379, 5.300588514, 5.318901936,
            5.346307566, 5.390937875, 5.419154658, 5.458622231, 5.468527101, 5.509306509,
            5.526128226, 5.53737365, 5.565153943, 5.592697166, 5.611157068, 5.632621303,
            5.644492942, 5.666788803, 5.686917643, 5.692747902, 5.732725504, 5.772307949,
            5.77751765, 5.791329702, 5.820139695, 5.826295204, 5.8469901, 5.858222326,
            5.878022005, 5.881312293, 5.888991942, 5.920024456, 5.936226041, 5.960476108
    };

    // 2. 物理站点风险分布 (总和需为 48)
    private static final int PHYS_LOW_COUNT = 24;  // 示例：12个低风险
    private static final int PHYS_MID_COUNT = 12;  // 示例：24个中风险
    private static final int PHYS_HIGH_COUNT = 12; // 示例：12个高风险

    // 3. 风险成本配置
    private static final double LOW_COST = 5.5;
    private static final double MID_COST = 7.5;
    private static final double HIGH_COST = 10.0;

    // 4. 鲁棒参数 Γ (0 <= R <= 48)
    private static final int R = 0;

    private static final List<String> ALL_77_IDS = Arrays.asList(
            "235","236","237","238","239","240","241","242","243","244",
            "245","246","247","248","249","250","251","252","253","254",
            "255","256","257","258","259","260","261","262","263","264",
            "265","266","267","268","269","270","271","272","273","274",
            "275","276","277","278","279","280","281","282","283","284",
            "285","286","287","288","289","290","291","292","293","294",
            "295","296","297","298","299","300","301","302","303","304",
            "305","306","307","308","309","310","311"
    );

    static {
        if (PHYS_LOW_COUNT + PHYS_MID_COUNT + PHYS_HIGH_COUNT != 48) {
            throw new IllegalArgumentException("物理站分布总和必须等于48！");
        }

        Random random = new Random();

        // --- 第一步：定义物理站点的风险属性 ---
        List<Integer> lowPhysGroup = new ArrayList<>();
        List<Integer> midPhysGroup = new ArrayList<>();
        List<Integer> highPhysGroup = new ArrayList<>();

        for (int i = 0; i < 48; i++) {
            if (i < PHYS_LOW_COUNT) lowPhysGroup.add(i);
            else if (i < PHYS_LOW_COUNT + PHYS_MID_COUNT) midPhysGroup.add(i);
            else highPhysGroup.add(i);
        }

        // --- 第二步：从48个物理点中随机选R个作为扰动点 (按比例或纯随机) ---
        List<Integer> allPhysIndices = new ArrayList<>();
        for (int i = 0; i < 48; i++) allPhysIndices.add(i);
        Collections.shuffle(allPhysIndices, random);
        Set<Integer> selectedPhysPoints = new HashSet<>(allPhysIndices.subList(0, R));

        // --- 第三步：映射 77 ID -> 48 物理点 并分配成本 ---
        for (int i = 0; i < ALL_77_IDS.size(); i++) {
            String id = ALL_77_IDS.get(i);
            int physIndex = i % 48; // 映射逻辑

            double finalCost;
            if (selectedPhysPoints.contains(physIndex)) {
                // 如果该物理点被选中，根据其原始风险等级分配成本
                if (physIndex < PHYS_LOW_COUNT) finalCost = LOW_COST;
                else if (physIndex < PHYS_LOW_COUNT + PHYS_MID_COUNT) finalCost = MID_COST;
                else finalCost = HIGH_COST;
            } else {
                // 未被选中，使用该物理点对应的名义成本
                finalCost = PHYSICAL_NOMINAL_COSTS[physIndex];
            }
            STATION_COST_MAP.put(id, finalCost);
        }

        System.out.println("===== 鲁棒 StationCostMapper 初始化 =====");
        System.out.println("Γ (扰动点数量) = " + R);
        System.out.println("物理分布: 低=" + PHYS_LOW_COUNT + " 中=" + PHYS_MID_COUNT + " 高=" + PHYS_HIGH_COUNT);
    }

    public static double getStationCost(String stationId) {
        return STATION_COST_MAP.getOrDefault(stationId, 5.0);
    }
}