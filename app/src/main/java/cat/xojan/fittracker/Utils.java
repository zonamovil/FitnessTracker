package cat.xojan.fittracker;

import java.text.SimpleDateFormat;

public class Utils {
    public static String millisToTime(long millis) {
        long second = (millis / 1000) % 60;
        long minute = (millis / (1000 * 60)) % 60;
        long hour = (millis / (1000 * 60 * 60)) % 24;

        return String.format("%02d:%02d:%02d", hour, minute, second);
    }

    public static String millisToDate(long startTime) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yy");
        return sdf.format(startTime);
    }
}
