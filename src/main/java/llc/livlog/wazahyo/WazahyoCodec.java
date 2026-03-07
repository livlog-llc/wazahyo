package llc.livlog.wazahyo;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 日本位置コード（和座標）仕様に基づくエンコード/デコードを提供するユーティリティクラスです。
 * <p>
 * 本クラスは以下の処理をサポートします。
 * </p>
 * <ul>
 *   <li>ひらがな+漢字（計88文字）を用いた5文字固定長コードへの変換</li>
 *   <li>混合基数 {@code [8,8,10,10,4,4,4,4,4,4]} による詳細メッシュの符号化/復号</li>
 *   <li>{@code mesh1Group} と {@code detail} を組み合わせた最終コードの符号化/復号</li>
 * </ul>
 */
public final class WazahyoCodec {
    /** 仕様で定義されたひらがな43文字。 */
    public static final String HIRAGANA = "あいうえおかきくけこさしすせそたちつてとなにぬねのはひふへほまみむめもやゆよらりるれろわ";
    /** 仕様で定義された漢字45文字。 */
    public static final String KANJI = "春夏秋冬月雪風雨霧霞星空陽朝夕夜山川海島岬峰谷沢滝湖沼岸浜浦森林野原丘里村町都花草木竹石岩";
    /** エンコード/デコードに使用する文字集合（計88文字）。 */
    public static final String CHARS = HIRAGANA + KANJI;

    /** 詳細メッシュを構成する混合基数。 */
    public static final int[] BASES = {8, 8, 10, 10, 4, 4, 4, 4, 4, 4};
    /** 文字集合の基数（88）。 */
    public static final int BASE = CHARS.length();
    /** 固定コード長（5文字）。 */
    public static final int CODE_LENGTH = 5;
    /** 9次メッシュコードの桁数。 */
    public static final int MESH9_CODE_LENGTH = 14;

    /** 1次メッシュ区分の総数。 */
    public static final int MESH1_GROUP_COUNT = 200;
    /** 詳細位置番号の上限（排他的）。 */
    public static final int DETAIL_MAX_EXCLUSIVE = 26_214_400;
    /** 仕様上で利用する全コード空間（排他的上限）。 */
    public static final long CAPACITY = 5_242_880_000L;

    /** 1次メッシュ番号一覧(JSON)のリソースパス。 */
    private static final String MESH1_CODES_RESOURCE = "/mesh1_groups.json";
    /** サポート対象の1次メッシュ番号（全175件）。 */
    public static final List<Integer> SUPPORTED_MESH1_CODES = loadSupportedMesh1Codes();
    /** 1次メッシュ番号から内部区分への逆引きマップ。 */
    private static final Map<Integer, Integer> MESH1_TO_GROUP = buildMesh1ToGroup();

    /**
     * インスタンス化を禁止するためのprivateコンストラクタです。
     */
    private WazahyoCodec() {
    }

    /**
     * 非負整数を和座標の文字集合によるN進数文字列へ変換します。
     *
     * @param n 変換対象の非負整数
     * @param length 出力文字列長
     * @return 変換後の固定長文字列
     * @throws IllegalArgumentException {@code n} が負、または指定桁数に収まらない場合
     */
    public static String toBase88(long n, int length) {
        if (n < 0) {
            throw new IllegalArgumentException("n must be non-negative");
        }
        char[] out = new char[length];
        for (int i = length - 1; i >= 0; i--) {
            out[i] = CHARS.charAt((int) (n % BASE));
            n /= BASE;
        }
        if (n != 0) {
            throw new IllegalArgumentException("n is too large for length=" + length);
        }
        return new String(out);
    }

    /**
     * 和座標文字列を整数値へ復号します。
     *
     * @param code 変換対象のコード
     * @return 復号された整数値
     * @throws IllegalArgumentException 空文字、または文字集合外の文字を含む場合
     */
    public static long fromBase88(String code) {
        if (code == null || code.isEmpty()) {
            throw new IllegalArgumentException("code must not be empty");
        }
        long n = 0;
        for (int i = 0; i < code.length(); i++) {
            char c = code.charAt(i);
            int idx = CHARS.indexOf(c);
            if (idx < 0) {
                throw new IllegalArgumentException("invalid character at " + i + ": " + c);
            }
            n = n * BASE + idx;
        }
        return n;
    }

