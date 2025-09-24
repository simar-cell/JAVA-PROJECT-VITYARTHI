# CCRM - Campus Course & Records Manager

This document provides a quick guide to using the CCRM console application and explains the format of the data files used for import.

## How to Run

1.  Ensure you have **Java Development Kit (JDK) 17 or newer** installed.
2.  Navigate to the project's root directory in your terminal or command prompt.
3.  Compile the source code: `javac -d out src/**/*.java`
4.  Run the application: `java -cp out edu.ccrm.cli.Main`

## Sample Commands & Workflow

Upon running, the application presents a main menu. Use the number keys to select an option and follow the on-screen prompts.

* **1. Manage Students**: Use this to add, list, update, or deactivate student records.
* **2. Manage Courses**: Use this to add, list, update, or deactivate courses. You can also search for courses by instructor, department, or semester.
* **3. Enrollment & Grading**: Use this to enroll students in courses, unenroll them, or record marks.
* **4. File Operations**: This option allows you to import data from a file, export current data, or create a backup.
* **5. Reports**: Access reports, such as a GPA distribution.
* **6. Exit**: Quits the application.

## Data File Format for Import

The program uses simple, comma-separated text files for importing data. Ensure your files are formatted correctly before importing.

* **Students Import (`students.txt`)**
    Each line represents one student with the following format:
    `regNo,fullName,email`
    **Example:**
    `S101,John Doe,john.doe@example.com`
    `S102,Jane Smith,jane.smith@example.com`

* **Courses Import (`courses.txt`)**
    Each line represents one course with the following format:
    `code,title,credits,instructor,semester,department`
    **Example:**
    `CS101,Intro to Java,3,Dr. Miller,FALL,Computer Science`
    `MATH202,Calculus I,4,Prof. Jones,SPRING,Mathematics`
