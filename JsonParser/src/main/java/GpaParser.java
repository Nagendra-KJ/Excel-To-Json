import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;

public class GpaParser
{
    public static void main(String[] args)
    {
        /*
            Read the path where the files were saved in the Grabber module and create a gpas.json file in the same place.
            Once done, use the same JSON object logic to make the required JSON file that needs be uploaded to the database.
         */
        File pathFile=new File("path.dat");
        try
        {
            FileInputStream fileInputStream = new FileInputStream(pathFile);
            int letter;
            StringBuilder path= new StringBuilder();
            while((letter=fileInputStream.read())!=-1)
                path.append((char) letter);
            FileWriter fileWriter = new FileWriter(new File(path.toString(),"gpas.json"));
            getUniversityRecord(path.toString()).write(fileWriter);
            fileWriter.close();
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }
    private static JSONObject getUniversityRecord(String path)
    {
        final String[] fileNames = {"Semester 1.xls", "Semester 2.xls", "Semester 3.xls", "Semester 4.xls",
                "Semester 5.xls", "Semester 6.xls", "Semester 7.xls", "Semester 8.xls"};
        JSONObject university=new JSONObject();
        Workbook workbook;
        for(String fileName:fileNames)
        {
            try
            {
                workbook= WorkbookFactory.create(new File(path,fileName));
                university.put(fileName.replaceAll(".xls",""),getBatchRecord(workbook));
            } catch (IOException ignored)
            {}
        }
        return university;
    }
    private static JSONObject getBatchRecord(Workbook year)
    {
        JSONObject batch=new JSONObject();
        for(int i=0;i<year.getNumberOfSheets();++i)
            batch.put(year.getSheetAt(i).getSheetName(),getDepartmentRecord(year.getSheetAt(i)));
        return batch;
    }
    private static JSONObject getDepartmentRecord(Sheet departmentSheet)
    {
        JSONObject department = new JSONObject();
        for(int i=1;i<departmentSheet.getPhysicalNumberOfRows()-1;++i)
            department.put(String.valueOf(i),departmentSheet.getRow(i).getCell(3).getNumericCellValue());
        return department;
    }
}
