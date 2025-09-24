import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// --- Enums ---
enum Semester {
    SPRING, SUMMER, FALL
}

enum Grade {
    S(10), A(9), B(8), C(7), D(6), E(5), F(0);

    private final int gradePoint;

    Grade(int gradePoint) {
        this.gradePoint = gradePoint;
    }

    public int getGradePoint() {
        return gradePoint;
    }

    // Helper method to get Grade from a string value
    public static Optional<Grade> fromString(String gradeString) {
        try {
            return Optional.of(Grade.valueOf(gradeString.toUpperCase()));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}

// --- Custom Exceptions ---
class DuplicateEnrollmentException extends Exception {
    public DuplicateEnrollmentException(String message) {
        super(message);
    }
}

class MaxCreditLimitExceededException extends Exception {
    public MaxCreditLimitExceededException(String message) {
        super(message);
    }
}

// --- Interfaces ---
interface Persistable {
    void save();
    void load();
}

interface Searchable<T> {
    List<T> search(String query);
}

// --- Abstract Classes ---
abstract class Person {
    private String id;
    private String fullName;
    private String email;

    public Person(String id, String fullName, String email) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
    }

    // Getters and Setters
    public String getId() { return id; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }

    public abstract void displayProfile();
}

// --- Domain Classes ---

// A simple interface to hold constants
interface Constants {
    String APP_NAME = "Campus Course & Records Manager (CCRM)";
    String DATA_FOLDER = "data/";
    String STUDENTS_FILE = "students.csv";
    String COURSES_FILE = "courses.csv";
    String ENROLLMENT_FILE = "enrollment.csv"; // New file for enrollment data
    int MAX_CREDITS_PER_SEMESTER = 20;
}

// Singleton design pattern for application-wide configuration
class AppConfig {
    private static AppConfig instance;
    private final String dataFolderPath;

    private AppConfig() {
        this.dataFolderPath = Constants.DATA_FOLDER;
        System.out.println("AppConfig initialized. Data folder: " + this.dataFolderPath);
        Path dataDir = Paths.get(dataFolderPath);
        if (!Files.exists(dataDir)) {
            try {
                Files.createDirectories(dataDir);
            } catch (IOException e) {
                System.err.println("Failed to create data directory: " + e.getMessage());
            }
        }
    }

    public static AppConfig getInstance() {
        if (instance == null) {
            instance = new AppConfig();
        }
        return instance;
    }

    public String getDataFolderPath() {
        return dataFolderPath;
    }
}

// Student class inheriting from Person
class Student extends Person {
    private String regNo;
    private final List<Enrollment> enrolledCourses;

    public Student(String id, String regNo, String fullName, String email) {
        super(id, fullName, email);
        this.regNo = regNo;
        this.enrolledCourses = new ArrayList<>();
    }
    
    // Updated constructor for loading from file, including enrolled courses
    public Student(String id, String regNo, String fullName, String email, List<Enrollment> enrollments) {
        super(id, fullName, email);
        this.regNo = regNo;
        this.enrolledCourses = new ArrayList<>(enrollments);
    }

    public String getRegNo() { return regNo; }

    public List<Enrollment> getEnrolledCourses() {
        return enrolledCourses;
    }
    
    public void addEnrollment(Enrollment enrollment) {
        this.enrolledCourses.add(enrollment);
    }

    public int getCurrentCredits() {
        return enrolledCourses.stream().mapToInt(e -> e.getCourse().getCredits()).sum();
    }

    public double calculateGPA() {
        if (enrolledCourses.isEmpty()) {
            return 0.0;
        }

        double totalGradePoints = 0;
        int totalCredits = 0;
        for (Enrollment enrollment : enrolledCourses) {
            Optional<Grade> grade = enrollment.getGrade();
            if (grade.isPresent()) {
                totalGradePoints += grade.get().getGradePoint() * enrollment.getCourse().getCredits();
                totalCredits += enrollment.getCourse().getCredits();
            }
        }
        return totalCredits > 0 ? totalGradePoints / totalCredits : 0.0;
    }

