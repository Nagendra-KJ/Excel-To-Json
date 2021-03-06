package com.rvceresults;

import com.google.common.collect.HashBiMap;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.json.JSONObject;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.Calendar;

abstract class Grabber implements ActionListener
{
    private final String[] fileNames = {"Semester 1.xls", "Semester 2.xls", "Semester 3.xls", "Semester 4.xls",
            "Semester 5.xls", "Semester 6.xls", "Semester 7.xls", "Semester 8.xls"}; //Final list of filenames present in the folder
    private final HashBiMap<String, Integer> gradeLookUp = HashBiMap.create(); //Bi directional lookup table for grade->number conversion
    FirefoxDriver driver; //Common driver passed around between classes
    HSSFCellStyle cellStyle;
    Sheet worksheet;//WorkSheet is passed among different classes to remove duplicate code.
    private String path;
    private File excelFile;
    private String collegeName;
    private JLabel usnMsg;
    private JFrame mainWindow;
    private HSSFWorkbook workbook;//Workbook is limited to this class but cannot be made local as then WorkSheet would
                                  //become local

    final void getResult() throws IOException
    {
        initialise();
        getCollegeResult();
        calculateRank();
        calculateAverage();
        writeToJSONFile();
        usnMsg.setText("Program will now exit");
        mainWindow.dispose();
    }

    abstract void writeToFile(Record student) throws IOException;

    private void initialise()
    {
        //Sets up the webdriver to run without opening the window or without using any gpu facilities
        collegeName = "RVCE";
        if (this.getClass().getSimpleName().equals("MsritGrabber"))
            collegeName = "MSRIT";
        setPath();
        if (path == null)
        {
            System.out.println("Issue with setting the path");
            driver.close();
            System.exit(new ExitStatus().EXIT_WITHOUT_PATH);
        }
        setUI();
        FirefoxOptions options = new FirefoxOptions();
        options.addArguments("--headless");
        options.addArguments("--disable-gpu");
        driver = new FirefoxDriver(options);
        initGradeLookUp();
    }

