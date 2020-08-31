package com.github.arielcarrera.env.var.replacer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.contrib.java.lang.system.ExpectedSystemExit;
import org.junit.contrib.java.lang.system.SystemOutRule;

public class EnvVarReplacerTest {

	@Rule
	public final EnvironmentVariables environmentVariables = new EnvironmentVariables();
	
	@Rule
    public final ExpectedSystemExit exit = ExpectedSystemExit.none();
	
	@Rule
	public final SystemOutRule systemOutRule = new SystemOutRule().enableLog();
	
	
	//file: test1
	@Test
	public void testMinimal() throws IOException {
		Path template = Paths.get("test-resources", "test1-template.xml");
		Path file = Paths.get("test-resources", "test1.xml");
		Files.copy(template, file , StandardCopyOption.REPLACE_EXISTING);
		
		environmentVariables.set("VAR_3_REQUIRED", "Test!");
		Assert.assertEquals("Test!", System.getenv("VAR_3_REQUIRED"));
		
		EnvVarReplacer.main(new String[] {file.toString()});
		Assert.assertTrue(compareFiles(file, Paths.get("test-resources", "test1-result.xml")));
	}
	
	@Test
	public void testComplete() throws IOException {
		Path template = Paths.get("test-resources", "test1-template.xml");
		Path file = Paths.get("test-resources", "test1.xml");
		Files.copy(template, file , StandardCopyOption.REPLACE_EXISTING);
		
		environmentVariables.set("VAR_1_CON_DEFAULT", "true");
		environmentVariables.set("VAR_2_CON_DEFAULT", "Custom content!");
		environmentVariables.set("VAR_3_REQUIRED", "Test!");
		environmentVariables.set("VAR_4_OPTIONAL", "It is optional!");
		Assert.assertEquals("true", System.getenv("VAR_1_CON_DEFAULT"));
		Assert.assertEquals("Custom content!", System.getenv("VAR_2_CON_DEFAULT"));
		Assert.assertEquals("Test!", System.getenv("VAR_3_REQUIRED"));
		Assert.assertEquals("It is optional!", System.getenv("VAR_4_OPTIONAL"));
		
		EnvVarReplacer.main(new String[] {file.toString()});
		Assert.assertTrue(compareFiles(file, Paths.get("test-resources", "test1-result-complete.xml")));
	}

	//file: test2
	@Test
	public void testVarRequired() throws IOException {
		exit.expectSystemExitWithStatus(EnvVarReplacer.ERROR_CODE_VAR_REQUIRED);
		EnvVarReplacer.main(new String[] {Paths.get("test-resources", "test2.xml").toString()});
	}
	
	//file: test5-backup
	@Test
	public void testWithBackup() throws IOException {
		Path template = Paths.get("test-resources", "test5-backup-template.xml");
		Path file = Paths.get("test-resources", "test5-backup.xml");
		Path backupFile = Paths.get("test-resources", "test5-backup.xml.bak");
		Files.copy(template, file , StandardCopyOption.REPLACE_EXISTING);
		Files.deleteIfExists(backupFile);
		environmentVariables.set("VAR_3_REQUIRED", "Test!");
		Assert.assertEquals("Test!", System.getenv("VAR_3_REQUIRED"));
		
		EnvVarReplacer.main(new String[] {file.toString(), "-b"});
		Assert.assertTrue(compareFiles(file, Paths.get("test-resources", "test5-backup-result.xml")));
		Assert.assertTrue(compareFiles(template, backupFile));

	}
	// file: test3
	@Test
	public void testWithFileAlreadyExistsBackup() {
		exit.expectSystemExitWithStatus(EnvVarReplacer.ERROR_CODE_BACKUP_ERROR_FILE_EXIST);
		EnvVarReplacer.main(new String[] {Paths.get("test-resources", "test3-backup-already-exists.xml").toString(),"-b"});
	}
	