    @Override
    public void displayProfile() {
        System.out.println("--- Student Profile ---");
        System.out.println("ID: " + getId());
        System.out.println("Registration No: " + getRegNo());
        System.out.println("Name: " + getFullName());
        System.out.println("Email: " + getEmail());
        System.out.println("Current Credits: " + getCurrentCredits());
        System.out.println("GPA: " + String.format("%.2f", calculateGPA()));
        System.out.println("Enrolled Courses:");
        if (enrolledCourses.isEmpty()) {
            System.out.println("  No courses enrolled.");
        } else {
            enrolledCourses.forEach(e -> System.out.println("  " + e));
        }
    }
}

// Instructor class inheriting from Person
class Instructor extends Person {
    public Instructor(String id, String fullName, String email) {
        super(id, fullName, email);
    }

    @Override
    public void displayProfile() {
        System.out.println("--- Instructor Profile ---");
        System.out.println("ID: " + getId());
        System.out.println("Name: " + getFullName());
        System.out.println("Email: " + getEmail());
    }
}

// Course class
class Course {
    private String code;
    private String title;
    private int credits;
    private Instructor instructor;
    private Semester semester;

    public Course(String code, String title, int credits, Instructor instructor, Semester semester) {
        this.code = code;
        this.title = title;
        this.credits = credits;
        this.instructor = instructor;
        this.semester = semester;
    }

    // Getters
    public String getCode() { return code; }
    public String getTitle() { return title; }
    public int getCredits() { return credits; }
    public Instructor getInstructor() { return instructor; }
    public Semester getSemester() { return semester; }

    @Override
    public String toString() {
        return "Course{" +
               "code='" + code + '\'' +
               ", title='" + title + '\'' +
               ", credits=" + credits +
               ", semester=" + semester +
               (instructor != null ? ", instructor='" + instructor.getFullName() + '\'' : "") +
               '}';
    }
}

// Enrollment class to link Student and Course
class Enrollment {
    private Student student;
    private Course course;
    private Optional<Grade> grade = Optional.empty();

    public Enrollment(Student student, Course course) {
        this.student = student;
        this.course = course;
    }
    
    // Constructor for loading from file
    public Enrollment(Student student, Course course, String gradeStr) {
        this(student, course);
        this.grade = Grade.fromString(gradeStr);
    }

    public Student getStudent() { return student; }
    public Course getCourse() { return course; }
    public Optional<Grade> getGrade() { return grade; }
    public void setGrade(Grade grade) { this.grade = Optional.of(grade); }

    @Override
    public String toString() {
        return "Enrollment{" +
               "studentId='" + student.getId() + '\'' +
               ", courseCode='" + course.getCode() + '\'' +
               ", grade=" + (grade.isPresent() ? grade.get() : "N/A") +
               '}';
    }
}

// --- Service Classes ---
class IOManager implements Persistable {
    private final Map<String, Student> students = new HashMap<>();
    private final Map<String, Course> courses = new HashMap<>();
    private final Map<String, Instructor> instructors = new HashMap<>();
    private final Map<String, List<String[]>> enrollmentData = new HashMap<>();

    public IOManager() {
        // Add a sample instructor to avoid null pointers
        Instructor instructor = new Instructor("I001", "Dr. Jane Doe", "jdoe@ccrm.edu");
        instructors.put(instructor.getId(), instructor);
    }

