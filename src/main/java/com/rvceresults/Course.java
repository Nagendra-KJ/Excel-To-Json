package com.rvceresults;
class Course {
    private String name,code,grade;

    String getName() {
        return name;
    }

    void setName(String name) {
        this.name = name;
    }

    Course() {
        this.name = "";
        this.code="";
    }
    /*
    public Course(String name, String code, char grade) {
        this.name = name;
        this.code = code;
        this.grade = grade;
    }
    */

    String getCode() {
        return code;
    }

    void setCode(String code) {
        this.code = code;
    }

    String getGrade() {
        return grade;
    }

    void setGrade(String grade) {
        this.grade = grade;
    }
}
