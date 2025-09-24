# JAVA-PROJECT-VITYARTHI
# Campus Course & Records Manager (CCRM)

Campus Course & Records Manager (CCRM) is a console-based Java application for managing students, courses, enrollments, grades, backups, and reports for an educational institute. It provides a simple CLI interface to perform common academic management tasks.

## Features

- **Student Management**
  - Add, list, search, update, and delete student records.
- **Course Management**
  - Add, list, search, update, and delete courses.
- **Enrollment & Grades**
  - Enroll or unenroll students in courses.
  - Record grades for enrolled courses.
  - Print transcripts for students.
- **Data Persistence**
  - All data is saved to CSV files and automatically loaded on startup.
  - Enrollment data is maintained separately for integrity.
- **Backup**
  - Easily create timestamped backups of all data files.
- **Reporting**
  - Generate reports, such as GPA distribution among students.

## File Structure

- `CCRM.java`: Main application file containing all logic.
- `data/`: Folder for persistent storage.
  - `students.csv`: Student records.
  - `courses.csv`: Course records.
  - `enrollment.csv`: Enrollment and grade records.
  - `backup_YYYYMMDD_HHmmss/`: Timestamped backup folders.

## Getting Started

1. **Clone the repository**
   ```bash
   git clone https://github.com/simar-cell/JAVA-PROJECT-VITYARTHI.git
   cd JAVA-PROJECT-VITYARTHI
   ```

2. **Compile the Java source**
   ```bash
   javac CCRM.java
   ```

3. **Run the application**
   ```bash
   java CCRM
   ```

The application will automatically create a `data/` folder and necessary CSV files on first run.

## Usage

After launching, you'll see a main menu:

```
--- Main Menu ---
1. Manage Students
2. Manage Courses
3. Manage Enrollment & Grades
4. Import/Export Data
5. Backup Data
6. Reports
7. Exit
Enter your choice:
```

Navigate through the menus to perform management tasks. Data is automatically saved when you exit.

## Data Model Overview

- **Student**: ID, Registration No., Name, Email, Enrolled Courses
- **Course**: Code, Title, Credits, Instructor, Semester
- **Enrollment**: Links Student and Course, includes grade
- **Instructor**: ID, Name, Email (sample instructor provided)

## Extensibility

- The design uses interfaces (`Persistable`, `Searchable`) and custom exceptions for easier future expansion.
- Data is separated by logical entities and backed in CSV for portability.

## Backups

Choose "Backup Data" from the main menu to create a timestamped backup of all CSV files in the `data/` folder. Backups will be stored in subfolders named `backup_YYYYMMDD_HHmmss`.

## Reporting

The "Reports" menu currently provides GPA distribution for all students.

## Contributing

Pull requests and suggestions are welcome! Please open issues for feature requests or bug reports.

## License

This project is provided for educational purposes. See [LICENSE](LICENSE) for details.

## Author

Developed by [simar-cell](https://github.com/simar-cell)
