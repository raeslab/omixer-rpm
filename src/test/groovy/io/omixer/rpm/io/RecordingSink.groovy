package io.omixer.rpm.io

import io.omixer.rpm.engine.ProfileSink

/** Records what a reader feeds: "sample S" and [sample, taxon, function, value] entries, in order */
class RecordingSink implements ProfileSink {

	final List events = []

	@Override
	void sample(String sample) {
		events << "sample $sample".toString()
	}

	@Override
	void add(String sample, String taxon, String function, double value) {
		events << [sample, taxon, function, value]
	}

	List values() {
		events.findAll { it instanceof List }
	}
}
