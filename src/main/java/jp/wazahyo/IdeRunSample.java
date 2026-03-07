package jp.wazahyo;

/**
 * IDEからソース実行するためのサンプルエントリポイントです。
 * <p>
 * mainメソッドをそのまま実行すると、緯度経度→和座標コード→復号結果を確認できます。
 */
public final class IdeRunSample {
    private IdeRunSample() {
    }

    /**
     * IDE実行向けのサンプル処理です。
     *
     * @param args 未使用
     */
    public static void main(String[] args) {
        // 東京駅付近（サンプル）
        double latitude = 35.681236;
        double longitude = 139.767125;

        String code = WazahyoCodec.encodeFromLatLon(latitude, longitude);
        String meshCode = WazahyoCodec.decodeToMeshCode(code);
        WazahyoCodec.LatLon center = WazahyoCodec.decodeToLatLon(code);

        System.out.println("=== IDE実行サンプル ===");
        System.out.printf("input lat/lon : %.6f, %.6f%n", latitude, longitude);
        System.out.println("wazahyo code  : " + code);
        System.out.println("mesh9 code    : " + meshCode);
        System.out.printf("decoded center: %.8f, %.8f%n", center.latitude(), center.longitude());
    }
}
