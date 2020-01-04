package com.rvceresults;

import java.util.ArrayList;
import java.util.List;

class Record
{
    private final String branch;
    private final String usn;
    private final String name;
    private final float sgpa;
    private final List<Course> subjects;
    private int sem;
    private float cgpa;

    Record(String branch, String usn, String name, float sgpa, int sem)
    {
        this.branch = branch;
        this.usn = usn;
        this.name = name;
        this.sgpa = sgpa;
        this.sem = sem;
        this.subjects = new ArrayList<>();
    }

    Record(String branch, String usn, String name, float sgpa, float cgpa, int sem)
    {
        this.branch = branch;
        this.usn = usn;
        this.name = name;
        this.sgpa = sgpa;
        this.sem = sem;
        this.cgpa = cgpa;
        this.subjects = new ArrayList<>();
    }

    String getBranch()
    {
        return branch;
    }

    String getUsn()
    {
        return usn;
    }

    String getName()
    {
        return name;
    }

    float getSgpa()
    {
        return sgpa;
    }

    int getSem()
    {
        return sem;
    }

    void setSem(int sem) {this.sem=sem;}

    void addCourse(Course course)
    {
        subjects.add(course);
    }

    int getCourseLength()
    {
        return subjects.size();
    }

    Course getCourse(int index)
    {
        return subjects.get(index);
    }

    float getCgpa()
    {
        return cgpa;
    }

    public void setCgpa(float cgpa)
    {
        this.cgpa = cgpa;
    }

    List<Course> getSubjects()
    {
        return subjects;
    }
}
