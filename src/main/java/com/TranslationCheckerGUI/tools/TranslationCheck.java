package com.TranslationCheckerGUI.tools;

import java.awt.Dimension;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingWorker;

import com.TranslationCheckerGUI.Dialogs.ConvertedFilesDialog;
import com.TranslationCheckerGUI.TranslationCheckerGUI;

public class TranslationCheck {

	Logger logger = Logger.getLogger(getClass().getName());
	private final JProgressBar progressBar;
	private final String[] LANGUAGES;
	private final String BASE_PATH;
	Properties settings;

	Settings settingsDAO = new Settings();

	public TranslationCheck(JProgressBar progressBar) {
		this.settings = settingsDAO.getSettings();
		this.progressBar = progressBar;
		this.LANGUAGES = settings.getProperty("languages").split(",");
		this.BASE_PATH = settings.getProperty("base.path");;
	}

	public void startTranslationCheck() {
		progressBar.setValue(0);
		progressBar.setVisible(true);
		SwingWorker<Void, Integer> worker = createSwingWorker();
		worker.addPropertyChangeListener(evt -> {
			if ("progress".equals(evt.getPropertyName())) {
				progressBar.setValue((Integer) evt.getNewValue());
			}
		});
		worker.execute();
	}

	private SwingWorker<Void, Integer> createSwingWorker() {
		return new SwingWorker<>() {
			final Map<Path, String> unreadableFiles = new HashMap<>();

			@Override
			protected Void doInBackground() throws Exception {
				progressBar.setValue(0);
				progressBar.setVisible(true);

				Map<String, List<LanguageProperties>> propertiesMap = new HashMap<>();
				List<String[]> convertedFiles = new ArrayList<>();

				boolean searchUnsetOnly = Boolean.parseBoolean(settings.getProperty("search.unset.only", "false"));
				boolean convertFiles = Boolean.parseBoolean(settings.getProperty("convert.files", "false"));

				String pathSetting = settings.getProperty("base.path", "PATH");
				Path files = Paths.get(pathSetting);

				if (!Files.exists(files) || !Files.isDirectory(files)) {
					JOptionPane.showMessageDialog(null,
							"Please set the base path in the settings, where you would like to search for localization.");
					return null;
				}

				for (String lang : LANGUAGES) {
					List<Path> paths = findAllPropertiesFiles(BASE_PATH, "messages_" + lang + ".properties");
					int completedSteps = 0;
					int totalSteps = paths.size();

					for (Path path : paths) {
						Properties properties = new Properties();
						Charset inputEncoding = getLocaleWithEncoding(lang).getEncoding();

						path = convertFile(lang, path, convertFiles, inputEncoding, convertedFiles);

						try (BufferedReader reader = Files.newBufferedReader(path, inputEncoding)) {
							properties.load(reader);
						} catch (IOException e) {
							String message = e.getMessage();
							path = Paths.get(convertPath(path.toString()));

							if (message != null && message.contains("Input length =")) {
								boolean encodingExists = false;
								for (localeWithEncoding locale : localeWithEncoding.values()) {
									if (locale.getEncoding().equals(inputEncoding)) {
										encodingExists = true;
										break;
									}
								}

								if (!encodingExists || !inputEncoding.equals(getLocaleWithEncoding(path.getFileName().toString().substring(9, 11)).getEncoding())) {
									logger.log(Level.WARNING, "MalformedInputException for file: " + path + ", wrong encoding detected: " + inputEncoding, e);
									unreadableFiles.put(path, "Wrong encoding detected: " + inputEncoding + " (should be " + getLocaleWithEncoding(path.getFileName().toString().substring(9, 11)).getEncoding() + ")");
								} else {
									unreadableFiles.put(path, "Malformed input detected: " + e.getMessage() + " (" + inputEncoding + ")");
								}
							} else {
								logger.log(Level.WARNING, "Error reading file: " + path, e);
								unreadableFiles.put(path, message);
							}
						}

						propertiesMap.computeIfAbsent(lang, k -> new ArrayList<>()).add(new LanguageProperties(properties, path));

						completedSteps++;
						setProgress((int) (((double) completedSteps / totalSteps) * 100));
					}

					if (!convertedFiles.isEmpty()) {
						ConvertedFilesDialog convertedFilesDialog = new ConvertedFilesDialog();
						convertedFilesDialog.showConvertedFilesDialog(convertedFiles);
					}
				}

				// Update the table with the properties
				TranslationCheckerGUI translationCheckerGUI = new TranslationCheckerGUI();
				translationCheckerGUI.updateTable(propertiesMap, searchUnsetOnly, LANGUAGES);
				return null;
			}

			private Path convertFile(String lang, Path path, boolean convertFiles, Charset inputEncoding, List<String[]> convertedFiles) {
				if (convertFiles) {
					Charset outputEncoding = getLocaleWithEncoding(lang).getEncoding();
					if (outputEncoding != null) {
						if (!inputEncoding.equals(outputEncoding)) {
							String inputFilePath = path.toString();
							try {
								Converter.convertFileEncoding(inputFilePath, inputEncoding, outputEncoding.name());
							} catch (Exception e) {
								logger.log(Level.WARNING, "Error converting file: " + inputFilePath, e);
								unreadableFiles.put(path, "Error converting file: " + e.getMessage());
								return null;
							}
							convertedFiles.add(new String[]{inputFilePath, inputEncoding.name(), outputEncoding.name()});
							path = Paths.get(inputFilePath);
						}
					} else {
						logger.log(Level.WARNING, "No encoding found for language: " + lang);
					}
				}
				return path;
			}

			@Override
			protected void done() {
				progressBar.setVisible(false);
				setProgress(0);
				try {
					get();
				} catch (InterruptedException | ExecutionException e) {
					logger.log(Level.SEVERE, "Error in SwingWorker", e);
				}

				if (!unreadableFiles.isEmpty()) {
					showFileWarningDialog(unreadableFiles);
				}
			}
		};
	}

