package org.interop.xoola.java.test;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({InterruptTest.class,TestXoolaMulti.class, TestXoolaOneToOne.class })
public class AllTests {
  
}
