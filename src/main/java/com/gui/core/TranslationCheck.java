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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.SwingWorker;

import com.gui.TranslationCheckerApp;
import com.gui.contsants.LanguagesConstant;
import com.gui.manager.LanguageManager;
import com.gui.manager.SettingsManager;
import com.gui.model.LanguageProperties;
import com.gui.services.FileEncodingConverter;
import com.gui.services.LocaleEncodingService;
import com.gui.ui.ConvertedFilesDialog;
import com.gui.ui.FileWarningDialog;

public class TranslationCheck {

	Logger logger = Logger.getLogger(getClass().getName());
	private final JProgressBar progressBar;
	private final String BASE_PATH;
	Properties settings;
	private final TranslationCheckerApp translationCheckerApp;

	SettingsManager settingsDAO = new SettingsManager();

	public TranslationCheck(JProgressBar progressBar, TranslationCheckerApp app) {
		this.settings = settingsDAO.getSettings();
		this.progressBar = progressBar;
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

				// Get all user-selected languages from LanguageManager
				LanguageManager languageManager = LanguageManager.getInstance();
				Map<String, Locale> userLanguages = languageManager.getAvailableLanguages();
				
				// Convert to LanguagesConstant array for backward compatibility
				List<LanguagesConstant> selectedLanguages = new ArrayList<>();
				for (LanguagesConstant lang : LanguagesConstant.values()) {
					String langCode = lang.getLanguageCode();
					if (userLanguages.containsKey(langCode)) {
						selectedLanguages.add(lang);
					}
				}
				
				LanguagesConstant[] languages = selectedLanguages.toArray(new LanguagesConstant[0]);
				int totalLanguages = languages.length;
				
				// Log the languages being processed
				logger.info("Processing languages: " + Arrays.toString(languages));
				
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

	// Helper method to search for properties files with progress updates using parallel processing
	private List<Path> findAllPropertiesFiles(String basePath, LanguagesConstant lang) throws IOException {
		// Update status label to show which language is being searched
		translationCheckerApp.setStatusLabel("Searching for " + lang.name() + " files...");
		SwingUtilities.invokeLater(() -> translationCheckerApp.setStatusLabel("Searching for " + lang.name() + " files..."));

		// Use lang.getLocale().getLanguage() instead of getLocale().toString()
		String prefix = "messages_" + lang.getLocale().getLanguage();
		List<Path> resultFiles = Collections.synchronizedList(new ArrayList<>());
		
		// First count how many total directories we'll be processing to provide accurate progress
		long startTime = System.currentTimeMillis();
		List<Path> topDirs = new ArrayList<>();
		
		// Update the UI to show we're scanning the directory structure
		SwingUtilities.invokeLater(() -> {
			translationCheckerApp.setStatusLabel("Scanning directory structure for " + lang.name() + "...");
			translationCheckerApp.updateProgressUI();
		});
		
		// Get all directories to search, but do it more efficiently
		try (Stream<Path> dirs = Files.walk(Paths.get(basePath), 1)) {
			topDirs = dirs.filter(Files::isDirectory)
					.filter(path -> !path.equals(Paths.get(basePath)))
					.filter(path -> !path.toString().contains("bin"))
					.filter(path -> !path.toString().contains("build"))
					.filter(path -> !path.toString().contains(".idea"))
					.collect(Collectors.toList());
		}
		
		// Show how many directories we'll be searching
		final int totalDirs = topDirs.size();
		SwingUtilities.invokeLater(() -> {
			translationCheckerApp.setStatusLabel("Found " + totalDirs + " directories to search for " + lang.name() + "...");
			translationCheckerApp.updateProgressUI();
		});
		
		// Create an atomic counter for progress tracking
		final AtomicInteger processedDirs = new AtomicInteger(0);
		final AtomicInteger totalFilesFound = new AtomicInteger(0);
		
		// Use a thread pool with a fixed number of threads for parallel processing
		int numThreads = Math.min(Runtime.getRuntime().availableProcessors(), totalDirs);
		if (numThreads < 1) numThreads = 1; // Ensure at least one thread
		
		ExecutorService executor = Executors.newFixedThreadPool(numThreads);
		List<Future<List<Path>>> futures = new ArrayList<>();
		
		// Submit directory search tasks to the thread pool
		for (Path dir : topDirs) {
			futures.add(executor.submit(() -> {
				List<Path> dirResults = new ArrayList<>();
				
				// Find property files in this directory
				try (Stream<Path> files = Files.walk(dir)) {
					List<Path> foundFiles = files
							.filter(path -> {
								String fileName = path.getFileName().toString();
								return fileName.startsWith(prefix) && fileName.endsWith(".properties");
							})
							.filter(path -> !path.toString().contains("bin" + File.separator))
							.filter(path -> !path.toString().contains("build" + File.separator))
							.filter(path -> !path.toString().contains(".idea" + File.separator))
							.collect(Collectors.toList());
					
					dirResults.addAll(foundFiles);
					
					// Update the shared results list
					resultFiles.addAll(foundFiles);
					
					// Update progress counters
					int currentDir = processedDirs.incrementAndGet();
					int filesFound = totalFilesFound.addAndGet(foundFiles.size());
					
					// Update UI with progress
					final String progressText = String.format("Searching %s: %s (%d/%d - %d%%) - %d files found", 
							lang.name(), 
							dir.getFileName().toString(),
							currentDir,
							totalDirs,
							(currentDir * 100 / totalDirs),
							filesFound);
					
					SwingUtilities.invokeLater(() -> {
						translationCheckerApp.setStatusLabel(progressText);
						translationCheckerApp.updateProgressUI();
					});
				}
				
				return dirResults;
			}));
		}
		
		// Also check the base directory itself for properties files in parallel
		Future<List<Path>> baseDirFuture = executor.submit(() -> {
			SwingUtilities.invokeLater(() -> {
				translationCheckerApp.setStatusLabel("Checking base directory for " + lang.name() + " files...");
				translationCheckerApp.updateProgressUI();
			});
			
			List<Path> baseResults = new ArrayList<>();
			try (Stream<Path> files = Files.list(Paths.get(basePath))) {
				List<Path> foundFiles = files
						.filter(path -> {
							String fileName = path.getFileName().toString();
							return fileName.startsWith(prefix) && fileName.endsWith(".properties");
						})
						.collect(Collectors.toList());
				
				baseResults.addAll(foundFiles);
				resultFiles.addAll(foundFiles);
				totalFilesFound.addAndGet(foundFiles.size());
			}
			
			return baseResults;
		});
		
		// Wait for all tasks to complete
		try {
			// Wait for all directory searches to complete
			for (Future<List<Path>> future : futures) {
				future.get();
			}
			
			// Wait for base directory search
			baseDirFuture.get();
			
		} catch (InterruptedException | ExecutionException e) {
			logger.log(Level.SEVERE, "Error during parallel file search", e);
		} finally {
			// Shutdown the executor service
			executor.shutdown();
		}
		
		// Calculate elapsed time for this language search
		long elapsedTime = System.currentTimeMillis() - startTime;
		final String timeFormatted = String.format("%.1f", elapsedTime / 1000.0);
		
		// Update status with the number of files found and time taken
		final String finalStatus = "Found " + resultFiles.size() + " " + lang.name() + " files in " + timeFormatted + " seconds";
		SwingUtilities.invokeLater(() -> {
			translationCheckerApp.setStatusLabel(finalStatus);
			translationCheckerApp.updateProgressUI();
		});
		
		return resultFiles;
	}
}