	// file: test4-override-backup
	@Test
	public void testWithForceBackup() throws IOException {
		Path path = Paths.get("test-resources", "test4-override-backup.xml");
		byte[] content = Files.readAllBytes(path);
		EnvVarReplacer.main(new String[] {path.toString(),"-fb"});
		Path backupPath = Paths.get("test-resources", "test4-override-backup.xml.bak");
		Assert.assertTrue(Files.exists(backupPath));
		byte[] backupContent = Files.readAllBytes(backupPath);
		Assert.assertTrue(Arrays.equals(content, backupContent));
	}
	
	@Test
	public void testInDebugMode() throws IOException {
		Path template = Paths.get("test-resources", "test6-debug-template.xml");
		Path file = Paths.get("test-resources", "test6-debug.xml");
		Files.copy(template, file , StandardCopyOption.REPLACE_EXISTING);
		
		environmentVariables.set("VAR_3_REQUIRED", "Test!");
		Assert.assertEquals("Test!", System.getenv("VAR_3_REQUIRED"));
		
		EnvVarReplacer.main(new String[] {file.toString(),"-d"});
		Assert.assertTrue(systemOutRule.getLog().contains("Moving tmp file from:test-resources"));
	}
	
	@Test
	public void testInTraceMode() throws IOException {
		Path template = Paths.get("test-resources", "test6-debug-template.xml");
		Path file = Paths.get("test-resources", "test6-debug.xml");
		Files.copy(template, file , StandardCopyOption.REPLACE_EXISTING);
		
		environmentVariables.set("VAR_3_REQUIRED", "Test!");
		Assert.assertEquals("Test!", System.getenv("VAR_3_REQUIRED"));
		
		EnvVarReplacer.main(new String[] {file.toString(),"-t"});
		Assert.assertTrue(systemOutRule.getLog().contains("Moving tmp file from:test-resources"));
		Assert.assertTrue(systemOutRule.getLog().contains("input : <?xml version=\"1.0\" encoding=\"UTF-8\"?>"));
		Assert.assertTrue(systemOutRule.getLog().contains("output: <?xml version=\"1.0\" encoding=\"UTF-8\"?>"));
	}
	
	// file: test7-multipleinline
	@Test
	public void testMutipleInline() throws IOException {
		Path template = Paths.get("test-resources", "test7-multipleinline-template.xml");
		Path file = Paths.get("test-resources", "test7-multipleinline.xml");
		Files.copy(template, file , StandardCopyOption.REPLACE_EXISTING);
		
		environmentVariables.set("VAR_A", "A");
		environmentVariables.set("VAR_B", "B");
		environmentVariables.set("VAR_C", "C");
		environmentVariables.set("VAR_D", "D");
		environmentVariables.set("VAR_E", "E");
		environmentVariables.set("VAR_F", "F");
		Assert.assertEquals("A", System.getenv("VAR_A"));
		Assert.assertEquals("B", System.getenv("VAR_B"));
		Assert.assertEquals("C", System.getenv("VAR_C"));
		Assert.assertEquals("D", System.getenv("VAR_D"));
		Assert.assertEquals("E", System.getenv("VAR_E"));
		Assert.assertEquals("F", System.getenv("VAR_F"));
		
		EnvVarReplacer.main(new String[] {file.toString()});
		Assert.assertTrue(compareFiles(file, Paths.get("test-resources", "test7-multipleinline-result.xml")));
	}
	

	//file: test8
	@Test
	public void testIssue7() throws IOException {
		Path template = Paths.get("test-resources", "test8-issue7-template.xml");
		Path file = Paths.get("test-resources", "test8-issue7.xml");
		Files.copy(template, file , StandardCopyOption.REPLACE_EXISTING);
		
		environmentVariables.set("V_HOST", "HOST");
		environmentVariables.set("V_NAME", "NAME");
		environmentVariables.set("V_USER", "USER");
		environmentVariables.set("V_PASS", "PASS");

		EnvVarReplacer.main(new String[] {file.toString()});
		Assert.assertTrue(compareFiles(file, Paths.get("test-resources", "test8-issue7-result.xml")));
	}
	
