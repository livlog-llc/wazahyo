package llc.livlog.wazahyo;

import java.util.Arrays;

/**
 * 和座標ライブラリをJARから簡易実行するためのCLIエントリポイントです。
 */
public final class App {
    /**
     * インスタンス化を禁止するためのprivateコンストラクタです。
     */
    private App() {
    }

    /**
     * コマンドライン引数を解析し、エンコード/デコードまたはデモ実行を行います。
     *
     * @param args コマンドライン引数
     */
    public static void main(String[] args) {
        try {
            if (args.length == 0) {
                demo();
                return;
            }

            switch (args[0]) {
                case "encode" -> encode(args);
                case "encode-mesh1" -> encodeByMesh1Code(args);
                case "encode-latlon" -> encodeLatLon(args);
                case "encode-meshcode" -> encodeMeshCode(args);
                case "decode" -> decode(args);
                case "decode-meshcode" -> decodeMeshCode(args);
                case "decode-latlon" -> decodeLatLon(args);
                case "mesh1-to-group" -> mesh1ToGroup(args);
                case "group-to-mesh1" -> groupToMesh1(args);
                case "help", "--help", "-h" -> usage();
                default -> usage();
            }
        } catch (IllegalArgumentException e) {
            System.err.println("入力エラー: " + e.getMessage());
            usage();
            System.exit(1);
        }
    }

    /**
     * 引数から {@code mesh1Group} と詳細メッシュ配列を受け取り、5文字コードを出力します。
     *
     * @param args コマンドライン引数
     */
    private static void encode(String[] args) {
        if (args.length != 3) {
            usage();
            return;
        }
        int mesh1Group = Integer.parseInt(args[1]);
        int[] parts = parseParts(args[2]);
        String code = WazahyoCodec.encode(mesh1Group, parts);
        System.out.println(code);
    }


    /**
     * 引数から1次メッシュ番号と詳細メッシュ配列を受け取り、5文字コードを出力します。
     *
     * @param args コマンドライン引数
     */
    private static void encodeByMesh1Code(String[] args) {
        if (args.length != 3) {
            usage();
            return;
        }
        int mesh1Code = Integer.parseInt(args[1]);
        int[] parts = parseParts(args[2]);
        String code = WazahyoCodec.encodeByMesh1Code(mesh1Code, parts);
        System.out.println(code);
    }


    /**
     * 引数から緯度経度を受け取り、5文字コードを出力します。
     *
     * @param args コマンドライン引数
     */
    private static void encodeLatLon(String[] args) {
        if (args.length != 3) {
            usage();
            return;
        }
        double latitude = Double.parseDouble(args[1]);
        double longitude = Double.parseDouble(args[2]);
        String code = WazahyoCodec.encodeFromLatLon(latitude, longitude);
        System.out.println(code);
    }

    /**
     * 引数から9次メッシュコードを受け取り、5文字コードを出力します。
     *
     * @param args コマンドライン引数
     */
    private static void encodeMeshCode(String[] args) {
        if (args.length != 2) {
            usage();
            return;
        }
        String code = WazahyoCodec.encodeFromMeshCode(args[1]);
        System.out.println(code);
    }

    /**
     * 引数から5文字コードを受け取り、復号結果を出力します。
     *
     * @param args コマンドライン引数
     */
    private static void decode(String[] args) {
        if (args.length != 2) {
            usage();
            return;
        }
        WazahyoCodec.Decoded decoded = WazahyoCodec.decode(args[1]);
        int mesh1Code = WazahyoCodec.decodedMesh1CodeOrMinusOne(decoded);
        System.out.println(decoded);
        if (mesh1Code >= 0) {
            System.out.println("mesh1Code=" + mesh1Code);
        } else {
            System.out.println("mesh1Code=unsupported-group:" + decoded.mesh1Group());
        }
    }



    /**
     * 引数から5文字コードを受け取り、9次メッシュコードを出力します。
     *
     * @param args コマンドライン引数
     */
    private static void decodeMeshCode(String[] args) {
        if (args.length != 2) {
            usage();
            return;
        }
        String meshCode = WazahyoCodec.decodeToMeshCode(args[1]);
        System.out.println(meshCode);
    }

    /**
     * 引数から5文字コードを受け取り、緯度経度（9次メッシュ中心）を出力します。
     *
     * @param args コマンドライン引数
     */
    private static void decodeLatLon(String[] args) {
        if (args.length != 2) {
            usage();
            return;
        }
        WazahyoCodec.LatLon latLon = WazahyoCodec.decodeToLatLon(args[1]);
        System.out.printf("%.8f,%.8f%n", latLon.latitude(), latLon.longitude());
    }

