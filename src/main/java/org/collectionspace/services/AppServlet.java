package org.collectionspace.services;

import java.io.File;
import java.io.IOException;
import java.security.Principal;
import java.util.Arrays;

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
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.SystemPrincipal;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;

@SuppressWarnings("serial")
public class AppServlet extends HttpServlet {

	private Logger logger = LoggerFactory.getLogger(AppServlet.class);
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

	public void printWorkspaceTree(Principal principal) {
		RepositoryManager repositoryManager = Framework.getService(RepositoryManager.class);
		for (String repositoryName : repositoryManager.getRepositoryNames()) {
			try (CoreSession session = CoreInstance.openCoreSession(repositoryName, principal)) {  // try-with-resources statement
				DocumentModel docModel = session.getRootDocument();
				printDocModelInfo(docModel, 0);				
			}
		}		
	}

	Principal getPrincipal() {
		NuxeoPrincipal principal = new SystemPrincipal(null);
		return principal;
	}

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

	private void stopNuxeo() throws Exception {
		// fb.stop();
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

		logger.debug("Testing DEBUG REMX level.");
		logger.debug("Testing INFO REMX level.");
		logger.debug("Testing ERROR REMX level.");
		logger.debug("Testing WARN REMX level.");
		//
		// Start, excercise, and stop Nuxeo
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
