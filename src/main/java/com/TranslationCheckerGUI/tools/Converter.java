package com.TranslationCheckerGUI.tools;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;

public class Converter {
	// Method to convert file encoding (if conversion is needed)
	public static void convertFileEncoding(String inputFilePath, Charset inputEncoding, String outputEncoding) {
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(inputFilePath), inputEncoding));
				BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(inputFilePath), outputEncoding))) {

			String line;
			while ((line = reader.readLine()) != null) {
				writer.write(line);
				writer.newLine();
			}

			System.out.println("Converted file " + inputFilePath + " from " + inputEncoding + " to " + outputEncoding);
		} catch (IOException e) {
			System.err.println("Error converting file: " + inputFilePath);
			e.printStackTrace();
		}
	}
}
