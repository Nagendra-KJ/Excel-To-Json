package com.rvceresults;
import java.util.ArrayList;
import java.util.List;
public class Record {
    private String branch,usn,name;
    private float sgpa;
    private int sem;
    private List<Course> subjects;
    private float cgpa;
    Record(String branch, String usn, String name, float sgpa, int sem) {
        this.branch = branch;
        this.usn = usn;
        this.name = name;
        this.sgpa = sgpa;
        this.sem = sem;
        this.subjects=new ArrayList<>();
    }
    Record(String branch,String usn,String name,float sgpa,float cgpa,int sem) {
        this.branch = branch;
        this.usn = usn;
        this.name = name;
        this.sgpa = sgpa;
        this.sem = sem;
        this.cgpa=cgpa;
        this.subjects=new ArrayList<>();
    }

    String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    String getUsn() {
        return usn;
    }

    public void setUsn(String usn) {
        this.usn = usn;
    }

    String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    float getSgpa() {
        return sgpa;
    }

    public void setSgpa(float sgpa) {
        this.sgpa = sgpa;
    }

    int getSem() {
        return sem;
    }

    void setSem(int sem) {
        this.sem = sem;
    }

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

    float getCgpa() {
        return cgpa;
    }

    public void setCgpa(float cgpa) {
        this.cgpa = cgpa;
    }

    List<Course> getSubjects() {
        return subjects;
    }

    public void setSubjects(List<Course> subjects) {
        this.subjects = subjects;
    }
}
