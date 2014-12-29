package org.collectionspace.services;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

public class AppServletTest {

  @Test
  public void test() {
    assertNotNull("DummyTest", new AppServlet());
  }

}