	private void showFileWarningDialog(Map<Path, String> unreadableFiles) {
		String[] columnNames = {"File", "Error"};
		Object[][] data = new Object[unreadableFiles.size()][2];

		int i = 0;
		for (Map.Entry<Path, String> entry : unreadableFiles.entrySet()) {
			data[i][0] = entry.getKey().toString();
			data[i][1] = entry.getValue();
			i++;
		}

		JTable table = new JTable(data, columnNames);
		JScrollPane scrollPane = new JScrollPane(table);
		scrollPane.setPreferredSize(new Dimension(800, 200));

		JOptionPane.showMessageDialog(null, scrollPane, "Unreadable Files", JOptionPane.WARNING_MESSAGE);
	}

	private localeWithEncoding getLocaleWithEncoding(String lang) {
		for (localeWithEncoding locale : localeWithEncoding.values()) {
			if (locale.getLocale().getLanguage().equals(lang)) {
				return locale;
			}
		}
		return localeWithEncoding.GERMAN;
	}

	// Inner class for handling encoding and locale
	public enum localeWithEncoding {
		GERMAN(Locale.GERMAN, "ISO-8859-1"),
		ENGLISH(Locale.ENGLISH, "ISO-8859-1"),
		FRENCH(Locale.FRENCH, "ISO-8859-1"),
		RUSSIAN(new Locale("ru"), "UTF-8");

		private final Locale locale;
		private final String encoding;

		localeWithEncoding(Locale locale, String encoding) {
			this.locale = locale;
			this.encoding = encoding;
		}

		public Locale getLocale() {
			return locale;
		}

		public Charset getEncoding() {
			return Charset.forName(encoding);
		}
	}

	// Inner class to hold language properties and associated file path
	public static class LanguageProperties {
		private final Properties properties;
		private final Path file;

		public LanguageProperties(Properties properties, Path file) {
			this.properties = properties;
			this.file = file;
		}

		public Properties getProperties() {
			return properties;
		}

		public Path getPath() {
			return file;
		}
	}

	private Charset getFileEncoding(Path path, Charset defaultEncoding) {
		// Implement the method logic here
		return defaultEncoding;
	}

	private String convertPath(String path) {
		// Implement the method logic here
		return path;
	}

	// Helper method to search for properties files
	private List<Path> findAllPropertiesFiles(String basePath, String fileName) throws IOException {
		System.out.println("Searching for all files named: " + fileName + " in " + basePath);

		try (Stream<Path> files = Files.walk(Paths.get(basePath))) {
			return files.filter(path -> path.getFileName().toString().equals(fileName))
					.filter(path -> !path.toString().contains("\\bin\\"))
					.filter(path -> !path.toString().contains("\\build\\"))
					.collect(Collectors.toList());
		}
	}
}