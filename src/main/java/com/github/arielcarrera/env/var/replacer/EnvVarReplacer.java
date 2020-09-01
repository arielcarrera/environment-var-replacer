package com.github.arielcarrera.env.var.replacer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.io.FilenameUtils;

import lombok.AllArgsConstructor;
import lombok.Data;

public class EnvVarReplacer {
	public static class RequiredEnvironmentVariableException extends Exception {
		private static final long serialVersionUID = -1250700098669029910L;
		String message;

		public RequiredEnvironmentVariableException(String message) {
			super();
			this.message = message;
		}

		@Override
		public String getMessage() {
			return message;
		}

		public void setMessage(String message) {
			this.message = message;
		}
	}

	public static final int ERROR_CODE_INVALID_ARGUMENTS = 1;
	public static final int ERROR_CODE_INVALID_PATH = 2;
	public static final int ERROR_CODE_FILE_NOT_FOUND = 3;
	public static final int ERROR_CODE_BACKUP_ERROR_FILE_EXIST = 4;
	public static final int ERROR_CODE_VAR_REQUIRED = 5;

	public static final int ERROR_CODE_ERROR_READING_FILE = -1;
	public static final int ERROR_CODE_ERROR_WRITING_FILE = -2;
	public static final int ERROR_CODE_BACKUP_ERROR = -3;
	public static final int ERROR_CODE_BACKUP_READ_ERROR = -4;
	public static final int ERROR_CODE_BACKUP_WRITE_ERROR = -5;
	//public static final int ERROR_CODE_DELETING_FILE = -6;
	public static final int ERROR_CODE_RENAMING_TMP_FILE = -7;

	private static boolean isBackupEnabled;
	private static boolean isForceBackupEnabled;
	private static boolean isDebugEnabled;
	private static boolean isTraceEnabled;
	private static boolean isSourceProperties;
	private static boolean isSourceConfigFile;
	private static boolean isRemovePrefixEnabled;
	private static boolean isFilterByPrefixEnabled;
	private static Properties properties;
	private static Map<String, String> filepathsMap;
	private static String[] paths;
	private static String[] configPaths;
	private static String prefix;
	private static String[] filterPrefixes;

	private static final String ERROR_MSG = "Invalid arguments.\n\nParameters: [-s] [FILE_PATH] [-p [PROPERTIES_FILE]] [-d] [-fb] [-b]\n"
			+ " Where:\n -s FILE_PATH: flag to indicate that a source file in FILE PATH is indicated and it is expected to contains a path by line\n"
			+ " FILE_PATH: comma-separated list of file-paths\n"
			+ " -p PROPERTIES_FILE: read from properties file.\n"
			+ " PROPERTIES_FILE: Path to the propertoies file. It is required when 'p' flag is enabled\n"
			+ " -d: debug mode\n"
			+ " -t: trace mode\n"
			+ " -b: crete backup file\n"
			+ " -fb: force/override backup file\n"
			+ " -rp PREFIX: indicate a prefix to be removed from properties names\n"
			+ " PREFIX: prefix to be removed from keys\n"
			+ " -fp PREFIX: indicate a list of prefixes to filter by\n"
			+ " PREFIX: prefixes";

