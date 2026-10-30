package io.omixer.rpm.engine;

/**
 * What a profile is: a sample, or one taxon of a sample.
 *
 * @param sample the sample name
 * @param taxon  the taxon in per-taxon runs, otherwise null
 */
public record ProfileKey(String sample, String taxon) {
}
