package com.rvceresults;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;

abstract class Grabber implements ActionListener {
    private static String path;
    FirefoxDriver driver;
    private JLabel usnMsg;
    final void getResult() throws IOException {
        initialise();
        getCollegeResult();
        driver.close();
    }
    abstract  void writeToFile(Record student) throws IOException;
    private void initialise() {
        //Sets up the webdriver to run without opening the window or without using any gpu facilities
        setPath();
        setUI();
        FirefoxOptions options = new FirefoxOptions();
        options.addArguments("--headless");
        options.addArguments("--disable-gpu");
        driver = new FirefoxDriver(options);
    }

    private void setUI() {
        //The layout is that of a GridBag inside a JPanel Container. The constraints help to position the objects
        JFrame mainWindow = new JFrame("Result Grabber");
        Container mainLayout = mainWindow.getContentPane();
        mainLayout.setLayout(new GridBagLayout());
        GridBagConstraints constr = new GridBagConstraints();
        constr.fill=GridBagConstraints.HORIZONTAL;

        /*Cancel button being added incase the user wants to cancel midway.
            It is added to the middle of the bottom row.
         */
        JButton btnCancel = new JButton("Cancel");
        constr.anchor=GridBagConstraints.PAGE_END;
        constr.gridx=2;
        constr.gridy=2;
        constr.gridwidth=1;
        btnCancel.addActionListener(this);
        mainLayout.add(btnCancel, constr);

        //This label shows the main message in the window. The USN will be shown below this

        JLabel mainMsg = new JLabel("Please note that the full operation will take time.");
        constr.fill=GridBagConstraints.HORIZONTAL;
        constr.anchor=GridBagConstraints.PAGE_START;
        constr.gridx=0;
        constr.gridy=0;
        constr.gridwidth=3;
        mainLayout.add(mainMsg, constr);

        //This Label shows the current USN whose result is being downloaded

        usnMsg=new JLabel("Initialising");
        constr.fill=GridBagConstraints.HORIZONTAL;
        constr.gridx=1;
        constr.gridy=1;
        constr.gridwidth=3;
        mainLayout.add(usnMsg, constr);


        //Setting window properties and setting it up
        mainWindow.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainWindow.pack();
        mainWindow.setLocationRelativeTo(null);
        mainWindow.setVisible(true);
    }
    void setUsnMsg(String usn)
    {
        this.usnMsg.setText(usn);
    }

    abstract void login(String usn);

    abstract Record getStudentDetails();

    abstract void getCourseDetails(Record student);

    void createHeader(Sheet worksheet, Record student) {
        /* The first row of each sheet in the Excel file contains the name of each field. This function
        populates that row of each sheet
         */
        Row headerRow = worksheet.createRow(0);
        headerRow.createCell(0).setCellValue("USN");
        headerRow.createCell(headerRow.getLastCellNum()).setCellValue("Name");
        for (int i = 0; i < student.getCourseLength(); ++i)
            headerRow.createCell(headerRow.getLastCellNum()).setCellValue(student.getCourse(i).getName() + "-" + student.getCourse(i).getCode());
        headerRow.createCell(headerRow.getLastCellNum()).setCellValue("SGPA");
        headerRow.createCell(headerRow.getLastCellNum()).setCellValue("CGPA");
        headerRow.createCell(headerRow.getLastCellNum()).setCellValue("Rank");
    }

    abstract boolean getStudentResult(String usn) throws IOException;

    abstract void getDepartmentResult(String department, int year) throws IOException;

    abstract void getBatchResult(int year) throws IOException;

    abstract void getCollegeResult() throws IOException;

    private void setPath() {
        JFileChooser fileChooser = new JFileChooser();
        File file = new File("Results");
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION)
            file=fileChooser.getSelectedFile();
        else
            System.exit(new ExitStatus().EXIT_WITHOUT_PATH);
        path = file.getAbsolutePath();
        path = path + "\\";
    }

    static String getPath() {
        return path;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        driver.close();
        String[] fileNames = {"Semester 1.xls", "Semester 2.xls", "Semester 3.xls","Semester 4.xls",
                "Semester 5.xls","Semester 6.xls","Semester 7.xls","Semester 8.xls"};        for (String fileName : fileNames) {
            File deleteFile = new File(path, fileName);
            if (deleteFile.exists())
            if(!deleteFile.delete())
                setUsnMsg("Unable to delete file "+deleteFile.getName());

        }
        System.exit(new ExitStatus().EXIT_ON_CANCEL);
    }
}
