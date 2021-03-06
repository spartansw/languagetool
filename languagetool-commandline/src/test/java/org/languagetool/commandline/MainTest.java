/* LanguageTool, a natural language style checker
 * Copyright (C) 2005 Daniel Naber (http://www.danielnaber.de)
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301
 * USA
 */
package org.languagetool.commandline;

import org.apache.commons.lang.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * Tests the basic features of the command-line interface.
 *
 * @author Marcin Miłkowski
 */
public class MainTest extends AbstractSecurityTestCase {

  private final File enTestFile;
  private final File xxRuleFile;
  private final File xxFalseFriendFile;
  private final File bitextFile;

  private ByteArrayOutputStream out;
  private ByteArrayOutputStream err;
  private PrintStream stdout;
  private PrintStream stderr;

  public MainTest(String testName) throws IOException {
    super(testName);
    enTestFile = writeToTempFile("This is an test.\n\n" +
        "This is a test of of language tool.\n\n" +
        "This is is a test of language tool.");
    xxRuleFile = writeToTempXMLFile("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
        "<rules lang=\"en\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
        "xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n" +
             "<category name=\"trivial category\">\n" +
        "<rule id=\"EXAMPLE_RULE\" name=\"External rule to test\">\n" +
            "<pattern><token>language</token><token>tool</token></pattern>\n" +
            "<message>This is wrong!</message>\n" +
            "<example correction=\"\">language tool</example>\n" +
            "</rule></category></rules>");

    xxFalseFriendFile = writeToTempXMLFile("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
        "<!DOCTYPE rules SYSTEM \"false-friends.dtd\">\n" +
        "<rules>\n" +
        "<rulegroup id=\"LASKA_SK_PL\">\n" +
        "        <rule>\n" +
        "            <pattern lang=\"sk\">\n" +
        "                <token>láska</token>\n" +
        "            </pattern>\n" +
        "            <translation lang=\"pl\">miłość</translation>\n" +
        "        </rule>\n" +
        "        <rule>\n" +
        "            <pattern lang=\"pl\">\n" +
        "                <token inflected=\"yes\">miłość</token>\n" +
        "            </pattern>\n" +
        "            <translation lang=\"sk\">laska</translation>\n" +
        "        </rule>\n" +
        "    </rulegroup>\n</rules>\n" +
        "    ");
    bitextFile = writeToTempXMLFile("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
        "<rules targetLang=\"pl\" xsi:noNamespaceSchemaLocation=\"../bitext.xsd\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n" +
        "<category name=\"idioms\">\n" +
        "<rule lang=\"pl\" id=\"red_herring\" name=\"Red herring -> odwraca uwagę\">\n" +
        "<pattern>\n" +
        "\t<source lang=\"en\">\n" +
        "\t\t<token>is</token>\n" +
        "\t\t<token>a</token>\n" +
        "\t\t<token>red</token>\n" +
        "\t\t<token>herring</token>\n" +
        "\t</source>\n" +
        "\t<target>\n" +
        "\t\t<token>jest</token>\n" +
        "\t\t<token>czerwony</token>\n" +
        "\t\t<token>śledź</token>\n" +
        "\t</target>\n" +
        "</pattern>\n" +
        "<message>Czy chodziło o <suggestion>odwraca uwagę</suggestion>?</message>\n" +
        "<example type=\"correct\"><srcExample>This is a red herring.</srcExample>\n" +
        "\t\t\t\t\t<trgExample>To odwraca uwagę.</trgExample></example>\n" +
        "<example type=\"incorrect\" correction=\"odwraca uwagę\"><srcExample>This <marker>is a red herring</marker>.</srcExample>\n" +
        "<trgExample>To <marker>jest czerwony śledź</marker>.</trgExample></example>\n" +
        "</rule></category></rules>\n");

  }

  @Override
  public void setUp() throws Exception {
    super.setUp();
    this.stdout = System.out;
    this.stderr = System.err;
    this.out = new ByteArrayOutputStream();
    this.err = new ByteArrayOutputStream();
    System.setOut(new PrintStream(this.out));
    System.setErr(new PrintStream(this.err));
  }

