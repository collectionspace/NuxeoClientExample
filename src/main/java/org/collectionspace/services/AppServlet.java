package org.collectionspace.services;

import java.io.File;
import java.io.IOException;
import java.security.Principal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.nuxeo.osgi.application.FrameworkBootstrap;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.core.api.repository.Repository;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.ecm.platform.filemanager.service.extension.CreationContainerListProvider;
import org.nuxeo.ecm.platform.filemanager.service.extension.CreationContainerListProviderDescriptor;
import org.nuxeo.ecm.platform.usermanager.UserManager;


@SuppressWarnings("serial")
public class AppServlet extends HttpServlet {

    private RepositoryManager repositoryMgr;
	private Logger logger = LoggerFactory.getLogger(AppServlet.class);
	private FrameworkBootstrap fb;
	private static final String CSPACE_NUXEO_HOME = "";
	private static final String NXSERVER = "C:/dev/tools/apache-tomcat-7.0.57/nxserver";
//	private static final String NXSERVER = "/Users/remillet/dev/tools/apache-tomcat-7.0.57/nxserver";

    private RepositoryManager getRepositoryManager() throws Exception {
        if (repositoryMgr == null) {
            repositoryMgr = Framework.getService(RepositoryManager.class);
        }
        return repositoryMgr;
    }
        
    public DocumentModelList getCreationContainers(Principal principal) {
        DocumentModelList containers = new DocumentModelListImpl();
        RepositoryManager repositoryManager = Framework.getService(RepositoryManager.class);
        for (String repositoryName : repositoryManager.getRepositoryNames()) {
            try (CoreSession session = CoreInstance.openCoreSession(repositoryName, principal)) {
                DocumentModel doc = session.getRootDocument();
                containers.add(doc);
                System.err.println(doc.getPathAsString());
            }
        }
        return containers;
    }
    
    Principal getPrincipal() {
        UserManager userManager = Framework.getService(UserManager.class);
        NuxeoPrincipal principal = userManager.getPrincipal("Administrator");
        return principal;
    }

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
		try {
			DocumentModelList documents = getCreationContainers(this.getPrincipal());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void stopNuxeo() throws Exception {
		fb.stop();
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {

		System.err.println("Testing System error messages. REMX.");
        //System.setProperty("nuxeo.log.dir", "/Users/remillet/dev/tools/apache-tomcat-7.0.57/nxserver/log");

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
			//stopNuxeo();
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