    /**
     * 1次メッシュ番号を内部区分へ変換して出力します。
     *
     * @param args コマンドライン引数
     */
    private static void mesh1ToGroup(String[] args) {
        if (args.length != 2) {
            usage();
            return;
        }
        int mesh1Code = Integer.parseInt(args[1]);
        int mesh1Group = WazahyoCodec.mesh1CodeToGroup(mesh1Code);
        System.out.println(mesh1Group);
    }

    /**
     * 内部区分を1次メッシュ番号へ逆変換して出力します。
     *
     * @param args コマンドライン引数
     */
    private static void groupToMesh1(String[] args) {
        if (args.length != 2) {
            usage();
            return;
        }
        int mesh1Group = Integer.parseInt(args[1]);
        int mesh1Code = WazahyoCodec.groupToMesh1Code(mesh1Group);
        System.out.println(mesh1Code);
    }

    /**
     * カンマ区切り文字列を詳細メッシュ配列へ変換します。
     *
     * @param raw カンマ区切りの10要素文字列
     * @return 詳細メッシュ配列
     * @throws IllegalArgumentException 要素数が10でない場合
     */
    private static int[] parseParts(String raw) {
        String[] split = raw.split(",");
        if (split.length != WazahyoCodec.BASES.length) {
            throw new IllegalArgumentException("parts must have 10 comma-separated values");
        }
        int[] parts = new int[split.length];
        for (int i = 0; i < split.length; i++) {
            parts[i] = Integer.parseInt(split[i].trim());
        }
        return parts;
    }

    /**
     * サンプル値を使ってエンコード/デコードの往復結果を表示します。
     */
    private static void demo() {
        int mesh1Group = 42;
        int[] parts = {1, 2, 3, 4, 1, 0, 2, 3, 1, 2};

        String code = WazahyoCodec.encode(mesh1Group, parts);
        WazahyoCodec.Decoded decoded = WazahyoCodec.decode(code);

        System.out.println("Demo encode/decode");
        System.out.println("mesh1Group=" + mesh1Group);
        System.out.println("parts=" + Arrays.toString(parts));
        System.out.println("code=" + code);
        System.out.println(decoded);
        System.out.println();
        usage();
    }

    /**
     * CLIの利用方法を標準出力へ表示します。
     */
    private static void usage() {
        System.out.println("Usage:");
        System.out.println("  java -jar wazahyo-1.0.0.jar encode <mesh1Group> <p0,p1,p2,p3,p4,p5,p6,p7,p8,p9>");
        System.out.println("  java -jar wazahyo-1.0.0.jar encode-mesh1 <mesh1Code> <p0,p1,p2,p3,p4,p5,p6,p7,p8,p9>");
        System.out.println("  java -jar wazahyo-1.0.0.jar encode-latlon <latitude> <longitude>");
        System.out.println("  java -jar wazahyo-1.0.0.jar encode-meshcode <14-digit-mesh9>");
        System.out.println("  java -jar wazahyo-1.0.0.jar decode <5-char-code>");
        System.out.println("  java -jar wazahyo-1.0.0.jar decode-meshcode <5-char-code>");
        System.out.println("  java -jar wazahyo-1.0.0.jar decode-latlon <5-char-code>");
        System.out.println("  java -jar wazahyo-1.0.0.jar mesh1-to-group <mesh1Code>");
        System.out.println("  java -jar wazahyo-1.0.0.jar group-to-mesh1 <mesh1Group>");
        System.out.println("  java -jar wazahyo-1.0.0.jar help");
        System.out.println("  java -jar wazahyo-1.0.0.jar   # demo");
        System.out.println();
        System.out.println("Example:");
        System.out.println("  java -jar wazahyo-1.0.0.jar encode 42 1,2,3,4,1,0,2,3,1,2");
        System.out.println("  java -jar wazahyo-1.0.0.jar decode せ春里か湖");
        System.out.println("  java -jar wazahyo-1.0.0.jar encode-latlon 35.681236 139.767125");
        System.out.println("  java -jar wazahyo-1.0.0.jar encode-meshcode 53394611323444");
        System.out.println("  java -jar wazahyo-1.0.0.jar decode-meshcode せ春里か湖");
        System.out.println("  java -jar wazahyo-1.0.0.jar decode-latlon せ春里か湖");
        System.out.println("  java -jar wazahyo-1.0.0.jar mesh1-to-group 5339");
        System.out.println("  java -jar wazahyo-1.0.0.jar group-to-mesh1 96");
    }
}
