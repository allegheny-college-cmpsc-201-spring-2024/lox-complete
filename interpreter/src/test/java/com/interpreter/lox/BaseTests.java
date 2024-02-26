package com.interpreter.lox;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Paths;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;

class BaseTests {

  /*
    Word to the accepted answer at SO, they're a real one.
    https://stackoverflow.com/questions/1119385/junit-test-for-system-out-println
  */

  private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
  private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();

  private final PrintStream originalOut = System.out;
  private final PrintStream originalErr = System.err;
  private final InputStream originalIn = System.in;

  @BeforeAll
  public void setUpStreams() {
    System.setOut(new PrintStream(outContent));
    System.setErr(new PrintStream(errContent));
  }

  static Stream<Arguments> testBreakStatement() throws Exception {
    URL resource = BaseTests.class.getClassLoader().getResource("test.lox");
    File file = Paths.get(resource.toURI()).toFile();
    String absPath = file.getAbsolutePath();
    return Stream.of(
      Arguments.of((Object) new String[]{absPath})
    );
  }

  static Stream<Arguments> testTernaryExpression() throws Exception {
    URL resource = BaseTests.class.getClassLoader().getResource("test.lox");
    File file = Paths.get(resource.toURI()).toFile();
    String absPath = file.getAbsolutePath();
    return Stream.of(
      Arguments.of((Object) new String[]{absPath})
    );
  }

  static Stream<Arguments> testContinueStatement() throws Exception {
    URL resource = BaseTests.class.getClassLoader().getResource("test.lox");
    File file = Paths.get(resource.toURI()).toFile();
    String absPath = file.getAbsolutePath();
    return Stream.of(
      Arguments.of((Object) new String[]{absPath})
    );
  }

  @MethodSource
  @ParameterizedTest
  void testBreakStatement(String[] args) throws Exception {
    Main.main(args);
    assertThat(
        outContent.toString().strip(),
        containsString("LOOP ENDED AT: 7.0")
    );
  }

  @MethodSource
  @ParameterizedTest
  void testTernaryExpression(String[] args) throws Exception {
    Main.main(args);
    assertThat(
        outContent.toString().strip(),
        containsString("LOOP BROKE AT 7.0: true")
    );
  }

  @MethodSource
  @ParameterizedTest
  void testContinueStatement(String[] args) throws Exception {
    Main.main(args);
    assertThat(
        outContent.toString().strip(),
        not(containsString("3"))
    );
  }

  @AfterAll
  public void restoreStreams() {
    System.setIn(originalIn);
    System.setOut(originalOut);
    System.setErr(originalErr);
  }

}
