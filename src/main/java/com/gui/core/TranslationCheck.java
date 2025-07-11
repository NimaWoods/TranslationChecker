package com.gui.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.SwingWorker;

import com.gui.TranslationCheckerApp;
import com.gui.contsants.LanguagesConstant;
import com.gui.manager.SettingsManager;
import com.gui.model.LanguageProperties;
import com.gui.services.FileEncodingConverter;
import com.gui.services.LocaleEncodingService;
import com.gui.ui.ConvertedFilesDialog;
import com.gui.ui.FileWarningDialog;

public class TranslationCheck {

	Logger logger = Logger.getLogger(getClass().getName());
	private final JProgressBar progressBar;
	private final String[] LANGUAGES;
	private final String BASE_PATH;
	Properties settings;
	private final TranslationCheckerApp translationCheckerApp;

	SettingsManager settingsDAO = new SettingsManager();

	public TranslationCheck(JProgressBar progressBar, TranslationCheckerApp app) {
		this.settings = settingsDAO.getSettings();
		this.progressBar = progressBar;
		this.LANGUAGES = Arrays.stream(LanguagesConstant.values()).map(LanguagesConstant::name).toArray(String[]::new);
		this.BASE_PATH = settings.getProperty("base.path");
		this.translationCheckerApp = app;
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
			final Map<String, String> unreadableFiles = new HashMap<>();

			@Override
			protected Void doInBackground() throws Exception {
				progressBar.setValue(0);
				progressBar.setVisible(true);
				
				// Show initial status message
				translationCheckerApp.setStatusLabel("Initializing file collection...");

				Map<LanguagesConstant, List<LanguageProperties>> propertiesMap = new HashMap<>();
				List<String[]> convertedFiles = new ArrayList<>();

				boolean searchUnsetOnly = Boolean.parseBoolean(settings.getProperty("search.unset.only", "false"));
				boolean convertFiles = Boolean.parseBoolean(settings.getProperty("convert.files", "false"));

				Path files = Paths.get(BASE_PATH);

				if (!Files.exists(files) || !Files.isDirectory(files)) {
					JOptionPane.showMessageDialog(null,
							"Please set the base path in the settings, where you would like to search for localization.");
					return null;
				}

				// Get total count of languages to process
				LanguagesConstant[] languages = LanguagesConstant.values();
				int totalLanguages = languages.length;
				
				// Show initial status that we're about to search for files
				translationCheckerApp.setStatusLabel("Initializing: Preparing to search for all language files...");
				setProgress(5); // Small initial progress
				
				// First phase: Find all property files for all languages
				Map<LanguagesConstant, List<Path>> allLanguageFiles = new HashMap<>();
				int languageCount = 0;
				
				// Estimate total files to process
				for (LanguagesConstant lang : languages) {
					translationCheckerApp.setStatusLabel("Finding " + lang.name() + " files... (" + languageCount + "/" + totalLanguages + " languages)");
					
					List<Path> foundFiles = findAllPropertiesFiles(BASE_PATH, lang);
					allLanguageFiles.put(lang, foundFiles);
					
					languageCount++;
					// Update progress for file discovery phase (up to 30%)
					int fileDiscoveryProgress = (int)((double)languageCount / totalLanguages * 30);
					setProgress(5 + fileDiscoveryProgress);
				}
				
				// Show total files found across all languages
				int totalFilesFound = allLanguageFiles.values().stream().mapToInt(List::size).sum();
				translationCheckerApp.setStatusLabel("Processing " + totalFilesFound + " files across " + totalLanguages + " languages");
				setProgress(35); // Progress after file discovery phase
				
				// Second phase: Process all the files we found
				languageCount = 0;
				int processedFiles = 0;
				
				for (LanguagesConstant lang : languages) {
					translationCheckerApp.setStatusLabel("Processing " + lang.name() + " files... (Language " + (languageCount+1) + "/" + totalLanguages + ")");
					
					List<Path> paths = allLanguageFiles.get(lang);
					int totalFiles = paths.size();
					translationCheckerApp.setStatusLabel("Processing " + lang.name() + ": " + totalFiles + " files");
					
					Charset inputEncoding = lang.getEncoding();
					int completedSteps = 0;

					int totalStepsForLanguage = paths.size();
					for (Path path : paths) {
						// Update file-specific status
						String fileName = path.getFileName().toString();
						translationCheckerApp.setStatusLabel("Processing " + lang.name() + ": " + fileName + " (" + completedSteps + "/" + totalStepsForLanguage + ")");
						
						Properties properties = new Properties();

						FileEncodingConverter converter = new FileEncodingConverter();
						path = converter.convertFile(lang.name(), path, convertFiles, inputEncoding, convertedFiles);
						unreadableFiles.putAll(converter.getUnreadableFiles());

						try (BufferedReader reader = Files.newBufferedReader(path, inputEncoding)) {
							properties.load(reader);
							for (String key : properties.stringPropertyNames()) {
								String value = properties.getProperty(key);
								System.out.println("Loaded key: " + key + " with value: " + value);
							}
						} catch (Exception e) {
							handleFileError(path, e, unreadableFiles, inputEncoding, lang.getLocale().getLanguage());
						}

						propertiesMap.computeIfAbsent(lang, k -> new ArrayList<>())
								.add(new LanguageProperties(properties, path));

						// Update progress
						completedSteps++;
						processedFiles++;
						
						// Calculate overall progress (35% for discovery + 60% for processing)
						int processingProgress = (int)(((double)processedFiles / totalFilesFound) * 60);
						setProgress(35 + processingProgress);
					}

					if (!convertedFiles.isEmpty()) {
						ConvertedFilesDialog convertedFilesDialog = new ConvertedFilesDialog();
						convertedFilesDialog.show(convertedFiles);
					}
				}

				// Update the table with the properties
				translationCheckerApp.updateTable(propertiesMap, searchUnsetOnly);
				return null;
			}

			private void handleFileError(Path path, Exception e, Map<String, String> unreadableFiles, Charset inputEncoding, String language) {
				String message = e.getMessage();

				if (message == null || message.isEmpty()) {
					message = "Unknown error: " + e.getClass().getSimpleName();
				}

				if (message.contains("Input length =")) {
					boolean encodingExists = Arrays.stream(LanguagesConstant.values())
							.anyMatch(lang -> lang.getEncoding().equals(inputEncoding));
					if (!encodingExists) {
						unreadableFiles.put(path.toString(), "Encoding not supported: " + inputEncoding + " (" + message + ")");
					} else if (!inputEncoding.equals(LocaleEncodingService.getLocaleWithEncoding(language).getEncoding())) {
						unreadableFiles.put(path.toString(), "Wrong encoding detected: " + inputEncoding);
					} else {
						unreadableFiles.put(path.toString(), "Malformed input detected: " + message + " (Encoding: " + inputEncoding + ")");
					}
				} else {
					unreadableFiles.put(path.toString(), message);
				}
			}

			@Override
			protected void done() {
				progressBar.setVisible(false);
				setProgress(0);
				try {
					get();

					if (!unreadableFiles.isEmpty()) {
						FileWarningDialog.show(unreadableFiles, "The following files could not be read:");
					}

				} catch (InterruptedException | ExecutionException e) {
					logger.log(Level.SEVERE, "Error in SwingWorker", e);
				}
			}
		};
	}

	// Helper method to search for properties files with progress updates
	private List<Path> findAllPropertiesFiles(String basePath, LanguagesConstant lang) throws IOException {
		// Update status label to show which language is being searched
		translationCheckerApp.setStatusLabel("Searching for " + lang.name() + " files...");

		// Use lang.getLocale().getLanguage() instead of getLocale().toString()
		String prefix = "messages_" + lang.getLocale().getLanguage();
		List<Path> resultFiles = new ArrayList<>();
		
		// First do a quick check of how many total directories we'll be processing
		// to give the user a sense of progress
		try (Stream<Path> dirs = Files.walk(Paths.get(basePath), 1)) {
			List<Path> topDirs = dirs.filter(Files::isDirectory)
					.filter(path -> !path.equals(Paths.get(basePath)))
					.filter(path -> !path.toString().contains("bin"))
					.filter(path -> !path.toString().contains("build"))
					.filter(path -> !path.toString().contains(".idea"))
					.collect(Collectors.toList());
			
			// Update progress as we search each directory
			for (Path dir : topDirs) {
				// Update status with current directory being searched
				translationCheckerApp.setStatusLabel("Searching " + lang.name() + ": " + dir.getFileName().toString());
				
				// Find property files in this directory
				try (Stream<Path> files = Files.walk(dir)) {
					List<Path> foundFiles = files.filter(path -> path.getFileName().toString().startsWith(prefix))
							.filter(path -> path.getFileName().toString().endsWith(".properties"))
							.filter(path -> !path.toString().contains("bin" + File.separator))
							.filter(path -> !path.toString().contains("build" + File.separator))
							.filter(path -> !path.toString().contains(".idea" + File.separator))
							.collect(Collectors.toList());
					
					resultFiles.addAll(foundFiles);
				}
				
				// We don't update progress here anymore - it's handled in the main method
			}
		}
		
		// Also check the base directory itself for properties files
		try (Stream<Path> files = Files.list(Paths.get(basePath))) {
			List<Path> foundFiles = files.filter(path -> path.getFileName().toString().startsWith(prefix))
					.filter(path -> path.getFileName().toString().endsWith(".properties"))
					.collect(Collectors.toList());
			
			resultFiles.addAll(foundFiles);
		}
		
		// Update status with the number of files found
		translationCheckerApp.setStatusLabel("Found " + resultFiles.size() + " " + lang.name() + " files");
		
		return resultFiles;
	}
}