    private void setUI()
    {
        //The layout is that of a GridBag inside a JPanel Container. The constraints help to position the objects
        mainWindow = new JFrame("Result Grabber");
        Container mainLayout = mainWindow.getContentPane();
        mainLayout.setLayout(new GridBagLayout());
        GridBagConstraints constr = new GridBagConstraints();
        constr.fill = GridBagConstraints.HORIZONTAL;

        /*Cancel button being added incase the user wants to cancel midway.
            It is added to the middle of the bottom row.
         */
        JButton btnCancel = new JButton("Cancel");
        constr.anchor = GridBagConstraints.PAGE_END;
        constr.gridx = 2;
        constr.gridy = 2;
        constr.gridwidth = 1;
        btnCancel.addActionListener(this);
        mainLayout.add(btnCancel, constr);

        //This label shows the main message in the window. The USN will be shown below this

        JLabel mainMsg = new JLabel("Please note that the full operation will take time.");
        constr.fill = GridBagConstraints.HORIZONTAL;
        constr.anchor = GridBagConstraints.PAGE_START;
        constr.gridx = 0;
        constr.gridy = 0;
        constr.gridwidth = 3;
        mainLayout.add(mainMsg, constr);

        //This Label shows the current USN whose result is being downloaded

        usnMsg = new JLabel("Initialising");
        constr.fill = GridBagConstraints.HORIZONTAL;
        constr.gridx = 1;
        constr.gridy = 1;
        constr.gridwidth = 3;
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

    void createHeader(Record student)
    {
        /* The first row of each sheet in the Excel file contains the name of each field. This function
        populates that row of each sheet
         */
        Row headerRow=initHeader();
        for (int i = 0; i < student.getCourseLength(); ++i)
            headerRow.createCell(headerRow.getLastCellNum()).setCellValue(student.getCourse(i).getName() + "-" + student.getCourse(i).getCode());

    }

    abstract boolean getStudentResult(String usn) throws IOException;

    abstract void getDepartmentResult(String department, int year) throws IOException;

    abstract void getBatchResult(int year) throws IOException;

    private void getCollegeResult() throws IOException
    {
        //Gets the result of the entire college by cycling through the results of MIN_BATCH to MAX_BATCH
        int MAX_BATCH = Calendar.getInstance().get(Calendar.YEAR) % 100 - 1;
        int MIN_BATCH = Calendar.getInstance().get(Calendar.YEAR) % 100 - 4;
        for (int i = MIN_BATCH; i <= MAX_BATCH; ++i)
            getBatchResult(i);
    }

    private void setPath()
    {
        JFileChooser fileChooser = new JFileChooser();
        File file = new File("Results");
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION)
            file = fileChooser.getSelectedFile();
        else
            System.exit(new ExitStatus().EXIT_WITHOUT_PATH);
        path = file.getAbsolutePath();
        path = path + File.separator + collegeName;
        File directory = new File(path);
        if(!directory.exists())
            if (!directory.mkdir())
                System.out.println("Folder creation issue");
        path = directory.getAbsolutePath();
        path = path + File.separator;
        try
        {
            File pathFile = new File("path.dat");
            if(!pathFile.exists())
                if(!pathFile.createNewFile())
                    System.out.println("Error in creating path file");
            FileWriter fileWriter = new FileWriter(pathFile);
            fileWriter.write(path);
            fileWriter.close();
        } catch (IOException e)
        {
            e.printStackTrace();
        }

    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        driver.close();
        String[] fileNames = {"Semester 1.xls", "Semester 2.xls", "Semester 3.xls", "Semester 4.xls",
                "Semester 5.xls", "Semester 6.xls", "Semester 7.xls", "Semester 8.xls"};
        for (String fileName : fileNames)
        {
            File deleteFile = new File(path, fileName);
            if (deleteFile.exists())
                if (!deleteFile.delete())
                    setUsnMsg("Unable to delete file " + deleteFile.getName());
        }
        Path folderPath = Paths.get(path).getParent();
        File deleteFolder = new File(folderPath.toString(), collegeName);
        if (deleteFolder.exists())
            if (!deleteFolder.delete())
                System.out.println("Unable to delete folder" + deleteFolder.getName());
        System.exit(new ExitStatus().EXIT_ON_CANCEL);
    }

    private void calculateGPAAverage()
    {
        for (String fileName : fileNames)
        {
            File excelFile = new File(path, fileName);
            try
            {
                FileInputStream fileInputStream = new FileInputStream(excelFile);
                HSSFWorkbook workbook = new HSSFWorkbook(fileInputStream);
                for (int j = 0; j < workbook.getNumberOfSheets(); ++j)
                {
                    HSSFSheet worksheet = workbook.getSheetAt(j);
                    int nRows = worksheet.getPhysicalNumberOfRows();
                    Row avgRow = worksheet.getRow(worksheet.getLastRowNum());
                    if (avgRow.getCell(0).toString().equals("AVERAGE"))
                        continue;
                    avgRow = worksheet.createRow(worksheet.getLastRowNum()+1);
                    avgRow.createCell(0).setCellValue("AVERAGE");
                    avgRow.createCell(1).setCellValue("AVERAGE");
                    String formula;
                    formula = "AVERAGE(D" + "2:D" + (nRows) + ")";
                    Cell avgCCell = avgRow.createCell(3);
                    avgCCell.setCellType(CellType.FORMULA);
                    avgCCell.setCellFormula(formula);
                    cellStyle = workbook.createCellStyle();
                    cellStyle.setDataFormat(workbook.createDataFormat().getFormat("#.00"));
                    avgCCell.setCellStyle(cellStyle);
                }
                FileOutputStream fileOutputStream = new FileOutputStream(excelFile);
                workbook.write(fileOutputStream);
                fileOutputStream.close();
                workbook.close();
                fileInputStream.close();
            } catch (IOException ignored)
            {
            }
        }

    }

