package com.me.project;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class ProgressServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		response.setContentType("text/event-stream");
		response.setCharacterEncoding("UTF-8");
		response.setHeader("Cache-Control", "no-cache");
		PrintWriter writer = response.getWriter();

		String currentPercent = UploadServlet.getPercentDone().replaceAll("\\s", "");
		
		StringBuilder data = new StringBuilder();

        //send the JSON-encoded data
        data.append("{\"current\":").append(currentPercent).append("}");

        if(currentPercent.equals("100")) {

        	System.out.println("Last Value : " + currentPercent);

        	// send after 10, we can get last value
        	writeEvent(response, "status", data.toString());

        	// send finalized response
			writeEvent(response, "status", "{\"complete\":true}");

        } else {

        	writeEvent(response, "status", data.toString());

        }
		writer.flush();
		writer.close();
            			

	}

	protected void writeEvent(HttpServletResponse response, String event, String message)throws IOException {
	
		// get the writer to send text responses
		PrintWriter writer = response.getWriter();
		
		// write the event type (make sure to include the double newline)
		writer.write("event: " + event + "\n\n");
		
		// write the actual data
		// this could be simple text or could be JSON-encoded text that the
		// client then decodes
		writer.write("data: " + message + "\n\n");
		
		// flush the buffers to make sure the container sends the bytes
		writer.flush();
		response.flushBuffer();
		
	}
	
}