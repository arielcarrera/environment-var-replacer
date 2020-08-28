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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FilenameUtils;

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

	private static final Pattern pattern = Pattern
			.compile("\\$\\{((?:(?![:])[\\w])+)(:((?:(?![:])[\\w\\s\\p{P}\\p{Sc}\\p{Sk}\\p{So}\\p{Z}])+)?)?}");

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
	public static final int ERROR_CODE_DELETING_FILE = -6;
	public static final int ERROR_CODE_RENAMING_TMP_FILE = -7;

	private static boolean isBackupEnabled;
	private static boolean isForceBackupEnabled;
	private static boolean isDebugEnabled;
	private static boolean isSourceProperties;
	private static boolean isSourceConfigFile;
	private static Properties properties;
	private static Map<String, String> filepathsMap;
	private static String[] paths;
	private static String[] configPaths;

	private static final String ERROR_MSG = "Invalid arguments.\n\nParameters: [-s] [FILE_PATH] [-p [PROPERTIES_FILE]] [-d] [-fb] [-b]\n"
			+ " Where:\n -s : flag to indicate that a source file in FILE PATH is indicated and it is expected to contains a path by line\n"
			+ " FILE_PATH: comma-separated list of file-paths\n"
			+ " -p: read from properties file.\n"
			+ " PROPERTIES_FILE: Path to the propertoies file. It is required when 'p' flag is enabled\n"
			+ " -d: debug mode\n"
			+ " -b: crete backup file\n"
			+ " -fb: force/override backup file";

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
		if (isBackupEnabled)
			isBackupEnabled = false;
		if (isForceBackupEnabled)
			isForceBackupEnabled = false;
		if (isDebugEnabled)
			isDebugEnabled = false;
		if (isSourceProperties)
			isSourceProperties = false;
		if (isSourceConfigFile)
			isSourceConfigFile = false;
		properties = null;
		paths = null;
		configPaths = null;
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
					Matcher matcher = pattern.matcher(line);
					while (matcher.find()) {
						if (isDebugEnabled)
							System.out.println("Ok match!");
						String expression = matcher.group(0);
						String keyName = matcher.group(1);
						String envVar = (isSourceProperties ? properties.getProperty(keyName) : System.getenv(keyName));
						boolean hasEnvVar = envVar != null;
						String group2 = matcher.group(2);
						boolean isRequired = !(group2 != null && group2.startsWith(":"));
						if (isRequired && !hasEnvVar) {
							throw new RequiredEnvironmentVariableException(
									"Environment Variable " + keyName + " is required - file: " + path);
						}
						Optional<String> defaultValue = Optional.ofNullable(matcher.group(3));

						line = line.replace(expression,
								(hasEnvVar ? envVar : (hasEnvVar ? envVar : defaultValue.orElse(""))));
					}
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
			}
		}
		if (!removeTmpFile) {
			Path origin = Paths.get(path);
			try {
				Files.delete(origin);
			} catch (IOException e) {
				System.err.println("Replacement - Error deleting file: " + path);
				if (isDebugEnabled)
					e.printStackTrace();
				System.exit(ERROR_CODE_DELETING_FILE);
			}
			Path tmp = Paths.get(tmpPath);
			try {
				String targetPath = filepathsMap.get(path);
				if (isDebugEnabled) System.out.println("Moving tmp file from:" + origin.toString() + " to: " + (targetPath != null ? targetPath : origin.toString()));
				Files.move(tmp, (targetPath != null ? Paths.get(targetPath) : origin));
			} catch (IOException e) {
				System.err.println("Replacement - Error moving tmp file: " + tmpPath + " a: " + path);
				if (isDebugEnabled)
					e.printStackTrace();
				System.exit(ERROR_CODE_RENAMING_TMP_FILE);
			}

		}
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
