package io.omixer.rpm.parsers;

import java.util.List;

import io.omixer.rpm.model.BasicFeature;
import io.omixer.utils.readers.FileLine;
import io.omixer.utils.readers.MatrixLineProcessor;

public abstract class AbstractLineProcessor implements MatrixLineProcessor<BasicFeature>{

	/** File name used when a line is processed without knowing where it comes from */
	private static final String UNKNOWN_FILE = "<unknown file>";

	protected int headerSampleStartIndex;

	public AbstractLineProcessor() {
	}

	protected AbstractLineProcessor(int headerSampleStartIndex) {
		this.headerSampleStartIndex = headerSampleStartIndex;
	}

	@Override
	public int getHeaderSampleStartIndex() {
		return headerSampleStartIndex;
	}

	/**
	 * Maps a matrix line, knowing its file and line number to report an invalid line
	 *
	 * Abstract, so that every processor implements it (the interface's default would call back
	 * {@link #process(String, String)}, which calls this method)
	 */
	@Override
	public abstract List<BasicFeature> process(FileLine line, String delimiter);

	/**
	 * Maps a line without knowing where it comes from: an invalid line is reported at an unknown position.
	 * Readers should call {@link #process(FileLine, String)}.
	 */
	@Override
	public final List<BasicFeature> process(String line, String delimiter) {
		return process(new FileLine(UNKNOWN_FILE, 0, line), delimiter);
	}
}
