NuxeoClientExample
==================

A simple web app that has an embedded instance of Nuxeo and runs on the latest version of Tomcat.  A value for NUXEO_HOME needs to be set as either an environment variable or as a Java system property.

You can download an archived tomcat deployment folder to test the web app at the following link: 
https://www.dropbox.com/s/nci5k3h037wacql/apache-tomcat-7.0.57.zip?dl=0

Be sure to set the environment variables BASEDIR, CATALINA_HOME, and NUXEO_HOME to the top-level of the tomcat deployment folder.

After launch:
* Verify in $NUXEO_HOME/logs/catalina.out that the server has successfully started
* Visit http://localhost:8080/nuxeoclientexample/ in your browser