    @Override
    public void save() {
        try {
            // Save students
            Path studentsPath = Paths.get(Constants.DATA_FOLDER, Constants.STUDENTS_FILE);
            try (BufferedWriter writer = Files.newBufferedWriter(studentsPath)) {
                writer.write("id,regNo,fullName,email\n");
                for (Student student : students.values()) {
                    writer.write(String.join(",", student.getId(), student.getRegNo(), student.getFullName(), student.getEmail()) + "\n");
                }
            }
            System.out.println("Students data saved successfully.");

            // Save courses
            Path coursesPath = Paths.get(Constants.DATA_FOLDER, Constants.COURSES_FILE);
            try (BufferedWriter writer = Files.newBufferedWriter(coursesPath)) {
                writer.write("code,title,credits,instructorId,semester\n");
                for (Course course : courses.values()) {
                    String instructorId = course.getInstructor() != null ? course.getInstructor().getId() : "N/A";
                    String semester = course.getSemester() != null ? course.getSemester().name() : "N/A";
                    writer.write(String.join(",", course.getCode(), course.getTitle(), String.valueOf(course.getCredits()), instructorId, semester) + "\n");
                }
            }
            System.out.println("Courses data saved successfully.");
            
            // Save enrollment data
            Path enrollmentPath = Paths.get(Constants.DATA_FOLDER, Constants.ENROLLMENT_FILE);
            try (BufferedWriter writer = Files.newBufferedWriter(enrollmentPath)) {
                writer.write("studentId,courseCode,grade\n");
                for(Student student : students.values()) {
                    for(Enrollment enrollment : student.getEnrolledCourses()) {
                        String grade = enrollment.getGrade().isPresent() ? enrollment.getGrade().get().name() : "";
                        writer.write(String.join(",", student.getId(), enrollment.getCourse().getCode(), grade) + "\n");
                    }
                }
            }
            System.out.println("Enrollment data saved successfully.");

        } catch (IOException e) {
            System.err.println("Failed to save data: " + e.getMessage());
        }
    }

    @Override
    public void load() {
        try {
            // Load students
            Path studentsPath = Paths.get(Constants.DATA_FOLDER, Constants.STUDENTS_FILE);
            if (Files.exists(studentsPath)) {
                try (BufferedReader reader = Files.newBufferedReader(studentsPath)) {
                    reader.lines().skip(1).forEach(line -> {
                        String[] parts = line.split(",", -1);
                        if (parts.length == 4) {
                            Student student = new Student(parts[0].trim(), parts[1].trim(), parts[2].trim(), parts[3].trim());
                            students.put(student.getId(), student);
                        }
                    });
                }
                System.out.println("Students data loaded successfully.");
            }

            // Load courses
            Path coursesPath = Paths.get(Constants.DATA_FOLDER, Constants.COURSES_FILE);
            if (Files.exists(coursesPath)) {
                try (BufferedReader reader = Files.newBufferedReader(coursesPath)) {
                    reader.lines().skip(1).forEach(line -> {
                        String[] parts = line.split(",", -1);
                        if (parts.length == 5) {
                            Instructor instructor = instructors.get(parts[3].trim());
                            Semester semester = Semester.valueOf(parts[4].trim());
                            Course course = new Course(parts[0].trim(), parts[1].trim(), Integer.parseInt(parts[2].trim()), instructor, semester);
                            courses.put(course.getCode(), course);
                        }
                    });
                }
                System.out.println("Courses data loaded successfully.");
            }
            
            // Load enrollment data
            Path enrollmentPath = Paths.get(Constants.DATA_FOLDER, Constants.ENROLLMENT_FILE);
            if(Files.exists(enrollmentPath)) {
                try (BufferedReader reader = Files.newBufferedReader(enrollmentPath)) {
                    reader.lines().skip(1).forEach(line -> {
                        String[] parts = line.split(",", -1);
                        if(parts.length == 3) {
                            String studentId = parts[0].trim();
                            String courseCode = parts[1].trim();
                            String gradeStr = parts[2].trim();
                            
                            Student student = students.get(studentId);
                            Course course = courses.get(courseCode);
                            
                            if(student != null && course != null) {
                                Enrollment enrollment = new Enrollment(student, course, gradeStr);
                                student.addEnrollment(enrollment);
                            }
                        }
                    });
                }
                System.out.println("Enrollment data loaded successfully.");
            }
            
        } catch (IOException e) {
            System.err.println("Failed to load data: " + e.getMessage());
        }
    }

    public Map<String, Student> getStudents() { return students; }
    public Map<String, Course> getCourses() { return courses; }
    public Map<String, Instructor> getInstructors() { return instructors; }
}

class StudentService implements Searchable<Student> {
    private final Map<String, Student> students;

    public StudentService(Map<String, Student> students) {
        this.students = students;
    }

