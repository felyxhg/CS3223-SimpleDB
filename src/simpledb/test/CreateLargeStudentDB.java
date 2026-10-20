package simpledb.test;

import java.util.Random;

import simpledb.plan.Planner;
import simpledb.server.SimpleDB;
import simpledb.tx.Transaction;

/**
 * Creates the database studentdb_large for the experiments.
 * It has the same schema as the student database, but contains
 * 10 departments, 50 courses, 200 sections, 1000 students
 * and 5000 enrollments. The values are generated with a fixed
 * random seed, so the data is the same on every run.
 * The indexes allow every join of the experiment queries
 * to be executed as index join.
 */
public class CreateLargeStudentDB {
    private static final int NUM_DEPTS = 10;
    private static final int NUM_COURSES = 50;
    private static final int NUM_SECTIONS = 200;
    private static final int NUM_STUDENTS = 1000;
    private static final int NUM_ENROLLS = 5000;

    public static void main(String[] args) {
        SimpleDB db = new SimpleDB("studentdb_large");
        Transaction tx = db.newTx();
        Planner planner = db.planner();
        Random rnd = new Random(3223); // fixed seed, so the data is the same on every run
        String[] grades = {"A", "A-", "B+", "B", "B-", "C", "D", "F"};
        String[] profs = {"turing", "newton", "einstein", "brando", "curie", "gauss", "euler", "lovelace"};

        // the indexes have to be created before the records are inserted
        planner.executeUpdate("create table STUDENT(SId int, SName varchar(10), MajorId int, GradYear int)", tx);
        planner.executeUpdate("create index StudentMajorId on student (MajorId) using hash", tx);
        planner.executeUpdate("create index StudentSId on student (SId) using btree", tx);
        planner.executeUpdate("create table DEPT(DId int, DName varchar(8))", tx);
        planner.executeUpdate("create table COURSE(CId int, Title varchar(20), DeptId int)", tx);
        planner.executeUpdate("create table SECTION(SectId int, CourseId int, Prof varchar(8), YearOffered int)", tx);
        planner.executeUpdate("create index SectionCourseId on section (CourseId) using hash", tx);
        planner.executeUpdate("create table ENROLL(EId int, StudentId int, SectionId int, Grade varchar(2))", tx);
        planner.executeUpdate("create index EnrollStudentId on enroll (StudentId) using btree", tx);
        planner.executeUpdate("create index EnrollSectionId on enroll (SectionId) using btree", tx);

        for (int i = 1; i <= NUM_DEPTS; i++)
            planner.executeUpdate("insert into DEPT(DId, DName) values (" + i + ", 'dept" + i + "')", tx);
        for (int i = 1; i <= NUM_COURSES; i++)
            planner.executeUpdate("insert into COURSE(CId, Title, DeptId) values (" + i + ", 'course" + i + "', "
                    + (1 + rnd.nextInt(NUM_DEPTS)) + ")", tx);
        for (int i = 1; i <= NUM_SECTIONS; i++)
            planner.executeUpdate("insert into SECTION(SectId, CourseId, Prof, YearOffered) values (" + i + ", "
                    + (1 + rnd.nextInt(NUM_COURSES)) + ", '" + profs[rnd.nextInt(profs.length)] + "', "
                    + (2018 + rnd.nextInt(6)) + ")", tx);
        for (int i = 1; i <= NUM_STUDENTS; i++)
            planner.executeUpdate("insert into STUDENT(SId, SName, MajorId, GradYear) values (" + i + ", 's" + i + "', "
                    + (1 + rnd.nextInt(NUM_DEPTS)) + ", " + (2019 + rnd.nextInt(6)) + ")", tx);
        for (int i = 1; i <= NUM_ENROLLS; i++)
            planner.executeUpdate("insert into ENROLL(EId, StudentId, SectionId, Grade) values (" + i + ", "
                    + (1 + rnd.nextInt(NUM_STUDENTS)) + ", " + (1 + rnd.nextInt(NUM_SECTIONS)) + ", '"
                    + grades[rnd.nextInt(grades.length)] + "')", tx);

        tx.commit();
        System.out.println("studentdb_large created");
    }
}