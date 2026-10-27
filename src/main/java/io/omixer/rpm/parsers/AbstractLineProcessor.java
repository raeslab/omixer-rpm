package io.omixer.rpm.parsers;

import io.omixer.rpm.model.BasicFeature;
import io.omixer.utils.readers.MatrixLineProcessor;

public abstract class AbstractLineProcessor implements MatrixLineProcessor<BasicFeature>{

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
}