    public void addStudent(Student student) {
        students.put(student.getId(), student);
    }

    public void listStudents() {
        if (students.isEmpty()) {
            System.out.println("No students found.");
            return;
        }
        students.values().forEach(s -> System.out.println("ID: " + s.getId() + ", Name: " + s.getFullName() + ", RegNo: " + s.getRegNo()));
    }

    @Override
    public List<Student> search(String query) {
        return students.values().stream()
                .filter(s -> s.getFullName().toLowerCase().contains(query.toLowerCase()) ||
                             s.getId().toLowerCase().contains(query.toLowerCase()) ||
                             s.getRegNo().toLowerCase().contains(query.toLowerCase()))
                .collect(Collectors.toList());
    }

    public void updateStudent(String id, String newFullName, String newEmail) {
        Student student = students.get(id);
        if (student != null) {
            student = new Student(id, student.getRegNo(), newFullName, newEmail, student.getEnrolledCourses());
            students.put(id, student);
            System.out.println("Student updated successfully.");
        } else {
            System.out.println("Student not found.");
        }
    }

    public void deleteStudent(String id) {
        if (students.remove(id) != null) {
            System.out.println("Student deleted successfully.");
        } else {
            System.out.println("Student not found.");
        }
    }
}

class CourseService implements Searchable<Course> {
    private final Map<String, Course> courses;
    private final Map<String, Instructor> instructors;

    public CourseService(Map<String, Course> courses, Map<String, Instructor> instructors) {
        this.courses = courses;
        this.instructors = instructors;
    }

    public void addCourse(Course course) {
        courses.put(course.getCode(), course);
    }

    public void listCourses() {
        if (courses.isEmpty()) {
            System.out.println("No courses found.");
            return;
        }
        courses.values().forEach(System.out::println);
    }

    @Override
    public List<Course> search(String query) {
        return courses.values().stream()
                .filter(c -> c.getTitle().toLowerCase().contains(query.toLowerCase()) ||
                             c.getCode().toLowerCase().contains(query.toLowerCase()))
                .collect(Collectors.toList());
    }

    public void updateCourse(String code, String newTitle, int newCredits, String instructorId, String semesterStr) {
        Course course = courses.get(code);
        if (course != null) {
            Instructor instructor = instructors.get(instructorId);
            Semester semester = Semester.valueOf(semesterStr.toUpperCase());
            
            Course newCourse = new Course(code, newTitle, newCredits, instructor, semester);
            courses.put(code, newCourse);
            System.out.println("Course updated successfully.");
        } else {
            System.out.println("Course not found.");
        }
    }

    public void deleteCourse(String code) {
        if (courses.remove(code) != null) {
            System.out.println("Course deleted successfully.");
        } else {
            System.out.println("Course not found.");
        }
    }
}

class EnrollmentService {
    private final Map<String, Student> students;
    private final Map<String, Course> courses;

    public EnrollmentService(Map<String, Student> students, Map<String, Course> courses) {
        this.students = students;
        this.courses = courses;
    }

    public void enrollStudent(String studentId, String courseCode) throws DuplicateEnrollmentException, MaxCreditLimitExceededException {
        Student student = students.get(studentId);
        Course course = courses.get(courseCode);

        if (student == null) {
            System.out.println("Student not found.");
            return;
        }
        if (course == null) {
            System.out.println("Course not found.");
            return;
        }

        boolean alreadyEnrolled = student.getEnrolledCourses().stream()
                .anyMatch(e -> e.getCourse().getCode().equals(courseCode));

        if (alreadyEnrolled) {
            throw new DuplicateEnrollmentException("Student is already enrolled in this course.");
        }

        if (student.getCurrentCredits() + course.getCredits() > Constants.MAX_CREDITS_PER_SEMESTER) {
            throw new MaxCreditLimitExceededException("Enrolling in this course would exceed the maximum credit limit.");
        }

        student.getEnrolledCourses().add(new Enrollment(student, course));
        System.out.println("Student " + student.getFullName() + " enrolled in " + course.getTitle() + ".");
    }

