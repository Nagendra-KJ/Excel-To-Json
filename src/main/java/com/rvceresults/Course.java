package com.rvceresults;

class Course
{
    private String name, code, grade;

    Course()
    {
        this.name = "";
        this.code = "";
    }

    String getName()
    {
        return name;
    }

    void setName(String name)
    {
        this.name = name;
    }

    String getCode()
    {
        return code;
    }

    void setCode(String code)
    {
        this.code = code;
    }

    String getGrade()
    {
        return grade;
    }

    void setGrade(String grade)
    {
        this.grade = grade;
    }
}
