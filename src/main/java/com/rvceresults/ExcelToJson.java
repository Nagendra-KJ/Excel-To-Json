package com.rvceresults;

import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.json.simple.*;

import java.io.*;

@SuppressWarnings("ALL")
public class ExcelToJson {
    private String[] fileNames = {"Semester 1.xls", "Semester 2.xls", "Semester 3.xls","Semester 4.xls",
            "Semester 5.xls","Semester 6.xls","Semester 7.xls","Semester 8.xls"};
    private Row headerRow;

    public static void main(String[] args) throws IOException {
        ExcelToJson writer = new ExcelToJson();
        Grabber grab = new RvceGrabber();
        grab.getResult();
        writer.calculateAverage();
        writer.writeToFile();
        System.exit(new ExitStatus().EXIT_ON_COMPLETION);
    }

    private void writeToFile() throws IOException {
        FileWriter fileWriter = new FileWriter(new File(RvceGrabber.getPath(), "dataset.json"));
        getUniversityRecord().writeJSONString(fileWriter);
        fileWriter.close();
    }

    private JSONObject getStudentRecord(Row studentRow) {
        JSONObject record = new JSONObject();
        for (int i = 1; i < headerRow.getPhysicalNumberOfCells(); ++i) {
            Cell dataCell=studentRow.getCell(i);
            if(dataCell==null)
                continue;
            else if (dataCell.getCellType() == CellType.STRING)
                record.put(headerRow.getCell(i).getStringCellValue(), dataCell.getStringCellValue());
            else if (dataCell.getCellType() == CellType.NUMERIC ||
                        dataCell.getCellType()==CellType.FORMULA)
                record.put(headerRow.getCell(i).getStringCellValue(), dataCell.getNumericCellValue());
        }
        return record;
    }

    private JSONObject getDepartmentRecord(Sheet departmentSheet) {
        JSONObject department = new JSONObject();
        headerRow = departmentSheet.getRow(0);
        for (int i = 1; i < departmentSheet.getPhysicalNumberOfRows(); ++i)
            department.put(departmentSheet.getRow(i).getCell(0).getStringCellValue(), getStudentRecord(departmentSheet.getRow(i)));
        return department;
    }

    private JSONObject getBatchRecord(Workbook year) {
        JSONObject batch = new JSONObject();
        for (int i = 0; i < year.getNumberOfSheets(); ++i)
            batch.put(year.getSheetAt(i).getSheetName(), getDepartmentRecord(year.getSheetAt(i)));
        return batch;
    }

    private JSONObject getUniversityRecord() throws IOException {

        JSONObject university = new JSONObject();
        Workbook workbook = null;
        for (int i = 0; i < fileNames.length; ++i) {
            try {
                workbook = WorkbookFactory.create(new File(RvceGrabber.getPath(), fileNames[i]));
                FormulaEvaluator formulaEvaluator = workbook.getCreationHelper().createFormulaEvaluator();
                formulaEvaluator.evaluateAll();
                university.put(fileNames[i].replaceAll(".xls", ""), getBatchRecord(workbook));
            } catch (FileNotFoundException e) {
                continue;
            }
        }
        return university;
    }
    private void calculateAverage() {
        HSSFWorkbook workbook=null;
        FileInputStream fileInputStream;
        FileOutputStream fileOutputStream;
        File excelFile;
        for(int i=0;i<fileNames.length;++i)
        {
            excelFile=new File(RvceGrabber.getPath(),fileNames[i]);
            try{
                fileInputStream=new FileInputStream(excelFile);
                workbook=new HSSFWorkbook(fileInputStream);
                for(int j=0;j<workbook.getNumberOfSheets();++j)
                {
                    HSSFSheet worksheet=(HSSFSheet)workbook.getSheetAt(j);
                    int nRows=worksheet.getPhysicalNumberOfRows();
                    Row avgRow=worksheet.createRow(worksheet.getLastRowNum()+1);
                    avgRow.createCell(0).setCellValue("AVERAGE");
                    avgRow.createCell(1).setCellValue("AVERAGE");
                    Row headerRow=worksheet.getRow(0);
                    Cell cgpaCell=headerRow.getCell(headerRow.getLastCellNum()-2);
                    char cgpaLetter=cgpaCell.getAddress().toString().charAt(0);
                    //char sgpaLetter=(char)(cgpaLetter-1);
                    String formula="";
                    formula="AVERAGE("+cgpaLetter+"2:"+cgpaLetter+(nRows)+")";
                    Cell avgCCell=avgRow.createCell(headerRow.getLastCellNum()-2);
                    avgCCell.setCellType(CellType.FORMULA);
                    avgCCell.setCellFormula(formula);
                    //formula="AVERAGE("+sgpaLetter+"2:"+sgpaLetter+(nRows)+")";
                    //Cell avgSCell=avgRow.createCell(headerRow.getLastCellNum()-3);
                    //avgSCell.setCellFormula(formula);
                }
                fileOutputStream=new FileOutputStream(excelFile);
                workbook.write(fileOutputStream);
                fileOutputStream.close();
                workbook.close();
                fileInputStream.close();
                //FormulaEvaluator formulaEvaluator = workbook.getCreationHelper().createFormulaEvaluator();
                //formulaEvaluator.evaluateAll();
            } catch (IOException e) {
                continue;
            }
        }

    }

}