    public void unenrollStudent(String studentId, String courseCode) {
        Student student = students.get(studentId);
        if (student == null) {
            System.out.println("Student not found.");
            return;
        }

        boolean removed = student.getEnrolledCourses().removeIf(e -> e.getCourse().getCode().equals(courseCode));
        if (removed) {
            System.out.println("Student " + student.getFullName() + " unenrolled from " + courseCode + ".");
        } else {
            System.out.println("Student was not enrolled in this course.");
        }
    }

    public void recordGrade(String studentId, String courseCode, String gradeString) {
        Student student = students.get(studentId);
        if (student == null) {
            System.out.println("Student not found.");
            return;
        }

        Optional<Grade> grade = Grade.fromString(gradeString);
        if (grade.isEmpty()) {
            System.out.println("Invalid grade. Please use S, A, B, C, D, E, or F.");
            return;
        }

        Optional<Enrollment> enrollment = student.getEnrolledCourses().stream()
                .filter(e -> e.getCourse().getCode().equals(courseCode))
                .findFirst();

        if (enrollment.isPresent()) {
            enrollment.get().setGrade(grade.get());
            System.out.println("Grade for " + student.getFullName() + " in " + courseCode + " recorded as " + grade.get() + ".");
        } else {
            System.out.println("Student is not enrolled in this course.");
        }
    }
}

class BackupService {
    public void createBackup() {
        try {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            Path backupDir = Paths.get(Constants.DATA_FOLDER, "backup_" + timestamp);
            Files.createDirectories(backupDir);

            Path studentsFile = Paths.get(Constants.DATA_FOLDER, Constants.STUDENTS_FILE);
            if (Files.exists(studentsFile)) {
                Files.copy(studentsFile, backupDir.resolve(Constants.STUDENTS_FILE), StandardCopyOption.REPLACE_EXISTING);
            }

            Path coursesFile = Paths.get(Constants.DATA_FOLDER, Constants.COURSES_FILE);
            if (Files.exists(coursesFile)) {
                Files.copy(coursesFile, backupDir.resolve(Constants.COURSES_FILE), StandardCopyOption.REPLACE_EXISTING);
            }
            
            Path enrollmentFile = Paths.get(Constants.DATA_FOLDER, Constants.ENROLLMENT_FILE);
            if (Files.exists(enrollmentFile)) {
                Files.copy(enrollmentFile, backupDir.resolve(Constants.ENROLLMENT_FILE), StandardCopyOption.REPLACE_EXISTING);
            }
            
            System.out.println("Backup created at: " + backupDir.toAbsolutePath());
        } catch (IOException e) {
            System.err.println("Failed to create backup: " + e.getMessage());
        }
    }
}

class ReportService {
    private final Map<String, Student> students;

    public ReportService(Map<String, Student> students) {
        this.students = students;
    }

    public void showGpaDistribution() {
        System.out.println("--- GPA Distribution Report ---");
        students.values().stream()
                .collect(Collectors.groupingBy(s -> (int) s.calculateGPA()))
                .forEach((gpa, studentList) -> System.out.printf("GPA Range %d-%d: %d students%n", gpa, gpa + 1, studentList.size()));
    }
}


// Main application class with the CLI menu
public class CCRM {

