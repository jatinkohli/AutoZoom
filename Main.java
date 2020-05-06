import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
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
import java.util.Scanner;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Main class
 * 
 * Make sure to include the JSON library in the lib folder as a project dependency.
 * Only works if Chrome is installed, works on both Windows and Mac OS (hopefully).
 * 
 * @author Jatin Kohli
 * @since 3/14/20
 */
public class Main {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss.SSS'Z'"); //Java date time sucks :(
    private static final int OFFSET = 120; // Time (seconds) to join a meeting before the scheduled time
    private static final int ALLOWABLE_DELAY = 380; // Time after the meeting starts where you will join automatically

    private static final String WINDOWS_CHROME_CMD = "cmd /c start chrome ";
    private static final String MAC_CHROME_CMD = "bash -c open -a \"Google Chrome\" ";

    private static Map<String, String> periodLinks;
    private static Queue<QueueElement> linkQueue;

    /**
     * Fills periodLinks with Zoom links for each corresponding period key.
     * Reads periods (keys) and links (values) from links.txt in the same directory.
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
     * Fills linkQueue with Zoom links and times.
     * Gets the schedule from the HarkerDev API using getScheduleJson(), gets the link corresponding to each period, and
     * creates a Queue representing the day's schedule.
     */
    private static void instantiateLinkQueue() {
        linkQueue = new LinkedList<QueueElement>();

        try {
            Iterator<Object> scheduleIterator = (new JSONObject(getScheduleJson())).getJSONArray("schedule").iterator();

            while (scheduleIterator.hasNext()) {
                JSONObject period = (JSONObject) scheduleIterator.next();

                String time = period.getString("start");
                String link = periodLinks.get(period.getString("name"));

                if (link != null) //Could be null if certain period has no associated link, like for office hours
                    linkQueue.add(new QueueElement(time, link));
            }
        } catch (JSONException e) {
            System.out.println("No Meetings or Schedule found for today");
            System.exit(-1);
        } catch (NullPointerException e1) { //Harker Dev Machine broke
            System.out.println("HarkerDev servers broke again :(");
            System.exit(-2);
        }
    }

    /**
     * Gets the JSON object representing today's schedule from the HarkerDev API.
     * Executes a curl command and parses the result.
     */
    private static String getScheduleJson() {
        LocalDate date = LocalDate.now();
        System.out.println("Current Date: " + date);
        System.out.println("Current Time: " + LocalTime.now() + "\r\n");

        String getScheduleCommand = String.format(
                "curl --request GET --url https://bell.dev.harker.org/api/schedule --header \"Content-Type: application/x-www-form-urlencoded\" --data month=%s --data day=%s --data year=%s",
                date.getMonthValue(), date.getDayOfMonth(), date.getYear());

        String schedule = null;

        try {
            Process process = (new ProcessBuilder()).command(getScheduleCommand.split(" ")).start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            schedule = reader.readLine();

            process.waitFor();
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("API result: " + schedule + "\r\n");

        return schedule;
    }

    /**
     * Main method
     */
    public static void main(String[] args) {
        instantiatePeriodLinks();
        instantiateLinkQueue();

        int currentTime = -1;
        int nextMeetingTime = -1;

        boolean isWindowsOS = System.getProperty("os.name").toLowerCase().indexOf("win") != -1;
        System.out.println("OS: " + System.getProperty("os.name"));
        System.out.println(linkQueue.size() + " meetings today!\r\n");

        while (!linkQueue.isEmpty()) {
            currentTime = LocalTime.now().toSecondOfDay();
            nextMeetingTime = LocalDateTime.parse(linkQueue.element().time, TIME_FORMATTER).toLocalTime().toSecondOfDay();

            int timeDiff = nextMeetingTime - currentTime - OFFSET;

            try {
                if (timeDiff > 0) {
                    System.out.println("Waiting for meeting to start...");
                    Thread.sleep((long)(timeDiff * 1000));
                }

                String link = linkQueue.remove().link;

                if (timeDiff >= -ALLOWABLE_DELAY) { //Don't join meeting if a certain amount of time after the meeting start time has passed
                    System.out.println("Opened link for next meeting.\r\n");
                    
                    String command = (isWindowsOS ? WINDOWS_CHROME_CMD : MAC_CHROME_CMD) + link;
                    (new ProcessBuilder()).command(command.split(" ")).start().waitFor();
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        System.out.println("No more meetings today :)");

        System.exit(0);
    }
}