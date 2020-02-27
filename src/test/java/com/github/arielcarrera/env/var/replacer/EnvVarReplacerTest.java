package com.github.arielcarrera.env.var.replacer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;

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
		Path template = Paths.get("test-resources/test1-template.xml");
		Path file = Paths.get("test-resources/test1.xml");
		Files.copy(template, file , StandardCopyOption.REPLACE_EXISTING);
		
		environmentVariables.set("VAR_3_REQUIRED", "Test!");
		Assert.assertEquals("Test!", System.getenv("VAR_3_REQUIRED"));
		
		EnvVarReplacer.main(new String[] {"test-resources/test1.xml"});
		Assert.assertTrue(compareFiles(file, Paths.get("test-resources/test1-result.xml")));
	}
	
	@Test
	public void testComplete() throws IOException {
		Path template = Paths.get("test-resources/test1-template.xml");
		Path file = Paths.get("test-resources/test1.xml");
		Files.copy(template, file , StandardCopyOption.REPLACE_EXISTING);
		
		environmentVariables.set("VAR_1_CON_DEFAULT", "true");
		environmentVariables.set("VAR_2_CON_DEFAULT", "Custom content!");
		environmentVariables.set("VAR_3_REQUIRED", "Test!");
		environmentVariables.set("VAR_4_OPTIONAL", "It is optional!");
		Assert.assertEquals("true", System.getenv("VAR_1_CON_DEFAULT"));
		Assert.assertEquals("Custom content!", System.getenv("VAR_2_CON_DEFAULT"));
		Assert.assertEquals("Test!", System.getenv("VAR_3_REQUIRED"));
		Assert.assertEquals("It is optional!", System.getenv("VAR_4_OPTIONAL"));
		
		EnvVarReplacer.main(new String[] {"test-resources/test1.xml"});
		Assert.assertTrue(compareFiles(file, Paths.get("test-resources/test1-result-complete.xml")));
	}

	//file: test2
	@Test
	public void testVarRequired() throws IOException {
		exit.expectSystemExitWithStatus(EnvVarReplacer.ERROR_CODE_VAR_REQUIRED);
		EnvVarReplacer.main(new String[] {"test-resources/test2.xml"});
	}
	
	//file: test5-backup
	@Test
	public void testWithBackup() throws IOException {
		Path template = Paths.get("test-resources/test5-backup-template.xml");
		Path file = Paths.get("test-resources/test5-backup.xml");
		Path backupFile = Paths.get("test-resources/test5-backup.xml.bak");
		Files.copy(template, file , StandardCopyOption.REPLACE_EXISTING);
		Files.deleteIfExists(backupFile);
		environmentVariables.set("VAR_3_REQUIRED", "Test!");
		Assert.assertEquals("Test!", System.getenv("VAR_3_REQUIRED"));
		
		EnvVarReplacer.main(new String[] {"test-resources/test5-backup.xml", "-b"});
		Assert.assertTrue(compareFiles(file, Paths.get("test-resources/test5-backup-result.xml")));
		Assert.assertTrue(compareFiles(template, backupFile));

	}
	// file: test3
	@Test
	public void testWithFileAlreadyExistsBackup() {
		exit.expectSystemExitWithStatus(EnvVarReplacer.ERROR_CODE_BACKUP_ERROR_FILE_EXIST);
		EnvVarReplacer.main(new String[] {"test-resources/test3-backup-already-exists.xml","-b"});
	}
	
	// file: test4-override-backup
	@Test
	public void testWithForceBackup() throws IOException {
		byte[] content = Files.readAllBytes(Paths.get("test-resources/test4-override-backup.xml"));
		EnvVarReplacer.main(new String[] {"test-resources/test4-override-backup.xml","-fb"});
		Path backupPath = Paths.get("test-resources/test4-override-backup.xml.bak");
		Assert.assertTrue(Files.exists(backupPath));
		byte[] backupContent = Files.readAllBytes(backupPath);
		Assert.assertTrue(Arrays.equals(content, backupContent));
	}
	
	@Test
	public void testInDebugMode() throws IOException {
		Path template = Paths.get("test-resources/test6-debug-template.xml");
		Path file = Paths.get("test-resources/test6-debug.xml");
		Files.copy(template, file , StandardCopyOption.REPLACE_EXISTING);
		
		environmentVariables.set("VAR_3_REQUIRED", "Test!");
		Assert.assertEquals("Test!", System.getenv("VAR_3_REQUIRED"));
		
		EnvVarReplacer.main(new String[] {"test-resources/test6-debug.xml","-d"});
		Assert.assertTrue(systemOutRule.getLog().matches("(Ok\\smatch!(\\r\\n)?){4}"));
	}
	
	// file: test7-multipleinline
	@Test
	public void testMutipleInline() throws IOException {
		Path template = Paths.get("test-resources/test7-multipleinline-template.xml");
		Path file = Paths.get("test-resources/test7-multipleinline.xml");
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
		
		EnvVarReplacer.main(new String[] {"test-resources/test7-multipleinline.xml"});
		Assert.assertTrue(compareFiles(file, Paths.get("test-resources/test7-multipleinline-result.xml")));
	}
	
	private boolean compareFiles(Path origin, Path target) throws IOException {
		byte[] originContent = Files.readAllBytes(origin);
		byte[] targetContent = Files.readAllBytes(target);
		return Arrays.equals(originContent, targetContent);
	}
}