  @Override
  public void tearDown() throws Exception {
    super.tearDown();
    System.setOut(this.stdout);
    System.setErr(this.stderr);
  }

  public void testUsageMessage() throws Exception {
    try {
      final String[] args = {"-h"};
      Main.main(args);
      fail("LT should have exited with status 0!");
    } catch (ExitException e) {
      final String output = new String(this.out.toByteArray());
      assertTrue(output.contains("Usage: java -jar languagetool-commandline.jar"));
      assertEquals("Exit status", 1, e.status);
    }
  }

  public void testPrintLanguages() throws Exception {
    try {
      final String[] args = {"--list"};
      Main.main(args);
      fail("LT should have exited with status 0!");
    } catch (ExitException e) {
      final String output = new String(this.out.toByteArray());
      assertTrue(output.contains("German"));
      assertTrue(output.contains("de-DE"));
      assertTrue(output.contains("English"));
      assertEquals("Exit status", 0, e.status);
    }
  }

  public void testFileWithExternalRule() throws Exception {
    //note: we pretend this is Breton because the English language tool is already initialized
    final String[] args = {"-l", "br", "--rulefile", getRuleFilePath(), getTestFilePath()};
    Main.main(args);
    final String stdout = new String(this.out.toByteArray());
    final String stderr = new String(this.err.toByteArray());
    assertTrue(stderr.indexOf("Expected text language: Breton") == 0);
    assertTrue(stdout.contains("Rule ID: EXAMPLE_RULE"));
  }

  public void testEnglishFile() throws Exception {
    final String[] args = {"-l", "en", getTestFilePath()};

    Main.main(args);
    final String stdout = new String(this.out.toByteArray());
    final String stderr = new String(this.err.toByteArray());
    assertTrue(stderr.indexOf("Expected text language: English") == 0);
    assertTrue(stdout.contains("1.) Line 1, column 9, Rule ID: EN_A_VS_AN"));
  }

  public void testEnglishFileAutoDetect() throws Exception {
    final String[] args = {"-adl", getTestFilePath()};

    Main.main(args);
    final String stdout = new String(this.out.toByteArray());
    final String stderr = new String(this.err.toByteArray());
    assertTrue(stderr.indexOf("Using English for file") == 0);
    assertTrue(stdout.contains("1.) Line 1, column 9, Rule ID: EN_A_VS_AN"));
  }

  public void testEnglishStdInAutoDetect() throws Exception {
    final String test = "This is an test.";
    final byte[] b = test.getBytes();
    System.setIn(new ByteArrayInputStream(b));
    final String[] args = {"-adl"};

    Main.main(args);
    final String stdout = new String(this.out.toByteArray());
    final String stderr = new String(this.err.toByteArray());
    assertTrue(stderr.indexOf("Working on STDIN...") == 0);
    assertTrue(stderr.contains("Language used is: English"));
    assertTrue(stdout.contains("1.) Line 1, column 9, Rule ID: EN_A_VS_AN"));
  }

  public void testStdInWithExternalFalseFriends() throws Exception {
    final String test = "Láska!\n";
    final byte[] b = test.getBytes();
    System.setIn(new ByteArrayInputStream(b));
    final String[] args = {"-l", "sk", "--falsefriends", getExternalFalseFriends(), "-m", "pl", "-"};

    Main.main(args);
    final String stdout = new String(this.out.toByteArray());
    final String stderr = new String(this.err.toByteArray());
    assertTrue(stderr.contains("Expected text language: Slovak"));
    assertTrue(stderr.contains("Working on STDIN..."));
    assertTrue(stdout.contains("Rule ID: LASKA"));
  }

  public void testEnglishFileVerbose() throws Exception {
    final String[] args = {"-l", "en", "-v", getTestFilePath()};

    Main.main(args);
    final String stdout = new String(this.out.toByteArray());
    final String stderr = new String(this.err.toByteArray());
    assertTrue(stderr.indexOf("Expected text language: English") == 0);
    assertTrue(stdout.contains("1.) Line 1, column 9, Rule ID: EN_A_VS_AN"));
    final String tagText = new String(this.err.toByteArray());
    assertTrue("Got: " + tagText, tagText.contains("<S> This[this/DT,B-NP-singular|E-NP-singular] is[be/VBZ,B-VP] an[a/DT,B-NP-singular] test[test/NN,E-NP-singular].[./.,</S>,O]"));
  }

