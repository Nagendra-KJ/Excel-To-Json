package com.rvceresults;

import java.io.IOException;


@SuppressWarnings("ALL")
/*All that is happening in this class is that once the respective grabber gets the results and stores them in the
Excel sheets, we are calculating the average by the formula. And evaluating the formula in the getUniversityRecord
and is written to dataset.json file.
Wololo
 */

/*
@Todo
0. Refactor to remove duplicities
1. Add college choice option
2. Add multi-threading support
19428353894058 Add AI for MSR lulzkie
 */
public class Driver
{
    public static void main(String[] args) throws IOException
    {
        RvceGrabber rvceGrabber = new RvceGrabber();
        rvceGrabber.getResult();
        System.exit(new ExitStatus().EXIT_ON_COMPLETION);
    }
}