	@Test
	public void testFromProperties() throws IOException {
		Path template = Paths.get("test-resources", "test1-template.xml");
		Path file = Paths.get("test-resources", "test1.xml");
		Files.copy(template, file , StandardCopyOption.REPLACE_EXISTING);
		
		EnvVarReplacer.main(new String[] {file.toString(), "-p", Paths.get("test-resources", "test.properties").toString()});
		Assert.assertTrue(compareFiles(file, Paths.get("test-resources", "test1-result-complete.xml")));
	}
	
	@Test
	public void testTargetPath() throws IOException {
		Path template = Paths.get("test-resources", "test1-template.xml");
		Path file = Paths.get("test-resources", "test1.xml");
		Path target = Paths.get("test-resources", "test1-target.xml");
		Files.copy(template, file , StandardCopyOption.REPLACE_EXISTING);
		Files.deleteIfExists(target);
		
		environmentVariables.set("VAR_3_REQUIRED", "Test!");
		Assert.assertEquals("Test!", System.getenv("VAR_3_REQUIRED"));
		
		EnvVarReplacer.main(new String[] {file.toString() +":" + target.toString()});
		Assert.assertTrue(compareFiles(target, Paths.get("test-resources", "test1-result.xml")));
	}
	
	@Test
	public void testWhitConfigFile() throws IOException {
		Path template = Paths.get("test-resources", "test1-template.xml");
		Path file = Paths.get("test-resources", "test1.xml");
		Path target = Paths.get("test-resources", "test1-target.xml");
		Files.copy(template, file , StandardCopyOption.REPLACE_EXISTING);
		Files.deleteIfExists(target);
		
		Path template2 = Paths.get("test-resources", "test7-multipleinline-template.xml");
		Path file2 = Paths.get("test-resources", "test7-multipleinline.xml");
		Files.copy(template2, file2 , StandardCopyOption.REPLACE_EXISTING);
		
		environmentVariables.set("VAR_3_REQUIRED", "Test!");
		environmentVariables.set("VAR_A", "A");
		environmentVariables.set("VAR_B", "B");
		environmentVariables.set("VAR_C", "C");
		environmentVariables.set("VAR_D", "D");
		environmentVariables.set("VAR_E", "E");
		environmentVariables.set("VAR_F", "F");
		environmentVariables.set("V_HOST", "HOST");
		environmentVariables.set("V_NAME", "NAME");
		environmentVariables.set("V_USER", "USER");
		environmentVariables.set("V_PASS", "PASS");
		
		Path configFile = Paths.get("test-resources", "replacer.cfg");
		EnvVarReplacer.main(new String[] {"-s", configFile.toString()});
		Assert.assertTrue(compareFiles(target, Paths.get("test-resources", "test1-result.xml")));
		Assert.assertTrue(compareFiles(file2, Paths.get("test-resources", "test7-multipleinline-result.xml")));
	}
	
	@Test
	public void testWhitConfigFileAndParameters() throws IOException {
		Path template = Paths.get("test-resources", "test1-template.xml");
		Path file = Paths.get("test-resources", "test1.xml");
		Path target = Paths.get("test-resources", "test1-target.xml");
		Files.copy(template, file, StandardCopyOption.REPLACE_EXISTING);
		Files.deleteIfExists(target);
		
		Path template2 = Paths.get("test-resources", "test7-multipleinline-template.xml");
		Path file2 = Paths.get("test-resources", "test7-multipleinline.xml");
		Files.copy(template2, file2, StandardCopyOption.REPLACE_EXISTING);
		
		environmentVariables.set("VAR_3_REQUIRED", "Test!");
		environmentVariables.set("VAR_A", "A");
		environmentVariables.set("VAR_B", "B");
		environmentVariables.set("VAR_C", "C");
		environmentVariables.set("VAR_D", "D");
		environmentVariables.set("VAR_E", "E");
		environmentVariables.set("VAR_F", "F");
		environmentVariables.set("V_HOST", "HOST");
		environmentVariables.set("V_NAME", "NAME");
		environmentVariables.set("V_USER", "USER");
		environmentVariables.set("V_PASS", "PASS");
		
		Path template3 = Paths.get("test-resources", "test8-issue7-template.xml");
		Path file3 = Paths.get("test-resources", "test8-issue7.xml");
		Files.copy(template3, file3, StandardCopyOption.REPLACE_EXISTING);

		Path configFile = Paths.get("test-resources", "replacer.cfg");
		EnvVarReplacer.main(new String[] {"-s", configFile.toString(), file3.toString()});
		Assert.assertTrue(compareFiles(target, Paths.get("test-resources", "test1-result.xml")));
		Assert.assertTrue(compareFiles(file2, Paths.get("test-resources", "test7-multipleinline-result.xml")));
		Assert.assertTrue(compareFiles(file3, Paths.get("test-resources", "test8-issue7-result.xml")));
	}
	
