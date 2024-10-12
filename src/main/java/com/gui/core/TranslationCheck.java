package com.gui.core;

import com.gui.TranslationCheckerApp;
import com.gui.contsants.LanguagesConstant;
import com.gui.manager.ConfigurationManager;
import com.gui.model.LanguageProperties;
import com.gui.services.FileEncodingConverter;
import com.gui.services.LocaleEncodingService;
import com.gui.ui.ConvertedFilesDialog;
import com.gui.ui.FileWarningDialog;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TranslationCheck {

	Logger logger = Logger.getLogger(getClass().getName());
	private final JProgressBar progressBar;
	private final String[] LANGUAGES;
	private final String BASE_PATH;
	Properties settings;
	private final TranslationCheckerApp translationCheckerApp;

	ConfigurationManager settingsDAO = new ConfigurationManager();

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

				// Iterate over all languages names
				for (LanguagesConstant lang : LanguagesConstant.values()) {

					Charset inputEncoding;
					if (lang == null) {
						inputEncoding = StandardCharsets.ISO_8859_1;
					} else {
						inputEncoding = lang.getEncoding();
					}

					List<Path> paths = findAllPropertiesFiles(BASE_PATH, lang);
					int completedSteps = 0;
					int totalSteps = paths.size();

					for (Path path : paths) {
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

						completedSteps++;
						setProgress((int) (((double) completedSteps / totalSteps) * 100));
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

	// Helper method to search for properties files
	private List<Path> findAllPropertiesFiles(String basePath, LanguagesConstant lang) throws IOException {

		// Verwende lang.getLocale().getLanguage() anstelle von getLocale().toString()
		String prefix = "messages_" + lang.getLocale().getLanguage();

		try (Stream<Path> files = Files.walk(Paths.get(basePath))) {
			return files.filter(path -> path.getFileName().toString().startsWith(prefix))
					.filter(path -> path.getFileName().toString().endsWith(".properties"))
					.filter(path -> !path.toString().contains("bin" + File.separator))
					.filter(path -> !path.toString().contains("build" + File.separator))
					.collect(Collectors.toList());
		}
	}
}
