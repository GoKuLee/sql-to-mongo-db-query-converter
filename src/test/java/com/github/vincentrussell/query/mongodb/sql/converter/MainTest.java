package com.github.vincentrussell.query.mongodb.sql.converter;

import org.apache.commons.cli.ParseException;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.ExpectedSystemExit;
import org.junit.contrib.java.lang.system.SystemOutRule;
import org.junit.contrib.java.lang.system.TextFromStandardInputStream;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import java.io.*;

import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringStartsWith.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.contrib.java.lang.system.TextFromStandardInputStream.emptyStandardInputStream;

public class MainTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Rule
    public SystemOutRule systemOutRule = new SystemOutRule().enableLog();

    @Rule
    public final TextFromStandardInputStream systemInMock = emptyStandardInputStream();

    @Rule
    public final ExpectedSystemExit exit = ExpectedSystemExit.none();

    File sourceFile;
    File destinationFile;


    @Before
    public void before() throws IOException {
        sourceFile = temporaryFolder.newFile();
        destinationFile = temporaryFolder.newFile();
    }

    @Test
    public void missingArgumentsThrowsExceptionAndPrintsHelp() throws ClassNotFoundException, ParseException, IOException, com.github.vincentrussell.query.mongodb.sql.converter.ParseException {
        exception.expect(ParseException.class);
        exception.expectMessage("Missing required option: s or i");
        Main.main(new String[0]);
    }

    @Test
    public void providedHostButNoDB() throws ClassNotFoundException, ParseException, IOException, com.github.vincentrussell.query.mongodb.sql.converter.ParseException {
        exception.expect(ParseException.class);
        exception.expectMessage("provided option h, but missing db");
        Main.main(new String[]{"-s", sourceFile.getAbsolutePath(), "-h", "localhost"});
    }

    @Test
    public void providedUsernameButNoPassword() throws ClassNotFoundException, ParseException, IOException, com.github.vincentrussell.query.mongodb.sql.converter.ParseException {
        exception.expect(ParseException.class);
        exception.expectMessage("provided option u, but missing p");
        Main.main(new String[]{"-s", sourceFile.getAbsolutePath(), "-h", "localhost","-db","database","-u","username"});
    }

    @Test
    public void interactiveMode() throws IOException,ParseException, ClassNotFoundException, InterruptedException, com.github.vincentrussell.query.mongodb.sql.converter.ParseException {
        exit.expectSystemExitWithStatus(0);
        systemInMock.provideLines("select column1 from my_table where value IN (\"theValue1\",\"theValue2\",\"theValue3\")");
        try {
            Main.main(new String[]{"-i"});
        } finally {
            Thread.sleep(1000);
            assertThat(systemOutRule.getLog(),startsWith(Main.ENTER_SQL_TEXT));
            String result = systemOutRule.getLog().replaceAll(Main.ENTER_SQL_TEXT,"");
            assertEquals("******Mongo Query:*********\n" +
                    "\n" +
                    "db.my_table.find({\n" +
                    "  \"$in\": [\n" +
                    "    \"theValue1\",\n" +
                    "    \"theValue2\",\n" +
                    "    \"theValue3\"\n" +
                    "  ]\n" +
                    "} , {\n" +
                    "  \"_id\": 0,\n" +
                    "  \"column1\": 1\n" +
                    "})".trim(), result.trim());

        }
    }

    @Test(expected = FileNotFoundException.class)
    public void sourceFileNotFound() throws IOException, ParseException, ClassNotFoundException, com.github.vincentrussell.query.mongodb.sql.converter.ParseException {
        sourceFile.delete();
        Main.main(new String[]{"-s", sourceFile.getAbsolutePath(), "-d", destinationFile.getAbsolutePath()});
    }

    @Test(expected = IOException.class)
    public void destinationExists() throws IOException, ParseException, ClassNotFoundException, com.github.vincentrussell.query.mongodb.sql.converter.ParseException {
        Main.main(new String[]{"-s", sourceFile.getAbsolutePath(), "-d", destinationFile.getAbsolutePath()});
    }

    @Test
    public void successfulRun() throws IOException, ClassNotFoundException, com.github.vincentrussell.query.mongodb.sql.converter.ParseException, ParseException {
        exit.expectSystemExitWithStatus(0);
        destinationFile.delete();
        try (FileOutputStream fileOutputStream = new FileOutputStream(sourceFile)) {
            IOUtils.write("select column1 from my_table where value IN (\"theValue1\",\"theValue2\",\"theValue3\")", fileOutputStream);
            Main.main(new String[]{"-s", sourceFile.getAbsolutePath(), "-d", destinationFile.getAbsolutePath()});
            assertTrue(destinationFile.exists());
            try (FileInputStream fileInputStream = new FileInputStream(destinationFile)) {
                assertEquals("******Mongo Query:*********\n" +
                        "\n" +
                        "db.my_table.find({\n" +
                        "  \"$in\": [\n" +
                        "    \"theValue1\",\n" +
                        "    \"theValue2\",\n" +
                        "    \"theValue3\"\n" +
                        "  ]\n" +
                        "} , {\n" +
                        "  \"_id\": 0,\n" +
                        "  \"column1\": 1\n" +
                        "})", IOUtils.toString(fileInputStream).trim());
            }

        }
    }


    @Test
    public void successfulRunSqlInline() throws IOException, ClassNotFoundException, com.github.vincentrussell.query.mongodb.sql.converter.ParseException, ParseException {
        exit.expectSystemExitWithStatus(0);
        destinationFile.delete();
            Main.main(new String[]{"-d", destinationFile.getAbsolutePath(),
                    "-sql","select column1 from my_table where value IN (\"theValue1\",\"theValue2\",\"theValue3\")"});
            assertTrue(destinationFile.exists());
            try (FileInputStream fileInputStream = new FileInputStream(destinationFile)) {
                assertEquals("******Mongo Query:*********\n" +
                        "\n" +
                        "db.my_table.find({\n" +
                        "  \"$in\": [\n" +
                        "    \"theValue1\",\n" +
                        "    \"theValue2\",\n" +
                        "    \"theValue3\"\n" +
                        "  ]\n" +
                        "} , {\n" +
                        "  \"_id\": 0,\n" +
                        "  \"column1\": 1\n" +
                        "})", IOUtils.toString(fileInputStream).trim());
            }

    }

    @Test
    public void successfulRunSystemOut() throws IOException, ParseException, ClassNotFoundException, com.github.vincentrussell.query.mongodb.sql.converter.ParseException {
        exit.expectSystemExitWithStatus(0);
        destinationFile.delete();
        try (FileOutputStream fileOutputStream = new FileOutputStream(sourceFile)) {
            IOUtils.write("select column1 from my_table where value IN (\"theValue1\",\"theValue2\",\"theValue3\")", fileOutputStream);
            Main.main(new String[]{"-s", sourceFile.getAbsolutePath()});

            assertEquals("******Mongo Query:*********\n" +
                    "\n" +
                    "db.my_table.find({\n" +
                    "  \"$in\": [\n" +
                    "    \"theValue1\",\n" +
                    "    \"theValue2\",\n" +
                    "    \"theValue3\"\n" +
                    "  ]\n" +
                    "} , {\n" +
                    "  \"_id\": 0,\n" +
                    "  \"column1\": 1\n" +
                    "})", systemOutRule.getLog().trim());
        }
    }

}