    /**
     * 詳細メッシュの各桁を混合基数で1つの通し番号に変換します。
     *
     * @param parts 各桁の値（長さ10）
     * @return 詳細位置番号
     * @throws IllegalArgumentException 長さ不正、または各要素が基数範囲外の場合
     */
    public static int encodeDetail(int[] parts) {
        if (parts == null || parts.length != BASES.length) {
            throw new IllegalArgumentException("parts length must be " + BASES.length);
        }
        int n = 0;
        for (int i = 0; i < BASES.length; i++) {
            int v = parts[i];
            int b = BASES[i];
            if (v < 0 || v >= b) {
                throw new IllegalArgumentException("parts[" + i + "] must be in [0," + (b - 1) + "]");
            }
            n = n * b + v;
        }
        return n;
    }

    /**
     * 詳細位置番号を混合基数の各桁に展開します。
     *
     * @param n 詳細位置番号
     * @return 展開後の配列（長さ10）
     * @throws IllegalArgumentException {@code n} が仕様範囲外の場合
     */
    public static int[] decodeDetail(int n) {
        if (n < 0 || n >= DETAIL_MAX_EXCLUSIVE) {
            throw new IllegalArgumentException("detail must be in [0," + (DETAIL_MAX_EXCLUSIVE - 1) + "]");
        }
        int[] values = new int[BASES.length];
        for (int i = BASES.length - 1; i >= 0; i--) {
            int b = BASES[i];
            values[i] = n % b;
            n /= b;
        }
        return values;
    }


    /**
     * 1次メッシュ番号がサポート対象かを判定します。
     *
     * @param mesh1Code 1次メッシュ番号（例: 5339）
     * @return サポート対象なら {@code true}
     */
    public static boolean isSupportedMesh1Code(int mesh1Code) {
        return MESH1_TO_GROUP.containsKey(mesh1Code);
    }

    /**
     * 1次メッシュ番号を内部の1次メッシュ区分へ変換します。
     *
     * @param mesh1Code 1次メッシュ番号（例: 5339）
     * @return 対応する区分インデックス（0始まり）
     * @throws IllegalArgumentException サポート対象外の場合
     */
    public static int mesh1CodeToGroup(int mesh1Code) {
        Integer group = MESH1_TO_GROUP.get(mesh1Code);
        if (group == null) {
            throw new IllegalArgumentException("unsupported mesh1Code: " + mesh1Code);
        }
        return group;
    }

    /**
     * 1次メッシュ区分を元の1次メッシュ番号へ逆変換します。
     *
     * @param mesh1Group 区分インデックス（0始まり）
     * @return 1次メッシュ番号
     * @throws IllegalArgumentException 区分が範囲外の場合
     */
    public static int groupToMesh1Code(int mesh1Group) {
        if (mesh1Group < 0 || mesh1Group >= SUPPORTED_MESH1_CODES.size()) {
            throw new IllegalArgumentException("mesh1Group must be in [0," + (SUPPORTED_MESH1_CODES.size() - 1) + "] for supported list");
        }
        return SUPPORTED_MESH1_CODES.get(mesh1Group);
    }

    /**
     * 1次メッシュ番号と詳細メッシュ配列から5文字コードを生成します。
     *
     * @param mesh1Code 1次メッシュ番号（JSON定義の175件のみ）
     * @param parts 詳細メッシュの各桁（長さ10）
     * @return 5文字コード
     */
    public static String encodeByMesh1Code(int mesh1Code, int[] parts) {
        int mesh1Group = mesh1CodeToGroup(mesh1Code);
        return encode(mesh1Group, parts);
    }

    /**
     * 復号結果の1次メッシュ区分を1次メッシュ番号へ変換します。
     *
     * @param decoded 復号結果
     * @return 1次メッシュ番号（サポート外区分の場合は {@code -1}）
     */
    public static int decodedMesh1CodeOrMinusOne(Decoded decoded) {
        int group = decoded.mesh1Group();
        if (group < 0 || group >= SUPPORTED_MESH1_CODES.size()) {
            return -1;
        }
        return SUPPORTED_MESH1_CODES.get(group);
    }