    private void writeToJSONFile() throws IOException
    {
        FileWriter fileWriter = new FileWriter(new File(path, "dataset.json"));
        getUniversityRecord().write(fileWriter);
        fileWriter.close();
    }

    private JSONObject getStudentRecord(Row studentRow,Row headerRow)
    {
        JSONObject record = new JSONObject();
        DecimalFormat formatter = new DecimalFormat("#.##");
        for (int i = 1; i < headerRow.getPhysicalNumberOfCells(); ++i)
        {
            Cell dataCell = studentRow.getCell(i);
            if (dataCell != null)
            {
                if (dataCell.getCellType() == CellType.STRING)
                    record.put(headerRow.getCell(i).getStringCellValue(), dataCell.getStringCellValue());
                else if (dataCell.getCellType() == CellType.NUMERIC ||
                        dataCell.getCellType() == CellType.FORMULA)
                    record.put(headerRow.getCell(i).getStringCellValue(), formatter.format(dataCell.getNumericCellValue()));
            }
        }
        return record;
    }

    private JSONObject getDepartmentRecord(Sheet departmentSheet)
    {
        JSONObject department = new JSONObject();
        Row headerRow = departmentSheet.getRow(0);
        for (int i = 1; i < departmentSheet.getPhysicalNumberOfRows(); ++i)
            try
            {
                department.put(departmentSheet.getRow(i).getCell(0).getStringCellValue(), getStudentRecord(departmentSheet.getRow(i),headerRow));
            }catch(NullPointerException ignored)
            {}
        return department;
    }

    private JSONObject getBatchRecord(Workbook year)
    {
        JSONObject batch = new JSONObject();
        for (int i = 0; i < year.getNumberOfSheets(); ++i)
            batch.put(year.getSheetAt(i).getSheetName(), getDepartmentRecord(year.getSheetAt(i)));
        return batch;
    }

    private JSONObject getUniversityRecord() throws IOException
    {

        JSONObject university = new JSONObject();
        Workbook workbook;
        for (String fileName : fileNames)
        {
            try
            {
                workbook = WorkbookFactory.create(new File(path, fileName));
                FormulaEvaluator formulaEvaluator = workbook.getCreationHelper().createFormulaEvaluator();
                formulaEvaluator.evaluateAll();
                university.put(fileName.replaceAll(".xls", ""), getBatchRecord(workbook));
            } catch (FileNotFoundException ignored)
            {
            }
        }
        return university;
    }

    private void calculateGradeAverage()
    {
        HSSFWorkbook workbook;
        FileInputStream fileInputStream;
        FileOutputStream fileOutputStream;
        File excelFile;
        for (String fileName : fileNames)
        {
            excelFile = new File(path, fileName);
            try
            {
                fileInputStream = new FileInputStream(excelFile);
                workbook = new HSSFWorkbook(fileInputStream);
                HSSFSheet worksheet;
                for (int sheetNum = 0; sheetNum < workbook.getNumberOfSheets(); ++sheetNum)
                {
                    worksheet = workbook.getSheetAt(sheetNum);
                    Row avgRow = worksheet.getRow(worksheet.getLastRowNum());
                    Row headerRow = worksheet.getRow(0);
                    int startingColumn = 5;
                    int endingColumn = headerRow.getCell(headerRow.getLastCellNum()-1).getAddress().toString().charAt(0) - 'A';
                    for (int i = startingColumn; i <= endingColumn; ++i)
                        getAverageGrade(i, avgRow, worksheet);
                }
                fileOutputStream = new FileOutputStream(excelFile);
                workbook.write(fileOutputStream);
                fileOutputStream.close();
                workbook.close();
                fileInputStream.close();
            } catch (IOException ignored)
            {
            }
        }
    }

    private void calculateAverage()
    {
        calculateGPAAverage();
        calculateGradeAverage();
    }

