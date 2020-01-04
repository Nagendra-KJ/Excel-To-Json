package com.rvceresults;

import java.io.IOException;


@SuppressWarnings("ALL")
/*All that is happening in this class is that once the respective grabber gets the results and stores them in the
Excel sheets, we are calculating the average by the formula. And evaluating the formula in the getUniversityRecord
and is written to dataset.json file.
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
    public static void main(String[] args)
    {
        RvceGrabber rvceGrabber = new RvceGrabber();
        try
        {
            rvceGrabber.getResult();
        } catch (IOException e)
        {
            System.out.println("There was an error!");
            e.printStackTrace();
        }
        System.exit(new ExitStatus().EXIT_ON_COMPLETION);
    }
}

