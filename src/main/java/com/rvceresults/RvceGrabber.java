/* The following is a program that gets the results of the entire college and stores it in Excel files.
Apache POI package is used for writing to an Excel File. All the students results which are scraped
of the website is stored in Excel 1997-2007 Excel file format.

The Selenium webdriver package is used to scrape the results off the webpage. We use xPath and other field
tags to get the required web elements.
 */
package com.rvceresults;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.io.IOException;
import java.util.Calendar;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


class RvceGrabber extends Grabber
{
    void login(String usn)
    {
        /*For each student, the driver opens the results.rvce.edu.in webpage, enters the USN, gets the
        captcha text from the page, solves it and enters it into the field and then hits submit.
        If the student exists, his results page is opened, otherwise, an empty page is opened
         */
        String resultsURL = "http://results.rvce.edu.in";
        driver.get(resultsURL);
        WebElement usnField = driver.findElement(By.name("usn"));
        WebElement captchaField = driver.findElement(By.name("captcha"));
        WebElement captchaText = driver.findElement(By.xpath("//label[contains(text(),'What is')]"));
        Pattern captchaPattern = Pattern.compile("[0-9]");
        Matcher match = captchaPattern.matcher(captchaText.getText());
        int num1 = 0, num2 = 0;
        if (match.find())
            num1 = Integer.parseInt(match.group());
        if (match.find())
            num2 = Integer.parseInt(match.group());
        usnField.sendKeys(usn);
        captchaField.sendKeys(Integer.toString(num1 + num2));
        WebElement buttonSubmit = driver.findElement(By.id("submit"));
        buttonSubmit.click();

    }

    Record getStudentDetails()
    {
        /*The result of the specified student is got by getting the results table from the page and then
        extracting each field by using regEx. This data is stored in a records object that contains all
        the necessary fields of the student. This record is then written to the Excel file.
         */
        String usn = "", branch = "", name = "";
        float gpa = 0;
        int sem = 0;
        List<WebElement> details = driver.findElements(By.xpath("//*[@id=\"no-more-tables\"]/table[1]/tbody/tr[1]"));
        try
        {
            String data = details.get(0).getText();
            Pattern p = Pattern.compile("[1-8]RV1[5-8][A-Z][A-Z]([0-9]){3}");
            Matcher m = p.matcher(data);
            if (m.find())
                usn = m.group().trim();
            p = Pattern.compile("([A-Z]* )+");
            m = p.matcher(data);
            if (m.find())
                branch = m.group().replaceAll("ENGINEERING", "").trim();
            if (m.find())
                name = m.group().trim();
            p = Pattern.compile("[0-9][0-9]?((\\.)[0-9][0-9]?)?$");
            m = p.matcher(data);
            if (m.find())
                gpa = Float.parseFloat(m.group().trim());
            p = Pattern.compile(" [1-8] ");
            m = p.matcher(data);
            if (m.find())
                sem = Integer.parseInt(m.group().trim());
            return new Record(branch, usn, name, gpa, sem);
        } catch (IndexOutOfBoundsException e)
        {
            return null;
        }
    }

    void getCourseDetails(Record student)
    {
        /*Each course detail of the specified student is got from the webpage by using xPath and stored in the
        same record of the student. All this is finally written to the Excel file. The do-while loop goes through
        each row of the table and gets the details of the student.
         */
        int i = 0;
        List<WebElement> table;
        do
        {
            Course course = new Course();
            ++i;
            table = driver.findElements(By.xpath("//*[@id=\"no-more-tables\"]/table[2]/tbody/tr[" + i + "]"));
            try
            {
                String courseData = table.get(0).getText();
                Pattern p = Pattern.compile("\\d{2}([A-Z]){2,3}([0-9]){1,2}([A-Z]){0,2}");
                Matcher m = p.matcher(courseData);
                if (m.find())
                    course.setCode(m.group().trim());
                p = Pattern.compile("[A-Z-0-9]+ ");
                m = p.matcher(courseData);
                if (m.find())
                {
                    while (m.find())
                        course.setName(course.getName() + m.group());
                }
                course.setName(course.getName().trim());
                p = Pattern.compile("[A-Z]$");
                m = p.matcher(courseData);
                if (m.find())
                {
                    course.setGrade(m.group());
                    student.addCourse(course);
                }
            } catch (IndexOutOfBoundsException e)
            {
                break;
            }
        } while (!table.get(0).getText().equals("Go Back"));
    }

