package com.gui.services;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.SwingWorker;
import com.gui.dialogs.ConvertedFilesDialog;
import com.gui.TranslationCheckerApp;
import com.gui.dialogs.FileWarningDialog;
import static com.gui.services.LocaleEncodingService.getLocaleWithEncoding;

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
		this.LANGUAGES = settings.getProperty("languages").split(",");
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
			Map<Path, String> unreadableFiles = new HashMap<>();

			@Override
			protected Void doInBackground() throws Exception {
				progressBar.setValue(0);
				progressBar.setVisible(true);

				Map<String, List<LocaleEncodingService.LanguageProperties>> propertiesMap = new HashMap<>();
				List<String[]> convertedFiles = new ArrayList<>();

				boolean searchUnsetOnly = Boolean.parseBoolean(settings.getProperty("search.unset.only", "false"));
				boolean convertFiles = Boolean.parseBoolean(settings.getProperty("convert.files", "false"));

				Path files = Paths.get(BASE_PATH);

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
						Charset inputEncoding = LocaleEncodingService.getLocaleWithEncoding(lang).getEncoding();

						FileEncodingConverter converter = new FileEncodingConverter();
						path = converter.convertFile(lang, path, convertFiles, inputEncoding, convertedFiles);
						unreadableFiles = converter.getUnreadableFiles();

						try (BufferedReader reader = Files.newBufferedReader(path, inputEncoding)) {
							properties.load(reader);
						} catch (IOException e) {
							handleFileError(path, e, unreadableFiles, inputEncoding);
						}

						propertiesMap.computeIfAbsent(lang, k -> new ArrayList<>()).add(new LocaleEncodingService.LanguageProperties(properties, path));

						completedSteps++;
						setProgress((int) (((double) completedSteps / totalSteps) * 100));
					}

					if (!convertedFiles.isEmpty()) {
						ConvertedFilesDialog convertedFilesDialog = new ConvertedFilesDialog();
						convertedFilesDialog.show(convertedFiles);
					}
				}

				// Update the table with the properties
				translationCheckerApp.updateTable(propertiesMap, searchUnsetOnly, LANGUAGES);
				return null;
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
					FileWarningDialog.show(unreadableFiles);
				}
			}
		};
	}

	private void handleFileError(Path path, IOException e, Map<Path, String> unreadableFiles, Charset inputEncoding) {
		String message = e.getMessage();
		if (message != null && message.contains("Input length =")) {
			boolean encodingExists = Arrays.stream(LocaleEncodingService.localeWithEncoding.values())
            .anyMatch(locale -> locale.getEncoding().equals(inputEncoding));

			if (!encodingExists || !inputEncoding.equals(getLocaleWithEncoding(path.getFileName().toString().substring(9, 11)).getEncoding())) {
				unreadableFiles.put(path, "Wrong encoding detected: " + inputEncoding);
			} else {
				unreadableFiles.put(path, "Malformed input detected: " + message);
			}
		} else {
			unreadableFiles.put(path, message);
		}
	}

	private String convertPath(String path) {
		return path.replace(BASE_PATH, "BASE_PATH");
	}

	// Helper method to search for properties files
	private List<Path> findAllPropertiesFiles(String basePath, String fileName) throws IOException {
		System.out.println("Searching for all files named: " + fileName + " in " + basePath);

		try (Stream<Path> files = Files.walk(Paths.get(basePath))) {
			return files.filter(path -> path.getFileName().toString().equals(fileName))
					.filter(path -> !path.toString().contains("bin" + File.separator))
					.filter(path -> !path.toString().contains("build" + File.separator))
					.collect(Collectors.toList());
		}
	}
}