    /**
     * 1次メッシュ区分と詳細メッシュ配列から5文字コードを生成します。
     *
     * @param mesh1Group 1次メッシュ区分（0〜199）
     * @param parts 詳細メッシュの各桁（長さ10）
     * @return 5文字コード
     * @throws IllegalArgumentException 引数が仕様範囲外の場合
     */
    public static String encode(int mesh1Group, int[] parts) {
        if (mesh1Group < 0 || mesh1Group >= MESH1_GROUP_COUNT) {
            throw new IllegalArgumentException("mesh1Group must be in [0," + (MESH1_GROUP_COUNT - 1) + "]");
        }
        int detail = encodeDetail(parts);
        return encodeFromDetail(mesh1Group, detail);
    }

    /**
     * 1次メッシュ区分と詳細位置番号から5文字コードを生成します。
     *
     * @param mesh1Group 1次メッシュ区分（0〜199）
     * @param detail 詳細位置番号（0〜26,214,399）
     * @return 5文字コード
     * @throws IllegalArgumentException 引数が仕様範囲外の場合
     */
    public static String encodeFromDetail(int mesh1Group, int detail) {
        if (mesh1Group < 0 || mesh1Group >= MESH1_GROUP_COUNT) {
            throw new IllegalArgumentException("mesh1Group must be in [0," + (MESH1_GROUP_COUNT - 1) + "]");
        }
        if (detail < 0 || detail >= DETAIL_MAX_EXCLUSIVE) {
            throw new IllegalArgumentException("detail must be in [0," + (DETAIL_MAX_EXCLUSIVE - 1) + "]");
        }
        long n = (long) detail * MESH1_GROUP_COUNT + mesh1Group;
        return toBase88(n, CODE_LENGTH);
    }

    /**
     * 5文字コードを復号して構造化データとして返します。
     *
     * @param code 5文字コード
     * @return 復号結果
     * @throws IllegalArgumentException コード長不正、文字不正、または容量範囲外の場合
     */
    public static Decoded decode(String code) {
        if (code == null || code.length() != CODE_LENGTH) {
            throw new IllegalArgumentException("code length must be " + CODE_LENGTH);
        }
        long n = fromBase88(code);
        if (n < 0 || n >= CAPACITY) {
            throw new IllegalArgumentException("code is outside defined capacity");
        }

        int mesh1Group = (int) (n % MESH1_GROUP_COUNT);
        int detail = (int) (n / MESH1_GROUP_COUNT);
        int[] parts = decodeDetail(detail);

        return new Decoded(code, n, mesh1Group, detail, parts);
    }

    /**
     * 緯度経度から9次メッシュを算出し、そのまま5文字コードへ変換します。
     *
     * @param latitude 緯度（度）
     * @param longitude 経度（度）
     * @return 5文字コード
     */
    public static String encodeFromLatLon(double latitude, double longitude) {
        Mesh9 mesh9 = latLonToMesh9(latitude, longitude);
        return encodeByMesh1Code(mesh9.mesh1Code(), mesh9.parts());
    }

    /**
     * 緯度経度から9次メッシュコード（14桁）を算出します。
     *
     * @param latitude 緯度（度）
     * @param longitude 経度（度）
     * @return 9次メッシュコード（14桁）
     */
    public static String latLonToMeshCode(double latitude, double longitude) {
        return latLonToMesh9(latitude, longitude).meshCode();
    }

    /**
     * 9次メッシュコード（14桁）を5文字コードへ変換します。
     *
     * @param meshCode 9次メッシュコード（14桁）
     * @return 5文字コード
     */
    public static String encodeFromMeshCode(String meshCode) {
        Mesh9 parsed = parseMesh9Code(meshCode);
        return encodeByMesh1Code(parsed.mesh1Code(), parsed.parts());
    }

