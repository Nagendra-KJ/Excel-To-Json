package com.rvceresults;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.*;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.Dimension;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class MsritGrabber extends Grabber
{
    private WebElement usnField, captchaField, btnSubmit;
    private String solvedCaptcha;
    private Map<String, String> depMap;
    private Map<String, Integer> courseMap;

    @Override
    void createHeader(Record student)
    {
        /* The first row of each sheet in the Excel file contains the name of each field. This function
        populates that row of each sheet
        The order of the header row is important as it determines where each cell is in future methods.
        We are adding the courses towards the end in MSR because the college allows for variable number of courses to be
        registered in the exam unlike RV.
         */
        Row headerRow = initHeader();
        for (int i = 0; i < student.getCourseLength(); ++i)
        {
            courseMap.put(student.getCourse(i).getCode(), (int) headerRow.getLastCellNum());
            headerRow.createCell(headerRow.getLastCellNum()).
                    setCellValue(student.getCourse(i).getName());
        }
    }

    @Override
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
        dataRow.createCell(dataRow.getLastCellNum()).setCellValue(student.getSgpa());
        Cell gpaCell = dataRow.createCell(dataRow.getLastCellNum());
        gpaCell.setCellValue(student.getCgpa());
        gpaCell.setCellStyle(cellStyle);
        for (int i = 0; i < student.getCourseLength(); ++i)
        {
            Integer courseColumn = courseMap.get(student.getCourse(i).getCode());
            if (courseColumn == null)
            {
                Row headerRow = worksheet.getRow(0);
                courseColumn = (int) headerRow.getLastCellNum();
                courseMap.put(student.getCourse(i).getCode(), (int) headerRow.getLastCellNum());
                headerRow.createCell(headerRow.getLastCellNum())
                        .setCellValue(student.getCourse(i).getName());
            }
            dataRow.createCell(courseColumn).setCellValue(student.getCourse(i).getGrade());
        }
        closeWorkbook();
    }

    private void breakCaptcha() throws IOException
    {
        String resultsUrl = "http://exam.msrit.edu/index.php";
        driver.get(resultsUrl);
        File captchaImg;
        captchaImg = driver.getScreenshotAs(OutputType.FILE);
        setUI(captchaImg);
    }

    private void setUI(File captchaImg) throws IOException
    {
        JPanel mainWindow = new JPanel();
        Box box = Box.createVerticalBox();
        BufferedImage img = ImageIO.read(captchaImg);
        JLabel imgLabel = new JLabel();
        imgLabel.setSize(new Dimension(750, 500));
        ImageIcon imgIcon = new ImageIcon(img);
        Image image = imgIcon.getImage().getScaledInstance(imgLabel.getWidth(), imgLabel.getHeight(), Image.SCALE_SMOOTH);
        imgLabel.setIcon(new ImageIcon(image));
        box.add(imgLabel);


        final JTextField captchaField = new JTextField("Enter the captcha you see in the image here");
        captchaField.addFocusListener(new FocusListener()
        {
            @Override
            public void focusGained(FocusEvent e)
            {
                if (captchaField.getText().equals("Enter the captcha you see in the image here"))
                    captchaField.setText("");
            }

            @Override
            public void focusLost(FocusEvent e)
            {
                if (captchaField.getText().isEmpty())
                    captchaField.setText("Enter the captcha you see in the image here");

            }
        });
        captchaField.setToolTipText("Enter the captcha you see in the image here");
        box.add(captchaField);

        mainWindow.add(box);

        int result = JOptionPane.showConfirmDialog(null, mainWindow,
                "Please enter the captcha you see in the screen", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION)
            solvedCaptcha = captchaField.getText();
        else if (result == JOptionPane.CANCEL_OPTION)
            System.exit(new ExitStatus().EXIT_ON_CANCEL);
    }

    @Override
    void login(String usn)
    {
        if (solvedCaptcha == null)
            try
            {
                breakCaptcha();
                initialisesMaps();
            } catch (IOException e)
            {
                e.printStackTrace();
            }
        if (solvedCaptcha == null)
            return;
        getDomElements();
        usnField.sendKeys(usn);
        captchaField.sendKeys(solvedCaptcha);
        btnSubmit.click();
        if (alertPresent())
        {
            solvedCaptcha = null;
            login(usn);
        }
    }

    private void initialisesMaps()
    {
        depMap = new HashMap<>();
        courseMap = new HashMap<>();
        depMap.put("AT", "Architecture");
        depMap.put("BT", "Biotechnology");
        depMap.put("CH", "Chemical");
        depMap.put("CV", "Civil");
        depMap.put("CS", "Computer Science");
        depMap.put("EC", "Electronics and Communication");
        depMap.put("EI", "Electronics and Instrumentation");
        depMap.put("EE", "Electrical and Electronics");
        depMap.put("IM", "Industrial Engineering");
        depMap.put("IS", "Information Science");
        depMap.put("ME", "Mechanical");
        depMap.put("ML", "Medical Electronics");
        depMap.put("TE", "Telecommunications");
    }

    private boolean alertPresent()
    {
        try
        {
            Alert alert = driver.switchTo().alert();
            alert.dismiss();
            return true;
        } catch (NoAlertPresentException e)
        {
            return false;
        }
    }

    private void getDomElements()
    {
        usnField = driver.findElement(By.id("usn"));
        captchaField = driver.findElement(By.id("osolCatchaTxt0"));
        btnSubmit = driver.findElement(By.className("buttongo"));
    }

    @Override
    Record getStudentDetails()
    {
        String usn = "", branch, name = "";
        float sgpa = 0;
        int sem;
        float cgpa = 0;
        try
        {
            WebElement personalDetails = driver.findElementByClassName("orange");

            Scanner scan = new Scanner(personalDetails.getText());
            scan.useDelimiter("USN :");
            if (scan.hasNext())
                name = scan.next().trim();
            if (scan.hasNext())
                usn = scan.next().trim();
            List<WebElement> academicDetails = driver.findElementsByClassName("box");
            Pattern p = Pattern.compile("([0-9].)([0-9]){1,2}");
            Matcher m = p.matcher(academicDetails.get(2).getText());
            if (m.find())
                sgpa = Float.parseFloat(m.group().trim());
            m = p.matcher(academicDetails.get(3).getText());
            if (m.find())
                cgpa = Float.parseFloat(m.group().trim());
            if (sgpa == 0 && cgpa == 0)
            {
                WebElement footerDetails = driver.findElementByClassName("footertab");
                p = Pattern.compile("([0-9].)([0-9]){1,2}");
                m = p.matcher(footerDetails.getText());
                if (m.find())
                    sgpa = Float.parseFloat(m.group().trim());
            }
            sem = (19 - Integer.parseInt(usn.substring(3, 5))) * 2 - 1;
            branch = depMap.get(usn.substring(5, 7));
            return new Record(branch, usn, name, sgpa, cgpa, sem);
        } catch (NoSuchElementException e)
        {
            return null;
        }
    }

    @Override
    void getCourseDetails(Record student)
    {
        List<WebElement> oddCourses = driver.findElementsByClassName("odd");
        extractCourseDetails(oddCourses, student);
        List<WebElement> evenCourses = driver.findElementsByClassName("even");
        extractCourseDetails(evenCourses, student);
    }

    private void extractCourseDetails(List<WebElement> courseList, Record student)
    {
        Scanner scan;
        for (WebElement singleCourse : courseList)
        {
            scan = new Scanner(singleCourse.getText());
            scan.useDelimiter(" ");
            Course course = new Course();
            while (scan.hasNext())
            {
                String code = scan.next();
                course.setCode(code);
                StringBuilder courseName = new StringBuilder();
                while (!scan.hasNextInt())
                {
                    courseName.append(scan.next());
                    courseName.append(" ");
                }
                course.setName(courseName.toString());
                while (scan.hasNextInt())
                    scan.next();
                if (scan.hasNext())
                    course.setGrade(scan.next());
            }
            student.addCourse(course);
        }

    }

    @Override
    boolean getStudentResult(String usn) throws IOException
    {
        boolean isNotSuccess = true;
        login(usn);
        Record student = getStudentDetails();
        if (student != null)
        {
            setUsnMsg(usn);
            getCourseDetails(student);
            sortCourses(student);
            int sem = getStudentSem(student);
            if (student.getSem() != sem)
                student.setSem(sem);
            writeToFile(student);
            isNotSuccess = false;
        }
        driver.navigate().back();
        return isNotSuccess;
    }

    private void sortCourses(Record student)
    {
        List<Course> subjectList = student.getSubjects();
        Collections.sort(subjectList, new Comparator<Course>()
        {
            @Override
            public int compare(Course course1, Course course2)
            {
                return course1.getCode().compareToIgnoreCase(course2.getCode());
            }
        });
    }

    private int getStudentSem(Record student)
    {
        String code = student.getCourse(student.getCourseLength() - 1).getCode();
        for (int i = 0; i < student.getCourseLength(); ++i)
        {
            if (student.getCourse(i).getCode().contains("L"))
            {
                code = student.getCourse(i).getCode();
                break;
            }
        }
        int sem = 0;
        for (int i = 0; i < code.length(); ++i)
        {
            if (code.charAt(i) > '0' && code.charAt(i) < '9')
            {
                sem = code.charAt(i) - '0';
                break;
            }
        }
        return sem;
    }


    @Override
    void getDepartmentResult(String department, int year) throws IOException
    {
        /*Gets the result of the entire department, cycles through the USNs 000-200. End of department is
        signified when more than 10 continuous USNs do not exist.
         */
        int invalidCount = 0;
        for (int i = 1; i < 300; ++i)
        {
            String usn = "1MS" +
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
    }

    @Override
    void getBatchResult(int year) throws IOException
    {
        //The result of an entire batch is obtained by getting the results of each of batches in the list.
        String[] branches = {"AT", "BT", "CH", "CV", "CS", "EE", "EC", "EI", "IM", "IS", "ME", "ML", "TE"};
        for (String branch : branches)
            getDepartmentResult(branch, year);
    }

    @Override
    void getCollegeResult() throws IOException
    {
        //Gets the result of the entire college by cycling through the results of MIN_BATCH to MAX_BATCH
        int MAX_BATCH = Calendar.getInstance().get(Calendar.YEAR) % 100 - 1;
        int MIN_BATCH = Calendar.getInstance().get(Calendar.YEAR) % 100 - 4;
        for (int i = MIN_BATCH; i <= MAX_BATCH; ++i)
            getBatchResult(i);
    }
}
