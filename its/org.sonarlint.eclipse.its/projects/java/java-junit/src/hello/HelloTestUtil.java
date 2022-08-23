package hello;

import junit.framework.Test;

public class HelloTestUtil {

  private HelloTestUtil() {
  }

  public static void main(String... args) throws InterruptedException {
    Thread.sleep(1);
    System.out.println("Hello");
  }

  public int unecessaryCast(Test test) {
    return (int) test.countTestCases();
  }

}
