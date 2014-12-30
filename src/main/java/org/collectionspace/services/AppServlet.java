package org.collectionspace.services;

import java.io.File;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.nuxeo.osgi.application.FrameworkBootstrap;

@SuppressWarnings("serial")
public class AppServlet extends HttpServlet {

	private Logger logger = LoggerFactory.getLogger(AppServlet.class);
	private FrameworkBootstrap fb;
	private static final String CSPACE_NUXEO_HOME = "";
//	private static final String NXSERVER = "C:/dev/tools/apache-tomcat-7.0.57/nxserver";
	private static final String NXSERVER = "/Users/remillet/dev/tools/apache-tomcat-7.0.57/nxserver";

	private File getNuxeoServerDir(String serverRootPath) throws IOException {
		File result = null;
		String errMsg = null;

		String path = NXSERVER;
		if (path != null) {
			File temp = new File(path);
			if (temp.exists() == true) {
				result = temp;
			} else {
				errMsg = "The Nuxeo EP configuration directory is missing or inaccessible at: '"
						+ path + "'.";
			}
		}

		if (result == null) {
			if (errMsg == null) {
				path = path != null ? path : "<empty>";
				errMsg = "Unknown error trying to find Nuxeo configuration: '"
						+ CSPACE_NUXEO_HOME + "' = " + path;
			}
			throw new IOException(errMsg);
		}

		return result;
	}

	private void startNuxeo() throws Exception {
		File nuxeoHomeDir = getNuxeoServerDir(null);

		if (logger.isInfoEnabled() == true) {
			logger.info("Starting Nuxeo EP server from configuration at: "
					+ nuxeoHomeDir.getCanonicalPath());
		}

		fb = new FrameworkBootstrap(AppServlet.class.getClassLoader(),
				nuxeoHomeDir);
		fb.setHostName("Tomcat");
		fb.setHostVersion("7.0.57");
		
		fb.initialize();
		fb.start();
	}

	private void excerciseNuxeo() {
		// Do something
	}

	private void stopNuxeo() throws Exception {
		fb.stop();
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {

		System.err.println("Testing System error messages. REMX.");
        System.setProperty("nuxeo.log.dir", "/Users/remillet/dev/tools/apache-tomcat-7.0.57/nxserver/log");

		logger.debug("Testing DEBUG REMX level.");
		logger.debug("Testing INFO REMX level.");
		logger.debug("Testing ERROR REMX level.");
		logger.debug("Testing WARN REMX level.");
		//
		// Start, excercise, and stop Nuxeo
		//
		try {
			startNuxeo();
			excerciseNuxeo();
			stopNuxeo();
		} catch (Exception x) {
			x.printStackTrace(System.err);  // REM - Replace with log statement
		}

		//
		// Original code
		//
		String destination = "/index.jsp";

		RequestDispatcher rd = getServletContext().getRequestDispatcher(
				destination);
		rd.forward(req, resp);
	}
}