    public static void main(String[] args) {
        AppConfig config = AppConfig.getInstance();
        IOManager ioManager = new IOManager();
        ioManager.load();
        
        StudentService studentService = new StudentService(ioManager.getStudents());
        CourseService courseService = new CourseService(ioManager.getCourses(), ioManager.getInstructors());
        EnrollmentService enrollmentService = new EnrollmentService(ioManager.getStudents(), ioManager.getCourses());
        BackupService backupService = new BackupService();
        ReportService reportService = new ReportService(ioManager.getStudents());
        
        Scanner scanner = new Scanner(System.in);
        int choice;
        boolean exit = false;

        System.out.println("Welcome to " + Constants.APP_NAME);

        while (!exit) {
            System.out.println("\n--- Main Menu ---");
            System.out.println("1. Manage Students");
            System.out.println("2. Manage Courses");
            System.out.println("3. Manage Enrollment & Grades");
            System.out.println("4. Import/Export Data");
            System.out.println("5. Backup Data");
            System.out.println("6. Reports");
            System.out.println("7. Exit");
            System.out.print("Enter your choice: ");

            try {
                choice = scanner.nextInt();
                scanner.nextLine(); // Consume newline left-over
                
                switch (choice) {
                    case 1:
                        handleStudentManagement(scanner, studentService);
                        break;
                    case 2:
                        handleCourseManagement(scanner, courseService);
                        break;
                    case 3:
                        handleEnrollmentAndGrades(scanner, enrollmentService, studentService);
                        break;
                    case 4:
                        System.out.println("Import/Export selected. Data is automatically loaded on start and saved on exit.");
                        break;
                    case 5:
                        backupService.createBackup();
                        break;
                    case 6:
                        handleReports(scanner, reportService);
                        break;
                    case 7:
                        ioManager.save();
                        exit = true;
                        System.out.println("Exiting " + Constants.APP_NAME + ". Goodbye!");
                        break;
                    default:
                        System.out.println("Invalid choice. Please enter a number between 1 and 7.");
                }
            } catch (java.util.InputMismatchException e) {
                System.out.println("Invalid input. Please enter a number.");
                scanner.nextLine(); // Clear the invalid input from the scanner
            }
        }
        scanner.close();
    }
    
    private static void handleStudentManagement(Scanner scanner, StudentService service) {
        System.out.println("\n--- Student Management ---");
        System.out.println("1. Add Student");
        System.out.println("2. List Students");
        System.out.println("3. Search Students");
        System.out.println("4. Update Student");
        System.out.println("5. Delete Student");
        System.out.println("6. Back to Main Menu");
        System.out.print("Enter your choice: ");
        int choice = scanner.nextInt();
        scanner.nextLine(); // Consume newline

        switch (choice) {
            case 1:
                System.out.print("Enter Student ID: ");
                String id = scanner.nextLine();
                System.out.print("Enter Registration Number: ");
                String regNo = scanner.nextLine();
                System.out.print("Enter Full Name: ");
                String fullName = scanner.nextLine();
                System.out.print("Enter Email: ");
                String email = scanner.nextLine();
                service.addStudent(new Student(id, regNo, fullName, email));
                System.out.println("Student added successfully.");
                break;
            case 2:
                service.listStudents();
                break;
            case 3:
                System.out.print("Enter search query (ID, RegNo, or Name): ");
                String query = scanner.nextLine();
                List<Student> results = service.search(query);
                if (results.isEmpty()) {
                    System.out.println("No students found matching the query.");
                } else {
                    results.forEach(Student::displayProfile);
                }
                break;
            case 4:
                System.out.print("Enter Student ID to update: ");
                String updateId = scanner.nextLine();
                System.out.print("Enter new Full Name: ");
                String newFullName = scanner.nextLine();
                System.out.print("Enter new Email: ");
                String newEmail = scanner.nextLine();
                service.updateStudent(updateId, newFullName, newEmail);
                break;
            case 5:
                System.out.print("Enter Student ID to delete: ");
                String deleteId = scanner.nextLine();
                service.deleteStudent(deleteId);
                break;
            case 6:
                return;
            default:
                System.out.println("Invalid choice.");
        }
    }
    
