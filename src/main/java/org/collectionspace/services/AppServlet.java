/*
 * An example servlet that starts up an embedded instance of the Nuxeo EP framework and makes
 * a few calls to the API.
 * 
 * The servlet lazy loads the Nuxeo EP instance once.
 */
package org.collectionspace.services;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.Principal;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.PropertyConfigurator;
import org.nuxeo.osgi.application.FrameworkBootstrap;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;

import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.SystemPrincipal;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;

@SuppressWarnings("serial")
public class AppServlet extends HttpServlet {
	
	private static Logger logger = LoggerFactory.getLogger(AppServlet.class);
	private FrameworkBootstrap fb = null;

	private static final String NUXEO_HOME_PROPERTY = "NUXEO_HOME";

	private String getNuxeoHomeProperty() {
		String nuxeoHomeDir = System.getProperty(NUXEO_HOME_PROPERTY);
		
		if (nuxeoHomeDir == null) {
			nuxeoHomeDir = System.getenv(NUXEO_HOME_PROPERTY);
			if (nuxeoHomeDir == null) {
				System.err.println(String.format(
						"A value for %s needs to be set either as an environment variable or a Java system property.",
						NUXEO_HOME_PROPERTY));
			}
		}
		return nuxeoHomeDir;
	}

	//
	// Get a list repositories and then print out a list of documents in them.
	//
	private void printWorkspaceTree(Principal principal) {
		RepositoryManager repositoryManager = Framework.getService(RepositoryManager.class);
		for (String repositoryName : repositoryManager.getRepositoryNames()) {
			try (CoreSession session = CoreInstance.openCoreSession(repositoryName, principal)) {  // try-with-resources statement
				DocumentModel docModel = session.getRootDocument();
				printDocModelInfo(docModel, 0);				
			}
		}		
	}

	//
	// Returns the "system" user for authenticating with the Nuxeo EP framework.
	//
	private Principal getPrincipal() {
		NuxeoPrincipal principal = new SystemPrincipal(null);
		return principal;
	}

	//
	// Return the root directory of the Nuxeo server
	//
	private File getNuxeoHomeDir() throws IOException {
		File result = null;
		String errMsg = null;

		String path = getNuxeoHomeProperty();
		if (path != null) {
			File temp = new File(path);
			if (temp.exists() == true) {
				result = temp;
			} else {
				errMsg = "The Nuxeo EP configuration directory is missing or inaccessible at: '" + path + "'.";
			}
		}

		if (result == null) {
			if (errMsg == null) {
				path = path != null ? path : "<empty>";
				errMsg = "Unknown error trying to find Nuxeo configuration: '" + "' = " + path;
			}
			throw new IOException(errMsg);
		}

		return result;
	}

	//
	// Initialize Nuxeo just once.  Synchronized for thread safety.
	//
	synchronized private void startNuxeo() throws Exception {
		if (fb == null) {
			File nuxeoHomeDir = getNuxeoHomeDir();

			if (logger.isInfoEnabled() == true) {
				logger.info("Starting Nuxeo EP server from configuration at: " + nuxeoHomeDir.getCanonicalPath());
			}

			fb = new FrameworkBootstrap(AppServlet.class.getClassLoader(), nuxeoHomeDir);
			fb.setHostName("Tomcat");
			fb.setHostVersion("7.0.57");

			fb.initialize();
			fb.start();
		}
	}

	//
	// Send some info about a DocumentModel and all it's children to standard out
	//
	private void printDocModelInfo(DocumentModel docModel, int level) {
		String tabs = "";
		for (int i = 0; i < level; i++) {
			tabs = tabs + "    ";
		}
		System.err.println(String.format("%s====", tabs));
		System.err.println(String.format("%sRepository: %s", tabs, docModel.getRepositoryName()));
		System.err.println(String.format("%sPath: %s", tabs, docModel.getPathAsString()));
		System.err.println(String.format("%sSchemas: %s", tabs, Arrays.toString(docModel.getSchemas())));
		System.err.println();

		CoreSession session = docModel.getCoreSession();
		DocumentModelList childList = session.getChildren(new IdRef(docModel.getId()));
		for (DocumentModel child : childList) {
			printDocModelInfo(child, level + 1); // Recurse
		}
	}

	//
	// Create a Nuxeo "Workspace" document/folder.
	//
	private DocumentModel createNuxeoWorkspace(DocumentModel parentDoc, String workspaceName) {
		DocumentModel result = null;
		String workspaceId = null;
		CoreSession coreSession = parentDoc.getCoreSession();

		try {
			DocumentModel doc = coreSession
					.createDocumentModel(parentDoc.getPathAsString(), workspaceName, "Workspace");
			doc.setPropertyValue("dc:title", workspaceName);
			doc.setPropertyValue("dc:description", "A Nuxeo workspace for " + workspaceName);
			result = doc = coreSession.createDocument(doc);
			workspaceId = doc.getId();
			coreSession.save();
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (result != null) {
			System.err.println(String.format("Created Workspace '%s' with ID=%s",
					workspaceName, workspaceId));
		}

		return result;
	}

	//
	// Create a "tree" four levels deep of Nuxeo "Workspace" documents in the default
	// repository.
	//
	private void createWorkspaceTree(String prefix, Principal adminUser) {
		RepositoryManager repositoryManager = Framework.getService(RepositoryManager.class);
		String defaultRepoName = repositoryManager.getDefaultRepositoryName();
		CoreSession session = CoreInstance.openCoreSession(defaultRepoName, adminUser);

		DocumentModel root = session.getRootDocument();
		for (int i = 0; i < 4; i++) {
			root = createNuxeoWorkspace(root, String.format("%s - %d", prefix, i));
		}
		
		CoreInstance.closeCoreSession(session);
	}

	//
	// Perform some example Nuxeo EP API calls to verifying embedded Nuxeo startup.
	//
	private void excerciseNuxeo() {
		try {
			TransactionHelper.startTransaction();
			Principal adminUser = this.getPrincipal();
			createWorkspaceTree("Level", adminUser);
			printWorkspaceTree(adminUser);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			TransactionHelper.commitOrRollbackTransaction();
		}
	}

	//
	// No need to stop Nuxeo EP instance.  Doesn't seem to let us restart again anyway.
	//
	private void stopNuxeo() throws Exception {
		// fb.stop();
	}

	//
	// The Servlet's default method for handling GET requests to the base URL
	//
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

//		URL in = Thread.currentThread().getContextClassLoader().getResource("log4j.properties");
//		PropertyConfigurator.configure(in.getFile());

		logger.debug("Testing DEBUG REMX level.");
		logger.info("Testing INFO REMX level.");
		logger.error("Testing ERROR REMX level.");
		logger.warn("Testing WARN REMX level.");
		//
		// Start, exercise, and stop Nuxeo
		//
		try {
//			startNuxeo();
//			excerciseNuxeo();
//			stopNuxeo();
		} catch (Exception x) {
			x.printStackTrace(System.err); // REM - TODO Replace with log
											// statement
		}

		//
		// Original code
		//
		String destination = "/index.jsp";

		RequestDispatcher rd = getServletContext().getRequestDispatcher(destination);
		rd.forward(req, resp);
	}
}
