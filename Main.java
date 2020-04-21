import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Scanner;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Main class
 * 
 * @author Jatin Kohli
 * @since 3/14/20
 */
public class Main {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss.SSS'Z'");
    private static final int OFFSET = 120; // Time (seconds) to join a meeting before the scheduled time
    private static final int ALLOWABLE_DELAY = 380; // Time after the meeting starts where you will join automatically

    private static Map<String, String> periodLinks;
    private static Queue<QueueElement> linkQueue;

    /**
     * Fills periodLinks with Zoom links for each corresponding period key
     */
    private static void instantiatePeriodLinks() {
        periodLinks = new HashMap<String, String>();

        try {
            Scanner linkScanner = new Scanner(new File("links.txt"));

            while (linkScanner.hasNextLine()) {
                String key = linkScanner.next();
                String value = linkScanner.next();

                periodLinks.put(key, value);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * Fills linkQueue with Zoom links and times
     */
    private static void instantiateLinkQueue() {
        linkQueue = new LinkedList<QueueElement>();

        try {
            Iterator<Object> scheduleIterator = (new JSONObject(getScheduleJson())).getJSONArray("schedule").iterator();

            while (scheduleIterator.hasNext()) {
                JSONObject period = (JSONObject) scheduleIterator.next();

                String time = period.getString("start");
                String link = periodLinks.get(period.getString("name"));

                if (link != null)
                    linkQueue.add(new QueueElement(time, link));
            }
        } catch (JSONException e) {
            System.out.println("No Schedule found for today");
            System.exit(-1);
        } catch (NullPointerException e1) {
            System.out.println("HarkerDev servers broke again :(");
            System.exit(-2);
        }

        // DayOfWeek day = LocalDate.now().getDayOfWeek();
        // if (day == DayOfWeek.WEDNESDAY || day == DayOfWeek.FRIDAY) //athletic pants time
        //     linkQueue.add(new QueueElement("2020-04-20T15:15:00.000Z", periodLinks.get("Fencing")));
    }

    /**
     * Gets the JSON object representing today's schedule from the HarkerDev API
     */
    private static String getScheduleJson() {
        LocalDate date = LocalDate.now();
        System.out.println("Current Date: " + date);
        System.out.println("Current Time: " + LocalTime.now());

        String getScheduleCommand = String.format(
                "curl --request GET --url https://bell.dev.harker.org/api/schedule --header \"Content-Type: application/x-www-form-urlencoded\" --data month=%s --data day=%s --data year=%s",
                date.getMonthValue(), date.getDayOfMonth(), date.getYear());

        String schedule = null;

        try {
            Process process = (new ProcessBuilder()).command("cmd.exe", "/c", getScheduleCommand).start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            schedule = reader.readLine();

            process.waitFor();
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println(schedule + "\r\n");

        return schedule;
    }

    /**
     * Main method
     */
    public static void main(String[] args) {
        instantiatePeriodLinks();
        instantiateLinkQueue();

        int currentTime;
        int nextMeetingTime = LocalDateTime.parse(linkQueue.element().time, TIME_FORMATTER).toLocalTime().toSecondOfDay();

        while (!linkQueue.isEmpty()) {
            currentTime = LocalTime.now().toSecondOfDay();
            int timeDiff = nextMeetingTime - currentTime - OFFSET;

            try {
                if (timeDiff > 0)
                    Thread.sleep((long)(timeDiff * 1000));

                String link = linkQueue.remove().link;
                if (timeDiff >= -ALLOWABLE_DELAY)
                    (new ProcessBuilder()).command("cmd.exe", "/c", "start Chrome " + link).start().waitFor();

                if (!linkQueue.isEmpty())
                    nextMeetingTime = LocalDateTime.parse(linkQueue.element().time, TIME_FORMATTER).toLocalTime().toSecondOfDay();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        System.exit(0);
    }
}