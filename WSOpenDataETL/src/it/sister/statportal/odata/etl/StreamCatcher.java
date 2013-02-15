package it.sister.statportal.odata.etl;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class StreamCatcher extends Thread
{
    InputStream is;
    String type;
    
    public StreamCatcher(InputStream is, String type)
    {
        this.is = is;
        this.type = type;
    }
    
    public void run()
    {
        try
        {
        	FileWriter writer = new FileWriter("/tmp/"+type+".txt");
        	
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String line=null;
            while ( (line = br.readLine()) != null)
                writer.write(line+"\n");
            writer.flush();
            writer.close();
        } catch (IOException ioe){
            ioe.printStackTrace();  
        }
    }
}
