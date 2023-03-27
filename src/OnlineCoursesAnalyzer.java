import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * This is just a demo for you, please run it on JDK17 (some statements may be not allowed in lower version).
 * This is just a demo, and you can extend and implement functions
 * based on this demo, or implement it in a different way.
 */
public class OnlineCoursesAnalyzer {

    List<Course> courses = new ArrayList<>();

    public OnlineCoursesAnalyzer(String datasetPath) {
        BufferedReader br = null;
        String line;
        try {
            br = new BufferedReader(new FileReader(datasetPath, StandardCharsets.UTF_8));
            br.readLine();
            while ((line = br.readLine()) != null) {
                String[] info = line.split(",(?=([^\\\"]*\\\"[^\\\"]*\\\")*[^\\\"]*$)", -1);
                Course course = new Course(info[0], info[1], new Date(info[2]), info[3], info[4], info[5],
                        Integer.parseInt(info[6]), Integer.parseInt(info[7]), Integer.parseInt(info[8]),
                        Integer.parseInt(info[9]), Integer.parseInt(info[10]), Double.parseDouble(info[11]),
                        Double.parseDouble(info[12]), Double.parseDouble(info[13]), Double.parseDouble(info[14]),
                        Double.parseDouble(info[15]), Double.parseDouble(info[16]), Double.parseDouble(info[17]),
                        Double.parseDouble(info[18]), Double.parseDouble(info[19]), Double.parseDouble(info[20]),
                        Double.parseDouble(info[21]), Double.parseDouble(info[22]));
                courses.add(course);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    //1
    public Map<String, Integer> getPtcpCountByInst() {
        Map<String, Integer> ptcpCount = new TreeMap<>();
        courses.forEach(course -> ptcpCount.put(course.institution,
                ptcpCount.getOrDefault(course.institution, 0) + course.participants));
        return ptcpCount;
    }

    //2
    public Map<String, Integer> getPtcpCountByInstAndSubject() {
        Map<String, Integer> ptcpCount = new HashMap<>();
        courses.forEach(course -> {
            String key = String.format("%s-%s", course.institution, course.subject);
            ptcpCount.put(key, ptcpCount.getOrDefault(key, 0) + course.participants);
        });
        return ptcpCount.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (e1, e2) -> e1, LinkedHashMap::new));
    }

    //3
    public Map<String, List<List<String>>> getCourseListOfInstructor() {
        Map<String, List<List<String>>> courseList = new HashMap<>();
        courses.stream().filter(course -> !course.instructors.contains(",")).forEach(course -> {
            if (!courseList.containsKey(course.instructors)) {
                courseList.put(course.instructors, new ArrayList<>(
                        Arrays.asList(new ArrayList<>(), new ArrayList<>())));
            }
            if (!courseList.get(course.instructors).get(0).contains(course.title)) {
                courseList.get(course.instructors).get(0).add(course.title);
            }
        });
        courses.stream().filter(course -> course.instructors.contains(",")).forEach(course -> {
            String[] instructors = course.instructors.split(",");
            for (String instructor : instructors) {
                if (!courseList.containsKey(instructor.strip())) {
                    courseList.put(instructor.strip(), new ArrayList<>(
                            Arrays.asList(new ArrayList<>(), new ArrayList<>())));
                }
                if (!courseList.get(instructor.strip()).get(1).contains(course.title)) {
                    courseList.get(instructor.strip()).get(1).add(course.title);
                }
            }
        });
        courseList.values().forEach(l -> {
            Collections.sort(l.get(0));
            Collections.sort(l.get(1));
        });
        return courseList;
    }

    //4
    public List<String> getCourses(int topK, String by) {
        Comparator<Course> comparator = (c1, c2) -> {
            if (by.equals("hours") && c1.totalHours != c2.totalHours) {
                return c1.totalHours > c2.totalHours ? -1 : 1;
            } else if (by.equals("participants") && c1.participants != c2.participants) {
                return c1.participants > c2.participants ? -1 : 1;
            } else {
                return c1.title.compareTo(c2.title);
            }
        };
        return courses.stream().sorted(comparator)
                .map(course -> course.title).distinct().limit(topK)
                .collect(Collectors.toList());
    }

    //5
    public List<String> searchCourses(String courseSubject, double percentAudited, double totalCourseHours) {
        return courses.stream()
                .filter(course -> course.subject.toLowerCase().contains(courseSubject.toLowerCase()))
                .filter(course -> course.percentAudited >= percentAudited)
                .filter(course -> course.totalHours <= totalCourseHours)
                .map(course -> course.title)
                .sorted()
                .distinct()
                .collect(Collectors.toList());
    }

    //6
    public List<String> recommendCourses(int age, int gender, int isBachelorOrHigher) {
        Map<String, Double> averageMedianAge = courses.stream().collect(Collectors.groupingBy(
                c -> c.number, HashMap::new, Collectors.averagingDouble(c -> c.medianAge)
        ));
        Map<String, Double> averageMale = courses.stream().collect(Collectors.groupingBy(
                c -> c.number, HashMap::new, Collectors.averagingDouble(c -> c.percentMale)
        ));
        Map<String, Double> averageBachelorOrHigher = courses.stream().collect(Collectors.groupingBy(
                c -> c.number, HashMap::new, Collectors.averagingDouble(c -> c.percentDegree)
        ));
        Map<String, Course> mapCourseNumberToCourse = new HashMap<>();
        courses.forEach(course -> {
            if (!mapCourseNumberToCourse.containsKey(course.number)) {
                mapCourseNumberToCourse.put(course.number, course);
            } else if (mapCourseNumberToCourse.get(course.number).launchDate.getTime()
                    < course.launchDate.getTime()) {
                mapCourseNumberToCourse.put(course.number, course);
            }
        });
        return courses.stream().map(course -> new Pair<>(course.number,
                Math.pow(age - averageMedianAge.get(course.number), 2.0)
                        + Math.pow(gender * 100 - averageMale.get(course.number), 2.0)
                        + Math.pow(isBachelorOrHigher * 100 - averageBachelorOrHigher.get(course.number), 2.0))
        ).sorted((o1, o2) -> {
            if (!o1.right.equals(o2.right)) {
                return o1.right.compareTo(o2.right);
            }
            return mapCourseNumberToCourse.get(o1.left).title
                    .compareTo(mapCourseNumberToCourse.get(o2.left).title);
        }).map(pair -> pair.left)
                .distinct()
                .map(c -> mapCourseNumberToCourse.get(c).title)
                .distinct().limit(10).collect(Collectors.toList());
    }

}

class Pair<L, R> {
    L left;
    R right;