    private void getAverageGrade(int cNum, Row avgRow, Sheet worksheet)
    {
        int sum = 0;
        int numStudents=worksheet.getPhysicalNumberOfRows()-2;
        for (int i = 1; i < worksheet.getLastRowNum(); ++i)
        {
            Row currentRow = worksheet.getRow(i);
            if(currentRow==null)
                continue;
            String grade = currentRow.getCell(cNum).getStringCellValue();
            Integer gradeWeight = gradeLookUp.get(grade);
            if (gradeWeight == null)
                gradeWeight = gradeLookUp.get("F");
            assert gradeWeight != null;
            sum += gradeWeight;
        }
        int average = Math.round((float) sum / numStudents);
        String averageGrade = gradeLookUp.inverse().get(average);
        if (avgRow.getCell(cNum) == null)
            avgRow.createCell(cNum);
        avgRow.getCell(cNum).setCellValue(averageGrade);
    }

    private void initGradeLookUp()
    {
        gradeLookUp.put("S", 10);
        gradeLookUp.put("A", 9);
        gradeLookUp.put("B", 8);
        gradeLookUp.put("C", 7);
        gradeLookUp.put("D", 6);
        gradeLookUp.put("E", 5);
        gradeLookUp.put("F", 4);
    }

    void openWorkbook(Record student) throws IOException
    {
        String filepath = path + "Semester " + student.getSem() + ".xls";
        excelFile = new File(filepath);
        if (!excelFile.exists())
        {
            if (!excelFile.createNewFile())
                return ;
            workbook = new HSSFWorkbook();
            FileOutputStream fileOutputStream = new FileOutputStream(excelFile);
            workbook.write(fileOutputStream);
            fileOutputStream.close();
        } else
        {
            FileInputStream fileInputStream = new FileInputStream(excelFile);
            workbook = new HSSFWorkbook(fileInputStream);
            fileInputStream.close();
        }
        worksheet = workbook.getSheet(student.getBranch());
        if (worksheet == null)
        {
            worksheet = workbook.createSheet(student.getBranch());
            createHeader(student);
        }
        cellStyle = workbook.createCellStyle();
        cellStyle.setDataFormat(workbook.createDataFormat().getFormat("0.00"));

    }

    void closeWorkbook() throws IOException
    {
        FileOutputStream fileOutputStream = new FileOutputStream(excelFile);
        workbook.write(fileOutputStream);
        fileOutputStream.close();
        workbook.close();
    }

    Row initHeader()
    {
        Row headerRow = worksheet.createRow(0);
        headerRow.createCell(0).setCellValue("USN");
        headerRow.createCell(headerRow.getLastCellNum()).setCellValue("Name");
        headerRow.createCell(headerRow.getLastCellNum()).setCellValue("SGPA");
        headerRow.createCell(headerRow.getLastCellNum()).setCellValue("CGPA");
        headerRow.createCell(headerRow.getLastCellNum()).setCellValue("Rank");
        return headerRow;
    }

    private void calculateRank()
    {
        for (String fileName : fileNames)
        {
            File excelFile = new File(path, fileName);
            try
            {
                FileInputStream fileInputStream = new FileInputStream(excelFile);
                HSSFWorkbook workbook = new HSSFWorkbook(fileInputStream);
                for (int j = 0; j < workbook.getNumberOfSheets(); ++j)
                {
                    HSSFSheet worksheet = workbook.getSheetAt(j);
                    int numStudents=worksheet.getPhysicalNumberOfRows()-1;
                    for (int i = 1; i <=numStudents; ++i)
                    {
                        Row dataRow = worksheet.getRow(i);
                        Cell gpaCell = dataRow.getCell(3);
                        Cell rankCell = dataRow.createCell(4);
                        String rankFormula = "RANK($" + gpaCell.getAddress().toString() +
                                ",$D" +  "$2:$D$"  + (numStudents+1) +")";
                        rankCell.setCellFormula(rankFormula);
                    }
                }
                FileOutputStream fileOutputStream=new FileOutputStream(excelFile);
                workbook.write(fileOutputStream);
                fileOutputStream.close();
                fileInputStream.close();
            } catch (IOException ignored)
            {
            }
        }
    }

}
