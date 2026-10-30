package io.omixer.rpm.output

import spock.lang.Specification

class OutputFormatSpec extends Specification {

	def "-e 1 writes per-sample files, -e 2 the matrices"() {

		expect:
		OutputFormat.PER_SAMPLE.writer() instanceof PerSampleWriter
		OutputFormat.MATRIX.writer() instanceof MatrixWriter
	}
}