  public void testEnglishFileApplySuggestions() throws Exception {
    final String[] args = {"-l", "en", "--apply", getTestFilePath()};

    Main.main(args);
    final String output = new String(this.out.toByteArray());
    assertTrue(output.contains("This is a test.\n\n" +
        "This is a test of language tool.\n\n" +
        "This is a test of language tool.")); // \r\n in Windows tests at the end...
  }

  public void testEnglishStdIn1() throws Exception {
    final String test = "This is an test.";
    final byte[] b = test.getBytes();
    System.setIn(new ByteArrayInputStream(b));
    final String[] args = {"-l", "en"};

    Main.main(args);
    final String stdout = new String(this.out.toByteArray());
    final String stderr = new String(this.err.toByteArray());
    assertTrue(stderr.indexOf("Expected text language: English") == 0);
    assertTrue(stdout.contains("1.) Line 1, column 9, Rule ID: EN_A_VS_AN"));
  }

  public void testEnglishStdIn2() throws Exception {
    final String test = "This is an test.";
    final byte[] b = test.getBytes();
    System.setIn(new ByteArrayInputStream(b));
    final String[] args = {"-l", "en", "-"};

    Main.main(args);
    final String stdout = new String(this.out.toByteArray());
    final String stderr = new String(this.err.toByteArray());
    assertTrue(stderr.indexOf("Expected text language: English") == 0);
    assertTrue(stdout.contains("1.) Line 1, column 9, Rule ID: EN_A_VS_AN"));
  }

  public void testEnglishStdIn3() throws Exception {
    final String test = "This is an test.";
    final byte[] b = test.getBytes();
    System.setIn(new ByteArrayInputStream(b));
    final String[] args = {"-l", "en", "-a", "-"};

    Main.main(args);
    final String output = new String(this.out.toByteArray());
    assertEquals("This is a test.\n", output);
  }

  public void testEnglishStdIn4() throws Exception {
    System.setIn(new FileInputStream(enTestFile));
    final String[] args = {"-l", "en", "--api", "-"};

    Main.main(args);
    final String output = new String(this.out.toByteArray());
    assertTrue("Got: " + output, output.contains("<error fromy=\"4\" fromx=\"5\" toy=\"4\" tox=\"10\" " +
        "ruleId=\"ENGLISH_WORD_REPEAT_RULE\" msg=\"Possible typo: you repeated a word\" replacements=\"is\" " +
        "context=\"This is is a test of language tool. \" contextoffset=\"5\" offset=\"5\" errorlength=\"5\" " +
        "category=\"Miscellaneous\" locqualityissuetype=\"duplication\"/>"));
    // note: the offset is relative to the sentence... this seems wrong - it happens because of the way
    // the command line client feeds the data into the check() methods.
  }

  //test line mode vs. para mode
  //first line mode
  public void testEnglishLineMode() throws Exception {
    final String test = "This is what I mean\nand you know it.";
    final byte[] b = test.getBytes();
    System.setIn(new ByteArrayInputStream(b));
    final String[] args = {"-l", "en", "-a", "-b", "-"};

    Main.main(args);
    final String output = new String(this.out.toByteArray());
    assertEquals("This is what I mean\nAnd you know it.\n", output);
  }

  //first line mode
  public void testEnglishParaMode() throws Exception {
    final String test = "This is what I mean\nand you know it.";
    final byte[] b = test.getBytes();
    System.setIn(new ByteArrayInputStream(b));
    final String[] args = {"-l", "en", "-a", "-"};

    Main.main(args);
    final String output = new String(this.out.toByteArray());
    assertEquals("This is what I mean\nand you know it.\n", output);
  }