	/**
	 * Processes a comma-separated list of files and replace expressions like ${}
	 * with environment variables
	 * 
	 * @param args
	 */
	public static void main(String[] args) {

		initDefaults();

		if (args.length < 1) {
			System.err.println(ERROR_MSG);
			System.exit(ERROR_CODE_INVALID_ARGUMENTS);
		}
		for (int i = 0; i < args.length; i++) {
			switch (args[i]) {
			case "-t":
				isTraceEnabled = true;
			case "-d":
				isDebugEnabled = true;
				break;
			case "-fb":
				isForceBackupEnabled = true;
			case "-b":
				isBackupEnabled = true;
				break;
			case "-p":
				isSourceProperties = true;
				if (i >= args.length) {
					System.err.println(ERROR_MSG);
					System.exit(ERROR_CODE_INVALID_ARGUMENTS);
				}
				i++;
				String propertiesFilePath = args[i];
				try (InputStream input = new FileInputStream(propertiesFilePath)) {
		            properties = new Properties();
		            properties.load(input);
		        } catch (IOException ex) {
		            ex.printStackTrace();
		        }
				break;
			case "-s":
				isSourceConfigFile = true;
				if (i >= args.length) {
					System.err.println(ERROR_MSG);
					System.exit(ERROR_CODE_INVALID_ARGUMENTS);
				}
				i++;
				configPaths = args[i].split(",");
				break;
			case "-rp":
				isRemovePrefixEnabled = true;
				if (i >= args.length) {
					System.err.println(ERROR_MSG);
					System.exit(ERROR_CODE_INVALID_ARGUMENTS);
				}
				i++;
				prefix = args[i];
				break;
			case "-fp":
				isFilterByPrefixEnabled = true;
				if (i >= args.length) {
					System.err.println(ERROR_MSG);
					System.exit(ERROR_CODE_INVALID_ARGUMENTS);
				}
				i++;
				filterPrefixes = args[i].split(",");
				break;
			default:
				if (paths != null) {
					System.err.println(ERROR_MSG);
					System.exit(ERROR_CODE_INVALID_ARGUMENTS);
				}
				paths = args[i].split(",");
			}
		}
		
		List<String> allPaths = paths != null ? new ArrayList<String>(Arrays.asList(paths)) : new ArrayList<String>();
		if (configPaths != null) {
			Arrays.stream(configPaths).filter(EnvVarReplacer::validate).map(FilenameUtils::normalizeNoEndSeparator)
					.filter(EnvVarReplacer::checkFile).forEach(path -> {
						readConfigFile(path, allPaths);
					});
			paths = allPaths.toArray(new String[allPaths.size()]);
		}
		
		Arrays.stream(paths).map(EnvVarReplacer::normalizeAndResolveTargetFiles)
				.filter(EnvVarReplacer::checkFile).filter(EnvVarReplacer::checkBackupFileExists)
				.forEach(EnvVarReplacer::replace);
	}

	private static void initDefaults() {
		isBackupEnabled = false;
		isForceBackupEnabled = false;
		isDebugEnabled = false;
		isTraceEnabled = false;
		isSourceProperties = false;
		isSourceConfigFile = false;
		isRemovePrefixEnabled = false;
		isFilterByPrefixEnabled = false;
		filterPrefixes = null;
		properties = null;
		paths = null;
		configPaths = null;
		prefix = null;
		filepathsMap = new HashMap<String, String>();
	}

	static boolean validate(String path) {
		if (path == null || path.isEmpty()) {
			System.err.println("Invalid path: " + path);
			System.exit(ERROR_CODE_INVALID_PATH);
		}
		return true;
	}
	
	static String normalizeAndResolveTargetFiles(String path) {
		validate(path);
		int indexOf = path.indexOf(":");
		if (indexOf >= 0) {
			//path with input:target format
			String origin = path.substring(0, indexOf);
			String target = path.substring(indexOf+1, path.length());
			if (origin.isEmpty() || target.isEmpty()) {
				System.err.println("Invalid path: " + path);
				System.exit(ERROR_CODE_INVALID_PATH);
			}
			String normalizedOrigin = FilenameUtils.normalizeNoEndSeparator(origin);
			filepathsMap.put(normalizedOrigin, FilenameUtils.normalizeNoEndSeparator(target));
			return normalizedOrigin;
		}
		return FilenameUtils.normalizeNoEndSeparator(path);
	}

	static boolean checkFile(String path) {
		if (!Files.isReadable(Paths.get(path))) {
			System.err.println("Invalid path: " + path + " (File not found)");
			System.exit(ERROR_CODE_FILE_NOT_FOUND);
		}
		return true;
	}

	static boolean checkBackupFileExists(String path) {
		if (!isBackupEnabled)
			return true;
		Path p = Paths.get(path + ".bak");
		if (Files.exists(p)) {
			if (!isForceBackupEnabled) {
				System.err.println("Backup file exists and force backup mode is disabled. File: " + path + ".bak");
				System.exit(ERROR_CODE_BACKUP_ERROR_FILE_EXIST);
			} else if (!Files.isWritable(p)) {
				System.err.println("Backup file is not writable: " + path + ".bak");
				System.exit(ERROR_CODE_BACKUP_WRITE_ERROR);
			}
		}

		return true;
	}
	
	static void readConfigFile(String path, List<String> all) {
		try (BufferedReader br = new BufferedReader(new FileReader(path))) {
			String line = br.readLine();
			while (line != null) {
				all.add(line);
				line = br.readLine();
			}
		} catch (FileNotFoundException e) {
			System.err.println("Replacement - Invalid path: " + path + " (File not found)");
			System.exit(ERROR_CODE_FILE_NOT_FOUND);
		} catch (IOException e) {
			System.err.println("Replacement - Error reading from file: " + path);
			if (isDebugEnabled)
				e.printStackTrace();
			System.exit(ERROR_CODE_ERROR_READING_FILE);
		}
	}

