package com.github.arielcarrera.env.var.replacer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Optional;
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
			.compile("\\$\\{([\\w]+)(:([\\w\\s\\p{P}\\p{Sc}\\p{Sk}\\p{So}\\p{Z}]+)?)?}");

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

	/**
	 * Processes a comma-separated list of files and replace expressions like ${}
	 * with environment variables
	 * 
	 * @param args
	 */
	public static void main(String[] args) {

		initDefaults();

		if (args.length < 1) {
			System.err.println(
					"Invalid arguments.\n\nParameters: [FILE_PATH]\n Where FILE_PATH: comma-separated list of file-paths");
			System.exit(ERROR_CODE_INVALID_ARGUMENTS);
		}
		if (args.length > 1) {
			for (int i = 1; i < args.length; i++) {
				switch (args[i]) {
				case "-d":
					isDebugEnabled = true;
					break;
				case "-fb":
					isForceBackupEnabled = true;
				case "-b":
					isBackupEnabled = true;
					break;
				}

			}
		}
		String[] paths = args[0].split(",");
		Arrays.stream(paths).filter(EnvVarReplacer::validate).map(FilenameUtils::normalizeNoEndSeparator)
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
	}

	static boolean validate(String path) {
		if (path == null || path.isEmpty()) {
			System.err.println("Invalid path: " + path);
			System.exit(ERROR_CODE_INVALID_PATH);
		}
		return true;
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
						String envVar = System.getenv(keyName);
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
				Files.move(tmp, origin);
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