    public Pair(L left, R right) {
        this.left = left;
        this.right = right;
    }

    @Override
    public String toString() {
        return String.format("%s-%s", left, right);
    }
}

class Course {
    String institution;
    String number;
    Date launchDate;
    String title;
    String instructors;
    String subject;
    int year;
    int honorCode;
    int participants;
    int audited;
    int certified;
    double percentAudited;
    double percentCertified;
    double percentCertified50;
    double percentVideo;
    double percentForum;
    double gradeHigherZero;
    double totalHours;
    double medianHoursCertification;
    double medianAge;
    double percentMale;
    double percentFemale;
    double percentDegree;

    public Course(String institution, String number, Date launchDate,
                  String title, String instructors, String subject,
                  int year, int honorCode, int participants,
                  int audited, int certified, double percentAudited,
                  double percentCertified, double percentCertified50,
                  double percentVideo, double percentForum, double gradeHigherZero,
                  double totalHours, double medianHoursCertification,
                  double medianAge, double percentMale, double percentFemale,
                  double percentDegree) {
        this.institution = institution;
        this.number = number;
        this.launchDate = launchDate;
        if (title.startsWith("\"")) title = title.substring(1);
        if (title.endsWith("\"")) title = title.substring(0, title.length() - 1);
        this.title = title;
        if (instructors.startsWith("\"")) instructors = instructors.substring(1);
        if (instructors.endsWith("\"")) instructors = instructors.substring(0, instructors.length() - 1);
        this.instructors = instructors;
        if (subject.startsWith("\"")) subject = subject.substring(1);
        if (subject.endsWith("\"")) subject = subject.substring(0, subject.length() - 1);
        this.subject = subject;
        this.year = year;
        this.honorCode = honorCode;
        this.participants = participants;
        this.audited = audited;
        this.certified = certified;
        this.percentAudited = percentAudited;
        this.percentCertified = percentCertified;
        this.percentCertified50 = percentCertified50;
        this.percentVideo = percentVideo;
        this.percentForum = percentForum;
        this.gradeHigherZero = gradeHigherZero;
        this.totalHours = totalHours;
        this.medianHoursCertification = medianHoursCertification;
        this.medianAge = medianAge;
        this.percentMale = percentMale;
        this.percentFemale = percentFemale;
        this.percentDegree = percentDegree;
    }
}