    /**
     * 5文字コードから9次メッシュコード（14桁）へ変換します。
     *
     * @param code 5文字コード
     * @return 9次メッシュコード（14桁）
     */
    public static String decodeToMeshCode(String code) {
        Decoded decoded = decode(code);
        int mesh1Code = decodedMesh1CodeOrMinusOne(decoded);
        if (mesh1Code < 0) {
            throw new IllegalArgumentException("decoded mesh1Group is outside supported mesh1 list: " + decoded.mesh1Group());
        }
        return buildMesh9Code(mesh1Code, decoded.parts());
    }

    /**
     * 5文字コードを復号し、9次メッシュ中心の緯度経度を返します。
     *
     * @param code 5文字コード
     * @return 緯度経度
     */
    public static LatLon decodeToLatLon(String code) {
        return meshCodeToLatLon(decodeToMeshCode(code));
    }

    /**
     * 9次メッシュコード（14桁）の中心緯度経度を返します。
     *
     * @param meshCode 9次メッシュコード（14桁）
     * @return 緯度経度
     */
    public static LatLon meshCodeToLatLon(String meshCode) {
        Mesh9 parsed = parseMesh9Code(meshCode);

        int mesh1Code = parsed.mesh1Code();
        int p0 = mesh1Code / 100;
        int p1 = mesh1Code % 100;
        int[] parts = parsed.parts();

        double lat = p0 / 1.5;
        double lon = 100.0 + p1;

        lat += parts[0] * (5.0 / 60.0);
        lon += parts[1] * (7.5 / 60.0);
        lat += parts[2] * (30.0 / 3600.0);
        lon += parts[3] * (45.0 / 3600.0);

        double latCell = 30.0 / 3600.0;
        double lonCell = 45.0 / 3600.0;
        for (int i = 4; i < BASES.length; i++) {
            latCell /= 2.0;
            lonCell /= 2.0;
            int d = parts[i];
            lat += (d / 2) * latCell;
            lon += (d % 2) * lonCell;
        }

        return new LatLon(lat + latCell / 2.0, lon + lonCell / 2.0);
    }

    private static Mesh9 latLonToMesh9(double latitude, double longitude) {
        if (!Double.isFinite(latitude) || !Double.isFinite(longitude)) {
            throw new IllegalArgumentException("latitude/longitude must be finite");
        }

        double lat15 = latitude * 1.5;
        int p = (int) Math.floor(lat15);

        double lonShift = longitude - 100.0;
        int q = (int) Math.floor(lonShift);

        if (p < 0 || p > 99 || q < 0 || q > 99) {
            throw new IllegalArgumentException("latitude/longitude is outside 1st mesh addressable range");
        }

        int mesh1Code = p * 100 + q;
        if (!isSupportedMesh1Code(mesh1Code)) {
            throw new IllegalArgumentException("mesh1Code is outside supported Japan land meshes: " + mesh1Code);
        }

        int[] parts = new int[BASES.length];
        double remLat = lat15 - p;
        double remLon = lonShift - q;

        remLat *= 8.0;
        remLon *= 8.0;
        parts[0] = (int) Math.floor(remLat);
        parts[1] = (int) Math.floor(remLon);
        remLat -= parts[0];
        remLon -= parts[1];

        remLat *= 10.0;
        remLon *= 10.0;
        parts[2] = (int) Math.floor(remLat);
        parts[3] = (int) Math.floor(remLon);
        remLat -= parts[2];
        remLon -= parts[3];

        for (int i = 4; i < BASES.length; i++) {
            remLat *= 2.0;
            remLon *= 2.0;
            int latBit = (int) Math.floor(remLat);
            int lonBit = (int) Math.floor(remLon);
            parts[i] = latBit * 2 + lonBit;
            remLat -= latBit;
            remLon -= lonBit;
        }

        return new Mesh9(mesh1Code, parts, buildMesh9Code(mesh1Code, parts));
    }