	@Test
	public void testRemovePrefix() throws IOException {
		Path template = Paths.get("test-resources", "test1-template.xml");
		Path file = Paths.get("test-resources", "test1.xml");
		Files.copy(template, file , StandardCopyOption.REPLACE_EXISTING);
		environmentVariables.set("3_REQUIRED", "Test!");
		Assert.assertEquals("Test!", System.getenv("3_REQUIRED"));
		
		EnvVarReplacer.main(new String[] {file.toString(), "-rp", "VAR_"});
		Assert.assertTrue(compareFiles(file, Paths.get("test-resources", "test1-result.xml")));
	}
	
	@Test
	public void testInnerExpressionDefault() throws IOException {
		Path template = Paths.get("test-resources", "test9-template.xml");
		Path file = Paths.get("test-resources", "test9.xml");
		Files.copy(template, file , StandardCopyOption.REPLACE_EXISTING);
		
		environmentVariables.set("VAR_3_REQUIRED", "Test!");
		Assert.assertEquals("Test!", System.getenv("VAR_3_REQUIRED"));
		environmentVariables.set("VAR_6_REQUIRED", "Test $!12321");
		Assert.assertEquals("Test $!12321", System.getenv("VAR_6_REQUIRED"));
		environmentVariables.set("VAR_7", "O$K");
		Assert.assertEquals("O$K", System.getenv("VAR_7"));
		
		
		EnvVarReplacer.main(new String[] {file.toString(), "-rp", "env."});
		Assert.assertTrue(compareFiles(file, Paths.get("test-resources", "test9-result.xml")));
	}
	
	@Test
	public void testInnerExpression() throws IOException {
		Path template = Paths.get("test-resources", "test9-template.xml");
		Path file = Paths.get("test-resources", "test9.xml");
		Files.copy(template, file , StandardCopyOption.REPLACE_EXISTING);
		
		environmentVariables.set("VAR_3_REQUIRED", "Test!");
		Assert.assertEquals("Test!", System.getenv("VAR_3_REQUIRED"));
		environmentVariables.set("VAR_4_OPTIONAL", "1234567890");
		Assert.assertEquals("1234567890", System.getenv("VAR_4_OPTIONAL"));
		environmentVariables.set("VAR_5_WITH_DEFAULT", "var5");
		Assert.assertEquals("var5", System.getenv("VAR_5_WITH_DEFAULT"));
		environmentVariables.set("VAR_6_REQUIRED", "{Test $!12321 - required value\\}");
		Assert.assertEquals("{Test $!12321 - required value\\}", System.getenv("VAR_6_REQUIRED"));
		
		EnvVarReplacer.main(new String[] {file.toString(), "-rp", "env."});
		Assert.assertTrue(compareFiles(file, Paths.get("test-resources", "test9-withVar5-result.xml")));
	}
	
	private boolean compareFiles(Path origin, Path target) throws IOException {
		List<String> originContent = Files.readAllLines(origin);
		List<String> targetContent = Files.readAllLines(target);
		if (originContent.size() != targetContent.size()) return false;
		for (int i = 0; i < originContent.size(); i++) {
			if (!originContent.get(i).equals(targetContent.get(i))) {
				System.err.println("Expected line: " + targetContent.get(i) + "\nResult line  : " + originContent.get(i));
				return false;
			}
		}
		return true;
	}
	
}
