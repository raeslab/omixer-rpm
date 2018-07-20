package org.omixer.rpm.parsers;

import org.omixer.rpm.model.BasicFeature;
import org.omixer.utils.readers.MatrixLineProcessor;

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