    private static Mesh9 parseMesh9Code(String meshCode) {
        if (meshCode == null || meshCode.length() != MESH9_CODE_LENGTH || !meshCode.chars().allMatch(Character::isDigit)) {
            throw new IllegalArgumentException("meshCode must be 14-digit 9th mesh code");
        }

        int mesh1Code = Integer.parseInt(meshCode.substring(0, 4));
        if (!isSupportedMesh1Code(mesh1Code)) {
            throw new IllegalArgumentException("unsupported mesh1Code: " + mesh1Code);
        }

        int[] parts = new int[BASES.length];
        parts[0] = Character.digit(meshCode.charAt(4), 10);
        parts[1] = Character.digit(meshCode.charAt(5), 10);
        parts[2] = Character.digit(meshCode.charAt(6), 10);
        parts[3] = Character.digit(meshCode.charAt(7), 10);
        for (int i = 4; i < BASES.length; i++) {
            int d = Character.digit(meshCode.charAt(i + 4), 10);
            if (d < 1 || d > 4) {
                throw new IllegalArgumentException("meshCode 4th-9th mesh digits must be in [1,4]");
            }
            parts[i] = d - 1;
        }
        encodeDetail(parts);
        return new Mesh9(mesh1Code, parts, meshCode);
    }

    private static String buildMesh9Code(int mesh1Code, int[] parts) {
        encodeDetail(parts);
        StringBuilder sb = new StringBuilder(MESH9_CODE_LENGTH);
        sb.append(String.format("%04d", mesh1Code));
        sb.append(parts[0]).append(parts[1]).append(parts[2]).append(parts[3]);
        for (int i = 4; i < parts.length; i++) {
            sb.append(parts[i] + 1);
        }
        return sb.toString();
    }

    /**
     * 緯度経度値。
     */
    public record LatLon(double latitude, double longitude) {
    }

    private record Mesh9(int mesh1Code, int[] parts, String meshCode) {
    }

    /**
     * 復号結果をまとめた不変データです。
     *
     * @param code 入力コード
     * @param n コードを88進として見た整数値
     * @param mesh1Group 1次メッシュ区分
     * @param detail 詳細位置番号
     * @param parts 詳細メッシュ各桁
     */
    public record Decoded(String code, long n, int mesh1Group, int detail, int[] parts) {
        /**
         * 復号結果を表示用文字列へ整形します。
         *
         * @return 表示用文字列
         */
        @Override
        public String toString() {
            return "Decoded{" +
                    "code='" + code + '\'' +
                    ", n=" + n +
                    ", mesh1Group=" + mesh1Group +
                    ", detail=" + detail +
                    ", parts=" + Arrays.toString(parts) +
                    '}';
        }
    }
    /**
     * JSONリソースからサポート対象の1次メッシュ番号一覧を読み込みます。
     */
    private static List<Integer> loadSupportedMesh1Codes() {
        try (InputStream in = WazahyoCodec.class.getResourceAsStream(MESH1_CODES_RESOURCE)) {
            if (in == null) {
                throw new IllegalStateException("Resource not found: " + MESH1_CODES_RESOURCE);
            }
            String json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            int arrayStart = json.indexOf('[');
            int arrayEnd = json.lastIndexOf(']');
            if (arrayStart < 0 || arrayEnd <= arrayStart) {
                throw new IllegalStateException("mesh1_groups.json does not contain a valid array");
            }
            String arrayBody = json.substring(arrayStart + 1, arrayEnd);
            Matcher matcher = Pattern.compile("\\d+").matcher(arrayBody);
            java.util.ArrayList<Integer> values = new java.util.ArrayList<>();
            while (matcher.find()) {
                values.add(Integer.parseInt(matcher.group()));
            }
            if (values.size() != 175) {
                throw new IllegalStateException("mesh1 code count must be 175, but was " + values.size());
            }
            return Collections.unmodifiableList(values);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load " + MESH1_CODES_RESOURCE, e);
        }
    }

    /**
     * 1次メッシュ番号から区分への逆引きマップを構築します。
     */
    private static Map<Integer, Integer> buildMesh1ToGroup() {
        Map<Integer, Integer> map = new HashMap<>();
        for (int i = 0; i < SUPPORTED_MESH1_CODES.size(); i++) {
            int code = SUPPORTED_MESH1_CODES.get(i);
            Integer old = map.put(code, i);
            if (old != null) {
                throw new IllegalStateException("Duplicate mesh1 code in list: " + code);
            }
        }
        return Collections.unmodifiableMap(map);
    }

}