    private static void handleCourseManagement(Scanner scanner, CourseService service) {
        System.out.println("\n--- Course Management ---");
        System.out.println("1. Add Course");
        System.out.println("2. List Courses");
        System.out.println("3. Search Courses");
        System.out.println("4. Update Course");
        System.out.println("5. Delete Course");
        System.out.println("6. Back to Main Menu");
        System.out.print("Enter your choice: ");
        int choice = scanner.nextInt();
        scanner.nextLine();

        switch (choice) {
            case 1:
                System.out.print("Enter Course Code: ");
                String code = scanner.nextLine();
                System.out.print("Enter Course Title: ");
                String title = scanner.nextLine();
                System.out.print("Enter Course Credits: ");
                int credits = scanner.nextInt();
                scanner.nextLine();
                System.out.print("Enter Semester (SPRING, SUMMER, FALL): ");
                String semesterStr = scanner.nextLine();
                service.addCourse(new Course(code, title, credits, null, Semester.valueOf(semesterStr.toUpperCase())));
                System.out.println("Course added successfully.");
                break;
            case 2:
                service.listCourses();
                break;
            case 3:
                System.out.print("Enter search query (Code or Title): ");
                String query = scanner.nextLine();
                List<Course> results = service.search(query);
                if (results.isEmpty()) {
                    System.out.println("No courses found matching the query.");
                } else {
                    results.forEach(System.out::println);
                }
                break;
            case 4:
                System.out.print("Enter Course Code to update: ");
                String updateCode = scanner.nextLine();
                System.out.print("Enter new Title: ");
                String newTitle = scanner.nextLine();
                System.out.print("Enter new Credits: ");
                int newCredits = scanner.nextInt();
                scanner.nextLine();
                System.out.print("Enter new Instructor ID (e.g., I001): ");
                String instructorId = scanner.nextLine();
                System.out.print("Enter new Semester (SPRING, SUMMER, FALL): ");
                String newSemesterStr = scanner.nextLine();
                service.updateCourse(updateCode, newTitle, newCredits, instructorId, newSemesterStr);
                break;
            case 5:
                System.out.print("Enter Course Code to delete: ");
                String deleteCode = scanner.nextLine();
                service.deleteCourse(deleteCode);
                break;
            case 6:
                return;
            default:
                System.out.println("Invalid choice.");
        }
    }

    private static void handleEnrollmentAndGrades(Scanner scanner, EnrollmentService service, StudentService studentService) {
        System.out.println("\n--- Enrollment & Grades ---");
        System.out.println("1. Enroll Student");
        System.out.println("2. Unenroll Student");
        System.out.println("3. Record Grade");
        System.out.println("4. Print Transcript");
        System.out.println("5. Back to Main Menu");
        System.out.print("Enter your choice: ");
        int choice = scanner.nextInt();
        scanner.nextLine();

        switch (choice) {
            case 1:
                System.out.print("Enter Student ID: ");
                String studentId = scanner.nextLine();
                System.out.print("Enter Course Code: ");
                String courseCode = scanner.nextLine();
                try {
                    service.enrollStudent(studentId, courseCode);
                } catch (DuplicateEnrollmentException | MaxCreditLimitExceededException e) {
                    System.out.println("Error: " + e.getMessage());
                }
                break;
            case 2:
                System.out.print("Enter Student ID: ");
                String studentIdUn = scanner.nextLine();
                System.out.print("Enter Course Code: ");
                String courseCodeUn = scanner.nextLine();
                service.unenrollStudent(studentIdUn, courseCodeUn);
                break;
            case 3:
                System.out.print("Enter Student ID: ");
                String studentIdGrade = scanner.nextLine();
                System.out.print("Enter Course Code: ");
                String courseCodeGrade = scanner.nextLine();
                System.out.print("Enter Grade (S, A, B, C, D, E, F): ");
                String gradeStr = scanner.nextLine();
                service.recordGrade(studentIdGrade, courseCodeGrade, gradeStr);
                break;
            case 4:
                System.out.print("Enter Student ID: ");
                String transcriptStudentId = scanner.nextLine();
                List<Student> students = studentService.search(transcriptStudentId);
                if (!students.isEmpty()) {
                    students.get(0).displayProfile();
                } else {
                    System.out.println("Student not found.");
                }
                break;
            case 5:
                return;
            default:
                System.out.println("Invalid choice.");
        }
    }

    private static void handleReports(Scanner scanner, ReportService service) {
        System.out.println("\n--- Reports ---");
        System.out.println("1. Show GPA Distribution");
        System.out.println("2. Back to Main Menu");
        System.out.print("Enter your choice: ");
        int choice = scanner.nextInt();
        scanner.nextLine();
        
        switch (choice) {
            case 1:
                service.showGpaDistribution();
                break;
            case 2:
                return;
            default:
                System.out.println("Invalid choice.");
        }
    }
}