  public void testPolishStdInDefaultOff() throws Exception {
    final String test = "To jest test, który zrobiłem, który mi się podoba.";
    final byte[] b = test.getBytes();
    System.setIn(new ByteArrayInputStream(b));
    final String[] args = {"-l", "pl", "-e", "PL_WORD_REPEAT", "-"};

    Main.main(args);
    final String stdout = new String(this.out.toByteArray());
    final String stderr = new String(this.err.toByteArray());
    assertTrue(stderr.indexOf("Expected text language: Polish") == 0);
    assertTrue(stderr.contains("Working on STDIN..."));
    assertTrue(stdout.contains("1.) Line 1, column 31, Rule ID: PL_WORD_REPEAT"));
  }

  public void testPolishApiStdInDefaultOff() throws Exception {
    final String test = "To jest test, który zrobiłem, który mi się podoba.";
    final byte[] b = test.getBytes();
    System.setIn(new ByteArrayInputStream(b));
    final String[] args = {"--api", "-l", "pl", "-e", "PL_WORD_REPEAT", "-"};
    Main.main(args);
    final String output = new String(this.out.toByteArray());
    assertThat(StringUtils.countMatches(output, "<error "), is(1));
    assertThat(StringUtils.countMatches(output, "<matches "), is(1));
    assertThat(StringUtils.countMatches(output, "</matches>"), is(1));  // https://github.com/languagetool-org/languagetool/issues/251
  }

  public void testPolishApiStdInDefaultOffNoErrors() throws Exception {
    final String test = "To jest test.";
    final byte[] b = test.getBytes();
    System.setIn(new ByteArrayInputStream(b));
    final String[] args = {"--api", "-l", "pl", "-e", "PL_WORD_REPEAT", "-"};
    Main.main(args);
    final String output = new String(this.out.toByteArray());
    assertThat(StringUtils.countMatches(output, "<error "), is(0));
    assertThat(StringUtils.countMatches(output, "<matches "), is(1));
    assertThat(StringUtils.countMatches(output, "</matches>"), is(1));
  }

  public void testPolishSpelling() throws Exception {
    final String test = "Zwuasdac?";
    final byte[] b = test.getBytes();
    System.setIn(new ByteArrayInputStream(b));
    final String[] args = {"-l", "pl", "-e", "MORFOLOGIK_RULE_PL_PL", "-"};

    Main.main(args);
    final String stdout = new String(this.out.toByteArray());
    final String stderr = new String(this.err.toByteArray());
    assertTrue(stderr.indexOf("Expected text language: Polish") == 0);
    assertTrue(stderr.contains("Working on STDIN..."));
    assertTrue(stdout.contains("1.) Line 1, column 1, Rule ID: MORFOLOGIK_RULE_PL_PL"));
  }

  public void testEnglishFileRuleDisabled() throws Exception {
    final String[] args = {"-l", "en", "-d", "EN_A_VS_AN", getTestFilePath()};

    Main.main(args);
    final String stdout = new String(this.out.toByteArray());
    final String stderr = new String(this.err.toByteArray());
    assertTrue(stderr.indexOf("Expected text language: English") == 0);
    assertTrue(!stdout.contains("Rule ID: EN_A_VS_AN"));
  }

  public void testEnglishFileRuleEnabled() throws Exception {
    final String[] args = {"-l", "en", "-e", "EN_A_VS_AN", getTestFilePath()};

    Main.main(args);
    final String stdout = new String(this.out.toByteArray());
    final String stderr = new String(this.err.toByteArray());
    assertTrue(stderr.indexOf("Expected text language: English") == 0);
    assertTrue(stdout.contains("Rule ID: EN_A_VS_AN"));
  }

  public void testEnglishFileFakeRuleEnabled() throws Exception {
    final String test = "Zwuasdac?";
    final byte[] b = test.getBytes();
    System.setIn(new ByteArrayInputStream(b));
    final String[] args = {"-l", "en", "-e", "FOO_BAR_BLABLA", "-"};

    Main.main(args);
    final String stderr = new String(this.err.toByteArray());
    assertTrue(stderr.indexOf("Expected text language: English") == 0);
  }