    boolean getStudentResult(String usn) throws IOException
    {
        //Gets all the details of a student with the given USN. Returns false if the student does not exist.
        boolean isNotSuccess = true;
        login(usn);
        Record student = getStudentDetails();
        if (student != null)
        {
            setUsnMsg(usn);
            getCourseDetails(student);
            writeToFile(student);
            isNotSuccess = false;
        }
        return isNotSuccess;
    }

    void getDepartmentResult(String department, int year) throws IOException
    {
        /*Gets the result of the entire department, cycles through the USNs 000-200. End of department is
        signified when more than 10 continuous USNs do not exist.
         */
        int invalidCount = 0;
        for (int i = 1; i < 200; ++i)
        {
            String usn = "1RV" +
                    year +
                    department +
                    String.format("%03d", i);
            if (getStudentResult(usn))
                ++invalidCount;
            else
                invalidCount = 0;
            if (invalidCount >= 10)
            {
                if (usn.contains("010"))
                    this.setUsnMsg("Department Results not yet announced for " + department);
                break;
            }
        }
        if (year < currentDiplomaYear())
            getDiplomaResult(department, year + 1);
    }

    void getBatchResult(int year) throws IOException
    {
        //The result of an entire batch is obtained by getting the results of each of batches in the list.
        String[] branches = {"AS", "BT", "CH", "CV", "CS", "EE", "EC", "EI", "IM", "IS", "ME", "TE"};
        for (String branch : branches)
            getDepartmentResult(branch, year);
    }

    void getCollegeResult() throws IOException
    {
        //Gets the result of the entire college by cycling through the results of MIN_BATCH to MAX_BATCH
        int MAX_BATCH = Calendar.getInstance().get(Calendar.YEAR) % 100 - 1;
        int MIN_BATCH = Calendar.getInstance().get(Calendar.YEAR) % 100 - 4;
        for (int i = MIN_BATCH; i <= MAX_BATCH; ++i)
            getBatchResult(i);
    }

    void writeToFile(Record student) throws IOException
    {
        /*HSSFWorkbook is the format of Excel 1997-2007 Workbooks. Each workbook contains the results of
        that semester of all the branches. Each sheet of the work book contains the results of that branch
        If the workbook, or any worksheet is already not present, it is created. Each row contains the
        results of a particular student. The first row of every worksheet called header row contains the
        field names for those specific columns.
         */
        openWorkbook(student);
        Row dataRow = worksheet.createRow(worksheet.getLastRowNum() + 1);
        dataRow.createCell(0).setCellValue(student.getUsn());
        dataRow.createCell(1).setCellValue(student.getName());
        for (int i = 0; i < student.getCourseLength(); ++i)
            dataRow.createCell(i + 2).setCellValue(student.getCourse(i).getGrade());
        Cell gpaCell = dataRow.createCell(dataRow.getLastCellNum());
        gpaCell.setCellValue(student.getSgpa());
        gpaCell.setCellStyle(cellStyle);
        dataRow.createCell(dataRow.getLastCellNum()).setCellValue(student.getSgpa());
        closeWorkbook(gpaCell,dataRow);
    }

    private void getDiplomaResult(String department, int year) throws IOException
    {
        /*Gets the result of the diploma students in the department, cycles through the USNs 400-500. End of department is
        signified when more than 10 continuous USNs do not exist.
         */
        int invalidCount = 0;
        for (int i = 400; i < 500; ++i)
        {
            String usn = "1RV" +
                    year +
                    department +
                    String.format("%03d", i);
            if (getStudentResult(usn))
                ++invalidCount;
            else invalidCount = 0;
            if (invalidCount >= 10)
            {
                if (usn.contains("010"))
                    this.setUsnMsg("No diploma students in  " + department);
                break;
            }
        }
    }

    private int currentDiplomaYear()
    {
        return Calendar.getInstance().get(Calendar.YEAR) % 100 - 1;
    }
}