	static void replace(String path) {
		if (isBackupEnabled) {
			doBackup(path);
		}
		boolean removeTmpFile = false;
		String tmpPath = path + ".tmp";
		try (BufferedWriter bw = new BufferedWriter(new FileWriter(tmpPath))) {
			try (BufferedReader br = new BufferedReader(new FileReader(path))) {
				String line = br.readLine();
				while (line != null) {
					if (isTraceEnabled) System.out.println("input : " + line);
					line = processLine(line);
					if (isTraceEnabled) System.out.println("output: " + line);
					bw.write(line);
					line = br.readLine();
					if (line != null) {
						bw.write(System.lineSeparator());
					}
				}
			} catch (FileNotFoundException e) {
				System.err.println("Replacement - Invalid path: " + path + " (File not found)");
				System.exit(ERROR_CODE_FILE_NOT_FOUND);
			} catch (IOException e) {
				System.err.println("Replacement - Error reading from file: " + path);
				if (isDebugEnabled)
					e.printStackTrace();
				System.exit(ERROR_CODE_ERROR_READING_FILE);
			}
		} catch (IOException e1) {
			System.err.println("Replacement - Error writing from file: " + tmpPath);
			if (isDebugEnabled)
				e1.printStackTrace();
			System.exit(ERROR_CODE_ERROR_WRITING_FILE);
		} catch (RequiredEnvironmentVariableException e) {
			System.err.println(e.getMessage());
			removeTmpFile = true;
		} finally {
			if (removeTmpFile) {
				try {
					Files.delete(Paths.get(tmpPath));
				} catch (IOException e) {
					System.err.println("Cannot remove tmp file: " + tmpPath);
					if (isDebugEnabled)
						e.printStackTrace();
				}
				System.exit(ERROR_CODE_VAR_REQUIRED);
			} else {
				Path origin = Paths.get(path);
				Path tmp = Paths.get(tmpPath);
				String targetPath = filepathsMap.get(path);
				Path target = (targetPath != null ? Paths.get(targetPath) : origin);
				try {
					if (isDebugEnabled) System.out.println("Moving tmp file from:" + tmp.toString() + " to: " + target.toString());
					Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
				} catch (IOException e) {
					System.err.println("Replacement - Error moving tmp file: " + tmpPath + " to: " + target);
					if (isDebugEnabled)
						e.printStackTrace();
					System.exit(ERROR_CODE_RENAMING_TMP_FILE);
				}
			}
		}

	}

	private static String processLine(String line) throws RequiredEnvironmentVariableException {
		ExpressionResult result = new ExpressionResult(true, line, new HashSet<String>(), new HashMap<String, String>());
		int i = 0;
		while ((result = processExpression(result)).isReplaced()) {
			i++;
			if (i > 99) throw new RuntimeException("Error processing line: " + line);
		}
		String string = result.getStr();
		//replace escaped end chars...
		for (String rplKey : result.getEscapedEndChars()) {
			string = string.replace(rplKey, "}");
		}
		//replace skipped regions...
		Set<String> keySet = result.getSkippedRegions().keySet();
		string = replaceRegion(string, keySet, result.getSkippedRegions());
		
		return string;
	}
	
	private static String replaceRegion(String str, Set<String> keys, Map<String, String> map) {
		Set<String> keysToRemove = new HashSet<String>();
		boolean replaced = false;
		for (String key : keys) {
			if (str.contains(key)) {
				keysToRemove.add(key);
				str = str.replace(key, map.get(key));
				replaced = true;
			}
		}
		//remove processed keys from pendings
		keys.removeAll(keysToRemove);
		return replaced && !keys.isEmpty() ? replaceRegion(str, keys, map) : str;
	}
	
