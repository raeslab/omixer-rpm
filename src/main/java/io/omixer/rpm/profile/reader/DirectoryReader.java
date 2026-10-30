package io.omixer.rpm.profile.reader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import io.omixer.rpm.exceptions.DuplicateSampleNameException;
import io.omixer.rpm.exceptions.InputFormatException;
import io.omixer.rpm.profile.ProfileSink;
import io.omixer.utils.Constants;
import io.omixer.utils.readers.FileLine;

/**
 * Reads a directory with one headerless file per sample, in file name order. A row is KO&lt;TAB&gt;abundance for
 * {@link InputFormat#KO}, taxon&lt;TAB&gt;KO&lt;TAB&gt;abundance otherwise (HUMAnN files included, BUGS.md B14).
 * Sub-directories and hidden files (e.g. .DS_Store) are skipped; any other file must be a sample.
 */
public final class DirectoryReader implements ProfileReader {

	private static final String KO_ROW = "KO<TAB>abundance";
	private static final String TAXON_KO_ROW = "taxon<TAB>KO<TAB>abundance";

	private final InputFormat format;

	public DirectoryReader(InputFormat format) {
		this.format = format;
	}

	/**
	 * @throws InputFormatException         if a line of a sample file does not follow the format
	 * @throws DuplicateSampleNameException if two files give the same sample name
	 * @throws IOException                  if the directory or a sample file cannot be read
	 */
	@Override
	public void read(Path input, ProfileSink sink) throws IOException {
		File directory = input.toFile();
		File[] files = directory.listFiles();
		if (files == null) {
			// input is a directory, so a null listing means it could not be read
			throw new IOException("Cannot list the files of input directory " + directory);
		}
		Arrays.sort(files, Comparator.comparing(File::getName));
		// sample name => the file it comes from, to report two files giving the same name
		Map<String, File> sampleFiles = new HashMap<>();
		for (File file : files) {
			if (!file.isFile() || file.isHidden()) {
				continue;
			}
			String sample = sampleName(file);
			File other = sampleFiles.putIfAbsent(sample, file);
			if (other != null) {
				throw new DuplicateSampleNameException("Files " + other.getName() + " and " + file.getName() + " in "
						+ directory + " both give sample name '" + sample + "'");
			}
			sink.sample(sample);
			readSample(file, sample, sink);
		}
	}

	/**
	 * The sample name of a file is its name without the last extension, e.g. run.v2.tsv gives run.v2
	 */
	private static String sampleName(File file) {
		String name = file.getName();
		int extension = name.lastIndexOf('.');
		return extension > 0 ? name.substring(0, extension) : name;
	}

	private void readSample(File file, String sample, ProfileSink sink) throws IOException {
		try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
			String text;
			int lineNumber = 0;
			while ((text = reader.readLine()) != null) {
				readRow(new FileLine(file.getName(), ++lineNumber, text), sample, sink);
			}
		}
	}

	private void readRow(FileLine line, String sample, ProfileSink sink) {
		String[] fields = line.text().split(Constants.TAB);
		boolean taxa = format != InputFormat.KO;
		String expected = taxa ? TAXON_KO_ROW : KO_ROW;
		if (fields.length < (taxa ? 3 : 2)) {
			throw new InputFormatException(line, expected, null);
		}
		double value;
		try {
			value = Double.parseDouble(fields[taxa ? 2 : 1]);
		} catch (NumberFormatException e) {
			// translate into an error that says which file and line
			throw new InputFormatException(line, expected, e);
		}
		sink.add(sample, taxa ? fields[0] : null, fields[taxa ? 1 : 0], value);
	}
}