  public void testEnglishFileAPI() throws Exception {
    final String[] args = {"-l", "en", "--api", getTestFilePath()};

    Main.main(args);
    final String output = new String(this.out.toByteArray());
    assertTrue(output.indexOf("<?xml version=\"1.0\" encoding=\"UTF-8\"?>") == 0);
    assertTrue(output.contains("<error fromy=\"0\" fromx=\"8\" toy=\"0\" tox=\"10\" ruleId=\"EN_A_VS_AN\" " +
        "msg=\"Use &apos;a&apos; instead of &apos;an&apos; if the following word doesn&apos;t start with a vowel sound, e.g. &apos;a sentence&apos;, " +
        "&apos;a university&apos;\" replacements=\"a\" context=\"This is an test.  This is a test of of language tool.  ...\" " +
        "contextoffset=\"8\" offset=\"8\" errorlength=\"2\" category=\"Miscellaneous\" locqualityissuetype=\"misspelling\"/>"));
  }

  public void testGermanFileWithURL() throws Exception {
    final File input = writeToTempFile("Ward ihr zufrieden damit?");

    final String[] args = {"-l", "de", "--api", input.getAbsolutePath()};

    Main.main(args);
    final String output = new String(this.out.toByteArray());
    assertTrue(output.indexOf("<?xml version=\"1.0\" encoding=\"UTF-8\"?>") == 0);
    assertTrue(output.contains("ruleId=\"WARD_VS_WART\" subId=\"1\""));
    //check URL part
    assertTrue(output.contains("url=\"http://www.korrekturen.de/beliebte_fehler/ward.shtml\""));

    //now check in normal mode and check for URL
    final String[] args2 = {"-l", "de", input.getAbsolutePath()};
    Main.main(args2);
    final String output2 = new String(this.out.toByteArray());
    assertTrue(output2.contains("More info: http://www.korrekturen.de/beliebte_fehler/ward.shtml"));
  }

  public void testPolishFileAPI() throws Exception {
    final File input = writeToTempFile("To jest świnia która się ślini.");

    final String[] args = {"-l", "pl", "--api", "-c", "utf-8", input.getAbsolutePath()};

    Main.main(args);
    final String output = new String(this.out.toByteArray(),"UTF-8");
    assertTrue(output.indexOf("<?xml version=\"1.0\" encoding=\"UTF-8\"?>") == 0);
    assertTrue(output.contains("<error fromy=\"0\" fromx=\"8\" toy=\"0\" tox=\"20\" ruleId=\"BRAK_PRZECINKA_KTORY\""));
    //This tests whether XML encoding is actually UTF-8:
    assertTrue(output.contains("msg=\"Brak przecinka w tym fragmencie zdania. Przecinek prawdopodobnie należy postawić tak: &apos;świnia, która&apos;.\" replacements=\"świnia, która\" "));
    assertTrue(output.contains("context=\"To jest świnia która się ślini."));
    assertTrue(output.contains("contextoffset=\"8\" offset=\"8\" errorlength=\"12\" category=\"Błędy interpunkcyjne\""));
  }

  public void testPolishLineNumbers() throws Exception {
    final File input = writeToTempFile(
        "Test.\n" +
            "Test.\n" +
            "Test.\n" +
            "Test.\n" +
            "Test.\n" +
            "Test.\n" +
            "\n" +
        "Test który wykaże błąd.");

    final String[] args = {"-l", "pl", "-c", "utf-8", input.getAbsolutePath()};

    Main.main(args);
    final String stdout = new String(this.out.toByteArray(),"UTF-8");
    final String stderr = new String(this.err.toByteArray());
    assertTrue(stderr.indexOf("Expected text language: Polish") == 0);
    assertTrue(stdout.contains("Line 8, column 1, Rule ID: BRAK_PRZECINKA_KTORY"));
  }

  public void testEnglishTagger() throws Exception {
    final String[] args = {"-l", "en", "--taggeronly", getTestFilePath()};
    Main.main(args);
    final String stdout = new String(this.out.toByteArray());
    final String stderr = new String(this.err.toByteArray());
    assertTrue(stderr.indexOf("Expected text language: English") == 0);
    assertTrue("Got: " + stdout, stdout.contains("<S> This[this/DT,B-NP-singular|E-NP-singular] is[be/VBZ,B-VP] an[a/DT,B-NP-singular] test[test/NN,E-NP-singular].[./.,</S>,O]"));
  }