	@Data @AllArgsConstructor
	private static class ExpressionResult{
		boolean replaced = false;
		String str;
		Set<String> escapedEndChars;
		Map<String, String> skippedRegions;
	}
	private static ExpressionResult processExpression(ExpressionResult exp) throws RequiredEnvironmentVariableException {
		char[] charArray = exp.getStr().toCharArray();
		int startTagIndex = -1;
		int keyEndIndex = -1;
		int endTagIndex = -1;
		boolean hasEscapedEndChar = false;
		
		for (int i = 0; i < charArray.length; i++) {
			if (charArray[i] == '$' ) {
				if (i + 1 < charArray.length && charArray[i+1] == '{') {
					// if exists '${' 
					startTagIndex = i;
					keyEndIndex = -1;
					i++; //skip ${
				}
			} else if (charArray[i] == ':') {
				if (keyEndIndex < 0) {
					keyEndIndex = i;
				}
			} else if (charArray[i] == '}') {
				if (startTagIndex > -1) {
					if (keyEndIndex > -1) {
						if (i - 1 >= 0 && charArray[i-1] == '\\') {
							hasEscapedEndChar = true;
						} else {
							endTagIndex = i;
							break;
						}
					} else  {
						keyEndIndex = i;
						endTagIndex = i;
						break;
					}
				}
				
			} else {
				// if startTagIndex exists but no boundary is determined..
				if (startTagIndex > -1 && keyEndIndex < 0) {
					// if char is not allowed... - | . | 0-1 | A-Z | _ | a-z | 
					char c = charArray[i];
					if (!(c == 45 || c == 46 || (c > 47 && c < 58) || (c > 64 && c < 91) || c == 95 || (c > 96 && c < 123))) {
						startTagIndex = -1;
					}
				}
				//else any char allowed
			}
		}

		//if an expression is detected...
		if (startTagIndex > -1 && keyEndIndex > -1 && endTagIndex > -1) {
			String keyName = exp.getStr().substring(startTagIndex+2, keyEndIndex);
			boolean toInclude = (isFilterByPrefixEnabled ? false : true);
			if (isFilterByPrefixEnabled) {
				for (String pre : filterPrefixes) {
					if (keyName.startsWith(pre)) {
						toInclude = true;
						break;
					}
				}
				if (!toInclude) {
					String randomUUID = "__SKIPPED__" + UUID.randomUUID().toString();
					exp.getSkippedRegions().put(randomUUID, exp.getStr().substring(startTagIndex, endTagIndex + 1));
					if (endTagIndex + 1 < exp.getStr().length()) {
						return new ExpressionResult(true, exp.getStr().substring(0, startTagIndex) + randomUUID + exp.getStr().substring(endTagIndex +1, exp.getStr().length()), exp.getEscapedEndChars(), exp.getSkippedRegions());
					}
					return new ExpressionResult(true, exp.getStr().substring(0, startTagIndex) + randomUUID, exp.getEscapedEndChars(), exp.getSkippedRegions());
				}
			}
			
			String value = resolveValue(keyName);
			boolean hasDefaultValue = keyEndIndex != endTagIndex;
			boolean isRequired = !hasDefaultValue;
			if (isRequired && value == null) {
				throw new RequiredEnvironmentVariableException(
						"Environment Variable " + keyName + " is required");
			}
			if (value == null) {
				value = exp.getStr().substring(keyEndIndex + 1, endTagIndex);
			}
			
			//replace escaped end tags....
			if (value.contains("\\}") || hasEscapedEndChar) {
				int indexOf = value.indexOf("\\}");
				while (indexOf > 0 ) {
					//generate random key to replace
					String randomUUID = "__REPLACE__" + UUID.randomUUID().toString();
					exp.getEscapedEndChars().add(randomUUID);
					value = value.substring(0, indexOf) + randomUUID + value.substring(indexOf + "\\}".length());
					indexOf = value.indexOf("\\}");
				}
			}
			
			if (endTagIndex + 1 < exp.getStr().length()) {
				return new ExpressionResult(true, exp.getStr().substring(0, startTagIndex) + value + exp.getStr().substring(endTagIndex +1, exp.getStr().length()), exp.getEscapedEndChars(), exp.getSkippedRegions());
			}
			return new ExpressionResult(true, exp.getStr().substring(0, startTagIndex) + value, exp.getEscapedEndChars(), exp.getSkippedRegions());
		}
		return new ExpressionResult(false, exp.getStr(), exp.getEscapedEndChars(), exp.getSkippedRegions());
	}

	private static String resolveValue(String keyName) {
		String key = (isRemovePrefixEnabled && keyName.startsWith(prefix) ? keyName.substring(prefix.length()) : keyName);
		return isSourceProperties ? properties.getProperty(key) : System.getenv(key);
	}

	private static void doBackup(String path) {
		Path source = Paths.get(path);
		Path target = Paths.get(path + ".bak");
		try {
			if (isForceBackupEnabled) {
				Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
			} else {
				Files.copy(source, target);
			}
		} catch (FileAlreadyExistsException e2) {
			System.err.println("Error during backup of file (File already exist): " + path);
			System.exit(ERROR_CODE_BACKUP_ERROR_FILE_EXIST);
		} catch (IOException e1) {
			System.err.println("Error during backup of file: " + path);
			if (isDebugEnabled)
				e1.printStackTrace();
			System.exit(ERROR_CODE_BACKUP_ERROR);
		}
	}

}
