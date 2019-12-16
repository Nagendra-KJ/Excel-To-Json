package com.rvceresults;
import java.io.*;


@SuppressWarnings("ALL")
/*All that is happening in this class is that once the respective grabber gets the results and stores them in the
Excel sheets, we are calculating the average by the formula. And evaluating the formula in the getUniversityRecord
and is written to dataset.json file.
 */

/*Todo
1. Add college choice option
2. Add multi-threading support
19428353894058 Add AI for MSR lulzkie
 */
public class ExcelToJson
{
        public static void main(String[] args) throws IOException {
                RvceGrabber RVgrabber=new RvceGrabber();
                RVgrabber.getResult();
                Grabber MSRgrabber=new MsritGrabber();
                MSRgrabber.getResult();
                System.exit(new ExitStatus().EXIT_ON_COMPLETION);
        }
}