  public void testBitextMode() throws Exception {
    final File input = writeToTempFile(
        "This is not actual.\tTo nie jest aktualne.\n" +
            "Test\tTest\n" +
        "ab\tVery strange data indeed, much longer than input");

    final String[] args = {"-l", "pl", "-c", "UTF-8", "--bitext", "-m", "en", input.getAbsolutePath()};
    Main.main(args);
    final String stdout = new String(this.out.toByteArray());
    final String stderr = new String(this.err.toByteArray());
    assertTrue(stderr.indexOf("Expected text language: Polish") == 0);
    assertTrue(stdout.contains("Message: Hint: \"aktualny\" (Polish) means \"current\", \"(the) latest\", \"up-to-date\" (English). Did you mean 'rzeczywisty'?"));
    assertTrue(stdout.contains("Line 1, column 32, Rule ID: ACTUAL"));
    assertTrue(stdout.contains("Line 3, column 3, Rule ID: TRANSLATION_LENGTH"));
  }

  public void testBitextModeWithDisabledRule() throws Exception {
    final File input = writeToTempFile(
        "this is not actual.\tTo nie jest aktualne.\n" +
            "test\tTest\n" +
        "ab\tVery strange data indeed, much longer than input");

    final String[] args = {"-l", "pl", "--bitext", "-m", "en", "-d", "UPPERCASE_SENTENCE_START,TRANSLATION_LENGTH", input.getAbsolutePath()};
    Main.main(args);
    final String stdout = new String(this.out.toByteArray());
    final String stderr = new String(this.err.toByteArray());
    assertTrue(stderr.indexOf("Expected text language: Polish") == 0);
    assertTrue(stdout.contains("Message: Hint: \"aktualny\" (Polish) means \"current\", \"(the) latest\", \"up-to-date\" (English). Did you mean 'rzeczywisty'?"));
    assertTrue(stdout.contains("Line 1, column 32, Rule ID: ACTUAL"));
    assertFalse(stdout.contains("Rule ID: TRANSLATION_LENGTH"));
  }

  public void testBitextModeWithEnabledRule() throws Exception {
    final File input = writeToTempFile(
        "this is not actual.\tTo nie jest aktualne.\n" +
            "test\tTest\n" +
        "ab\tVery strange data indeed, much longer than input");

    final String[] args = {"-l", "pl", "--bitext", "-m", "en", "-e", "TRANSLATION_LENGTH", input.getAbsolutePath()};
    Main.main(args);
    final String stdout = new String(this.out.toByteArray());
    final String stderr = new String(this.err.toByteArray());
    assertTrue(stderr.indexOf("Expected text language: Polish") == 0);
    assertFalse(stdout.contains("Message: Hint: \"aktualny\" (Polish) means \"current\", \"(the) latest\", \"up-to-date\" (English). Did you mean 'rzeczywisty'?"));
    assertFalse(stdout.contains("Line 1, column 32, Rule ID: ACTUAL"));
    assertTrue(stdout.contains("Rule ID: TRANSLATION_LENGTH"));
  }

  public void testBitextModeApply() throws Exception {
    final File input = writeToTempFile("There is a dog.\tNie ma psa.");
    final String[] args = {"-l", "pl", "--bitext", "-m", "en", "--apply", input.getAbsolutePath()};
    Main.main(args);
    final String output = new String(this.out.toByteArray());
    assertTrue(output.startsWith("Istnieje psa."));
  }

  public void testBitextWithExternalRule() throws Exception {
    final File input = writeToTempFile("This is a red herring.\tTo jest czerwony śledź.");
    final String[] args = {"-l", "pl", "-c", "UTF-8", "--bitext", "-m", "en", "--bitextrules",
        bitextFile.getAbsolutePath(), input.getAbsolutePath()};
    Main.main(args);
    final String output = new String(this.out.toByteArray());
    assertTrue("red_herring rule should be in the output" + output, output.contains("Rule ID: red_herring"));
  }


  public void testListUnknown() throws Exception {
    final String[] args = {"-l", "pl", "-u", getTestFilePath()};
    Main.main(args);
    final String stdout = new String(this.out.toByteArray());
    final String stderr = new String(this.err.toByteArray());
    assertTrue(stderr.indexOf("Expected text language: Polish") == 0);
    assertTrue(stdout.contains("Unknown words: [., This, an, is, language, of, tool]"));
  }

