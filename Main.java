import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

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

    private static Map<String, String> periodLinks;
    private static Queue<QueueElement> linkQueue;

    /**
     * Fills periodLinks with Zoom links for each corresponding period key
     */
    private static void instantiatePeriodLinks() {
        periodLinks = new HashMap<String, String>();

        periodLinks.put("P1", "https://harker.zoom.us/wc/join/5408748374");
        periodLinks.put("P2", "https://harker.zoom.us/wc/join/8699692694");
        periodLinks.put("P3", "https://harker.zoom.us/wc/join/4328893126");
        periodLinks.put("P5", "https://harker.zoom.us/wc/join/6614123433");
        periodLinks.put("P6", "https://harker.zoom.us/wc/join/8904802894");
        periodLinks.put("P7", "https://harker.zoom.us/wc/join/9655066157");
        periodLinks.put("Advisory", "https://www.youtube.com/watch?v=dQw4w9WgXcQ");
    }

    /**
     * Fills linkQueue with Zoom links and times
     */
    private static void instantiateLinkQueue() {
        linkQueue = new LinkedList<QueueElement>();

        try {
            Iterator<Object> scheduleIterator = (new JSONObject(getScheduleJson())).getJSONArray("schedule").iterator();

            while (scheduleIterator.hasNext()) {
                JSONObject period = (JSONObject)scheduleIterator.next();
                
                String time = period.getString("start");
                String link = periodLinks.get(period.getString("name"));

                if (link != null) 
                    linkQueue.add(new QueueElement(time, link));
            }
        } catch (JSONException e) {
            System.out.println("No Schedule found for today");
        }
    }

    /**
     * Gets the JSON object representing today's schedule from the HarkerDev API
     */
    private static String getScheduleJson() {
        LocalDate date = LocalDate.now();

        String getScheduleCommand = String.format(
            "curl --request GET --url https://bell.dev.harker.org/api/schedule --header \"Content-Type: application/x-www-form-urlencoded\" --data month=%s --data day=%s --data year=%s",
            date.getMonthValue(), date.getDayOfMonth(), date.getYear()
        );

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

            if (Math.abs(currentTime - nextMeetingTime) < 60) {
                try {
                    String link = linkQueue.remove().link;
                    (new ProcessBuilder()).command("cmd.exe", "/c", "start Chrome " + link).start().waitFor();

                    if (!linkQueue.isEmpty())
                        nextMeetingTime = LocalDateTime.parse(linkQueue.element().time, TIME_FORMATTER).toLocalTime().toSecondOfDay();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        System.exit(0);
    }
}