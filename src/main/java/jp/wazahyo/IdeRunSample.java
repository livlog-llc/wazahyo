package jp.wazahyo;

/**
 * IDEからソース実行するためのサンプルエントリポイントです。
 * <p>
 * 緯度経度→9次メッシュコード→和座標コードへの変換と、各ルートからのデコード結果を確認できます。
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

        // 1) 緯度経度 -> meshCode
        String meshCodeFromLatLon = WazahyoCodec.latLonToMeshCode(latitude, longitude);

        // 2) 緯度経度 -> WazahyoCode（正規ルート）
        String wazahyoFromLatLon = WazahyoCodec.encodeFromLatLon(latitude, longitude);

        // 3) meshCode -> WazahyoCode
        String wazahyoFromMeshCode = WazahyoCodec.encodeFromMeshCode(meshCodeFromLatLon);

        // 4) WazahyoCode -> meshCode / 緯度経度（中心）
        String decodedMeshCode = WazahyoCodec.decodeToMeshCode(wazahyoFromLatLon);
        WazahyoCodec.LatLon decodedCenter = WazahyoCodec.decodeToLatLon(wazahyoFromLatLon);

        // 5) meshCode -> 緯度経度（中心）
        WazahyoCodec.LatLon meshCenter = WazahyoCodec.meshCodeToLatLon(meshCodeFromLatLon);

        System.out.println("=== IDE実行サンプル（変換フロー確認） ===");
        System.out.printf("input lat/lon                : %.6f, %.6f%n", latitude, longitude);
        System.out.println("lat/lon -> meshCode          : " + meshCodeFromLatLon);
        System.out.println("lat/lon -> WazahyoCode       : " + wazahyoFromLatLon);
        System.out.println("meshCode -> WazahyoCode      : " + wazahyoFromMeshCode);
        System.out.println("WazahyoCode -> meshCode      : " + decodedMeshCode);
        System.out.printf("WazahyoCode -> center lat/lon: %.8f, %.8f%n", decodedCenter.latitude(), decodedCenter.longitude());
        System.out.printf("meshCode -> center lat/lon   : %.8f, %.8f%n", meshCenter.latitude(), meshCenter.longitude());

        System.out.println();
        System.out.println("一致確認");
        System.out.println("meshCode round-trip          : " + meshCodeFromLatLon.equals(decodedMeshCode));
        System.out.println("wazahyo consistency          : " + wazahyoFromLatLon.equals(wazahyoFromMeshCode));
    }
}