  public void testNoListUnknown() throws Exception {
    final String[] args = {"-l", "pl", getTestFilePath()};
    Main.main(args);
    final String stdout = new String(this.out.toByteArray());
    final String stderr = new String(this.err.toByteArray());
    assertTrue(stderr.indexOf("Expected text language: Polish") == 0);
    assertTrue(!stdout.contains("Unknown words: [This, an, is]"));
  }

  public void testLangWithCountryVariant() throws Exception {
    final File input = writeToTempFile("This is modelling.");
    final String[] args = {"-l", "en-US", input.getAbsolutePath()};
    Main.main(args);
    final String stdout = new String(this.out.toByteArray());
    final String stderr = new String(this.err.toByteArray());
    assertTrue(stderr.indexOf("Expected text language: English (US)") == 0);
    assertTrue(stdout.contains("MORFOLOGIK_RULE_EN_US"));
  }

  public void testValencianCatalan() throws Exception {
    File input = writeToTempFile("Que sigui així.");
    String[] args = {"-l", "ca-ES-valencia", input.getAbsolutePath()};
    Main.main(args);
    String stdout = new String(this.out.toByteArray());
    String stderr = new String(this.err.toByteArray());
    assertTrue(stderr.indexOf("Expected text language: Catalan (Valencian)") == 0);
    assertTrue(stdout.contains("EXIGEIX_VERBS_VALENCIANS"));
  }

  public void testCatalan() throws Exception {
    File input = writeToTempFile("Que siga així.");
    String[] args = {"-l", "ca-ES", input.getAbsolutePath()};
    Main.main(args);
    String stdout = new String(this.out.toByteArray());
    String stderr = new String(this.err.toByteArray());
    assertTrue(stderr.indexOf("Expected text language: Catalan") == 0);
    assertTrue(stdout.contains("EXIGEIX_VERBS_CENTRAL"));
  }

  public void testCatalan2() throws Exception {
    File input = writeToTempFile("Que siga així.");
    String[] args = {"-l", "ca", input.getAbsolutePath()};
    Main.main(args);
    String stdout = new String(this.out.toByteArray());
    String stderr = new String(this.err.toByteArray());
    assertTrue(stderr.indexOf("Expected text language: Catalan") == 0);
    assertTrue(stdout.contains("EXIGEIX_VERBS_CENTRAL"));
  }

  public void testNoXmlFilteringByDefault() throws Exception {
    final File input = writeToTempFile("This < is is > filtered.");
    final String[] args = {input.getAbsolutePath()};
    Main.main(args);
    final String output = new String(this.out.toByteArray());
    assertTrue(output.contains("ENGLISH_WORD_REPEAT_RULE"));
  }

  public void testXmlFiltering() throws Exception {
    final File input = writeToTempFile("This < is is > filtered.");
    final String[] args = {"--xmlfilter", input.getAbsolutePath()};
    Main.main(args);
    final String output = new String(this.out.toByteArray());
    assertFalse(output.contains("ENGLISH_WORD_REPEAT_RULE"));
  }

  private File writeToTempFile(String content) throws IOException {
    final File tempFile = createTempFile();
    try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(tempFile), "UTF-8"))) {
      writer.println(content);
    }
    return tempFile;
  }

  private File createTempFile() throws IOException {
    final File input = File.createTempFile(MainTest.class.getName(), ".txt");
    input.deleteOnExit();
    return input;
  }

  private File writeToTempXMLFile(String content) throws IOException {
    final File tempFile = createTempXMLFile();
    try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(tempFile), "UTF-8"))) {
      writer.println(content);
    }
    return tempFile;
  }

  private File createTempXMLFile() throws IOException {
    final File input = File.createTempFile("rules-xx-", ".xml");
    input.deleteOnExit();
    return input;
  }

  private String getTestFilePath() {
    return enTestFile.getAbsolutePath();
  }

  private String getRuleFilePath() {
    return xxRuleFile.getAbsolutePath();
  }

  private String getExternalFalseFriends() {
    return xxFalseFriendFile.getAbsolutePath();
  }

}
