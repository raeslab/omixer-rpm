package io.omixer.rpm.engine;

/**
 * What an input reader feeds, in input order: each sample before its values, and every value of the input, zeros and
 * functions outside the module database included. The sink decides what to keep.
 */
public interface ProfileSink {

	/** A sample of the input, before any of its values */
	void sample(String sample);

	/**
	 * @param sample   a sample given to {@link #sample(String)}
	 * @param taxon    the taxon of the value, or null when the input has none
	 * @param function the function id, e.g. a KO
	 * @param value    the abundance
	 */
	void add(String sample, String taxon, String function, double value);
}
