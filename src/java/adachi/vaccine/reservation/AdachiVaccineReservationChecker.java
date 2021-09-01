package adachi.vaccine.reservation;

import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.stream.IntStream;

public class AdachiVaccineReservationChecker {

    private static final Logger logger = Logger.getLogger("AdachiVaccineReservationChecker");

    private static final HttpClient client = HttpClient.newBuilder().build();

    private static final String BASE_DIR = "output";

    private static final String HTML_DIR = BASE_DIR + "\\html";

    private static final String LOG_DIR = BASE_DIR + "\\log";

    static {
        // Loggerの設定
        logger.setLevel(Level.INFO);
        try {
            FileHandler handler = new FileHandler(LOG_DIR + "\\adachi-vaccine-reservation-checker.log");
            handler.setFormatter(new SimpleFormatter());
            logger.addHandler(handler);
        } catch (IOException e) {
            logger.log(Level.WARNING, "ロガーの設定でエラー", e);
        }
    }

    public static void main(String[] args) {

        logger.log(Level.INFO, "出力ディレクトリの作成 開始");
        try {
            if (Files.notExists(Path.of(BASE_DIR))) Files.createDirectory(Path.of(BASE_DIR));
            if (Files.notExists(Path.of(HTML_DIR))) Files.createDirectory(Path.of(HTML_DIR));
            if (Files.notExists(Path.of(LOG_DIR))) Files.createDirectory(Path.of(LOG_DIR));
        } catch (IOException e) {
            logger.log(Level.SEVERE, "出力ディレクトリの作成に失敗", e);
            return;
        }
        logger.log(Level.INFO, "出力ディレクトリの作成 終了");

        logger.log(Level.INFO, "足立区コロナワクチン予約空き状況確認 開始");
        LocalDate today = LocalDate.now();
        int year = today.getYear();
        int month = today.getMonthValue();
        int[] months = { month, month + 1 };
        IntStream.of(months).forEach(m -> check(year, m));
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        logger.log(Level.INFO, "足立区コロナワクチン予約空き状況確認 終了");
    }

    private static void check(int year, int month) {
        HttpRequest req = HttpRequest.newBuilder(URI.create("https://adachi.hbf-rsv.jp/mypage/status/?year=%s&month=%s".formatted(year, month))).build();
        client.sendAsync(req, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenAccept(body -> {
                    Path outputDir = Path.of(HTML_DIR + "\\%s月".formatted(month));
                    if (Files.notExists(outputDir, LinkOption.NOFOLLOW_LINKS)) {
                        try {
                            Files.createDirectory(outputDir);
                        } catch (IOException e) {
                            logger.log(Level.SEVERE, "HTMLファイル出力ディレクトリ作成失敗", e);
                            return;
                        }
                    } else {
                        try {
                            Files.list(outputDir).forEach(file -> {
                                try {
                                    Files.delete(file);
                                } catch (IOException e) {
                                    logger.log(Level.WARNING, "ファイルの削除に失敗 : %s".formatted(file.getFileName()), e);
                                }
                            });
                        } catch (IOException e) {
                            logger.log(Level.WARNING, "古いHTMLファイルの削除に失敗", e);
                        }
                    }
                    Path filePath = Path.of(outputDir + "\\足立区コロナワクチン予約空き状況（%s月）- %s.html".formatted(month, LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddhhmmss"))));
                    try (FileWriter fw = new FileWriter(filePath.toString())) {
                        logger.log(Level.INFO, "HTMLファイル出力 開始");
                        fw.write(body);
                        logger.log(Level.INFO, "HTMLファイル出力 終了");
                        logger.log(Level.INFO, "ファイル出力先：%s".formatted(filePath.toString()));
                    } catch (IOException e) {
                        logger.log(Level.SEVERE, "HTMLファイル出力失敗", e);
                    }
                });
    }
}
